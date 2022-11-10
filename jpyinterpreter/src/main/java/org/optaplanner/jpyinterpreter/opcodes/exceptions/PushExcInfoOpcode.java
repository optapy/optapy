package org.optaplanner.jpyinterpreter.opcodes.exceptions;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.ExceptionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.errors.PythonBaseException;

public class PushExcInfoOpcode extends AbstractOpcode {

    public PushExcInfoOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata
                .pop()
                .push(ValueSourceInfo.of(this, PythonBaseException.BASE_EXCEPTION_TYPE))
                .push(stackMetadata.getTOSValueSource());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.pushExcInfo(functionMetadata, stackMetadata);
    }
}
