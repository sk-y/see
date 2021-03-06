package see.evaluation;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import see.evaluation.evaluators.SimpleEvaluator;
import see.exceptions.EvaluationException;
import see.exceptions.SeeRuntimeException;
import see.functions.VarArgFunction;
import see.parser.config.ConfigBuilder;
import see.tree.Node;
import see.tree.immutable.ImmutableFunctionNode;

import javax.annotation.Nonnull;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SimpleEvaluatorTest {


    final VarArgFunction<Object, Object> epicFail = new VarArgFunction<Object, Object>() {
        @Override
        public Object apply(@Nonnull List<Object> input) {
            throw new EpicFailException();
        }
    };

    final VarArgFunction<Object, Object> fail = new VarArgFunction<Object, Object>() {
        @Override
        public Object apply(@Nonnull List<Object> input) {
            throw new RuntimeException();
        }
    };

    Evaluator evaluator = SimpleEvaluator.fromConfig(ConfigBuilder.defaultConfig()
            .addFunction("epicFail", epicFail)
            .addFunction("fail", fail)
            .build());

    /**
     * Test that all runtime exceptions are wrapped in EvaluationException
     * @throws Exception
     */
    @Test(expected = EvaluationException.class)
    public void testExceptionTranslationForRuntime() throws Exception {
        Node<Object> tree = new ImmutableFunctionNode<Object, Object>("fail");

        evaluator.evaluate(tree, ImmutableMap.<String, Object>of());
    }

    /**
     * Test that subclasses of EvaluationException are not wrapped
     * @throws Exception
     */
    @Test
    public void testExceptionTranslation() throws Exception {
        Node<Object> tree = new ImmutableFunctionNode<Object, Object>("epicFail");

        try {
            evaluator.evaluate(tree, ImmutableMap.<String, Object>of());
            fail("Exception expected");
        } catch (SeeRuntimeException e) {
            assertThat(e.getCause(), instanceOf(EpicFailException.class));
        }
    }

    private static class EpicFailException extends EvaluationException {
        public EpicFailException() {
            super("It failed");
        }
    }
}
