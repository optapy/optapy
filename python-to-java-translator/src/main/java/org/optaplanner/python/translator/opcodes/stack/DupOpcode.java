package org.optaplanner.python.translator.opcodes.stack;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.StackManipulationImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class DupOpcode extends AbstractOpcode {

    public DupOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        return stackTypesBeforeInstruction.push(stackTypesBeforeInstruction.getTOSType());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.duplicateTOS(functionMetadata.methodVisitor);
    }
}
