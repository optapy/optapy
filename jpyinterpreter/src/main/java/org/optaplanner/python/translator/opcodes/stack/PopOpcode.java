package org.optaplanner.python.translator.opcodes.stack;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.StackManipulationImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class PopOpcode extends AbstractOpcode {

    public PopOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.popTOS(functionMetadata.methodVisitor);
    }
}
