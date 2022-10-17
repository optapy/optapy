package org.optaplanner.jpyinterpreter.opcodes.stack;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.StackManipulationImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class DupTwoOpcode extends AbstractOpcode {

    public DupTwoOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        return stackTypesBeforeInstruction
                .push(stackTypesBeforeInstruction.getValueSourceForStackIndex(1))
                .push(stackTypesBeforeInstruction.getValueSourceForStackIndex(0));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.duplicateTOSAndTOS1(functionMetadata.methodVisitor);
    }
}
