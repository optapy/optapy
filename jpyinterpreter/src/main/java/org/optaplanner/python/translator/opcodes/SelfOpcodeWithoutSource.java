package org.optaplanner.python.translator.opcodes;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.StackMetadata;

public class SelfOpcodeWithoutSource implements Opcode {

    @Override
    public int getBytecodeIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isJumpTarget() {
        throw new UnsupportedOperationException();
    }
}
