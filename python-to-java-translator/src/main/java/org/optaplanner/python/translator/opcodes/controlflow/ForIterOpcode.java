package org.optaplanner.python.translator.opcodes.controlflow;

import java.util.List;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.types.PythonLikeType;

public class ForIterOpcode extends AbstractControlFlowOpcode {

    public ForIterOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(
                getBytecodeIndex() + 1,
                getBytecodeIndex() + instruction.arg);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(stackMetadata.push(PythonLikeType.getBaseType()),
                stackMetadata.pop());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.iterateIterator(functionMetadata.methodVisitor, instruction,
                functionMetadata.bytecodeCounterToLabelMap);
    }
}
