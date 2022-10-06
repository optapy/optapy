package org.optaplanner.python.translator.opcodes.collection;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class UnpackSequenceOpcode extends AbstractOpcode {

    public UnpackSequenceOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackMetadata newStackMetadata = stackMetadata.pop();
        for (int i = 0; i < instruction.arg; i++) {
            newStackMetadata = newStackMetadata.push(ValueSourceInfo.of(this, BASE_TYPE,
                    stackMetadata.getValueSourcesUpToStackIndex(1)));
        }
        return newStackMetadata;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.unpackSequence(functionMetadata.methodVisitor, instruction.arg,
                stackMetadata.localVariableHelper);
    }
}
