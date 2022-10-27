package org.optaplanner.jpyinterpreter.opcodes.stack;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.StackManipulationImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class RotateThreeOpcode extends AbstractOpcode {

    public RotateThreeOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata
                .pop()
                .pop()
                .pop()
                .push(stackMetadata.getValueSourceForStackIndex(0))
                .push(stackMetadata.getValueSourceForStackIndex(2))
                .push(stackMetadata.getValueSourceForStackIndex(1));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.rotateThree(functionMetadata.methodVisitor);
    }
}
