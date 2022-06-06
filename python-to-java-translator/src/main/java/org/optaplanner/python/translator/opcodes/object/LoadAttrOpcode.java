package org.optaplanner.python.translator.opcodes.object;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.ObjectImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeType;

public class LoadAttrOpcode extends AbstractOpcode {

    public LoadAttrOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop().push(ValueSourceInfo.of(this, PythonLikeType.getBaseType(),
                stackMetadata.getValueSourcesUpToStackIndex(1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ObjectImplementor.getAttribute(functionMetadata, functionMetadata.methodVisitor, functionMetadata.className,
                stackMetadata,
                instruction);
    }
}
