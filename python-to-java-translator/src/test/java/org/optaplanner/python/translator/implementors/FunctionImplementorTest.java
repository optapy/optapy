package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.MyObject;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.types.JavaMethodReference;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.UnaryLambdaReference;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class FunctionImplementorTest {
    @Test
    public void testCallFunction() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .getAttribute("concatToName")
                .loadConstant(" is awesome!")
                .callFunction(1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        MyObject object = new MyObject();
        object.name = "My name";
        assertThat(javaFunction.apply(object)).isEqualTo("My name is awesome!");
    }

    public static int keywordTestFunction(int first, int second, int third) {
        return first + 2 * second + 3 * third;
    }

    @Test
    public void testCallFunctionWithKeywords() throws NoSuchMethodException {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("function")
                .loadParameter("function")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .loadConstant(List.of("third", "second"))
                .callFunctionWithKeywords(3)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        PythonLikeFunction pythonLikeFunction = new JavaMethodReference(
                FunctionImplementorTest.class.getMethod("keywordTestFunction", int.class, int.class, int.class),
                Map.of("first", 0, "second", 1, "third", 2));
        assertThat(javaFunction.apply(pythonLikeFunction)).isEqualTo(13); // 1 + 2*3 + 3*2
    }

    @Test
    public void testCallFunctionUnpackIterable() throws NoSuchMethodException {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("function")
                .loadParameter("function")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .callFunctionUnpack(false)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        PythonLikeFunction pythonLikeFunction = new JavaMethodReference(
                FunctionImplementorTest.class.getMethod("keywordTestFunction", int.class, int.class, int.class),
                Map.of("first", 0, "second", 1, "third", 2));
        assertThat(javaFunction.apply(pythonLikeFunction)).isEqualTo(14); // 1 + 2*2 + 3*3
    }

    @Test
    public void testCallFunctionUnpackIterableAndKeywords() throws NoSuchMethodException {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("function")
                .loadParameter("function")
                .loadConstant(1)
                .tuple(1)
                .loadConstant(2)
                .loadConstant(3)
                .loadConstant(List.of("third", "second"))
                .constDict(2)
                .callFunctionUnpack(true)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        PythonLikeFunction pythonLikeFunction = new JavaMethodReference(
                FunctionImplementorTest.class.getMethod("keywordTestFunction", int.class, int.class, int.class),
                Map.of("first", 0, "second", 1, "third", 2));
        assertThat(javaFunction.apply(pythonLikeFunction)).isEqualTo(13); // 1 + 2*3 + 3*2
    }

    @Test
    public void testCallMethodOnType() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .loadMethod("concatToName")
                .loadConstant(" is awesome!")
                .callMethod(1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        MyObject object = new MyObject();
        object.name = "My name";
        assertThat(javaFunction.apply(object)).isEqualTo("My name is awesome!");
    }

    @Test
    public void testCallMethodOnInstance() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .loadMethod("attributeFunction")
                .loadConstant(" is awesome!")
                .callMethod(1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        MyObject object = new MyObject();
        object.attributeFunction = new UnaryLambdaReference((suffix) -> PythonString.valueOf("My name" + suffix), Map.of());

        assertThat(javaFunction.apply(object)).isEqualTo("My name is awesome!");
    }

    @Test
    public void testMakeFunction() {
        PythonCompiledFunction dependentFunction = PythonFunctionBuilder.newFunction()
                .loadFreeVariable("a")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Class<?> dependentFunctionClass = translatePythonBytecodeToClass(dependentFunction, PythonLikeFunction.class);

        PythonCompiledFunction parentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(0)
                .storeCellVariable("a")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_CLOSURE, 0)
                .tuple(1)
                .loadConstant(dependentFunctionClass)
                .loadConstant("parent.sub")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.MAKE_FUNCTION, 8)
                .storeVariable("sub")
                .loadConstant(1)
                .storeCellVariable("a")
                .loadVariable("sub")
                .callFunction(0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(parentFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(1);
    }

    @Test
    public void testMakeFunctionComplex() {
        PythonCompiledFunction secondDependentFunction = PythonFunctionBuilder.newFunction()
                .loadFreeVariable("sub1")
                .loadFreeVariable("sub2")
                .loadFreeVariable("parent")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_ADD)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_ADD)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Class<?> secondDependentFunctionClass =
                translatePythonBytecodeToClass(secondDependentFunction, PythonLikeFunction.class);

        PythonCompiledFunction firstDependentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .storeCellVariable("sub1")
                .loadConstant(100)
                .storeCellVariable("sub2")
                .loadFreeVariable("parent")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.POP_TOP)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_CLOSURE, 0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_CLOSURE, 1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_CLOSURE, 2)
                .tuple(3)
                .loadConstant(secondDependentFunctionClass)
                .loadConstant("parent.sub.sub")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.MAKE_FUNCTION, 8)
                .storeVariable("function")
                .loadConstant(300)
                .storeCellVariable("sub2")
                .loadVariable("function")
                .callFunction(0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Class<?> firstDependentFunctionClass = translatePythonBytecodeToClass(firstDependentFunction, PythonLikeFunction.class);

        PythonCompiledFunction parentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(10)
                .storeCellVariable("parent")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_CLOSURE, 0)
                .tuple(1)
                .loadConstant(firstDependentFunctionClass)
                .loadConstant("parent.sub")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.MAKE_FUNCTION, 8)
                .storeVariable("sub")
                .loadConstant(20)
                .storeCellVariable("parent")
                .loadVariable("sub")
                .callFunction(0)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(parentFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(321);
    }
}