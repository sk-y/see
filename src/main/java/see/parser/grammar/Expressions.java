package see.parser.grammar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.errors.ParsingException;
import org.parboiled.support.Var;
import see.functions.ContextCurriedFunction;
import see.functions.Function;
import see.parser.config.FunctionResolver;
import see.parser.config.GrammarConfiguration;
import see.parser.numbers.NumberFactory;
import see.tree.FunctionNode;
import see.tree.Node;
import see.tree.VarNode;
import see.tree.immutable.ImmutableConstNode;
import see.tree.immutable.ImmutableFunctionNode;
import see.tree.immutable.ImmutableVarNode;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;
import static see.parser.grammar.PropertyAccess.Simple;

@SuppressWarnings({"InfiniteRecursion"})
class Expressions extends AbstractGrammar {
    final Literals literals;

    final NumberFactory numberFactory;
    final FunctionResolver functions;

    final Character argumentSeparator;

    final Set<String> keywords = ImmutableSet.of("if", "then", "else", "return");

    Expressions(GrammarConfiguration config) {
        numberFactory = config.getNumberFactory();
        functions = config.getFunctions();
        
        argumentSeparator = numberFactory.getDecimalSeparator() == ',' ? ';' : ',';
        literals = Parboiled.createParser(Literals.class, numberFactory.getDecimalSeparator());
    }

    Rule ReturnExpression() {
        ListVar<Node<Object>> statements = new ListVar<Node<Object>>();
        return Sequence(
                ExpressionList(), statements.append(pop()),
                T("return"), RightExpression(), Optional(T(";")), statements.append(pop()),
                push(makeSeqNode(statements.get()))
        );
    }

    /**
     * List of zero or more terms. Pushes one node.
     * @return rule
     */
    Rule ExpressionList() {
        ListVar<Node<Object>> statements = new ListVar<Node<Object>>();
        return Sequence(
                ZeroOrMore(Term(), statements.append(pop())),
                push(makeSeqNode(statements.get()))
        );
    }

    /**
     * A if..then..else or expression ending with semicolon.
     * Pushes it's value to stack
     * @return rule
     */
    Rule Term() {
        return FirstOf(Conditional(), Iteration(), TerminatedExpression());
    }

    /**
     * An expression ending with semicolon.
     * @return constructed rule
     */
    Rule TerminatedExpression() {
        return Sequence(Expression(), T(";"));
    }

    /**
     * For loop, like one in Java 5, but without type declaration.
     * @return constructed rule
     */
    Rule Iteration() {
        return Sequence(
                T("for"), T("("), VarName(), T(":"), Atom(), T(")"),
                Block(),
                swap3() && push(makeFNode("for", of(pop(), pop(), pop())))
                );
    }

    /**
     * Wraps list of expressions in FunctionNode with Sequence function
     * Short-circuits if list has only one element.
     * Returns seq node with empty arguments if expressions are empty.
     * Expects sequence function to map to operator ';'
     * @param statements list of expressions to wrap
     * @return constructed node
     */
    Node<Object> makeSeqNode(List<Node<Object>> statements) {
        return statements.size() == 1 ? statements.get(0) : makeFNode(";", statements);
    }

    Rule Expression() {
        return FirstOf(PropertyAssignment(), VariableAssignment(), RightExpression());
    }

    /**
     * Assignment to variable. Pushes one node.
     * @return constructed rule
     */
    Rule VariableAssignment() {
        return Sequence(VarName(), T("="), Expression(), pushBinOp("="));
    }

    /**
     * Assignment to property. Pushes one node.
     * Treated differently from variable assignment, as it doesn't require context access.
     * @return constructed rule
     */
    Rule PropertyAssignment() {
        ListVar<Node<?>> props = new ListVar<Node<?>>();
        return Sequence(PropertyAccess(props), T("="), Expression(), props.append(makeUNode("props.target", pop())),
                push(makeFNode(".=", props.get())));
    }

    /**
     * Special form. Matches variable, pushes variable name.
     * @return rule
     */
    Rule VarName() {
        return Sequence(Variable(), push(new ImmutableConstNode<Object>(getVarName((VarNode<?>) pop()))));
    }

    String getVarName(VarNode<?> node) {
        return node.getName();
    }

    Rule Conditional() {
        ListVar<Node<Object>> args = new ListVar<Node<Object>>();
        return Sequence(
                T("if"), T("("), RightExpression(), args.append(pop()), T(")"),
                Block(), args.append(pop()),
                Optional(T("else"), Block(), args.append(pop())),
                push(makeFNode("if", args.get()))
        );
    }

    Rule Block() {
        return FirstOf(
                Sequence(T("{"), ExpressionList(), T("}")),
                Term()
        );
    }

    Rule RightExpression() {
        return OrExpression();
    }

    Rule OrExpression() {
        return repeatWithOperator(AndExpression(), "||");
    }

    Rule AndExpression() {
        return repeatWithOperator(EqualExpression(), "&&");
    }

    Rule EqualExpression() {
        return repeatWithOperator(RelationalExpression(), FirstOf("!=", "=="));
    }

    Rule RelationalExpression() {
        return repeatWithOperator(AdditiveExpression(), FirstOf("<=", ">=", "<", ">"));
    }

    Rule AdditiveExpression() {
        return repeatWithOperator(MultiplicativeExpression(), FirstOf("+", "-"));
    }

    Rule MultiplicativeExpression() {
        return repeatWithOperator(UnaryExpression(), FirstOf("*", "/"));
    }

    Rule UnaryExpression() {
        Var<String> op = new Var<String>("");
        return FirstOf(
                Sequence(
                        T(AnyOf("+-!"), op.set(match())),
                        UnaryExpression(),
                        push(makeUNode(op.get(), pop()))),
                PowerExpression()
        );
    }

    Rule PowerExpression() {
        return Sequence(PropertyRead(),
                Optional(T("^"), UnaryExpression(), pushBinOp("^")));
    }

    Rule PropertyRead() {
        ListVar<Node<?>> props = new ListVar<Node<?>>();
        return FirstOf(
                Sequence(PropertyAccess(props), push(makeFNode(".", props.get()))),
                Atom()
        );
    }

    Rule PropertyAccess(ListVar<? super Node<?>> props) {
        return Sequence(Atom(), props.append(makeUNode("props.target", pop())),
                OneOrMore(FirstOf(
                        T(".", Identifier(), props.append(new ImmutableConstNode<Simple>(new Simple(match())))),
                        Sequence(T("["), RightExpression(), props.append(makeUNode("[]", pop())), T("]"))
                ))
        );
    }

    Rule Atom() {
        return FirstOf(
                Constant(),
                SpecialForm(),
                Function(),
                Variable(),
                Sequence(T("("), Expression(), T(")"))
        );
    }

    /**
     * Repeat rule with separator, combining results into binary tree.
     * Matches like rep1sep, but combines results.
     * @param rule rule to match. Expected to push one node.
     * @param separator separator between rules
     * @return rule
     */
    Rule repeatWithOperator(Rule rule, Object separator) {
        Var<String> operator = new Var<String>("");
        return Sequence(rule,
                ZeroOrMore(
                        T(separator, operator.set(match())),
                        rule,
                        pushBinOp(operator.get())
                )
        );
    }

    /**
     * Combines two entries on top of the stack into FunctionNode with specified operator
     * @param operator function name
     * @return true if operation succeded
     */
    boolean pushBinOp(String operator) {
        return swap() && push(makeFNode(operator, ImmutableList.of(pop(), pop())));
    }

    /**
     * Construct unary function node.
     * Short-circuits for unary plus.
     * @param operator unary operator
     * @param expr operator argument
     * @return constructed node
     */
    Node<Object> makeUNode(String operator, Node<Object> expr) {
        if (operator.equals("+")) {
            return expr;
        } else {
            return makeFNode(operator, ImmutableList.of(expr));
        }
    }

    /**
     * Construct function node with resolved function
     * @param name function name
     * @param args argument list
     * @return constructed node
     */
    FunctionNode<Object, Object> makeFNode(String name, List<? extends Node<?>> args) {
        ContextCurriedFunction<Function<List<Object>,Object>> function = functions.get(name);
        if (function == null) {
            throw new ParsingException("Function not found: " + name);
        }
        return new ImmutableFunctionNode<Object, Object>(function, (List<Node<Object>>) args);
    }

    /**
     * Constant. Pushes ImmutableConstNode(value)
     * @return rule
     */
    @SuppressSubnodes
    Rule Constant() {
        return FirstOf(String(), Float(), Int());
    }

    /**
     * Function application. Pushes FunctionNode(f, args).
     * @return rule
     */
    Rule Function() {
        Var<String> function = new Var<String>("");
        ListVar<Node<Object>> args = new ListVar<Node<Object>>();
        return Sequence(
                T(FirstOf(Identifier(), "if"), function.set(matchTrim())),
                ArgumentList(args),
                push(makeFNode(function.get(), args.get()))
        );
    }

    /**
     * A special form.
     * @return rule
     */
    Rule SpecialForm() {
        return IsDefined();
    }

    /**
     * Special form for isDefined function.
     * Matches like function application, but requires one Variable inside.
     * @return rule
     */
    Rule IsDefined() {
        return Sequence(
                T("isDefined"),
                T("("), VarName(), T(")"),
                push(makeFNode("isDefined", ImmutableList.of(pop())))
        );
    }

    Rule ArgumentList(ListVar<Node<Object>> args) {
        return Sequence(T("("), repsep(Sequence(Expression(), args.append(pop())), ArgumentSeparator()), T(")"));
    }

    Rule ArgumentSeparator() {
        return T(argumentSeparator);
    }

    Rule Variable() {
        return T(Identifier(), push(new ImmutableVarNode<Object>(match())));
    }

    /**
     * String literal. Expected to push it's value/
     * @return rule
     */
    Rule String() {
        return T(literals.StringLiteral(), push(new ImmutableConstNode<Object>(stripQuotes(match()))));
    }

    /**
     * Floating point literal. Expected to push it's value
     * @return rule
     */
    Rule Float() {
        return T(literals.FloatLiteral(), push(new ImmutableConstNode<Object>(matchNumber())));
    }

    /**
     * Integer literal. Expected to push it's value.
     * @return constructed rule
     */
    Rule Int() {
        return T(literals.IntLiteral(), push(new ImmutableConstNode<Object>(matchNumber())));
    }

    @WhitespaceSafe
    @SuppressSubnodes
    Rule Identifier() {
        return Sequence(Name(), !keywords.contains(match()));
    }

    @WhitespaceSafe
    Rule Name() {
        return Sequence(literals.Letter(), ZeroOrMore(literals.LetterOrDigit()));
    }

    Number matchNumber() {
        return numberFactory.getNumber(match());
    }

    /**
     * Return input without first and last character.
     * I.e. "str" -> str
     * @param input input string
     * @return truncated input
     */
    String stripQuotes(String input) {
        return input.substring(1, input.length() - 1);
    }

}
