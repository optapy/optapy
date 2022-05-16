package org.optaplanner.python.translator.opcodes.collection;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeType;

public class UnpackSequenceOpcode extends AbstractOpcode {

    public UnpackSequenceOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackMetadata newStackMetadata = stackMetadata.pop();
        for (int i = 0; i < instruction.arg; i++) {
            newStackMetadata = newStackMetadata.push(PythonLikeType.getBaseType());
        }
        return newStackMetadata;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.unpackSequence(functionMetadata.methodVisitor, instruction.arg,
                stackMetadata.localVariableHelper);
    }
}
