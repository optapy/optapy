package org.optaplanner.python.translator.opcodes.collection;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class SetItemOpcode extends AbstractOpcode {

    public SetItemOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(3);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.setItem(functionMetadata.methodVisitor, stackMetadata.localVariableHelper);
    }
}
