package org.optaplanner.jpyinterpreter.opcodes.generator;

import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.GeneratorImplementor;
import org.optaplanner.jpyinterpreter.opcodes.controlflow.AbstractControlFlowOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;

public class SendOpcode extends AbstractControlFlowOpcode {
    int jumpTarget;

    public SendOpcode(PythonBytecodeInstruction instruction, int jumpTarget) {
        super(instruction);
        this.jumpTarget = jumpTarget;
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        jumpTarget = originalBytecodeIndexToNewBytecodeIndex.get(jumpTarget);
        super.relabel(originalBytecodeIndexToNewBytecodeIndex);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(
                getBytecodeIndex() + 1,
                jumpTarget);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(
                stackMetadata.pop()
                        .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(2))),
                stackMetadata.pop());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        GeneratorImplementor.progressSubgenerator(functionMetadata, stackMetadata, jumpTarget);
    }
}
