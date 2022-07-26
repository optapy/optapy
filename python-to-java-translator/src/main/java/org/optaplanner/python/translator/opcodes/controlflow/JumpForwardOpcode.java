package org.optaplanner.python.translator.opcodes.controlflow;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.JumpImplementor;

public class JumpForwardOpcode extends AbstractControlFlowOpcode {

    public JumpForwardOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(getBytecodeIndex() + instruction.arg + 1);
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        int originalBytecodeIndex = instruction.offset;
        int originalTargetBytecodeIndex = originalBytecodeIndex + instruction.arg + 1;
        int newBytecodeIndex = originalBytecodeIndexToNewBytecodeIndex.get(originalBytecodeIndex);
        int newTargetBytecodeIndex = originalBytecodeIndexToNewBytecodeIndex.get(originalTargetBytecodeIndex);

        instruction.arg = newTargetBytecodeIndex - newBytecodeIndex - 1;
        super.relabel(originalBytecodeIndexToNewBytecodeIndex);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(stackMetadata.copy());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        JumpImplementor.jumpRelative(functionMetadata.methodVisitor, instruction,
                functionMetadata.bytecodeCounterToLabelMap);
    }

    @Override
    public boolean isForcedJump() {
        return true;
    }
}
