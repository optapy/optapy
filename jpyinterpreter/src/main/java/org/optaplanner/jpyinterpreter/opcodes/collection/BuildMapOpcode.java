package org.optaplanner.jpyinterpreter.opcodes.collection;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.CollectionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;

public class BuildMapOpcode extends AbstractOpcode {

    public BuildMapOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2 * instruction.arg).push(ValueSourceInfo.of(this, BuiltinTypes.DICT_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(2 * instruction.arg)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.buildMap(PythonLikeDict.class, functionMetadata.methodVisitor, instruction.arg);
    }
}
