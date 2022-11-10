package org.optaplanner.jpyinterpreter.opcodes.stack;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.StackManipulationImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class SwapOpcode extends AbstractOpcode {

    public SwapOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata
                .set(instruction.arg - 1, stackMetadata.getTOSValueSource())
                .set(0, stackMetadata.getValueSourceForStackIndex(instruction.arg - 1));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.swapTOSWithIndex(functionMetadata, stackMetadata, instruction.arg - 1);
    }
}
