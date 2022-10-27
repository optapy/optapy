package org.optaplanner.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;
import static org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.optaplanner.jpyinterpreter.CompareOp;
import org.optaplanner.jpyinterpreter.MyObject;
import org.optaplanner.jpyinterpreter.OpcodeIdentifier;
import org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.jpyinterpreter.PythonCompiledFunction;
import org.optaplanner.jpyinterpreter.types.PythonCode;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.wrappers.JavaMethodReference;
import org.optaplanner.jpyinterpreter.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class FunctionImplementorTest {
    @Test
    public void testCallFunction() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .getAttribute("concatToName")
                .loadConstant(" is awesome!")
                .callFunction(1)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
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
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
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
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
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
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
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
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
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
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
        MyObject object = new MyObject();
        object.attributeFunction =
                (positionalArgs, namedArgs, callerInstance) -> PythonString.valueOf("My name" + positionalArgs.get(0));

        assertThat(javaFunction.apply(object)).isEqualTo("My name is awesome!");
    }

    @Test
    public void testMakeFunction() {
        PythonCompiledFunction dependentFunction = PythonFunctionBuilder.newFunction()
                .loadFreeVariable("a")
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Class<?> dependentFunctionClass = PythonBytecodeToJavaBytecodeTranslator
                .translatePythonBytecodeToClass(dependentFunction, PythonLikeFunction.class);

        PythonCompiledFunction parentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(0)
                .storeCellVariable("a")
                .op(OpcodeIdentifier.LOAD_CLOSURE, 0)
                .tuple(1)
                .loadConstant(dependentFunctionClass)
                .loadConstant("parent.sub")
                .op(OpcodeIdentifier.MAKE_FUNCTION, 8)
                .storeVariable("sub")
                .loadConstant(1)
                .storeCellVariable("a")
                .loadVariable("sub")
                .callFunction(0)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Supplier javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(parentFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(1);
    }

    @Test
    public void testMakeFunctionComplex() {
        PythonCompiledFunction secondDependentFunction = PythonFunctionBuilder.newFunction()
                .loadFreeVariable("sub1")
                .loadFreeVariable("sub2")
                .loadFreeVariable("parent")
                .op(OpcodeIdentifier.BINARY_ADD)
                .op(OpcodeIdentifier.BINARY_ADD)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Class<?> secondDependentFunctionClass =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(secondDependentFunction,
                        PythonLikeFunction.class);

        PythonCompiledFunction firstDependentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .storeCellVariable("sub1")
                .loadConstant(100)
                .storeCellVariable("sub2")
                .loadFreeVariable("parent")
                .op(OpcodeIdentifier.POP_TOP)
                .op(OpcodeIdentifier.LOAD_CLOSURE, 0)
                .op(OpcodeIdentifier.LOAD_CLOSURE, 1)
                .op(OpcodeIdentifier.LOAD_CLOSURE, 2)
                .tuple(3)
                .loadConstant(secondDependentFunctionClass)
                .loadConstant("parent.sub.sub")
                .op(OpcodeIdentifier.MAKE_FUNCTION, 8)
                .storeVariable("function")
                .loadConstant(300)
                .storeCellVariable("sub2")
                .loadVariable("function")
                .callFunction(0)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Class<?> firstDependentFunctionClass = PythonBytecodeToJavaBytecodeTranslator
                .translatePythonBytecodeToClass(firstDependentFunction, PythonLikeFunction.class);

        PythonCompiledFunction parentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(10)
                .storeCellVariable("parent")
                .op(OpcodeIdentifier.LOAD_CLOSURE, 0)
                .tuple(1)
                .loadConstant(firstDependentFunctionClass)
                .loadConstant("parent.sub")
                .op(OpcodeIdentifier.MAKE_FUNCTION, 8)
                .storeVariable("sub")
                .loadConstant(20)
                .storeCellVariable("parent")
                .loadVariable("sub")
                .callFunction(0)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Supplier javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(parentFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(321);
    }

    @Test
    public void testEnumDir() {
        // Test a complicated list comp used in enums (ensuring it creates valid Java bytecode)
        PythonCompiledFunction firstListComp = PythonFunctionBuilder.newFunction(".0")
                .list(0)
                .loadParameter(".0")
                .loop(loopBuilder -> {
                    loopBuilder.storeVariable("cls")
                            .loadVariable("cls")
                            .getAttribute("__dict__")
                            .op(OpcodeIdentifier.GET_ITER)
                            .loop(innerLoopBuilder -> {
                                innerLoopBuilder.storeVariable("m")
                                        .loadVariable("m")
                                        .loadConstant(0)
                                        .op(OpcodeIdentifier.BINARY_SUBSCR)
                                        .loadConstant("_")
                                        .compare(CompareOp.NOT_EQUALS)
                                        .ifTrue(ifBuilder -> {
                                            ifBuilder.loadVariable("m")
                                                    .loadFreeVariable("self")
                                                    .getAttribute("_member_map_")
                                                    .op(OpcodeIdentifier.CONTAINS_OP, 1)
                                                    .ifTrue(innerIfBuilder -> {
                                                        ifBuilder.loadVariable("m")
                                                                .op(OpcodeIdentifier.LIST_APPEND, 3);
                                                    });
                                        });
                            });
                })
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonCompiledFunction secondListComp = PythonFunctionBuilder.newFunction(".0")
                .list(0)
                .loadParameter(".0")
                .loop(loopBuilder -> {
                    loopBuilder.storeVariable("m")
                            .loadVariable("m")
                            .loadConstant(0)
                            .op(OpcodeIdentifier.BINARY_SUBSCR)
                            .loadConstant("_")
                            .compare(CompareOp.NOT_EQUALS)
                            .ifTrue(ifBuilder -> {
                                ifBuilder.loadVariable("m")
                                        .op(OpcodeIdentifier.LIST_APPEND, 2);
                            });
                })
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonCode firstListCompCode = new PythonCode(
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(firstListComp, PythonLikeFunction.class));

        PythonCode secondListCompCode = new PythonCode(
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(secondListComp,
                        PythonLikeFunction.class));

        PythonCompiledFunction dirFunction = PythonFunctionBuilder.newFunction("self")
                .op(OpcodeIdentifier.LOAD_CLOSURE, 0)
                .tuple(1)
                .loadConstant(firstListCompCode)
                .loadConstant("Enum.__dir__.<locals>.<listcomp>")
                .op(OpcodeIdentifier.MAKE_FUNCTION, 8)
                .loadCellVariable("self")
                .getAttribute("__class__")
                .loadMethod("mro")
                .callMethod(0)
                .op(OpcodeIdentifier.GET_ITER)
                .callFunction(1)
                .loadConstant(secondListCompCode)
                .loadConstant("Enum.__dir__.<locals>.<listcomp>")
                .op(OpcodeIdentifier.MAKE_FUNCTION, 0)
                .loadCellVariable("self")
                .getAttribute("__dict__")
                .op(OpcodeIdentifier.GET_ITER)
                .callFunction(1)
                .op(OpcodeIdentifier.BINARY_ADD)
                .storeVariable("added_behavior")
                .list(0)
                .loadConstant(List.of("__class__", "__doc__", "__module__"))
                .op(OpcodeIdentifier.LIST_EXTEND, 1)
                .loadVariable("added_behavior")
                .op(OpcodeIdentifier.BINARY_ADD)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(dirFunction, Function.class);
    }
}
