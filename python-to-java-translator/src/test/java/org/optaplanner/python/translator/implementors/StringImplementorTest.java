package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.createInstance;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class StringImplementorTest {
    @Test
    public void testBuildString() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant("My name")
                .loadConstant(" is ")
                .loadConstant("awesome!")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.BUILD_STRING, 3)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo("My name is awesome!");
    }

    @Test
    public void testFormat() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item", "format")
                .loadParameter("item")
                .loadParameter("format")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.FORMAT_VALUE, 4)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        BiFunction javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);

        assertThat(javaFunction.apply(12, "x")).isEqualTo("c");
        assertThat(javaFunction.apply(12, "o")).isEqualTo("14");
        assertThat(javaFunction.apply(12, "")).isEqualTo("12");
        assertThat(javaFunction.apply("hello", "")).isEqualTo("hello");
    }

    @Test
    public void testFormatWithoutParameter() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.FORMAT_VALUE, 0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);

        assertThat(javaFunction.apply(12)).isEqualTo("12");
        assertThat(javaFunction.apply("hello")).isEqualTo("hello");
    }

    @Test
    public void testPrint() {
        PythonCompiledFunction compiledFunction = PythonFunctionBuilder.newFunction("to_print")
                .loadParameter("to_print")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.PRINT_EXPR)
                .loadConstant(null)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);

        Class<? extends Consumer> functionClass = translatePythonBytecodeToClass(compiledFunction, Consumer.class);

        Consumer consumer = createInstance(functionClass, interpreter);

        consumer.accept("Value 1");

        Mockito.verify(interpreter).print(PythonString.valueOf("Value 1"));

        Mockito.reset(interpreter);

        consumer.accept(10);

        Mockito.verify(interpreter).print(PythonInteger.valueOf(10));
    }
}
