package org.optaplanner.python.translator.opcodes.dunder;

import java.util.Optional;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.StackMetadata;
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
            StackMetadata stackTypesBeforeInstruction) {
        PythonLikeType leftOperand =
                Optional.ofNullable(stackTypesBeforeInstruction.getTypeAtStackIndex(1)).orElseGet(PythonLikeType::getBaseType);
        PythonLikeType rightOperand =
                Optional.ofNullable(stackTypesBeforeInstruction.getTypeAtStackIndex(0)).orElseGet(PythonLikeType::getBaseType);

        Optional<PythonKnownFunctionType> maybeKnownFunctionType = leftOperand.getMethodType(operator.getDunderMethod());
        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature = knownFunctionType.getFunctionForParameters(rightOperand);
            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                return stackTypesBeforeInstruction.pop().pop().push(functionSignature.getReturnType());
            }
        }

        // TODO: Right dunder method
        return stackTypesBeforeInstruction.pop().pop().push(PythonLikeType.getBaseType());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.binaryOperator(functionMetadata.methodVisitor, stackMetadata, operator);
    }
}
