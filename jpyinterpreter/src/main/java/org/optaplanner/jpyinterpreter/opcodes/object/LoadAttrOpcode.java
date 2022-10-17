package org.optaplanner.jpyinterpreter.opcodes.object;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.ObjectImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class LoadAttrOpcode extends AbstractOpcode {

    public LoadAttrOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType tosType = stackMetadata.getTOSType();
        return tosType.getInstanceFieldDescriptor(functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg))
                .map(fieldDescriptor -> stackMetadata.pop()
                        .push(ValueSourceInfo.of(this, fieldDescriptor.getFieldPythonLikeType(),
                                stackMetadata.getValueSourcesUpToStackIndex(1))))
                .orElseGet(() -> stackMetadata.pop().push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(1))));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ObjectImplementor.getAttribute(functionMetadata, functionMetadata.methodVisitor, functionMetadata.className,
                stackMetadata,
                instruction);
    }
}
