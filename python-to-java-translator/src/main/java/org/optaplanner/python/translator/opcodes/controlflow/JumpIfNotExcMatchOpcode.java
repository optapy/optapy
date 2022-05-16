package org.optaplanner.python.translator.opcodes.controlflow;

import java.util.List;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.JumpImplementor;

public class JumpIfNotExcMatchOpcode extends AbstractControlFlowOpcode {

    public JumpIfNotExcMatchOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(
                getBytecodeIndex() + 1,
                instruction.arg);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(stackMetadata.pop(2),
                stackMetadata.pop(2));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        JumpImplementor.popAndJumpIfExceptionDoesNotMatch(functionMetadata.methodVisitor, instruction,
                functionMetadata.bytecodeCounterToLabelMap);
    }
}
