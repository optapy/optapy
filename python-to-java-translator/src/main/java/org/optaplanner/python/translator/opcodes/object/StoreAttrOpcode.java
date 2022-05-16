package org.optaplanner.python.translator.opcodes.object;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.ObjectImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class StoreAttrOpcode extends AbstractOpcode {

    public StoreAttrOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ObjectImplementor.setAttribute(functionMetadata.methodVisitor, functionMetadata.className, instruction,
                stackMetadata.localVariableHelper);
    }
}
