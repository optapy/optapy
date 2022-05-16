package org.optaplanner.python.translator.opcodes.variable;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.VariableImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class DeleteFastOpcode extends AbstractOpcode {

    public DeleteFastOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.setLocalVariableType(instruction.arg, null);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.deleteLocalVariable(functionMetadata.methodVisitor, instruction,
                stackMetadata.localVariableHelper);
    }
}
