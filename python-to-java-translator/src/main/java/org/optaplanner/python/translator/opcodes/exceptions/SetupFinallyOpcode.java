package org.optaplanner.python.translator.opcodes.exceptions;

import java.util.ArrayList;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.ExceptionImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;

public class SetupFinallyOpcode extends AbstractOpcode {

    public SetupFinallyOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.copy();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.createTryFinallyBlock(functionMetadata.methodVisitor, functionMetadata.className, instruction,
                stackMetadata.localVariableHelper,
                functionMetadata.bytecodeCounterToLabelMap,
                (bytecodeIndex, runnable) -> {
                    functionMetadata.bytecodeCounterToCodeArgumenterList
                            .computeIfAbsent(bytecodeIndex, key -> new ArrayList<>()).add(runnable);
                });
    }
}
