package org.optaplanner.python.translator.opcodes.dunder;

import static org.optaplanner.python.translator.types.BuiltinTypes.BOOLEAN_TYPE;

import org.optaplanner.python.translator.CompareOp;
import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.DunderOperatorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class CompareOpcode extends AbstractOpcode {

    public CompareOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop(2).push(ValueSourceInfo.of(this, BOOLEAN_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.compareValues(functionMetadata.methodVisitor, stackMetadata,
                CompareOp.getOp(instruction.arg));
    }
}
