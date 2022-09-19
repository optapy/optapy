package org.optaplanner.python.translator.opcodes.collection;

import static org.optaplanner.python.translator.types.BuiltinTypes.SET_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.collections.PythonLikeSet;

public class BuildSetOpcode extends AbstractOpcode {

    public BuildSetOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(instruction.arg).push(ValueSourceInfo.of(this, SET_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.buildCollection(PythonLikeSet.class, functionMetadata.methodVisitor, instruction.arg);
    }
}
