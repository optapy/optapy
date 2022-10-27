package org.optaplanner.jpyinterpreter.opcodes.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.ExceptionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.controlflow.AbstractControlFlowOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.errors.PythonBaseException;
import org.optaplanner.jpyinterpreter.types.errors.PythonTraceback;

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
                        .pushTemp(BuiltinTypes.NONE_TYPE)
                        .pushTemp(BuiltinTypes.INT_TYPE)
                        .pushTemp(BuiltinTypes.NONE_TYPE)
                        .pushTemp(PythonTraceback.TRACEBACK_TYPE)
                        .pushTemp(PythonBaseException.BASE_EXCEPTION_TYPE)
                        .pushTemp(BuiltinTypes.TYPE_TYPE));
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
