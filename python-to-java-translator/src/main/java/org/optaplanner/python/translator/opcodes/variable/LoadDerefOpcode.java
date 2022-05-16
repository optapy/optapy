package org.optaplanner.python.translator.opcodes.variable;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.VariableImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class LoadDerefOpcode extends AbstractOpcode {

    public LoadDerefOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.push(stackMetadata.getCellVariableType(instruction.arg));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.loadCellVariable(functionMetadata.methodVisitor, instruction,
                stackMetadata.localVariableHelper);
    }
}
