package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonLikeTuple;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CollectionImplementorTest {

    @Test
    public void testGetIterator() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(PythonBytecodeInstruction.OpCode.GET_ITER)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.GET_ITER)
                .loop(block -> {
                    block.loadVariable("sum");
                    block.op(PythonBytecodeInstruction.OpCode.BINARY_ADD);
                    block.storeVariable("sum");
                })
                .loadVariable("sum")
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(6L);
    }

    @Test
    public void testContains() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(PythonBytecodeInstruction.OpCode.CONTAINS_OP, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.CONTAINS_OP, 1)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.LIST_APPEND, 2)
                .op(PythonBytecodeInstruction.OpCode.LIST_APPEND, 1)
                .op(PythonBytecodeInstruction.OpCode.LIST_APPEND, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get())
                .isEqualTo(List.of(PythonInteger.valueOf(3), PythonInteger.valueOf(2), PythonInteger.valueOf(1)));
    }

    @Test
    public void testSetAdd() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .set(0)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .op(PythonBytecodeInstruction.OpCode.SET_ADD, 2)
                .op(PythonBytecodeInstruction.OpCode.SET_ADD, 1)
                .op(PythonBytecodeInstruction.OpCode.SET_ADD, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.LIST_EXTEND, 1)
                .op(PythonBytecodeInstruction.OpCode.LIST_EXTEND, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get())
                .isEqualTo(List.of(PythonInteger.valueOf(2), PythonInteger.valueOf(3), PythonInteger.valueOf(1)));
    }

    @Test
    public void testListToTuple() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .list(3)
                .op(PythonBytecodeInstruction.OpCode.LIST_TO_TUPLE)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.UNPACK_SEQUENCE, 3)
                .storeVariable("a")
                .storeVariable("b")
                .storeVariable("c")
                .loadVariable("a")
                .loadVariable("b")
                .loadVariable("c")
                .tuple(3)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(List.of(1, 2, 3))).isEqualTo(List.of(1, 2, 3));
        assertThatCode(() -> javaFunction.apply(List.of(1, 2))).hasMessage("not enough values to unpack (expected 3, got 2)");
        assertThatCode(() -> javaFunction.apply(List.of(1, 2, 3, 4))).hasMessage("too many values to unpack (expected 3)");
    }

    @Test
    public void testUnpackSequenceWithTail() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("sequence")
                .loadParameter("sequence")
                .op(PythonBytecodeInstruction.OpCode.UNPACK_EX, 3)
                .storeVariable("a")
                .storeVariable("b")
                .storeVariable("c")
                .storeVariable("tail")
                .loadVariable("tail")
                .loadVariable("a")
                .op(PythonBytecodeInstruction.OpCode.LIST_APPEND, 0)
                .loadVariable("b")
                .op(PythonBytecodeInstruction.OpCode.LIST_APPEND, 0)
                .loadVariable("c")
                .op(PythonBytecodeInstruction.OpCode.LIST_APPEND, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(List.of(1, 2, 3))).isEqualTo(List.of(1, 2, 3));
        assertThatCode(() -> javaFunction.apply(List.of(1, 2))).hasMessage("not enough values to unpack (expected 3, got 2)");
        assertThat(javaFunction.apply(List.of(1, 2, 3, 4, 5))).isEqualTo(List.of(4, 5, 1, 2, 3));
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
                .op(PythonBytecodeInstruction.OpCode.SET_UPDATE, 1)
                .op(PythonBytecodeInstruction.OpCode.SET_UPDATE, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        Supplier javaFunction = translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get())
                .isEqualTo(Set.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2), PythonInteger.valueOf(3)));
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
                .op(PythonBytecodeInstruction.OpCode.MAP_ADD, 5)
                .op(PythonBytecodeInstruction.OpCode.MAP_ADD, 3)
                .op(PythonBytecodeInstruction.OpCode.MAP_ADD, 1)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.DICT_UPDATE, 1)
                .op(PythonBytecodeInstruction.OpCode.DICT_UPDATE, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.DICT_MERGE, 1)
                .op(PythonBytecodeInstruction.OpCode.DICT_MERGE, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.DICT_MERGE, 1)
                .op(PythonBytecodeInstruction.OpCode.DICT_MERGE, 0)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
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
                .op(PythonBytecodeInstruction.OpCode.DUP_TOP)
                .loadParameter("value")
                .op(PythonBytecodeInstruction.OpCode.ROT_TWO)
                .loadParameter("key")
                .op(PythonBytecodeInstruction.OpCode.STORE_SUBSCR)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        BiFunction javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply(1, 2)).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2)));

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("key", "value")
                .loadConstant(1)
                .loadConstant(2)
                .list(2)
                .op(PythonBytecodeInstruction.OpCode.DUP_TOP)
                .loadParameter("value")
                .op(PythonBytecodeInstruction.OpCode.ROT_TWO)
                .loadParameter("key")
                .op(PythonBytecodeInstruction.OpCode.STORE_SUBSCR)
                .op(PythonBytecodeInstruction.OpCode.RETURN_VALUE)
                .build();

        javaFunction = translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply(1, 0)).isEqualTo(List.of(PythonInteger.valueOf(1), PythonInteger.valueOf(0)));
    }
}
