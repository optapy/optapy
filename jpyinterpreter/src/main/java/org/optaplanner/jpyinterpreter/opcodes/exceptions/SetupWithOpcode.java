package org.optaplanner.jpyinterpreter.opcodes.exceptions;

import java.util.List;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.ExceptionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.controlflow.AbstractControlFlowOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.errors.PythonBaseException;
import org.optaplanner.jpyinterpreter.types.errors.PythonTraceback;

public class SetupWithOpcode extends AbstractControlFlowOpcode {
    int jumpTarget;

    public SetupWithOpcode(PythonBytecodeInstruction instruction, int jumpTarget) {
        super(instruction);
        this.jumpTarget = jumpTarget;
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(
                getBytecodeIndex() + 1,
                jumpTarget);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(
                stackMetadata
                        .pop()
                        .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(), stackMetadata.getTOSValueSource()))
                        .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getTOSValueSource())),
                stackMetadata
                        .pop()
                        .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(), stackMetadata.getTOSValueSource()))
                        .pushTemp(BuiltinTypes.NONE_TYPE)
                        .pushTemp(BuiltinTypes.INT_TYPE)
                        .pushTemp(BuiltinTypes.NONE_TYPE)
                        .pushTemp(PythonTraceback.TRACEBACK_TYPE)
                        .pushTemp(PythonBaseException.BASE_EXCEPTION_TYPE)
                        .pushTemp(BuiltinTypes.TYPE_TYPE));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.startWith(jumpTarget, functionMetadata, stackMetadata);
    }
}
