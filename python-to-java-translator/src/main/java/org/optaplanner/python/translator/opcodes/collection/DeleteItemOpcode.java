package org.optaplanner.python.translator.opcodes.collection;

import org.objectweb.asm.Opcodes;
import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.DunderOperatorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

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
        DunderOperatorImplementor.binaryOperator(functionMetadata.methodVisitor, PythonBinaryOperators.DELETE_ITEM);
        functionMetadata.methodVisitor.visitInsn(Opcodes.POP); // DELETE_ITEM ignore results of delete function
    }
}
