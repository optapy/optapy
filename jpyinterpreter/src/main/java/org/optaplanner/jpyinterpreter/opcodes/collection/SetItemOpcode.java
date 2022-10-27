package org.optaplanner.jpyinterpreter.opcodes.collection;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.CollectionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class SetItemOpcode extends AbstractOpcode {

    public SetItemOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(3);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.setItem(functionMetadata, stackMetadata);
    }
}
