package org.optaplanner.jpyinterpreter.opcodes.collection;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.CollectionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.PythonSlice;

public class BuildSliceOpcode extends AbstractOpcode {

    public BuildSliceOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(instruction.arg).push(ValueSourceInfo.of(this, PythonSlice.SLICE_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.buildSlice(functionMetadata, stackMetadata, instruction.arg);
    }
}
