package org.optaplanner.jpyinterpreter.opcodes.exceptions;

import java.util.List;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.ExceptionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.controlflow.AbstractControlFlowOpcode;

public class ReraiseOpcode extends AbstractControlFlowOpcode {

    public ReraiseOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of();
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of();
    }

    @Override
    public boolean isForcedJump() {
        return true;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.reraise(functionMetadata.methodVisitor);
    }
}
