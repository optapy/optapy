package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.CompareOp;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class DunderOperatorImplementorTest {
    @Test
    public void testComparisons() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        BiPredicate javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN_OR_EQUALS)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.GREATER_THAN)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.GREATER_THAN_OR_EQUALS)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

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
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        BiPredicate javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.NOT_EQUALS)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);
    }

    private BiFunction getMathFunction(PythonBytecodeInstruction.OpcodeIdentifier opcodeIdentifier) {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .op(opcodeIdentifier)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        return translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
    }

    @Test
    public void testMathOp() {
        BiFunction javaFunction = getMathFunction(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_ADD);

        assertThat(javaFunction.apply(1L, 2L)).isEqualTo(3L);

        javaFunction = getMathFunction(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_SUBTRACT);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1L);

        javaFunction = getMathFunction(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_TRUE_DIVIDE);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1.5d);

        javaFunction = getMathFunction(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_FLOOR_DIVIDE);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1L);
    }
}
