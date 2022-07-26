package org.optaplanner.python.translator.opcodes.controlflow;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
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
                getBytecodeIndex() + instruction.arg + 1);
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
        return List.of(stackMetadata.push(ValueSourceInfo.of(this, PythonLikeType.getBaseType(),
                stackMetadata.getValueSourcesUpToStackIndex(1))),
                stackMetadata.pop());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.iterateIterator(functionMetadata.methodVisitor, instruction,
                stackMetadata, functionMetadata);
    }
}
