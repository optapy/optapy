package org.optaplanner.python.translator.opcodes.object;

import static org.optaplanner.python.translator.types.BuiltinTypes.BOOLEAN_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.PythonBuiltinOperatorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class IsOpcode extends AbstractOpcode {

    public IsOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2).push(ValueSourceInfo.of(this, BOOLEAN_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonBuiltinOperatorImplementor.isOperator(functionMetadata.methodVisitor, instruction);
    }
}
