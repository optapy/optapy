package org.optaplanner.jpyinterpreter.opcodes.variable;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.VariableImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class DeleteDerefOpcode extends AbstractOpcode {

    public DeleteDerefOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.setCellVariableValueSource(VariableImplementor.getCellIndex(functionMetadata, instruction.arg),
                null);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.deleteCellVariable(functionMetadata, stackMetadata,
                VariableImplementor.getCellIndex(functionMetadata, instruction.arg));
    }
}
