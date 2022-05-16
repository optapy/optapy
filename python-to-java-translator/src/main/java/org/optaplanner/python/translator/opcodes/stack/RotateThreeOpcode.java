package org.optaplanner.python.translator.opcodes.stack;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.StackManipulationImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class RotateThreeOpcode extends AbstractOpcode {

    public RotateThreeOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        return stackTypesBeforeInstruction
                .pop()
                .pop()
                .pop()
                .push(stackTypesBeforeInstruction.getTypeAtStackIndex(0))
                .push(stackTypesBeforeInstruction.getTypeAtStackIndex(2))
                .push(stackTypesBeforeInstruction.getTypeAtStackIndex(1));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.rotateThree(functionMetadata.methodVisitor);
    }
}
