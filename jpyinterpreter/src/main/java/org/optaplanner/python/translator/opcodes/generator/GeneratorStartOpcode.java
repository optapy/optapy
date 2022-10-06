package org.optaplanner.python.translator.opcodes.generator;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.GeneratorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

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
