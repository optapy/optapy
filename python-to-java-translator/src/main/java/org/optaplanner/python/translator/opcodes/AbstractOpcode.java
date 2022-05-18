package org.optaplanner.python.translator.opcodes;

import java.util.List;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;

public abstract class AbstractOpcode implements Opcode {
    protected final PythonBytecodeInstruction instruction;

    public AbstractOpcode(PythonBytecodeInstruction instruction) {
        this.instruction = instruction;
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