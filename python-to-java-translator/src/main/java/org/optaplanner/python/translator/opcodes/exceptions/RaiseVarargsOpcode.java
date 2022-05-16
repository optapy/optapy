package org.optaplanner.python.translator.opcodes.exceptions;

import java.util.List;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.ExceptionImplementor;
import org.optaplanner.python.translator.opcodes.controlflow.AbstractControlFlowOpcode;

public class RaiseVarargsOpcode extends AbstractControlFlowOpcode {

    public RaiseVarargsOpcode(PythonBytecodeInstruction instruction) {
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
        ExceptionImplementor.raiseWithOptionalExceptionAndCause(functionMetadata.methodVisitor, instruction,
                stackMetadata.localVariableHelper);
    }
}
