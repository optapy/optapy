package org.optaplanner.python.translator.opcodes.collection;

import static org.optaplanner.python.translator.types.BuiltinTypes.DICT_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.collections.PythonLikeDict;

public class BuildConstantKeyMapOpcode extends AbstractOpcode {

    public BuildConstantKeyMapOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(instruction.arg + 1).push(ValueSourceInfo.of(this,
                DICT_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.buildConstKeysMap(PythonLikeDict.class, functionMetadata.methodVisitor, instruction.arg);
    }
}
