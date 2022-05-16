package org.optaplanner.python.translator.opcodes.stack;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.StackManipulationImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class DupTwoOpcode extends AbstractOpcode {

    public DupTwoOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        return stackTypesBeforeInstruction
                .push(stackTypesBeforeInstruction.getTypeAtStackIndex(1))
                .push(stackTypesBeforeInstruction.getTypeAtStackIndex(0));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.duplicateTOSAndTOS1(functionMetadata.methodVisitor);
    }
}
