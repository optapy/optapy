package org.optaplanner.python.translator.opcodes.function;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.FunctionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonKnownFunctionType;
import org.optaplanner.python.translator.types.PythonLikeType;

public class CallMethodOpcode extends AbstractOpcode {

    public CallMethodOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg + 1);
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            PythonLikeType[] parameterTypes =
                    new PythonLikeType[knownFunctionType.isStatic() ? instruction.arg : instruction.arg + 1];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameterTypes[parameterTypes.length - i - 1] = stackMetadata.getTypeAtStackIndex(i);
            }
            return knownFunctionType.getFunctionForParameters(parameterTypes)
                    .map(functionSignature -> stackMetadata.pop(instruction.arg + 2).push(ValueSourceInfo.of(this,
                            functionSignature.getReturnType(),
                            stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 2))))
                    .orElseGet(() -> stackMetadata.pop(instruction.arg + 2)
                            .push(ValueSourceInfo.of(this, PythonLikeType.getBaseType(),
                                    stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 2))));
        }
        return stackMetadata.pop(instruction.arg + 2).push(ValueSourceInfo.of(this, PythonLikeType.getBaseType(),
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.callMethod(functionMetadata.methodVisitor, stackMetadata, instruction,
                stackMetadata.localVariableHelper);
    }
}
