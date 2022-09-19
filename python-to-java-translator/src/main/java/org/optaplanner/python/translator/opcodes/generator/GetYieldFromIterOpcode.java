package org.optaplanner.python.translator.opcodes.generator;

import static org.optaplanner.python.translator.types.BuiltinTypes.ITERATOR_TYPE;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.GeneratorImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class GetYieldFromIterOpcode extends AbstractOpcode {

    public GetYieldFromIterOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop().push(ValueSourceInfo.of(this, ITERATOR_TYPE));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        GeneratorImplementor.getYieldFromIter(functionMetadata, stackMetadata);
    }
}
