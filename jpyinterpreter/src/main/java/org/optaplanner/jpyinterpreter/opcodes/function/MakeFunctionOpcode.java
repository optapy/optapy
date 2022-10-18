package org.optaplanner.jpyinterpreter.opcodes.function;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.FunctionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;

public class MakeFunctionOpcode extends AbstractOpcode {

    public MakeFunctionOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2 + Integer.bitCount(instruction.arg))
                .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(),
                        stackMetadata.getValueSourcesUpToStackIndex(2 + Integer.bitCount(instruction.arg))));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.createFunction(functionMetadata, stackMetadata, instruction);
    }
}
