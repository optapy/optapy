package org.optaplanner.python.translator.opcodes.string;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.StringImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonString;

public class FormatValueOpcode extends AbstractOpcode {

    public FormatValueOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        if ((instruction.arg & 4) == 4) {
            // There is a format argument above the value
            return stackTypesBeforeInstruction.pop(2).push(PythonString.STRING_TYPE);
        } else {
            // There is no format argument above the value
            return stackTypesBeforeInstruction.pop().push(PythonString.STRING_TYPE);
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.formatValue(functionMetadata.methodVisitor, instruction);
    }
}
