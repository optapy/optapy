package org.optaplanner.optapy.translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.optaplanner.optapy.translator.PythonBytecodeInstruction.OpCode;
import static org.optaplanner.optapy.translator.PythonBytecodeToJavaBytecodeTranslator.CompareOp;
import static org.optaplanner.optapy.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.LongToIntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.optaplanner.optapy.translator.types.PythonLikeType;

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

        public PythonFunctionBuilder tuple(int count) {
            PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
            instruction.opcode = OpCode.BUILD_TUPLE;
            instruction.offset = instructionList.size();
            instruction.arg = count;
            instruction.argval = count;
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
