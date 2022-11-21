package org.optaplanner.jpyinterpreter.opcodes.function;

import java.util.List;
import java.util.stream.Collectors;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.FunctionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonKnownFunctionType;
import org.optaplanner.jpyinterpreter.types.PythonLikeGenericType;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class CallOpcode extends AbstractOpcode {

    public CallOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg + 1);
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            List<String> keywordArgumentNameList = stackMetadata.getCallKeywordNameList();
            List<PythonLikeType> callStackParameterTypes = stackMetadata.getValueSourcesUpToStackIndex(instruction.arg)
                    .stream().map(ValueSourceInfo::getValueType).collect(Collectors.toList());

            return knownFunctionType.getFunctionForParameters(instruction.arg - keywordArgumentNameList.size(),
                    keywordArgumentNameList,
                    callStackParameterTypes)
                    .map(functionSignature -> stackMetadata.pop(instruction.arg + 2).push(ValueSourceInfo.of(this,
                            functionSignature.getReturnType(),
                            stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 2))))
                    .orElseGet(() -> stackMetadata.pop(instruction.arg + 2)
                            .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                                    stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 2))))
                    .setCallKeywordNameList(List.of());
        }

        functionType = stackMetadata.getTypeAtStackIndex(instruction.arg);
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            List<String> keywordArgumentNameList = stackMetadata.getCallKeywordNameList();
            List<PythonLikeType> callStackParameterTypes = stackMetadata.getValueSourcesUpToStackIndex(instruction.arg)
                    .stream().map(ValueSourceInfo::getValueType).collect(Collectors.toList());

            return knownFunctionType.getFunctionForParameters(instruction.arg - keywordArgumentNameList.size(),
                    keywordArgumentNameList,
                    callStackParameterTypes)
                    .map(functionSignature -> stackMetadata.pop(instruction.arg + 2).push(ValueSourceInfo.of(this,
                            functionSignature.getReturnType(),
                            stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 2))))
                    .orElseGet(() -> stackMetadata.pop(instruction.arg + 2)
                            .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                                    stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 2))))
                    .setCallKeywordNameList(List.of());
        }

        return stackMetadata.pop(instruction.arg + 2).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg + 2))).setCallKeywordNameList(List.of());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.call(functionMetadata, stackMetadata, instruction.arg);
    }
}
