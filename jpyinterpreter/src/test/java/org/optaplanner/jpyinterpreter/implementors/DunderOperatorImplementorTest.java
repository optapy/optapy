package org.optaplanner.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;
import org.optaplanner.jpyinterpreter.CompareOp;
import org.optaplanner.jpyinterpreter.OpcodeIdentifier;
import org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.jpyinterpreter.PythonCompiledFunction;
import org.optaplanner.jpyinterpreter.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class DunderOperatorImplementorTest {
    @Test
    public void testComparisons() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        BiPredicate javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN_OR_EQUALS)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.GREATER_THAN)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.GREATER_THAN_OR_EQUALS)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);
    }

    @Test
    public void testEquals() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.EQUALS)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        BiPredicate javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.NOT_EQUALS)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);
    }

    private BiFunction getMathFunction(OpcodeIdentifier opcodeIdentifier) {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .op(opcodeIdentifier)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        return PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
    }

    @Test
    public void testMathOp() {
        BiFunction javaFunction = getMathFunction(OpcodeIdentifier.BINARY_ADD);

        assertThat(javaFunction.apply(1L, 2L)).isEqualTo(3L);

        javaFunction = getMathFunction(OpcodeIdentifier.BINARY_SUBTRACT);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1L);

        javaFunction = getMathFunction(OpcodeIdentifier.BINARY_TRUE_DIVIDE);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1.5d);

        javaFunction = getMathFunction(OpcodeIdentifier.BINARY_FLOOR_DIVIDE);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1L);
    }
}
