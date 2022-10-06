package org.optaplanner.python.translator.opcodes.function;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.FunctionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeFunction;

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
        FunctionImplementor.createFunction(functionMetadata.methodVisitor, functionMetadata.className,
                instruction, stackMetadata.localVariableHelper);
    }
}
