package org.optaplanner.python.translator.opcodes.controlflow;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.CollectionImplementor;

public class ForIterOpcode extends AbstractControlFlowOpcode {
    int jumpTarget;

    public ForIterOpcode(PythonBytecodeInstruction instruction, int jumpTarget) {
        super(instruction);
        this.jumpTarget = jumpTarget;
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(
                getBytecodeIndex() + 1,
                jumpTarget);
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        jumpTarget = originalBytecodeIndexToNewBytecodeIndex.get(jumpTarget);
        super.relabel(originalBytecodeIndexToNewBytecodeIndex);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(stackMetadata.push(ValueSourceInfo.of(this, BASE_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(1))),
                stackMetadata.pop());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.iterateIterator(functionMetadata.methodVisitor, jumpTarget,
                stackMetadata, functionMetadata);
    }
}
