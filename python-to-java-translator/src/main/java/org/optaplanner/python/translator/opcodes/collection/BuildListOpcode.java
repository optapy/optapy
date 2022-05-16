package org.optaplanner.python.translator.opcodes.collection;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeList;

public class BuildListOpcode extends AbstractOpcode {

    public BuildListOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(instruction.arg).push(PythonLikeList.LIST_TYPE);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.buildCollection(PythonLikeList.class, functionMetadata.methodVisitor, instruction.arg);
    }
}
