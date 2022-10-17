package org.optaplanner.jpyinterpreter.opcodes;

import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;

public abstract class AbstractOpcode implements Opcode {
    protected final PythonBytecodeInstruction instruction;

    public AbstractOpcode(PythonBytecodeInstruction instruction) {
        this.instruction = instruction;
    }

    public PythonBytecodeInstruction getInstruction() {
        return instruction;
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        instruction.offset = originalBytecodeIndexToNewBytecodeIndex.get(instruction.offset);
    }

    @Override
    public int getBytecodeIndex() {
        return instruction.offset;
    }

    @Override
    public boolean isJumpTarget() {
        return instruction.isJumpTarget;
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(getStackMetadataAfterInstruction(functionMetadata, stackMetadata));
    }

    protected abstract StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata);

    @Override
    public String toString() {
        return instruction.toString();
    }
}
