package org.optaplanner.python.translator.opcodes.collection;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.LIST_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class UnpackSequenceWithTailOpcode extends AbstractOpcode {

    public UnpackSequenceWithTailOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackMetadata newStackMetadata = stackMetadata.pop();

        newStackMetadata = newStackMetadata
                .push(ValueSourceInfo.of(this, LIST_TYPE, stackMetadata.getValueSourcesUpToStackIndex(1)));
        for (int i = 0; i < instruction.arg; i++) {
            newStackMetadata = newStackMetadata.push(ValueSourceInfo.of(this, BASE_TYPE,
                    stackMetadata.getValueSourcesUpToStackIndex(1)));
        }
        return newStackMetadata;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.unpackSequenceWithTail(functionMetadata.methodVisitor, instruction.arg,
                stackMetadata.localVariableHelper);
    }
}
