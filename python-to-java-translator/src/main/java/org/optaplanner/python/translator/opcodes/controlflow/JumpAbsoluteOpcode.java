package org.optaplanner.python.translator.opcodes.controlflow;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.JumpImplementor;

public class JumpAbsoluteOpcode extends AbstractControlFlowOpcode {

    public JumpAbsoluteOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(instruction.arg);
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        instruction.arg = originalBytecodeIndexToNewBytecodeIndex.get(instruction.arg);
        super.relabel(originalBytecodeIndexToNewBytecodeIndex);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(stackMetadata.copy());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        JumpImplementor.jumpAbsolute(functionMetadata.methodVisitor, instruction,
                functionMetadata.bytecodeCounterToLabelMap);
    }

    @Override
    public boolean isForcedJump() {
        return true;
    }
}
