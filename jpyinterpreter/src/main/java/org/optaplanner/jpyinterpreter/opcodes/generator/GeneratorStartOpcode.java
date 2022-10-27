package org.optaplanner.jpyinterpreter.opcodes.generator;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.GeneratorImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class GeneratorStartOpcode extends AbstractOpcode {

    public GeneratorStartOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        GeneratorImplementor.generatorStart(functionMetadata, stackMetadata);
    }
}
