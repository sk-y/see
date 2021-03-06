package see.evaluation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import see.evaluation.evaluators.SimpleEvaluator;
import see.functions.ContextCurriedFunction;
import see.functions.PureFunction;
import see.functions.VarArgFunction;
import see.parser.config.ConfigBuilder;
import see.parser.numbers.IntegerFactory;
import see.tree.Node;
import see.tree.immutable.ImmutableConstNode;
import see.tree.immutable.ImmutableFunctionNode;
import see.tree.immutable.ImmutableVarNode;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LazyVisitorTest {

    ContextCurriedFunction<Integer, Integer> plus;
    ContextCurriedFunction<Integer, Integer> fail;
    ContextCurriedFunction<Integer, Integer> cond;

    Evaluator evaluator;

    @Before
    public void setUp() throws Exception {
        plus = PureFunction.wrap(new VarArgFunction<Integer, Integer>() {
            @Override
            public Integer apply(@Nonnull List<Integer> input) {
                int result = 0;
                for (Integer value : input) {
                    result += value;
                }
                return result;
            }
        });
        fail = PureFunction.wrap(new VarArgFunction<Integer, Integer>() {
            @Override
            public Integer apply(@Nonnull List<Integer> input) {
                throw new UnsupportedOperationException("Fail");
            }
        });
        cond = PureFunction.wrap(new VarArgFunction<Integer, Integer>() {
            @Override
            public Integer apply(@Nonnull List<Integer> input) {
                if (input.get(0) != 0) {
                    return input.get(1);
                } else {
                    return input.get(2);
                }
            }
        });
        evaluator = SimpleEvaluator.fromConfig(ConfigBuilder.defaultConfig()
                .addFunction("plus", plus)
                .addFunction("fail", fail)
                .addFunction("cond", cond)
                .setNumberFactory(new IntegerFactory())
                .build());
    }

    @Test
    public void testEvaluation() throws Exception {
        List<Node<Integer>> arguments = ImmutableList.<Node<Integer>>of(new ImmutableConstNode<Integer>(1), new ImmutableConstNode<Integer>(2));
        Node<Integer> tree = new ImmutableFunctionNode<Integer, Integer>("plus", arguments);

        int result = evaluator.evaluate(tree, new HashMap<String, Object>());

        assertEquals(3, result);
    }

    @Test
    public void testLazy() throws Exception {
        Node<Integer> failNode = new ImmutableFunctionNode<Integer, Integer>("fail");
        ImmutableList<Node<Integer>> conditionArgs = ImmutableList.of(
                new ImmutableConstNode<Integer>(1),
                new ImmutableConstNode<Integer>(42),
                failNode
        );
        Node<Integer> tree = new ImmutableFunctionNode<Integer, Integer>("cond", conditionArgs);

        int result = evaluator.evaluate(tree, Collections.<String, Object>emptyMap());
        assertEquals(42, result);
    }

    @Test
    public void testVariables() throws Exception {
        List<Node<Integer>> plusArgs = ImmutableList.of(new ImmutableVarNode<Integer>("a"), new ImmutableConstNode<Integer>(5));
        Node<Integer> tree = new ImmutableFunctionNode<Integer, Integer>("plus", plusArgs);

        ImmutableMap<String, Object> context = ImmutableMap.<String, Object>of("a", 4);

        int result = evaluator.evaluate(tree, context);

        assertEquals(9, result);
    }
}
