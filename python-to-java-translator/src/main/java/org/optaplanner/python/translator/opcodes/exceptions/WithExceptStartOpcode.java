package org.optaplanner.python.translator.opcodes.exceptions;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.ExceptionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class WithExceptStartOpcode extends AbstractOpcode {

    public WithExceptStartOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata
                .push(ValueSourceInfo.of(this, BASE_TYPE, stackMetadata.getValueSourceForStackIndex(6)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.handleExceptionInWith(functionMetadata, stackMetadata);
    }
}
