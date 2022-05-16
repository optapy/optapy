package org.optaplanner.python.translator.opcodes.dunder;

import org.optaplanner.python.translator.CompareOp;
import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.DunderOperatorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonBoolean;

public class CompareOpcode extends AbstractOpcode {

    public CompareOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        return stackTypesBeforeInstruction.pop(2).push(PythonBoolean.BOOLEAN_TYPE);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.compareValues(functionMetadata.methodVisitor, CompareOp.getOp(instruction.arg));
    }
}
