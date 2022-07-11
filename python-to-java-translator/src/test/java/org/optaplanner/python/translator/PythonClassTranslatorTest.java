package org.optaplanner.python.translator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.builtins.ObjectBuiltinOperations;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

public class PythonClassTranslatorTest {

    @Test
    public void testPythonClassTranslation() throws ClassNotFoundException, NoSuchMethodException {
        PythonCompiledClass compiledClass = new PythonCompiledClass();

        PythonCompiledFunction initFunction = PythonFunctionBuilder.newFunction("self", "age")
                .loadParameter("age")
                .loadParameter("self")
                .storeAttribute("age")
                .loadConstant(null)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonCompiledFunction ageFunction = PythonFunctionBuilder.newFunction("self")
                .loadParameter("self")
                .getAttribute("age")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonCompiledFunction helloWorldFunction = PythonFunctionBuilder.newFunction()
                .loadConstant("hello world")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        compiledClass.className = "MyClass";
        compiledClass.superclassList = List.of(PythonLikeType.getBaseType());
        compiledClass.staticAttributeNameToObject = Map.of("type_variable", new PythonString("type_value"));
        compiledClass.staticAttributeNameToClassInstance = Map.of();
        compiledClass.typeAnnotations = Map.of("age", PythonInteger.INT_TYPE);
        compiledClass.instanceFunctionNameToPythonBytecode = Map.of("__init__", initFunction,
                "get_age", ageFunction);
        compiledClass.staticFunctionNameToPythonBytecode = Map.of("hello_world", helloWorldFunction);
        compiledClass.classFunctionNameToPythonBytecode = Map.of();

        PythonLikeType classType = PythonClassTranslator.translatePythonClass(compiledClass);
        Class<?> generatedClass = PythonBytecodeToJavaBytecodeTranslator.asmClassLoader.loadClass(
                classType.getJavaTypeInternalName().replace('/', '.'));

        assertThat(generatedClass).hasPublicFields(PythonClassTranslator.getJavaMethodName("get_age"),
                PythonClassTranslator.getJavaFieldName("age"));
        assertThat(generatedClass).hasPublicMethods(
                PythonClassTranslator.getJavaMethodName("__init__"),
                PythonClassTranslator.getJavaMethodName("get_age"));
        assertThat(generatedClass.getMethod(PythonClassTranslator.getJavaMethodName("get_age")).getParameterTypes()).isEmpty();

        PythonLikeObject classObject = classType.__call__(List.of(PythonInteger.valueOf(10)), Map.of());
        PythonLikeFunction getAgeFunction = (PythonLikeFunction) ObjectBuiltinOperations.getAttribute(classObject, "get_age");
        assertThat(getAgeFunction.__call__(List.of(), Map.of())).isEqualTo(PythonInteger.valueOf(10));
    }
}
