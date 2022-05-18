package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.MyObject;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.types.PythonBoolean;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ObjectImplementorTest {

    @Test
    public void testIs() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.DUP_TOP)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.IS_OP, 0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.TRUE);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.DUP_TOP)
                .loadConstant(0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_ADD)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.IS_OP, 0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.FALSE);

        // Test is not
        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.DUP_TOP)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.IS_OP, 1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.FALSE);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.DUP_TOP)
                .loadConstant(0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_ADD)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.IS_OP, 1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.TRUE);
    }

    @Test
    public void testGetAttribute() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .getAttribute("name")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        MyObject object = new MyObject();
        object.name = "My name";
        assertThat(javaFunction.apply(object)).isEqualTo("My name");
    }

    @Test
    public void testSetAttribute() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item", "value")
                .loadParameter("value")
                .loadParameter("item")
                .storeAttribute("name")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        BiConsumer javaFunction = translatePythonBytecode(pythonCompiledFunction, BiConsumer.class);
        MyObject object = new MyObject();
        object.name = "My name";
        javaFunction.accept(object, "New name");
        assertThat(object.name).isEqualTo("New name");
    }

}