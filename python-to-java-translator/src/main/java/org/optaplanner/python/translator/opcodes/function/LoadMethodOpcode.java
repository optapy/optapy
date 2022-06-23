package org.optaplanner.python.translator.opcodes.function;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.FunctionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeGenericType;
import org.optaplanner.python.translator.types.PythonLikeType;

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
                        .push(ValueSourceInfo.of(this, PythonLikeFunction.FUNCTION_TYPE,
                                stackMetadata.getValueSourcesUpToStackIndex(1)))
                        .push(ValueSourceInfo.of(this, PythonLikeType.getBaseType(),
                                stackMetadata.getValueSourcesUpToStackIndex(1)))); // either TOS or NULL
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.loadMethod(functionMetadata, functionMetadata.methodVisitor, functionMetadata.className,
                functionMetadata.pythonCompiledFunction, stackMetadata, instruction);
    }
}
