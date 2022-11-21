package org.optaplanner.jpyinterpreter.opcodes.function;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.FunctionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeGenericType;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class LoadMethodOpcode extends AbstractOpcode {

    public LoadMethodOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType stackTosType = stackMetadata.getTOSType();
        PythonLikeType tosType;
        if (stackTosType instanceof PythonLikeGenericType) {
            tosType = ((PythonLikeGenericType) stackTosType).getOrigin();
        } else {
            tosType = stackTosType;
        }

        return tosType.getMethodType(functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg))
                .map(knownFunction -> stackMetadata.pop()
                        .push(ValueSourceInfo.of(this, knownFunction, stackMetadata.getValueSourcesUpToStackIndex(1)))
                        .push(ValueSourceInfo.of(this, tosType, stackMetadata.getValueSourcesUpToStackIndex(1))) // TOS, since we know the function exists
                )
                .orElseGet(() -> stackMetadata.pop()
                        .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(),
                                stackMetadata.getValueSourcesUpToStackIndex(1)))
                        .push(ValueSourceInfo.of(this, BuiltinTypes.NULL_TYPE,
                                stackMetadata.getValueSourcesUpToStackIndex(1)))); // either TOS or NULL
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.loadMethod(functionMetadata, functionMetadata.methodVisitor, functionMetadata.className,
                functionMetadata.pythonCompiledFunction, stackMetadata, instruction);
    }
}
