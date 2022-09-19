package org.optaplanner.python.translator.opcodes.exceptions;

import static org.optaplanner.python.translator.types.BuiltinTypes.INT_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.NONE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.TYPE_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.ExceptionImplementor;
import org.optaplanner.python.translator.opcodes.OpcodeWithoutSource;
import org.optaplanner.python.translator.opcodes.controlflow.AbstractControlFlowOpcode;
import org.optaplanner.python.translator.types.errors.PythonBaseException;
import org.optaplanner.python.translator.types.errors.PythonTraceback;

public class SetupFinallyOpcode extends AbstractControlFlowOpcode {
    int jumpTarget;

    public SetupFinallyOpcode(PythonBytecodeInstruction instruction, int jumpTarget) {
        super(instruction);
        this.jumpTarget = jumpTarget;
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(getBytecodeIndex() + 1,
                jumpTarget);
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        jumpTarget = originalBytecodeIndexToNewBytecodeIndex.get(jumpTarget);
        super.relabel(originalBytecodeIndexToNewBytecodeIndex);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(stackMetadata.copy(),
                stackMetadata.copy()
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), NONE_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), INT_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), NONE_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), PythonTraceback.TRACEBACK_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), PythonBaseException.BASE_EXCEPTION_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), TYPE_TYPE)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.createTryFinallyBlock(functionMetadata.methodVisitor, functionMetadata.className,
                jumpTarget,
                stackMetadata,
                functionMetadata.bytecodeCounterToLabelMap,
                (bytecodeIndex, runnable) -> {
                    functionMetadata.bytecodeCounterToCodeArgumenterList
                            .computeIfAbsent(bytecodeIndex, key -> new ArrayList<>()).add(runnable);
                });
    }
}
