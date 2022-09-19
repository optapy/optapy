package org.optaplanner.python.translator.opcodes.dunder;

import static org.optaplanner.python.translator.types.BuiltinTypes.BOOLEAN_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.DunderOperatorImplementor;
import org.optaplanner.python.translator.implementors.PythonBuiltinOperatorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class NotOpcode extends AbstractOpcode {

    public NotOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop().push(ValueSourceInfo.of(this, BOOLEAN_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.unaryOperator(functionMetadata.methodVisitor, stackMetadata,
                PythonUnaryOperator.AS_BOOLEAN);
        PythonBuiltinOperatorImplementor.performNotOnTOS(functionMetadata.methodVisitor);
    }
}
