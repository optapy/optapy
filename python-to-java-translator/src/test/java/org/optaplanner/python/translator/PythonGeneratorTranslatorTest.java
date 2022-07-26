package org.optaplanner.python.translator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.types.PythonBoolean;
import org.optaplanner.python.translator.types.PythonGenerator;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.errors.PythonAssertionError;
import org.optaplanner.python.translator.types.errors.StopIteration;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

public class PythonGeneratorTranslatorTest {

    @Test
    public void testSimpleGenerator() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("value")
                .loadParameter("value")
                .op(OpcodeIdentifier.YIELD_VALUE)
                .loadConstant(null)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Function.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.apply(1);

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testMultipleYieldsGenerator() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("value1", "value2", "value3")
                .loadParameter("value1")
                .op(OpcodeIdentifier.YIELD_VALUE)
                .op(OpcodeIdentifier.POP_TOP)
                .loadParameter("value2")
                .op(OpcodeIdentifier.YIELD_VALUE)
                .op(OpcodeIdentifier.POP_TOP)
                .loadParameter("value3")
                .op(OpcodeIdentifier.YIELD_VALUE)
                .op(OpcodeIdentifier.POP_TOP)
                .loadConstant(null)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        TriFunction generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, TriFunction.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.apply(1, 2, 3);

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(2));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(3));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testGeneratorWithLoop() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(OpcodeIdentifier.GET_ITER)
                .loop(builder -> {
                    builder.op(OpcodeIdentifier.YIELD_VALUE)
                            .op(OpcodeIdentifier.POP_TOP);
                })
                .loadConstant(null)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Supplier generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Supplier.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.get();

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(2));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(3));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testGeneratorWithTryExcept() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction()
                .tryCode(tryBuilder -> {
                    tryBuilder.loadConstant(1)
                            .op(OpcodeIdentifier.YIELD_VALUE)
                            .op(OpcodeIdentifier.POP_TOP)
                            .op(OpcodeIdentifier.LOAD_ASSERTION_ERROR)
                            .op(OpcodeIdentifier.RAISE_VARARGS, 1);
                }, true).except(PythonAssertionError.ASSERTION_ERROR_TYPE, exceptBuilder -> {
                    exceptBuilder.loadConstant(2)
                            .op(OpcodeIdentifier.YIELD_VALUE)
                            .op(OpcodeIdentifier.POP_TOP);
                }, false)
                .andFinally(finallyBuilder -> {
                    finallyBuilder.loadConstant(3)
                            .op(OpcodeIdentifier.YIELD_VALUE)
                            .op(OpcodeIdentifier.POP_TOP);
                }, false)
                .tryEnd()
                .loadConstant(null)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Supplier generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Supplier.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.get();

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(2));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(3));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testSendingValues() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("value1")
                .loadParameter("value1")
                .op(OpcodeIdentifier.YIELD_VALUE)
                .op(OpcodeIdentifier.YIELD_VALUE)
                .op(OpcodeIdentifier.YIELD_VALUE)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Function.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.apply(1);

        assertThat(generator.send(PythonInteger.valueOf(0))).isEqualTo(PythonInteger.valueOf(1)); // first sent value is ignored
        assertThat(generator.send(PythonInteger.valueOf(1))).isEqualTo(PythonInteger.valueOf(1));
        assertThat(generator.send(PythonInteger.valueOf(2))).isEqualTo(PythonInteger.valueOf(2));
        assertThatCode(() -> generator.send(PythonInteger.valueOf(3))).isInstanceOf(StopIteration.class)
                .matches(error -> ((StopIteration) error).getValue().equals(PythonInteger.valueOf(3)));
    }

    @Test
    public void testThrowingValues() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction()
                .tryCode(tryBuilder -> {
                    tryBuilder
                            .loadConstant(false)
                            .op(OpcodeIdentifier.YIELD_VALUE)
                            .op(OpcodeIdentifier.RETURN_VALUE);
                }, true)
                .except(ValueError.VALUE_ERROR_TYPE, exceptBuilder -> {
                    exceptBuilder.loadConstant(true)
                            .op(OpcodeIdentifier.YIELD_VALUE)
                            .op(OpcodeIdentifier.POP_TOP)
                            .loadConstant(null)
                            .op(OpcodeIdentifier.RETURN_VALUE);
                }, true)
                .tryEnd()
                .build();

        Supplier generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Supplier.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.get();

        assertThat(generator.next()).isEqualTo(PythonBoolean.FALSE);
        assertThat(generator.throwValue(new ValueError())).isEqualTo(PythonBoolean.TRUE);
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }
}
