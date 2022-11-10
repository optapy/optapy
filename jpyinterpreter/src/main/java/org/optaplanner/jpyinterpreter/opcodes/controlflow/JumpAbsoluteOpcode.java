package org.optaplanner.jpyinterpreter.opcodes.controlflow;

import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.JumpImplementor;

public class JumpAbsoluteOpcode extends AbstractControlFlowOpcode {
    int jumpTarget;

    public JumpAbsoluteOpcode(PythonBytecodeInstruction instruction, int jumpTarget) {
        super(instruction);
        this.jumpTarget = jumpTarget;
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(jumpTarget);
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        jumpTarget = originalBytecodeIndexToNewBytecodeIndex.get(jumpTarget);
        super.relabel(originalBytecodeIndexToNewBytecodeIndex);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(stackMetadata.copy());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        JumpImplementor.jumpAbsolute(functionMetadata, stackMetadata, jumpTarget);
    }

    @Override
    public boolean isForcedJump() {
        return true;
    }
}
