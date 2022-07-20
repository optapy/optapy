package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.createInstance;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.optaplanner.python.translator.OpcodeIdentifier;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class VariableImplementorTest {
    @Test
    public void testGlobalVariables() {
        PythonCompiledFunction setterCompiledFunction = PythonFunctionBuilder.newFunction("value")
                .loadParameter("value")
                .storeGlobalVariable("my_global")
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonCompiledFunction getterCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadGlobalVariable("my_global")
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        AtomicReference<PythonLikeObject> myGlobalReference = new AtomicReference<>();
        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);

        Mockito.when(interpreter.getGlobal(Mockito.any(), Mockito.eq("my_global")))
                .thenAnswer(invocationOnMock -> myGlobalReference.get());
        Mockito.doAnswer(invocationOnMock -> {
            myGlobalReference.set(invocationOnMock.getArgument(2, PythonLikeObject.class));
            return null;
        }).when(interpreter).setGlobal(Mockito.any(), Mockito.eq("my_global"), Mockito.any());

        Class<? extends Consumer> setterFunctionClass = translatePythonBytecodeToClass(setterCompiledFunction, Consumer.class);
        Class<? extends Supplier> getterFunctionClass = translatePythonBytecodeToClass(getterCompiledFunction, Supplier.class);

        Consumer setter = createInstance(setterFunctionClass, interpreter);
        Supplier getter = createInstance(getterFunctionClass, interpreter);

        setter.accept("Value 1");

        Mockito.verify(interpreter).setGlobal(Mockito.any(), Mockito.eq("my_global"),
                Mockito.eq(PythonString.valueOf("Value 1")));
        assertThat(getter.get()).isEqualTo(PythonString.valueOf("Value 1"));

        setter.accept("Value 2");

        Mockito.verify(interpreter).setGlobal(Mockito.any(), Mockito.eq("my_global"),
                Mockito.eq(PythonString.valueOf("Value 2")));
        assertThat(getter.get()).isEqualTo(PythonString.valueOf("Value 2"));
    }
}
