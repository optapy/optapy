package org.optaplanner.jpyinterpreter.opcodes.stack;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.StackManipulationImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class CopyOpcode extends AbstractOpcode {

    public CopyOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.push(stackMetadata.getValueSourceForStackIndex(instruction.arg - 1));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, instruction.arg - 1);
    }
}
