package org.optaplanner.python.translator.opcodes.generator;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.GeneratorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class YieldFromOpcode extends AbstractOpcode {

    public YieldFromOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2).push(ValueSourceInfo.of(this, BASE_TYPE));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        GeneratorImplementor.yieldFrom(instruction, functionMetadata, stackMetadata);
    }
}