package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.LongToIntFunction;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JavaPythonTypeConversionImplementorTest {
    @Test
    public void testLoadParameter() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("b")
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        BiFunction javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);

        assertThat(javaFunction.apply("a", "b")).isEqualTo("b");
        assertThat(javaFunction.apply(1, 2)).isEqualTo(2);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply("a", "b")).isEqualTo("a");
        assertThat(javaFunction.apply(1, 2)).isEqualTo(1);
    }

    @Test
    public void testLoadPrimitiveParameter() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        LongToIntFunction javaFunction = translatePythonBytecode(pythonCompiledFunction, LongToIntFunction.class);

        assertThat(javaFunction.applyAsInt(1L)).isEqualTo(1);
    }

    @Test
    public void testReturnBoolean() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(Boolean.TRUE)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Predicate javaFunction = translatePythonBytecode(pythonCompiledFunction, Predicate.class);

        assertThat(javaFunction.test("Hi")).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(Boolean.FALSE)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Predicate.class);

        assertThat(javaFunction.test("Hi")).isEqualTo(false);
    }

    @Test
    public void testReturnVoid() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(null)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Consumer javaFunction = translatePythonBytecode(pythonCompiledFunction, Consumer.class);

        assertThatCode(() -> javaFunction.accept(null)).doesNotThrowAnyException();
    }
}
