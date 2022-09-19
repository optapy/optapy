package org.optaplanner.python.translator.opcodes.string;

import static org.optaplanner.python.translator.types.BuiltinTypes.STRING_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.StringImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class FormatValueOpcode extends AbstractOpcode {

    public FormatValueOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        if ((instruction.arg & 4) == 4) {
            // There is a format argument above the value
            return stackMetadata.pop(2)
                    .push(ValueSourceInfo.of(this, STRING_TYPE,
                            stackMetadata.getValueSourcesUpToStackIndex(2)));
        } else {
            // There is no format argument above the value
            return stackMetadata.pop()
                    .push(ValueSourceInfo.of(this, STRING_TYPE,
                            stackMetadata.getValueSourcesUpToStackIndex(1)));
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.formatValue(functionMetadata.methodVisitor, instruction);
    }
}
