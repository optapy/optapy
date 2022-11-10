package org.optaplanner.jpyinterpreter.opcodes.variable;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.VariableImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class LoadDerefOpcode extends AbstractOpcode {

    public LoadDerefOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.push(
                stackMetadata.getCellVariableValueSource(VariableImplementor.getCellIndex(functionMetadata, instruction.arg)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.loadCellVariable(functionMetadata, stackMetadata,
                VariableImplementor.getCellIndex(functionMetadata, instruction.arg));
    }
}
