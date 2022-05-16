package org.optaplanner.python.translator.opcodes.controlflow;

import java.util.List;

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
