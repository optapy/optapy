package org.optaplanner.python.translator.opcodes.function;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.FunctionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class CallFunctionUnpackOpcode extends AbstractOpcode {

    public CallFunctionUnpackOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        if ((instruction.arg & 1) == 1) {
            // Stack is callable, iterable, map
            return stackMetadata.pop(3).push(ValueSourceInfo.of(this, BASE_TYPE,
                    stackMetadata.getValueSourcesUpToStackIndex(3)));
        } else {
            // Stack is callable, iterable
            return stackMetadata.pop(2).push(ValueSourceInfo.of(this, BASE_TYPE,
                    stackMetadata.getValueSourcesUpToStackIndex(2)));
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.callFunctionUnpack(functionMetadata, stackMetadata, instruction);
    }
}
