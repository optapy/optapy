package org.optaplanner.python.translator.opcodes.string;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.StringImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class PrintExprOpcode extends AbstractOpcode {

    public PrintExprOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.print(functionMetadata, stackMetadata);
    }
}
