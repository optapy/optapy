package org.optaplanner.python.translator.opcodes.variable;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.VariableImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonCell;

public class LoadClosureOpcode extends AbstractOpcode {

    public LoadClosureOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.push(PythonCell.CELL_TYPE);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.loadCell(functionMetadata.methodVisitor, instruction,
                stackMetadata.localVariableHelper);
    }
}
