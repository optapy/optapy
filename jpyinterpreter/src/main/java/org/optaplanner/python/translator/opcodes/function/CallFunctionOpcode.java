package org.optaplanner.python.translator.opcodes.function;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.FunctionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonKnownFunctionType;
import org.optaplanner.python.translator.types.PythonLikeGenericType;
import org.optaplanner.python.translator.types.PythonLikeType;

public class CallFunctionOpcode extends AbstractOpcode {

    public CallFunctionOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg);
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            return knownFunctionType.getDefaultFunctionSignature()
                    .map(functionSignature -> stackMetadata.pop(instruction.arg + 1).push(ValueSourceInfo.of(this,
                            functionSignature.getReturnType(),
                            stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 1))))
                    .orElseGet(() -> stackMetadata.pop(instruction.arg + 1)
                            .push(ValueSourceInfo.of(this, BASE_TYPE,
                                    stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 1))));
        }
        return stackMetadata.pop(instruction.arg + 1).push(ValueSourceInfo.of(this, BASE_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.callFunction(functionMetadata.methodVisitor, stackMetadata, instruction);
    }
}
