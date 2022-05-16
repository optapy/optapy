package org.optaplanner.python.translator.opcodes.string;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.StringImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonString;

public class BuildStringOpcode extends AbstractOpcode {

    public BuildStringOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        return stackTypesBeforeInstruction.pop(instruction.arg).push(PythonString.STRING_TYPE);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.buildString(functionMetadata.methodVisitor, instruction.arg);
    }
}
