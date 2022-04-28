package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

public class StackManipulationImplementorTest {
    @Test
    public void testRot2() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .op(PythonBytecodeInstruction.OpCode.ROT_TWO)
                .tuple(2)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Supplier<?> javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);

        assertThat(javaFunction.get()).isEqualTo(List.of(2, 1));
    }

    @Test
    public void testRot3() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .op(PythonBytecodeInstruction.OpCode.ROT_THREE)
                .tuple(3)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Supplier<?> javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);

        assertThat(javaFunction.get()).isEqualTo(List.of(3, 1, 2));
    }

    @Test
    public void testRot4() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .loadConstant(4)
                .op(PythonBytecodeInstruction.OpCode.ROT_FOUR)
                .tuple(4)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Supplier<?> javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);

        assertThat(javaFunction.get()).isEqualTo(List.of(4, 1, 2, 3));
    }
}
