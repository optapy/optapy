package org.optaplanner.python.translator.opcodes.stack;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.StackManipulationImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class RotateTwoOpcode extends AbstractOpcode {

    public RotateTwoOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata
                .pop()
                .pop()
                .push(stackMetadata.getValueSourceForStackIndex(0))
                .push(stackMetadata.getValueSourceForStackIndex(1));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.swap(functionMetadata.methodVisitor);
    }
}
