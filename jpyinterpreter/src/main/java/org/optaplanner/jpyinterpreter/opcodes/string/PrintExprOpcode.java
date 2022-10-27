package org.optaplanner.jpyinterpreter.opcodes.string;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.StringImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

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
