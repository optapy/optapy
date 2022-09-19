package org.optaplanner.python.translator.opcodes.dunder;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;

import java.util.Optional;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.DunderOperatorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonKnownFunctionType;
import org.optaplanner.python.translator.types.PythonLikeType;

public class BinaryDunderOpcode extends AbstractOpcode {

    final PythonBinaryOperators operator;

    public BinaryDunderOpcode(PythonBytecodeInstruction instruction, PythonBinaryOperators operator) {
        super(instruction);
        this.operator = operator;
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        PythonLikeType leftOperand =
                Optional.ofNullable(stackMetadata.getTypeAtStackIndex(1)).orElse(BASE_TYPE);
        PythonLikeType rightOperand =
                Optional.ofNullable(stackMetadata.getTypeAtStackIndex(0)).orElse(BASE_TYPE);

        Optional<PythonKnownFunctionType> maybeKnownFunctionType = leftOperand.getMethodType(operator.getDunderMethod());
        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature = knownFunctionType.getFunctionForParameters(rightOperand);
            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                return stackMetadata.pop().pop().push(ValueSourceInfo.of(this, functionSignature.getReturnType(),
                        stackMetadata.getValueSourcesUpToStackIndex(2)));
            }
        }

        // TODO: Right dunder method
        return stackMetadata.pop().pop()
                .push(ValueSourceInfo.of(this, BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.binaryOperator(functionMetadata.methodVisitor, stackMetadata, operator);
    }
}
