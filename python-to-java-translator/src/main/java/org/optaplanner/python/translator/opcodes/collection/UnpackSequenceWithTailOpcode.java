package org.optaplanner.python.translator.opcodes.collection;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeList;
import org.optaplanner.python.translator.types.PythonLikeType;

public class UnpackSequenceWithTailOpcode extends AbstractOpcode {

    public UnpackSequenceWithTailOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackMetadata newStackMetadata = stackMetadata.pop();

        newStackMetadata = newStackMetadata.push(PythonLikeList.LIST_TYPE);
        for (int i = 0; i < instruction.arg; i++) {
            newStackMetadata = newStackMetadata.push(PythonLikeType.getBaseType());
        }
        return newStackMetadata;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.unpackSequenceWithTail(functionMetadata.methodVisitor, instruction.arg,
                stackMetadata.localVariableHelper);
    }
}
