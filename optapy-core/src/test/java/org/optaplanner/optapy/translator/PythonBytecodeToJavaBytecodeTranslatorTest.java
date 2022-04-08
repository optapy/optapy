package org.optaplanner.optapy.translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.optaplanner.optapy.translator.PythonBytecodeInstruction.OpCode;

import static org.optaplanner.optapy.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;

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
import org.optaplanner.optapy.translator.types.PythonBoolean;
import org.optaplanner.optapy.translator.types.PythonInteger;

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

        BiFunction javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        return javaFunction;
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
                .loadParameter("item")
                .loadConstant(" is awesome!")
                .callFunction(2)
                .op(OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        MyObject object = new MyObject();
        object.name = "My name";
        assertThat(javaFunction.apply(object)).isEqualTo("My name is awesome!");
    }

    private static class PythonFunctionBuilder {
        List<PythonBytecodeInstruction> instructionList = new ArrayList<>();
        List<String> co_names = new ArrayList<>();
        List<String> co_varnames = new ArrayList<>();
        List<Object> co_consts = new ArrayList<>();

        public static PythonFunctionBuilder newFunction(String... parameters) {
            PythonFunctionBuilder out = new PythonFunctionBuilder();
            out.co_varnames.addAll(Arrays.asList(parameters));
            out.co_names.addAll(out.co_varnames);
            return out;
        }

        public PythonCompiledFunction build() {
            PythonCompiledFunction out = new PythonCompiledFunction();
            out.instructionList = instructionList;
            out.co_constants = co_consts;
            out.co_varnames = co_varnames;
            out.co_names = co_names;
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
                co_consts.add(constant);
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
