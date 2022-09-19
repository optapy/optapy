package org.optaplanner.python.translator.opcodes.exceptions;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.INT_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.NONE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.TYPE_TYPE;

import java.util.List;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.ExceptionImplementor;
import org.optaplanner.python.translator.opcodes.OpcodeWithoutSource;
import org.optaplanner.python.translator.opcodes.controlflow.AbstractControlFlowOpcode;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.errors.PythonBaseException;
import org.optaplanner.python.translator.types.errors.PythonTraceback;

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
                        .push(ValueSourceInfo.of(this, BASE_TYPE, stackMetadata.getTOSValueSource())),
                stackMetadata
                        .pop()
                        .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(), stackMetadata.getTOSValueSource()))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), NONE_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), INT_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), NONE_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), PythonTraceback.TRACEBACK_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), PythonBaseException.BASE_EXCEPTION_TYPE))
                        .push(ValueSourceInfo.of(new OpcodeWithoutSource(), TYPE_TYPE)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.startWith(jumpTarget, functionMetadata, stackMetadata);
    }
}
