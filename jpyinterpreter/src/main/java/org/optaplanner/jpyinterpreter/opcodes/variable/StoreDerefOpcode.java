package org.optaplanner.jpyinterpreter.opcodes.variable;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.VariableImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class StoreDerefOpcode extends AbstractOpcode {

    public StoreDerefOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop().setCellVariableValueSource(
                VariableImplementor.getCellIndex(functionMetadata, instruction.arg), stackMetadata.getTOSValueSource());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.storeInCellVariable(functionMetadata, stackMetadata,
                VariableImplementor.getCellIndex(functionMetadata, instruction.arg));
    }
}
