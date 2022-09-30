package org.optaplanner.python.translator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.INT_TYPE;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.builtins.ObjectBuiltinOperations;
import org.optaplanner.python.translator.types.BuiltinTypes;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.numeric.PythonInteger;
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
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonCompiledFunction ageFunction = PythonFunctionBuilder.newFunction("self")
                .loadParameter("self")
                .getAttribute("age")
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonCompiledFunction helloWorldFunction = PythonFunctionBuilder.newFunction()
                .loadConstant("hello world")
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        compiledClass.className = "MyClass";
        compiledClass.superclassList = List.of(BASE_TYPE);
        compiledClass.staticAttributeNameToObject = Map.of("type_variable", new PythonString("type_value"));
        compiledClass.staticAttributeNameToClassInstance = Map.of();
        compiledClass.typeAnnotations = Map.of("age", INT_TYPE);
        compiledClass.instanceFunctionNameToPythonBytecode = Map.of("__init__", initFunction,
                "get_age", ageFunction);
        compiledClass.staticFunctionNameToPythonBytecode = Map.of("hello_world", helloWorldFunction);
        compiledClass.classFunctionNameToPythonBytecode = Map.of();

        PythonLikeType classType = PythonClassTranslator.translatePythonClass(compiledClass);
        Class<?> generatedClass = BuiltinTypes.asmClassLoader.loadClass(
                classType.getJavaTypeInternalName().replace('/', '.'));

        assertThat(generatedClass).hasPublicFields(PythonClassTranslator.getJavaMethodName("get_age"),
                PythonClassTranslator.getJavaFieldName("age"));
        assertThat(generatedClass).hasPublicMethods(
                PythonClassTranslator.getJavaMethodName("__init__"),
                PythonClassTranslator.getJavaMethodName("get_age"));
        assertThat(generatedClass.getMethod(PythonClassTranslator.getJavaMethodName("get_age")).getParameterTypes()).isEmpty();

        PythonLikeObject classObject = classType.__call__(List.of(PythonInteger.valueOf(10)), Map.of());
        PythonLikeFunction getAgeFunction =
                (PythonLikeFunction) ObjectBuiltinOperations.getAttribute(classObject, PythonString.valueOf("get_age"));
        assertThat(getAgeFunction.__call__(List.of(), Map.of())).isEqualTo(PythonInteger.valueOf(10));
    }

    @Test
    public void testPythonClassComparable() throws ClassNotFoundException {
        PythonCompiledFunction initFunction = PythonFunctionBuilder.newFunction("self", "key")
                .loadParameter("key")
                .loadParameter("self")
                .storeAttribute("key")
                .loadConstant(null)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function<CompareOp, PythonCompiledFunction> getCompareFunction =
                compareOp -> PythonFunctionBuilder.newFunction("self", "other")
                        .loadParameter("self")
                        .getAttribute("key")
                        .loadParameter("other")
                        .getAttribute("key")
                        .compare(compareOp)
                        .op(OpcodeIdentifier.RETURN_VALUE)
                        .build();

        for (CompareOp compareOp : List.of(CompareOp.LESS_THAN, CompareOp.GREATER_THAN,
                CompareOp.LESS_THAN_OR_EQUALS, CompareOp.GREATER_THAN_OR_EQUALS)) {
            PythonCompiledFunction comparisonFunction = getCompareFunction.apply(compareOp);

            PythonCompiledClass compiledClass = new PythonCompiledClass();
            compiledClass.className = "MyClass";
            compiledClass.superclassList = List.of(BASE_TYPE);
            compiledClass.staticAttributeNameToObject = Map.of();
            compiledClass.staticAttributeNameToClassInstance = Map.of();
            compiledClass.typeAnnotations = Map.of("key", INT_TYPE);
            compiledClass.instanceFunctionNameToPythonBytecode = Map.of("__init__", initFunction,
                    compareOp.dunderMethod, comparisonFunction);
            compiledClass.staticFunctionNameToPythonBytecode = Map.of();
            compiledClass.classFunctionNameToPythonBytecode = Map.of();

            PythonLikeType classType = PythonClassTranslator.translatePythonClass(compiledClass);
            Class<?> generatedClass = BuiltinTypes.asmClassLoader.loadClass(
                    classType.getJavaTypeInternalName().replace('/', '.'));

            assertThat(Comparable.class.isAssignableFrom(generatedClass)).isTrue();
            assertThat(generatedClass).hasPublicFields(PythonClassTranslator.getJavaFieldName("key"));
            assertThat(generatedClass).hasPublicMethods(
                    PythonClassTranslator.getJavaMethodName("__init__"),
                    "compareTo");

            Comparable<Object> object1 = (Comparable<Object>) classType.__call__(List.of(PythonInteger.valueOf(1)), Map.of());
            Comparable<Object> object2 = (Comparable<Object>) classType.__call__(List.of(PythonInteger.valueOf(2)), Map.of());
            Comparable<Object> object1b = (Comparable<Object>) classType.__call__(List.of(PythonInteger.valueOf(1)), Map.of());

            assertThat(object1.compareTo(object2))
                    .withFailMessage(compareOp.name() + " a < b incorrect")
                    .isLessThan(0);
            assertThat(object2.compareTo(object1))
                    .withFailMessage(compareOp.name() + " a > b incorrect")
                    .isGreaterThan(0);
            assertThat(object1.compareTo(object1))
                    .withFailMessage(compareOp.name() + " a == a incorrect")
                    .isEqualTo(0);
            assertThat(object1.compareTo(object1b))
                    .withFailMessage(compareOp.name() + " a == b incorrect")
                    .isEqualTo(0);
        }
    }
}
