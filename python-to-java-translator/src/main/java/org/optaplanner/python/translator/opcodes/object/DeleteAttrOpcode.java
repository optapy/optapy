package org.optaplanner.python.translator.opcodes.object;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.ObjectImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class DeleteAttrOpcode extends AbstractOpcode {

    public DeleteAttrOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(1);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ObjectImplementor.deleteAttribute(functionMetadata.methodVisitor, functionMetadata.className, instruction);
    }
}
