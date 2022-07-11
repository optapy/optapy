package org.optaplanner.python.translator.dag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.types.PythonBoolean.BOOLEAN_TYPE;
import static org.optaplanner.python.translator.types.PythonInteger.INT_TYPE;
import static org.optaplanner.python.translator.types.PythonIterator.ITERATOR_TYPE;
import static org.optaplanner.python.translator.types.PythonLikeTuple.TUPLE_TYPE;
import static org.optaplanner.python.translator.types.PythonLikeType.TYPE_TYPE;
import static org.optaplanner.python.translator.types.PythonNone.NONE_TYPE;
import static org.optaplanner.python.translator.types.PythonString.STRING_TYPE;
import static org.optaplanner.python.translator.types.errors.PythonAssertionError.ASSERTION_ERROR_TYPE;
import static org.optaplanner.python.translator.types.errors.PythonBaseException.BASE_EXCEPTION_TYPE;
import static org.optaplanner.python.translator.types.errors.PythonTraceback.TRACEBACK_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.CompareOp;
import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.opcodes.Opcode;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.errors.PythonAssertionError;
import org.optaplanner.python.translator.types.errors.StopIteration;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

public class FlowGraphTest {

    private static PythonLikeType OBJECT_TYPE = PythonLikeType.getBaseType();

    static FlowGraph getFlowGraph(FunctionMetadata functionMetadata, StackMetadata initialStackMetadata,
            PythonCompiledFunction function) {
        List<Opcode> out = new ArrayList<>(function.instructionList.size());
        for (PythonBytecodeInstruction instruction : function.instructionList) {
            out.add(Opcode.lookupOpcodeForInstruction(instruction, Integer.MAX_VALUE));
        }
        return FlowGraph.createFlowGraph(functionMetadata, initialStackMetadata, out);
    }

    static FunctionMetadata getFunctionMetadata(PythonCompiledFunction function) {
        FunctionMetadata out = new FunctionMetadata();
        out.className = FlowGraphTest.class.getName();
        out.pythonCompiledFunction = function;
        out.bytecodeCounterToLabelMap = new HashMap<>();
        out.bytecodeCounterToCodeArgumenterList = new HashMap<>();
        return out;
    }

    static StackMetadata getInitialStackMetadata(int locals, int cells) {
        StackMetadata initialStackMetadata = new StackMetadata();
        initialStackMetadata.stackValueSources = new ArrayList<>();
        initialStackMetadata.localVariableValueSources = new ArrayList<>(locals);
        initialStackMetadata.cellVariableValueSources = new ArrayList<>(cells);

        for (int i = 0; i < locals; i++) {
            initialStackMetadata.localVariableValueSources.add(null);
        }

        for (int i = 0; i < cells; i++) {
            initialStackMetadata.cellVariableValueSources.add(null);
        }

        return initialStackMetadata;
    }

    @Test
    public void testStackMetadataForBasicOps() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant("Hi")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.ROT_TWO)
                .tuple(2)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(0, 0);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = flowGraph.getStackMetadataForOperations()
                .stream().map(FrameData::from).collect(Collectors.toList());

        assertThat(stackMetadataList).containsExactly(
                new FrameData(),
                new FrameData().stack(INT_TYPE),
                new FrameData().stack(INT_TYPE, STRING_TYPE),
                new FrameData().stack(STRING_TYPE, INT_TYPE),
                new FrameData().stack(TUPLE_TYPE));
    }

    @Test
    public void testStackMetadataForLocalVariables() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .storeVariable("one")
                .loadConstant("2")
                .storeVariable("two")
                .loadVariable("one")
                .loadVariable("two")
                .tuple(2)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(2, 0);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = flowGraph.getStackMetadataForOperations()
                .stream().map(FrameData::from).collect(Collectors.toList());

        assertThat(stackMetadataList).containsExactly(
                new FrameData().locals(null, null),
                new FrameData().stack(INT_TYPE).locals(null, null),
                new FrameData().stack().locals(INT_TYPE, null),
                new FrameData().stack(STRING_TYPE).locals(INT_TYPE, null),
                new FrameData().stack().locals(INT_TYPE, STRING_TYPE),
                new FrameData().stack(INT_TYPE).locals(INT_TYPE, STRING_TYPE),
                new FrameData().stack(INT_TYPE, STRING_TYPE).locals(INT_TYPE, STRING_TYPE),
                new FrameData().stack(TUPLE_TYPE).locals(INT_TYPE, STRING_TYPE));
    }

    @Test
    public void testStackMetadataForLoops() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(0)
                .storeVariable("sum")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.GET_ITER)
                .loop(block -> {
                    block.loadVariable("sum");
                    block.op(PythonBytecodeInstruction.OpcodeIdentifier.BINARY_ADD);
                    block.storeVariable("sum");
                })
                .loadVariable("sum")
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(1, 0);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = flowGraph.getStackMetadataForOperations()
                .stream().map(FrameData::from).collect(Collectors.toList());

        assertThat(stackMetadataList).containsExactly(
                new FrameData().locals((PythonLikeType) null), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE).locals((PythonLikeType) null), // STORE
                new FrameData().stack().locals(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE).locals(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE, INT_TYPE).locals(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE, INT_TYPE, INT_TYPE).locals(INT_TYPE), // TUPLE(3)

                // Type information is lost because Tuple is not generic
                new FrameData().stack(TUPLE_TYPE).locals(INT_TYPE), // ITERATOR
                new FrameData().stack(ITERATOR_TYPE).locals(OBJECT_TYPE), // NEXT
                new FrameData().stack(ITERATOR_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // LOAD_VAR
                new FrameData().stack(ITERATOR_TYPE, OBJECT_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // ADD
                new FrameData().stack(ITERATOR_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // STORE
                new FrameData().stack(ITERATOR_TYPE).locals(OBJECT_TYPE), // JUMP_ABS
                new FrameData().stack().locals(OBJECT_TYPE), // NOP
                new FrameData().stack().locals(OBJECT_TYPE), // LOAD_VAR
                new FrameData().stack(OBJECT_TYPE).locals(OBJECT_TYPE) // RETURN
        );
    }

    @Test
    public void testStackMetadataForExceptions() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .tryCode(code -> {
                    code.loadConstant(5)
                            .loadConstant(5)
                            .compare(CompareOp.LESS_THAN)
                            .ifTrue(block -> {
                                block.loadConstant("Try").op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE);
                            })
                            .op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_ASSERTION_ERROR)
                            .op(PythonBytecodeInstruction.OpcodeIdentifier.RAISE_VARARGS, 1);
                }, true)
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE);
                }, true)
                .tryEnd()
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(0, 0);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = flowGraph.getStackMetadataForOperations()
                .stream().map(FrameData::from).collect(Collectors.toList());

        assertThat(stackMetadataList).containsExactly(
                new FrameData().stack(), // SETUP_TRY
                new FrameData().stack(), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE, INT_TYPE), // COMPARE
                new FrameData().stack(BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                new FrameData().stack(), // LOAD_CONSTANT
                new FrameData().stack(STRING_TYPE), // RETURN
                new FrameData().stack(), // NOP
                new FrameData().stack(), // LOAD_ASSERTION_ERROR
                new FrameData().stack(ASSERTION_ERROR_TYPE), // RAISE
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // except handler; DUP_TOP,
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE,
                        TYPE_TYPE), // LOAD_CONSTANT
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, TYPE_TYPE,
                        TYPE_TYPE), // JUMP_IF_NOT_EXC_MATCH
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE), // POP_EXCEPT
                new FrameData().stack(), // LOAD_CONSTANT
                new FrameData().stack(STRING_TYPE), // RETURN
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE,
                        TYPE_TYPE) // RAISE
        );
    }

    @Test
    public void testStackMetadataForTryFinally() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .tryCode(code -> {
                    code.loadConstant(1)
                            .loadConstant(1)
                            .compare(CompareOp.EQUALS)
                            .ifTrue(block -> {
                                block.op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_ASSERTION_ERROR)
                                        .op(PythonBytecodeInstruction.OpcodeIdentifier.RAISE_VARARGS, 1);
                            })
                            .loadConstant(1)
                            .loadConstant(2)
                            .compare(CompareOp.EQUALS)
                            .ifTrue(block -> {
                                block.loadConstant(new StopIteration())
                                        .op(PythonBytecodeInstruction.OpcodeIdentifier.RAISE_VARARGS, 1);
                            });
                }, false)
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").storeGlobalVariable("exception");
                }, false)
                .andFinally(code -> {
                    code.loadConstant("Finally")
                            .storeGlobalVariable("finally");
                }, false)
                .tryEnd()
                .loadConstant(1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(0, 0);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = flowGraph.getStackMetadataForOperations()
                .stream().map(FrameData::from).collect(Collectors.toList());

        assertThat(stackMetadataList).containsExactly(
                new FrameData().stack(), // SETUP_TRY
                new FrameData().stack(), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE, INT_TYPE), // COMPARE
                new FrameData().stack(BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                new FrameData().stack(), // LOAD_ASSERTION_ERROR
                new FrameData().stack(ASSERTION_ERROR_TYPE), // RAISE
                new FrameData().stack(), // NOP
                new FrameData().stack(), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE, INT_TYPE), // COMPARE
                new FrameData().stack(BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                new FrameData().stack(), // LOAD_CONSTANT
                new FrameData().stack(StopIteration.STOP_ITERATION_TYPE), // RAISE
                new FrameData().stack(), // NOP
                new FrameData().stack(), // JUMP_ABSOLUTE
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // except handler; DUP_TOP,
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE,
                        TYPE_TYPE), // LOAD_CONSTANT
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, TYPE_TYPE,
                        TYPE_TYPE), // JUMP_IF_NOT_EXC_MATCH
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE), // POP_EXCEPT
                new FrameData().stack(), // LOAD_CONSTANT
                new FrameData().stack(STRING_TYPE), // STORE_GLOBAL
                new FrameData().stack(), // JUMP_ABSOLUTE
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE, BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE, TRACEBACK_TYPE), // POP_TOP
                new FrameData().stack(NONE_TYPE, INT_TYPE, NONE_TYPE), // POP_EXCEPT
                new FrameData().stack(), // FINALLY; Catch target; NO-OP
                new FrameData().stack(), // FINALLY; Load constant
                new FrameData().stack(STRING_TYPE), // STORE
                new FrameData().stack(), // RAISE
                new FrameData().stack(), // END_TRY; FINALLY; Load constant
                new FrameData().stack(STRING_TYPE), // STORE
                new FrameData().stack(), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE) // RETURN
        );
    }

    @Test
    public void testStackMetadataForIfStatementsThatExitEarly() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .storeVariable("a")
                .loadVariable("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifTrue(block -> {
                    block.loadConstant("10");
                    block.storeVariable("a");
                    block.loadVariable("a");
                    block.op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE);
                })
                .loadConstant(-10)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(1, 0);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = flowGraph.getStackMetadataForOperations()
                .stream().map(FrameData::from).collect(Collectors.toList());

        assertThat(stackMetadataList).containsExactly(
                new FrameData().stack().locals((PythonLikeType) null), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE).locals((PythonLikeType) null), // STORE
                new FrameData().stack().locals(INT_TYPE), // LOAD_VARIABLE
                new FrameData().stack(INT_TYPE).locals(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE, INT_TYPE).locals(INT_TYPE), // COMPARE_OP
                new FrameData().stack(BOOLEAN_TYPE).locals(INT_TYPE), // POP_JUMP_IF_TRUE
                new FrameData().stack().locals(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(STRING_TYPE).locals(INT_TYPE), // STORE
                new FrameData().stack().locals(STRING_TYPE), // LOAD_VARIABLE
                new FrameData().stack(STRING_TYPE).locals(STRING_TYPE), // RETURN
                new FrameData().stack().locals(INT_TYPE), // NOP
                new FrameData().stack().locals(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE).locals(INT_TYPE) // RETURN
        );
    }

    @Test
    public void testStackMetadataForIfStatementsThatDoNotExitEarly() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .storeVariable("a")
                .loadVariable("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifTrue(block -> {
                    block.loadConstant("10");
                    block.storeVariable("a");
                })
                .loadConstant(-10)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(1, 0);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = flowGraph.getStackMetadataForOperations()
                .stream().map(FrameData::from).collect(Collectors.toList());

        assertThat(stackMetadataList).containsExactly(
                new FrameData().stack().locals((PythonLikeType) null), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE).locals((PythonLikeType) null), // STORE
                new FrameData().stack().locals(INT_TYPE), // LOAD_VARIABLE
                new FrameData().stack(INT_TYPE).locals(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE, INT_TYPE).locals(INT_TYPE), // COMPARE_OP
                new FrameData().stack(BOOLEAN_TYPE).locals(INT_TYPE), // POP_JUMP_IF_TRUE
                new FrameData().stack().locals(INT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(STRING_TYPE).locals(INT_TYPE), // STORE
                new FrameData().stack().locals(OBJECT_TYPE), // NOP
                new FrameData().stack().locals(OBJECT_TYPE), // LOAD_CONSTANT
                new FrameData().stack(INT_TYPE).locals(OBJECT_TYPE) // RETURN
        );
    }

    private static class FrameData {
        List<PythonLikeType> stackTypes;
        List<PythonLikeType> localVariableTypes;
        List<PythonLikeType> cellTypes;

        public FrameData() {
            stackTypes = new ArrayList<>();
            localVariableTypes = new ArrayList<>();
            cellTypes = new ArrayList<>();
        }

        public static FrameData from(StackMetadata stackMetadata) {
            FrameData out = new FrameData();
            stackMetadata.stackValueSources.forEach(valueSourceInfo -> {
                if (valueSourceInfo != null) {
                    out.stackTypes.add(valueSourceInfo.getValueType());
                } else {
                    out.stackTypes.add(null);
                }
            });
            stackMetadata.localVariableValueSources.forEach(valueSourceInfo -> {
                if (valueSourceInfo != null) {
                    out.localVariableTypes.add(valueSourceInfo.getValueType());
                } else {
                    out.localVariableTypes.add(null);
                }
            });
            stackMetadata.cellVariableValueSources.forEach(valueSourceInfo -> {
                if (valueSourceInfo != null) {
                    out.cellTypes.add(valueSourceInfo.getValueType());
                } else {
                    out.cellTypes.add(null);
                }
            });
            return out;
        }

        public FrameData copy() {
            FrameData out = new FrameData();
            out.stackTypes.addAll(stackTypes);
            out.localVariableTypes.addAll(localVariableTypes);
            out.cellTypes.addAll(cellTypes);
            return out;
        }

        public FrameData stack(PythonLikeType... valueTypes) {
            FrameData out = copy();
            out.stackTypes.addAll(Arrays.asList(valueTypes));
            return out;
        }

        public FrameData locals(PythonLikeType... valueTypes) {
            FrameData out = copy();
            out.localVariableTypes.addAll(Arrays.asList(valueTypes));
            return out;
        }

        public FrameData cells(PythonLikeType... valueTypes) {
            FrameData out = copy();
            out.cellTypes.addAll(Arrays.asList(valueTypes));
            return out;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FrameData frameData = (FrameData) o;
            return Objects.equals(stackTypes, frameData.stackTypes)
                    && Objects.equals(localVariableTypes, frameData.localVariableTypes)
                    && Objects.equals(cellTypes, frameData.cellTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stackTypes, localVariableTypes, cellTypes);
        }

        @Override
        public String toString() {
            return "FrameData{" +
                    "stackTypes=" + stackTypes +
                    ", localVariableTypes=" + localVariableTypes +
                    ", cellTypes=" + cellTypes +
                    '}';
        }
    }
}
