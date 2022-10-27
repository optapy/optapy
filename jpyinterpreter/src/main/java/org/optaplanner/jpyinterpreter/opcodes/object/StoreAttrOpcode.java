package org.optaplanner.jpyinterpreter.opcodes.object;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.ObjectImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

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
        ObjectImplementor.setAttribute(functionMetadata, functionMetadata.methodVisitor, functionMetadata.className,
                stackMetadata,
                instruction, stackMetadata.localVariableHelper);
    }
}
