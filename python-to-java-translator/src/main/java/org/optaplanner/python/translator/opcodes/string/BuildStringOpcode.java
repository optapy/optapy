package org.optaplanner.python.translator.opcodes.string;

import static org.optaplanner.python.translator.types.BuiltinTypes.STRING_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.StringImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class BuildStringOpcode extends AbstractOpcode {

    public BuildStringOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop(instruction.arg).push(
                ValueSourceInfo.of(this, STRING_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(instruction.arg)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.buildString(functionMetadata.methodVisitor, instruction.arg);
    }
}
