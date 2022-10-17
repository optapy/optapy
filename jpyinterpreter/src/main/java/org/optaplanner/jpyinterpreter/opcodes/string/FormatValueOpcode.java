package org.optaplanner.jpyinterpreter.opcodes.string;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.StringImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;

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
                    .push(ValueSourceInfo.of(this, BuiltinTypes.STRING_TYPE,
                            stackMetadata.getValueSourcesUpToStackIndex(2)));
        } else {
            // There is no format argument above the value
            return stackMetadata.pop()
                    .push(ValueSourceInfo.of(this, BuiltinTypes.STRING_TYPE,
                            stackMetadata.getValueSourcesUpToStackIndex(1)));
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.formatValue(functionMetadata.methodVisitor, instruction);
    }
}
