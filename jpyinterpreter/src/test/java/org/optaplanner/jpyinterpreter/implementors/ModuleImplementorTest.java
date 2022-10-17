package org.optaplanner.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.optaplanner.jpyinterpreter.OpcodeIdentifier;
import org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.jpyinterpreter.PythonCompiledFunction;
import org.optaplanner.jpyinterpreter.PythonInterpreter;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonModule;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ModuleImplementorTest {

    @Test
    public void testImportName() {
        Map<String, PythonLikeObject> globalsMap = Map.of("__module__", PythonString.valueOf("__main__"));
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("from_list", "level")
                .usingGlobalsMap(globalsMap)
                .loadParameter("level")
                .loadParameter("from_list")
                .loadModule("module")
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);
        PythonModule module = new PythonModule(new HashMap<>());

        when(interpreter.importModule(PythonInteger.ZERO, List.of(), globalsMap, Map.of(), "module")).thenReturn(module);

        Class<? extends BiFunction> javaFunctionClass =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(pythonCompiledFunction, BiFunction.class);
        BiFunction javaFunction = PythonBytecodeToJavaBytecodeTranslator.createInstance(javaFunctionClass, interpreter);

        assertThat(javaFunction.apply(List.of(), PythonInteger.ZERO)).isSameAs(module);
        verify(interpreter).importModule(PythonInteger.ZERO, List.of(), globalsMap, Map.of(), "module");
    }

    @Test
    public void testImportNameWithAttributes() {
        Map<String, PythonLikeObject> globalsMap = Map.of("__module__", PythonString.valueOf("__main__"));
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("from_list", "level")
                .usingGlobalsMap(globalsMap)
                .loadParameter("level")
                .loadParameter("from_list")
                .loadModule("module")
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);
        PythonModule module = new PythonModule(new HashMap<>());

        when(interpreter.importModule(PythonInteger.ZERO, List.of(PythonString.valueOf("item")), globalsMap, Map.of(),
                "module")).thenReturn(module);

        Class<? extends BiFunction> javaFunctionClass =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(pythonCompiledFunction, BiFunction.class);
        BiFunction javaFunction = PythonBytecodeToJavaBytecodeTranslator.createInstance(javaFunctionClass, interpreter);

        assertThat(javaFunction.apply(List.of(PythonString.valueOf("item")), PythonInteger.ZERO)).isSameAs(module);
        verify(interpreter).importModule(PythonInteger.ZERO, List.of(PythonString.valueOf("item")), globalsMap, Map.of(),
                "module");
    }

    @Test
    public void testImportFrom() {
        Map<String, PythonLikeObject> globalsMap = Map.of("__module__", PythonString.valueOf("__main__"));
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .usingGlobalsMap(globalsMap)
                .loadConstant(0)
                .loadConstant(List.of(PythonString.valueOf("item1"), PythonString.valueOf("item2")))
                .loadModule("module")
                .getFromModule("item1")
                .storeVariable("a")
                .getFromModule("item2")
                .storeVariable("b")
                .op(OpcodeIdentifier.POP_TOP)
                .loadVariable("a")
                .loadVariable("b")
                .op(OpcodeIdentifier.BINARY_ADD)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);
        PythonModule module = new PythonModule(new HashMap<>());
        module.addItem("item1", PythonInteger.valueOf(1));
        module.addItem("item2", PythonInteger.valueOf(2));

        when(interpreter.importModule(PythonInteger.ZERO, List.of(PythonString.valueOf("item1"), PythonString.valueOf("item2")),
                globalsMap, Map.of(), "module")).thenReturn(module);

        Class<? extends Supplier> javaFunctionClass =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(pythonCompiledFunction, Supplier.class);
        Supplier javaFunction = PythonBytecodeToJavaBytecodeTranslator.createInstance(javaFunctionClass, interpreter);

        assertThat(javaFunction.get()).isEqualTo(PythonInteger.valueOf(3));
        verify(interpreter).importModule(PythonInteger.ZERO,
                List.of(PythonString.valueOf("item1"), PythonString.valueOf("item2")), globalsMap, Map.of(), "module");
    }
}
