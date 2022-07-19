package org.optaplanner.python.translator.opcodes.module;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.ModuleImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeType;

public class ImportFromOpcode extends AbstractOpcode {

    public ImportFromOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.push(ValueSourceInfo.of(this, PythonLikeType.getBaseType(), stackMetadata.getTOSValueSource()));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ModuleImplementor.importFrom(functionMetadata, stackMetadata, instruction);
    }
}
