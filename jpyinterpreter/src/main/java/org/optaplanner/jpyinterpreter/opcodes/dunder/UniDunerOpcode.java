package org.optaplanner.jpyinterpreter.opcodes.dunder;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonFunctionSignature;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.DunderOperatorImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonKnownFunctionType;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class UniDunerOpcode extends AbstractOpcode {

    final PythonUnaryOperator operator;

    public UniDunerOpcode(PythonBytecodeInstruction instruction, PythonUnaryOperator operator) {
        super(instruction);
        this.operator = operator;
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        PythonLikeType operand =
                Optional.ofNullable(stackMetadata.getTOSType()).orElse(BuiltinTypes.BASE_TYPE);

        Optional<PythonKnownFunctionType> maybeKnownFunctionType = operand.getMethodType(operator.getDunderMethod());
        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature = knownFunctionType.getFunctionForParameters();
            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                return stackMetadata.pop().push(ValueSourceInfo.of(this, functionSignature.getReturnType(),
                        stackMetadata.getValueSourcesUpToStackIndex(1)));
            }
        }

        return stackMetadata.pop()
                .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.unaryOperator(functionMetadata.methodVisitor, stackMetadata, operator);
    }
}
