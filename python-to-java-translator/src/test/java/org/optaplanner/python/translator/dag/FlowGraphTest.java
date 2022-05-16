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
import java.util.HashMap;
import java.util.List;

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

    static FlowGraph getFlowGraph(PythonCompiledFunction function) {
        List<Opcode> out = new ArrayList<>(function.instructionList.size());
        for (PythonBytecodeInstruction instruction : function.instructionList) {
            out.add(Opcode.lookupOpcodeForInstruction(instruction, Integer.MAX_VALUE));
        }
        return FlowGraph.createFlowGraph(out);
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
        initialStackMetadata.stackTypes = new ArrayList<>();
        initialStackMetadata.localVariableTypes = new ArrayList<>(locals);
        initialStackMetadata.cellVariableTypes = new ArrayList<>(cells);

        for (int i = 0; i < locals; i++) {
            initialStackMetadata.localVariableTypes.add(null);
        }

        for (int i = 0; i < cells; i++) {
            initialStackMetadata.cellVariableTypes.add(null);
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

        FlowGraph flowGraph = getFlowGraph(pythonCompiledFunction);
        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(0, 0);
        List<StackMetadata> stackMetadataList = flowGraph.getStackMetadataForOperations(functionMetadata, metadata);

        assertThat(stackMetadataList).containsExactly(
                metadata,
                metadata.stack(INT_TYPE),
                metadata.stack(INT_TYPE, STRING_TYPE),
                metadata.stack(STRING_TYPE, INT_TYPE),
                metadata.stack(TUPLE_TYPE));
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

        FlowGraph flowGraph = getFlowGraph(pythonCompiledFunction);
        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(2, 0);
        List<StackMetadata> stackMetadataList = flowGraph.getStackMetadataForOperations(functionMetadata, metadata);

        assertThat(stackMetadataList).containsExactly(
                metadata,
                metadata.stack(INT_TYPE).locals(null, null),
                metadata.stack().locals(INT_TYPE, null),
                metadata.stack(STRING_TYPE).locals(INT_TYPE, null),
                metadata.stack().locals(INT_TYPE, STRING_TYPE),
                metadata.stack(INT_TYPE).locals(INT_TYPE, STRING_TYPE),
                metadata.stack(INT_TYPE, STRING_TYPE).locals(INT_TYPE, STRING_TYPE),
                metadata.stack(TUPLE_TYPE).locals(INT_TYPE, STRING_TYPE));
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

        FlowGraph flowGraph = getFlowGraph(pythonCompiledFunction);
        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(1, 0);
        List<StackMetadata> stackMetadataList = flowGraph.getStackMetadataForOperations(functionMetadata, metadata);

        assertThat(stackMetadataList).containsExactly(
                metadata, // LOAD_CONSTANT
                metadata.stack(INT_TYPE), // STORE
                metadata.stack().locals(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE).locals(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE, INT_TYPE).locals(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE, INT_TYPE, INT_TYPE).locals(INT_TYPE), // TUPLE(3)

                // Type information is lost because Tuple is not generic
                metadata.stack(TUPLE_TYPE).locals(INT_TYPE), // ITERATOR
                metadata.stack(ITERATOR_TYPE).locals(OBJECT_TYPE), // NEXT
                metadata.stack(ITERATOR_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // LOAD_VAR
                metadata.stack(ITERATOR_TYPE, OBJECT_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // ADD
                metadata.stack(ITERATOR_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // STORE
                metadata.stack(ITERATOR_TYPE).locals(OBJECT_TYPE), // JUMP_ABS
                metadata.stack().locals(OBJECT_TYPE), // NOP
                metadata.stack().locals(OBJECT_TYPE), // LOAD_VAR
                metadata.stack(OBJECT_TYPE).locals(OBJECT_TYPE) // RETURN
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
                })
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE);
                })
                .tryEnd()
                .loadConstant(null)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        FlowGraph flowGraph = getFlowGraph(pythonCompiledFunction);
        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(0, 0);
        List<StackMetadata> stackMetadataList = flowGraph.getStackMetadataForOperations(functionMetadata, metadata);

        assertThat(stackMetadataList).containsExactly(
                metadata.stack(), // SETUP_TRY
                metadata.stack(), // LOAD_CONSTANT
                metadata.stack(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE, INT_TYPE), // COMPARE
                metadata.stack(BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                metadata.stack(), // LOAD_CONSTANT
                metadata.stack(STRING_TYPE), // RETURN
                metadata.stack(), // NOP
                metadata.stack(), // LOAD_ASSERTION_ERROR
                metadata.stack(ASSERTION_ERROR_TYPE), // RAISE
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // JUMP_ABSOLUTE
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // except handler; DUP_TOP,
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, TYPE_TYPE), // LOAD_CONSTANT
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, TYPE_TYPE, TYPE_TYPE), // JUMP_IF_NOT_EXC_MATCH
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // LOAD_CONSTANT
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, STRING_TYPE), // RETURN
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // POP_EXCEPT
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // RAISE
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // NOP
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // END_TRY, LOAD_CONSTANT
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, NONE_TYPE) // RETURN
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
                })
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").storeGlobalVariable("exception");
                })
                .andFinally(code -> {
                    code.loadConstant("Finally")
                            .storeGlobalVariable("finally");
                })
                .tryEnd()
                .loadConstant(1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
                .build();

        FlowGraph flowGraph = getFlowGraph(pythonCompiledFunction);
        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(0, 0);
        List<StackMetadata> stackMetadataList = flowGraph.getStackMetadataForOperations(functionMetadata, metadata);

        assertThat(stackMetadataList).containsExactly(
                metadata.stack(), // SETUP_TRY
                metadata.stack(), // LOAD_CONSTANT
                metadata.stack(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE, INT_TYPE), // COMPARE
                metadata.stack(BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                metadata.stack(), // LOAD_ASSERTION_ERROR
                metadata.stack(ASSERTION_ERROR_TYPE), // RAISE
                metadata.stack(), // NOP
                metadata.stack(), // LOAD_CONSTANT
                metadata.stack(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE, INT_TYPE), // COMPARE
                metadata.stack(BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                metadata.stack(), // LOAD_CONSTANT
                metadata.stack(StopIteration.STOP_ITERATION_TYPE), // RAISE
                metadata.stack(), // NOP
                metadata.stack(), // JUMP_ABSOLUTE
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // except handler; DUP_TOP,
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, TYPE_TYPE), // LOAD_CONSTANT
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, TYPE_TYPE, TYPE_TYPE), // JUMP_IF_NOT_EXC_MATCH
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // LOAD_CONSTANT
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE, STRING_TYPE), // RETURN
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // NOP
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE, TYPE_TYPE), // POP_TOP
                metadata.stack(TRACEBACK_TYPE, BASE_EXCEPTION_TYPE), // POP_TOP
                metadata.stack(TRACEBACK_TYPE), // POP_TOP
                metadata.stack(), // FINALLY; Load constant
                metadata.stack(STRING_TYPE), // STORE
                metadata.stack(), // RAISE
                metadata.stack(), // END_TRY; FINALLY; Load constant
                metadata.stack(STRING_TYPE), // STORE
                metadata.stack(), // LOAD_CONSTANT
                metadata.stack(INT_TYPE) // RETURN
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

        FlowGraph flowGraph = getFlowGraph(pythonCompiledFunction);
        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(1, 0);
        List<StackMetadata> stackMetadataList = flowGraph.getStackMetadataForOperations(functionMetadata, metadata);

        assertThat(stackMetadataList).containsExactly(
                metadata.stack().locals((PythonLikeType) null), // LOAD_CONSTANT
                metadata.stack(INT_TYPE).locals((PythonLikeType) null), // STORE
                metadata.stack().locals(INT_TYPE), // LOAD_VARIABLE
                metadata.stack(INT_TYPE).locals(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE, INT_TYPE).locals(INT_TYPE), // COMPARE_OP
                metadata.stack(BOOLEAN_TYPE).locals(INT_TYPE), // POP_JUMP_IF_TRUE
                metadata.stack().locals(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(STRING_TYPE).locals(INT_TYPE), // STORE
                metadata.stack().locals(STRING_TYPE), // LOAD_VARIABLE
                metadata.stack(STRING_TYPE).locals(STRING_TYPE), // RETURN
                metadata.stack().locals(INT_TYPE), // NOP
                metadata.stack().locals(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE).locals(INT_TYPE) // RETURN
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

        FlowGraph flowGraph = getFlowGraph(pythonCompiledFunction);
        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(1, 0);
        List<StackMetadata> stackMetadataList = flowGraph.getStackMetadataForOperations(functionMetadata, metadata);

        assertThat(stackMetadataList).containsExactly(
                metadata.stack().locals((PythonLikeType) null), // LOAD_CONSTANT
                metadata.stack(INT_TYPE).locals((PythonLikeType) null), // STORE
                metadata.stack().locals(INT_TYPE), // LOAD_VARIABLE
                metadata.stack(INT_TYPE).locals(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE, INT_TYPE).locals(INT_TYPE), // COMPARE_OP
                metadata.stack(BOOLEAN_TYPE).locals(INT_TYPE), // POP_JUMP_IF_TRUE
                metadata.stack().locals(INT_TYPE), // LOAD_CONSTANT
                metadata.stack(STRING_TYPE).locals(INT_TYPE), // STORE
                metadata.stack().locals(OBJECT_TYPE), // NOP
                metadata.stack().locals(OBJECT_TYPE), // LOAD_CONSTANT
                metadata.stack(INT_TYPE).locals(OBJECT_TYPE) // RETURN
        );
    }
}
