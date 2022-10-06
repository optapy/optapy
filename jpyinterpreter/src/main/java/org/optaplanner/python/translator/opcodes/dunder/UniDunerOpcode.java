package org.optaplanner.python.translator.opcodes.dunder;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;

import java.util.Optional;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.DunderOperatorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonKnownFunctionType;
import org.optaplanner.python.translator.types.PythonLikeType;

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
                Optional.ofNullable(stackMetadata.getTOSType()).orElse(BASE_TYPE);

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
                .push(ValueSourceInfo.of(this, BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.unaryOperator(functionMetadata.methodVisitor, stackMetadata, operator);
    }
}
