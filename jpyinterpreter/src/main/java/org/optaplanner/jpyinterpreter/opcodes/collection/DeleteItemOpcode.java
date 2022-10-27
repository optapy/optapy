package org.optaplanner.jpyinterpreter.opcodes.collection;

import org.objectweb.asm.Opcodes;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.DunderOperatorImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class DeleteItemOpcode extends AbstractOpcode {

    public DeleteItemOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.binaryOperator(functionMetadata.methodVisitor, stackMetadata,
                PythonBinaryOperators.DELETE_ITEM);
        functionMetadata.methodVisitor.visitInsn(Opcodes.POP); // DELETE_ITEM ignore results of delete function
    }
}
