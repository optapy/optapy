package org.optaplanner.optapy.translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.optaplanner.optapy.translator.PythonBytecodeInstruction.OpCode;

import static org.optaplanner.optapy.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;
import static org.optaplanner.optapy.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongToIntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.optaplanner.optapy.translator.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.optapy.translator.types.JavaMethodReference;
import org.optaplanner.optapy.translator.types.PythonBoolean;
import org.optaplanner.optapy.translator.types.PythonInteger;
import org.optaplanner.optapy.translator.types.PythonLikeFunction;
import org.optaplanner.optapy.translator.types.PythonLikeTuple;
import org.optaplanner.optapy.translator.types.PythonString;
import org.optaplanner.optapy.translator.types.UnaryLambdaReference;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PythonBytecodeToJavaBytecodeTranslatorTest {

    @Test
    public void testRot2() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .op(OpCode.ROT_TWO)
                .tuple(2)
                .op(OpCode.RETURN_VALUE)
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
                .op(OpCode.ROT_THREE)
                .tuple(3)
                .op(OpCode.RETURN_VALUE)
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
                .op(OpCode.ROT_FOUR)
                .tuple(4)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier<?> javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);

        assertThat(javaFunction.get()).isEqualTo(List.of(4, 1, 2, 3));
    }

    @Test
    public void testLoadParameter() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("b")
                .op(OpCode.RETURN_VALUE)
                .build();

        BiFunction javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);

        assertThat(javaFunction.apply("a", "b")).isEqualTo("b");
        assertThat(javaFunction.apply(1, 2)).isEqualTo(2);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply("a", "b")).isEqualTo("a");
        assertThat(javaFunction.apply(1, 2)).isEqualTo(1);
    }

    @Test
    public void testLoadPrimitiveParameter() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .op(OpCode.RETURN_VALUE)
                .build();

        LongToIntFunction javaFunction = translatePythonBytecode(pythonCompiledFunction, LongToIntFunction.class);

        assertThat(javaFunction.applyAsInt(1L)).isEqualTo(1);
    }

    @Test
    public void testReturnBoolean() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(Boolean.TRUE)
                .op(OpCode.RETURN_VALUE)
                .build();

        Predicate javaFunction = translatePythonBytecode(pythonCompiledFunction, Predicate.class);

        assertThat(javaFunction.test("Hi")).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(Boolean.FALSE)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Predicate.class);

        assertThat(javaFunction.test("Hi")).isEqualTo(false);
    }

    @Test
    public void testReturnVoid() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(null)
                .op(OpCode.RETURN_VALUE)
                .build();

        Consumer javaFunction = translatePythonBytecode(pythonCompiledFunction, Consumer.class);

        assertThatCode(() -> javaFunction.accept(null)).doesNotThrowAnyException();
    }

    @Test
    public void testComparisons() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN)
                .op(OpCode.RETURN_VALUE)
                .build();

        BiPredicate javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN_OR_EQUALS)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.GREATER_THAN)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.GREATER_THAN_OR_EQUALS)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);
    }

    @Test
    public void testEquals() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.EQUALS)
                .op(OpCode.RETURN_VALUE)
                .build();

        BiPredicate javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.NOT_EQUALS)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);
    }

    private BiFunction getMathFunction(OpCode opCode) {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .op(opCode)
                .op(OpCode.RETURN_VALUE)
                .build();

        return translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
    }

    @Test
    public void testMathOp() {
        BiFunction javaFunction = getMathFunction(OpCode.BINARY_ADD);

        assertThat(javaFunction.apply(1L, 2L)).isEqualTo(3L);

        javaFunction = getMathFunction(OpCode.BINARY_SUBTRACT);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1L);

        javaFunction = getMathFunction(OpCode.BINARY_TRUE_DIVIDE);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1.5d);

        javaFunction = getMathFunction(OpCode.BINARY_FLOOR_DIVIDE);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1L);
    }

    @Test
    public void testGetIterator() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(OpCode.GET_ITER)
                .op(OpCode.RETURN_VALUE)
                .build();

        Iterable javaFunction = translatePythonBytecode(pythonCompiledFunction, Iterable.class);
        assertThat(javaFunction).containsExactly(1L, 2L, 3L);
    }

    @Test
    public void testIteration() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(0)
                .storeVariable("sum")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(OpCode.GET_ITER)
                .loop(block -> {
                    block.loadVariable("sum");
                    block.op(OpCode.BINARY_ADD);
                    block.storeVariable("sum");
                })
                .loadVariable("sum")
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(6L);
    }

    @Test
    public void testIs() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(OpCode.DUP_TOP)
                .op(OpCode.IS_OP, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.TRUE);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(OpCode.DUP_TOP)
                .loadConstant(0)
                .op(OpCode.BINARY_ADD)
                .op(OpCode.IS_OP, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.FALSE);

        // Test is not
        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(OpCode.DUP_TOP)
                .op(OpCode.IS_OP, 1)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.FALSE);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(OpCode.DUP_TOP)
                .loadConstant(0)
                .op(OpCode.BINARY_ADD)
                .op(OpCode.IS_OP, 1)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.TRUE);
    }

    @Test
    public void testIfTrue() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifTrue(block -> {
                    block.loadConstant(10);
                    block.op(OpCode.RETURN_VALUE);
                })
                .loadConstant(-10)
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(1L)).isEqualTo(10L);
        assertThat(javaFunction.apply(10L)).isEqualTo(-10L);
    }

    @Test
    public void testIfFalse() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifFalse(block -> {
                    block.loadConstant(10);
                    block.op(OpCode.RETURN_VALUE);
                })
                .loadConstant(-10)
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(1L)).isEqualTo(-10L);
        assertThat(javaFunction.apply(10L)).isEqualTo(10L);
    }

    @Test
    public void testIfTrueKeepTop() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifTruePopTop(block -> {
                    block.loadConstant(true);
                    block.op(OpCode.RETURN_VALUE);
                })
                .op(OpCode.RETURN_VALUE) // Top is False (block was skipped)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(1L)).isEqualTo(PythonBoolean.TRUE);
        assertThat(javaFunction.apply(10L)).isEqualTo(PythonBoolean.FALSE);
    }

    @Test
    public void testIfFalseKeepTop() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifFalsePopTop(block -> {
                    block.loadConstant(false);
                    block.op(OpCode.RETURN_VALUE);
                })
                .op(OpCode.RETURN_VALUE) // Top is True (block was skipped)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(1L)).isEqualTo(PythonBoolean.TRUE);
        assertThat(javaFunction.apply(10L)).isEqualTo(PythonBoolean.FALSE);
    }

    @Test
    public void testContains() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(OpCode.CONTAINS_OP, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Predicate javaFunction = translatePythonBytecode(pythonCompiledFunction, Predicate.class);
        assertThat(javaFunction.test(1L)).isEqualTo(true);
        assertThat(javaFunction.test(4L)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(OpCode.CONTAINS_OP, 1)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, Predicate.class);
        assertThat(javaFunction.test(1L)).isEqualTo(false);
        assertThat(javaFunction.test(4L)).isEqualTo(true);
    }

    @Test
    public void testSet() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .set(3)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Set.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2)));
    }

    @Test
    public void testListAppend() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .list(0)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .op(OpCode.LIST_APPEND, 2)
                .op(OpCode.LIST_APPEND, 1)
                .op(OpCode.LIST_APPEND, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(List.of(PythonInteger.valueOf(3), PythonInteger.valueOf(2), PythonInteger.valueOf(1)));
    }

    @Test
    public void testSetAdd() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .set(0)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .op(OpCode.SET_ADD, 2)
                .op(OpCode.SET_ADD, 1)
                .op(OpCode.SET_ADD, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Set.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2)));
    }

    @Test
    public void testListExtend() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .list(0)
                .loadConstant(1)
                .list(1)
                .loadConstant(2)
                .loadConstant(3)
                .list(2)
                .op(OpCode.LIST_EXTEND, 1)
                .op(OpCode.LIST_EXTEND, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(List.of(PythonInteger.valueOf(2), PythonInteger.valueOf(3), PythonInteger.valueOf(1)));
    }

    @Test
    public void testListToTuple() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .list(3)
                .op(OpCode.LIST_TO_TUPLE)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        Object out = javaFunction.get();
        assertThat(out).isInstanceOf(PythonLikeTuple.class);
        assertThat(out).asList().containsExactly(1, 2, 3);
    }

    @Test
    public void testUnpackSequence() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("sequence")
                .loadParameter("sequence")
                .op(OpCode.UNPACK_SEQUENCE, 3)
                .storeVariable("a")
                .storeVariable("b")
                .storeVariable("c")
                .loadVariable("a")
                .loadVariable("b")
                .loadVariable("c")
                .tuple(3)
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(List.of(1,2,3))).isEqualTo(List.of(1,2,3));
        assertThatCode(() -> javaFunction.apply(List.of(1,2))).hasMessage("not enough values to unpack (expected 3, got 2)");
        assertThatCode(() -> javaFunction.apply(List.of(1,2,3,4))).hasMessage("too many values to unpack (expected 3)");
    }

    @Test
    public void testUnpackSequenceWithTail() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("sequence")
                .loadParameter("sequence")
                .op(OpCode.UNPACK_EX, 3)
                .storeVariable("a")
                .storeVariable("b")
                .storeVariable("c")
                .storeVariable("tail")
                .loadVariable("tail")
                .loadVariable("a")
                .op(OpCode.LIST_APPEND, 0)
                .loadVariable("b")
                .op(OpCode.LIST_APPEND, 0)
                .loadVariable("c")
                .op(OpCode.LIST_APPEND, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(List.of(1,2,3))).isEqualTo(List.of(1,2,3));
        assertThatCode(() -> javaFunction.apply(List.of(1,2))).hasMessage("not enough values to unpack (expected 3, got 2)");
        assertThat(javaFunction.apply(List.of(1, 2, 3, 4, 5))).isEqualTo(List.of(4, 5, 1, 2, 3));
    }
    @Test
    public void testBuildString() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant("My name")
                .loadConstant(" is ")
                .loadConstant("awesome!")
                .op(OpCode.BUILD_STRING, 3)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo("My name is awesome!");
    }


    @Test
    public void testSetUpdate() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .set(0)
                .loadConstant(1)
                .loadConstant(2)
                .set(2)
                .loadConstant(2)
                .loadConstant(3)
                .set(2)
                .op(OpCode.SET_UPDATE, 1)
                .op(OpCode.SET_UPDATE, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Set.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2), PythonInteger.valueOf(3)));
    }

    @Test
    public void testMapAdd() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .dict(0)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(3)
                .loadConstant(6)
                .op(OpCode.MAP_ADD, 5)
                .op(OpCode.MAP_ADD, 3)
                .op(OpCode.MAP_ADD, 1)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                                                        PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                                                        PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testMapUpdate() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .dict(0)
                .loadConstant(1)
                .loadConstant(2)
                .dict(1)
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(3)
                .loadConstant(6)
                .dict(2)
                .op(OpCode.DICT_UPDATE, 1)
                .op(OpCode.DICT_UPDATE, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                                                        PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                                                        PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testMapMergeNoDuplicates() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .dict(0)
                .loadConstant(1)
                .loadConstant(2)
                .dict(1)
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(3)
                .loadConstant(6)
                .dict(2)
                .op(OpCode.DICT_MERGE, 1)
                .op(OpCode.DICT_MERGE, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                                                        PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                                                        PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testMapMergeDuplicates() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .dict(0)
                .loadConstant(1)
                .loadConstant(2)
                .dict(1)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .loadConstant(4)
                .dict(2)
                .op(OpCode.DICT_MERGE, 1)
                .op(OpCode.DICT_MERGE, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThatCode(javaFunction::get).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testMap() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(3)
                .loadConstant(6)
                .dict(3)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                                                        PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                                                        PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testConstKeyMap() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(6)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .constDict(3)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                                                        PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                                                        PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testSetItem() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("key", "value")
                .dict(0)
                .op(OpCode.DUP_TOP)
                .loadParameter("value")
                .op(OpCode.ROT_TWO)
                .loadParameter("key")
                .op(OpCode.STORE_SUBSCR)
                .op(OpCode.RETURN_VALUE)
                .build();

        BiFunction javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply(1, 2)).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2)));

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("key", "value")
                .loadConstant(1)
                .loadConstant(2)
                .list(2)
                .op(OpCode.DUP_TOP)
                .loadParameter("value")
                .op(OpCode.ROT_TWO)
                .loadParameter("key")
                .op(OpCode.STORE_SUBSCR)
                .op(OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply(1, 0)).isEqualTo(List.of(PythonInteger.valueOf(1), PythonInteger.valueOf(0)));
    }

    @Test
    public void testGetAttribute() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .getAttribute("name")
                .op(OpCode.RETURN_VALUE)
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
                .op(OpCode.RETURN_VALUE)
                .build();

        BiConsumer javaFunction = translatePythonBytecode(pythonCompiledFunction, BiConsumer.class);
        MyObject object = new MyObject();
        object.name = "My name";
        javaFunction.accept(object,"New name");
        assertThat(object.name).isEqualTo("New name");
    }

    @Test
    public void testCallFunction() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .getAttribute("concatToName")
                .loadConstant(" is awesome!")
                .callFunction(1)
                .op(OpCode.RETURN_VALUE)
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
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        PythonLikeFunction pythonLikeFunction = new JavaMethodReference(PythonBytecodeToJavaBytecodeTranslatorTest.class.getMethod("keywordTestFunction", int.class, int.class, int.class),
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
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        PythonLikeFunction pythonLikeFunction = new JavaMethodReference(PythonBytecodeToJavaBytecodeTranslatorTest.class.getMethod("keywordTestFunction", int.class, int.class, int.class),
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
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        PythonLikeFunction pythonLikeFunction = new JavaMethodReference(PythonBytecodeToJavaBytecodeTranslatorTest.class.getMethod("keywordTestFunction", int.class, int.class, int.class),
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
                .op(OpCode.RETURN_VALUE)
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
                .op(OpCode.RETURN_VALUE)
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
                .op(OpCode.RETURN_VALUE)
                .build();

        Class<?> dependentFunctionClass = translatePythonBytecodeToClass(dependentFunction, PythonLikeFunction.class);

        PythonCompiledFunction parentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(0)
                .storeCellVariable("a")
                .op(OpCode.LOAD_CLOSURE, 0)
                .tuple(1)
                .loadConstant(dependentFunctionClass)
                .loadConstant("parent.sub")
                .op(OpCode.MAKE_FUNCTION, 8)
                .storeVariable("sub")
                .loadConstant(1)
                .storeCellVariable("a")
                .loadVariable("sub")
                .callFunction(0)
                .op(OpCode.RETURN_VALUE)
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
                .op(OpCode.BINARY_ADD)
                .op(OpCode.BINARY_ADD)
                .op(OpCode.RETURN_VALUE)
                .build();

        Class<?> secondDependentFunctionClass = translatePythonBytecodeToClass(secondDependentFunction, PythonLikeFunction.class);

        PythonCompiledFunction firstDependentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .storeCellVariable("sub1")
                .loadConstant(100)
                .storeCellVariable("sub2")
                .loadFreeVariable("parent")
                .op(OpCode.POP_TOP)
                .op(OpCode.LOAD_CLOSURE, 0)
                .op(OpCode.LOAD_CLOSURE, 1)
                .op(OpCode.LOAD_CLOSURE, 2)
                .tuple(3)
                .loadConstant(secondDependentFunctionClass)
                .loadConstant("parent.sub.sub")
                .op(OpCode.MAKE_FUNCTION, 8)
                .storeVariable("function")
                .loadConstant(300)
                .storeCellVariable("sub2")
                .loadVariable("function")
                .callFunction(0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Class<?> firstDependentFunctionClass = translatePythonBytecodeToClass(firstDependentFunction, PythonLikeFunction.class);

        PythonCompiledFunction parentFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(10)
                .storeCellVariable("parent")
                .op(OpCode.LOAD_CLOSURE, 0)
                .tuple(1)
                .loadConstant(firstDependentFunctionClass)
                .loadConstant("parent.sub")
                .op(OpCode.MAKE_FUNCTION, 8)
                .storeVariable("sub")
                .loadConstant(20)
                .storeCellVariable("parent")
                .loadVariable("sub")
                .callFunction(0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(parentFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(321);
    }

    @Test
    public void testFormat() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item", "format")
                .loadParameter("item")
                .loadParameter("format")
                .op(OpCode.FORMAT_VALUE, 4)
                .op(OpCode.RETURN_VALUE)
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
                .op(OpCode.FORMAT_VALUE, 0)
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);

        assertThat(javaFunction.apply(12)).isEqualTo("12");
        assertThat(javaFunction.apply("hello")).isEqualTo("hello");
    }

    private static class PythonFunctionBuilder {
        List<PythonBytecodeInstruction> instructionList = new ArrayList<>();
        List<String> co_names = new ArrayList<>();
        List<String> co_varnames = new ArrayList<>();

        List<String> co_cellvars = new ArrayList<>();
        List<String> co_freevars = new ArrayList<>();
        List<Object> co_consts = new ArrayList<>();

        int co_argcount = 0;
        int co_kwonlyargcount = 0;

        public static PythonFunctionBuilder newFunction(String... parameters) {
            PythonFunctionBuilder out = new PythonFunctionBuilder();
            out.co_varnames.addAll(Arrays.asList(parameters));
            out.co_names.addAll(out.co_varnames);
            out.co_argcount = parameters.length;
            out.co_kwonlyargcount = 0;
            return out;
        }

        public PythonCompiledFunction build() {
            PythonCompiledFunction out = new PythonCompiledFunction();
            out.instructionList = instructionList;
            out.co_constants = co_consts;
            out.co_varnames = co_varnames;
            out.co_names = co_names;
            out.co_argcount = co_argcount;
            out.co_kwonlyargcount = co_kwonlyargcount;
            out.co_cellvars = co_cellvars;
            out.co_freevars = co_freevars;
            return out;
        }

        public PythonFunctionBuilder op(OpCode opcode) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = opcode;
            instruction.offset = instructionList.size();
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder op(OpCode opcode, int arg) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = opcode;
            instruction.offset = instructionList.size();
            instruction.arg = arg;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder loop(Consumer<PythonFunctionBuilder> blockBuilder) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.FOR_ITER;
            instruction.offset = instructionList.size();
            instruction.isJumpTarget = true;
            instructionList.add(instruction);

            blockBuilder.accept(this);

            PythonBytecodeInstruction jumpBackInstruction = new PythonBytecodeInstruction();
            jumpBackInstruction.opcode = OpCode.JUMP_ABSOLUTE;
            jumpBackInstruction.offset = instructionList.size();
            jumpBackInstruction.arg = instruction.offset;
            jumpBackInstruction.isJumpTarget = true;
            instructionList.add(jumpBackInstruction);

            instruction.arg = instructionList.size() - instruction.offset;

            PythonBytecodeInstruction afterLoopInstruction = new PythonBytecodeInstruction();
            afterLoopInstruction.opcode = OpCode.NOP;
            afterLoopInstruction.offset = instructionList.size();
            afterLoopInstruction.isJumpTarget = true;
            instructionList.add(afterLoopInstruction);

            return this;
        }

        public PythonFunctionBuilder ifTrue(Consumer<PythonFunctionBuilder> blockBuilder) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.POP_JUMP_IF_FALSE; // Skip block if False (i.e. enter block if True)
            instruction.offset = instructionList.size();
            instructionList.add(instruction);

            blockBuilder.accept(this);

            PythonBytecodeInstruction afterIfInstruction = new PythonBytecodeInstruction();
            afterIfInstruction.opcode = OpCode.NOP;
            afterIfInstruction.offset = instructionList.size();
            afterIfInstruction.isJumpTarget = true;
            instructionList.add(afterIfInstruction);

            instruction.arg = afterIfInstruction.offset;

            return this;
        }

        public PythonFunctionBuilder ifFalse(Consumer<PythonFunctionBuilder> blockBuilder) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.POP_JUMP_IF_TRUE; // Skip block if True (i.e. enter block if False)
            instruction.offset = instructionList.size();
            instructionList.add(instruction);

            blockBuilder.accept(this);

            PythonBytecodeInstruction afterIfInstruction = new PythonBytecodeInstruction();
            afterIfInstruction.opcode = OpCode.NOP;
            afterIfInstruction.offset = instructionList.size();
            afterIfInstruction.isJumpTarget = true;
            instructionList.add(afterIfInstruction);

            instruction.arg = afterIfInstruction.offset;

            return this;
        }

        public PythonFunctionBuilder ifTruePopTop(Consumer<PythonFunctionBuilder> blockBuilder) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.JUMP_IF_FALSE_OR_POP; // Skip block if False (i.e. enter block if True)
            instruction.offset = instructionList.size();
            instructionList.add(instruction);

            blockBuilder.accept(this);

            PythonBytecodeInstruction afterIfInstruction = new PythonBytecodeInstruction();
            afterIfInstruction.opcode = OpCode.NOP;
            afterIfInstruction.offset = instructionList.size();
            afterIfInstruction.isJumpTarget = true;
            instructionList.add(afterIfInstruction);

            instruction.arg = afterIfInstruction.offset;

            return this;
        }

        public PythonFunctionBuilder ifFalsePopTop(Consumer<PythonFunctionBuilder> blockBuilder) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.JUMP_IF_TRUE_OR_POP; // Skip block if True (i.e. enter block if False)
            instruction.offset = instructionList.size();
            instructionList.add(instruction);

            blockBuilder.accept(this);

            PythonBytecodeInstruction afterIfInstruction = new PythonBytecodeInstruction();
            afterIfInstruction.opcode = OpCode.NOP;
            afterIfInstruction.offset = instructionList.size();
            afterIfInstruction.isJumpTarget = true;
            instructionList.add(afterIfInstruction);

            instruction.arg = afterIfInstruction.offset;

            return this;
        }

        public PythonFunctionBuilder list(int count) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.BUILD_LIST;
            instruction.offset = instructionList.size();
            instruction.arg = count;
            instruction.argval = count;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder tuple(int count) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.BUILD_TUPLE;
            instruction.offset = instructionList.size();
            instruction.arg = count;
            instruction.argval = count;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder dict(int count) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.BUILD_MAP;
            instruction.offset = instructionList.size();
            instruction.arg = count;
            instruction.argval = count;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder constDict(int count) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.BUILD_CONST_KEY_MAP;
            instruction.offset = instructionList.size();
            instruction.arg = count;
            instruction.argval = count;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder set(int count) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.BUILD_SET;
            instruction.offset = instructionList.size();
            instruction.arg = count;
            instruction.argval = count;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder callFunction(int argc) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.CALL_FUNCTION;
            instruction.offset = instructionList.size();
            instruction.arg = argc;
            instruction.argval = argc;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder callFunctionWithKeywords(int argc) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.CALL_FUNCTION_KW;
            instruction.offset = instructionList.size();
            instruction.arg = argc;
            instruction.argval = argc;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder callFunctionUnpack(boolean hasKeywords) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.CALL_FUNCTION_EX;
            instruction.offset = instructionList.size();
            instruction.arg = (hasKeywords)? 1 : 0;
            instruction.argval = instruction.arg;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder loadMethod(String methodName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.LOAD_METHOD;
            instruction.offset = instructionList.size();

            int methodIndex = co_names.indexOf(methodName);
            if (methodIndex == -1) {
                methodIndex = co_names.size();
                co_names.add(methodName);
            }

            instruction.arg = methodIndex;
            instruction.argval = methodIndex;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder callMethod(int argc) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.CALL_METHOD;
            instruction.offset = instructionList.size();
            instruction.arg = argc;
            instruction.argval = argc;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder getAttribute(String attributeName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.LOAD_ATTR;
            instruction.offset = instructionList.size();

            int attributeIndex = co_names.indexOf(attributeName);
            if (attributeIndex == -1) {
                attributeIndex = co_names.size();
                co_names.add(attributeName);
            }

            instruction.arg = attributeIndex;
            instruction.argval = attributeIndex;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder storeAttribute(String attributeName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.STORE_ATTR;
            instruction.offset = instructionList.size();

            int attributeIndex = co_names.indexOf(attributeName);
            if (attributeIndex == -1) {
                attributeIndex = co_names.size();
                co_names.add(attributeName);
            }

            instruction.arg = attributeIndex;
            instruction.argval = attributeIndex;
            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder loadConstant(Object constant) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.LOAD_CONST;
            instruction.offset = instructionList.size();

            int index = co_consts.indexOf(constant);
            if (index == -1) {
                index = co_consts.size();
                co_consts.add(JavaPythonTypeConversionImplementor.wrapJavaObject(constant));
            }

            instruction.arg = index;

            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder loadParameter(String parameterName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.LOAD_FAST;
            instruction.offset = instructionList.size();
            instruction.arg = co_varnames.indexOf(parameterName);

            if (instruction.arg == -1) {
                throw new IllegalArgumentException("Parameter (" + parameterName + ") is not in the parameter list (" +
                        co_varnames + ").");
            }

            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder loadVariable(String variableName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.LOAD_FAST;
            instruction.offset = instructionList.size();
            instruction.arg = co_varnames.indexOf(variableName);

            if (instruction.arg == -1) {
                co_varnames.add(variableName);
                instruction.arg = co_varnames.size() - 1;
            }

            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder storeVariable(String variableName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.STORE_FAST;
            instruction.offset = instructionList.size();
            instruction.arg = co_varnames.indexOf(variableName);

            if (instruction.arg == -1) {
                co_varnames.add(variableName);
                instruction.arg = co_varnames.size() - 1;
            }

            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder loadCellVariable(String variableName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.LOAD_DEREF;
            instruction.offset = instructionList.size();
            instruction.arg = co_cellvars.indexOf(variableName);

            if (instruction.arg == -1) {
                co_cellvars.add(variableName);
                instruction.arg = co_cellvars.size() - 1;
            }

            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder storeCellVariable(String variableName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.STORE_DEREF;
            instruction.offset = instructionList.size();
            instruction.arg = co_cellvars.indexOf(variableName);

            if (instruction.arg == -1) {
                co_cellvars.add(variableName);
                instruction.arg = co_cellvars.size() - 1;
            }

            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder loadFreeVariable(String variableName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.LOAD_DEREF;
            instruction.offset = instructionList.size();
            instruction.arg = co_freevars.indexOf(variableName);

            if (instruction.arg == -1) {
                co_freevars.add(variableName);
                instruction.arg = co_freevars.size() - 1;
            }

            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder storeFreeVariable(String variableName) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.STORE_DEREF;
            instruction.offset = instructionList.size();
            instruction.arg = co_freevars.indexOf(variableName);

            if (instruction.arg == -1) {
                co_freevars.add(variableName);
                instruction.arg = co_freevars.size() - 1;
            }

            instructionList.add(instruction);
            return this;
        }

        public PythonFunctionBuilder compare(CompareOp compareOp) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.COMPARE_OP;
            instruction.offset = instructionList.size();
            instruction.arg = compareOp.id;

            instructionList.add(instruction);
            return this;
        }
    }
}
