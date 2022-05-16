package org.optaplanner.python.translator.opcodes.dunder;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.DunderOperatorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeType;

public class UniDunerOpcode extends AbstractOpcode {

    final PythonUnaryOperator operator;

    public UniDunerOpcode(PythonBytecodeInstruction instruction, PythonUnaryOperator operator) {
        super(instruction);
        this.operator = operator;
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        return stackTypesBeforeInstruction.pop().push(PythonLikeType.getBaseType());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.unaryOperator(functionMetadata.methodVisitor, operator);
    }
}
