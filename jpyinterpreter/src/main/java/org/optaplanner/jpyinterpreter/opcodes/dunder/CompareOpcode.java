package org.optaplanner.jpyinterpreter.opcodes.dunder;

import org.optaplanner.jpyinterpreter.CompareOp;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.DunderOperatorImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;

public class CompareOpcode extends AbstractOpcode {

    public CompareOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop(2).push(ValueSourceInfo.of(this, BuiltinTypes.BOOLEAN_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.compareValues(functionMetadata.methodVisitor, stackMetadata,
                CompareOp.getOp(instruction.arg));
    }
}
