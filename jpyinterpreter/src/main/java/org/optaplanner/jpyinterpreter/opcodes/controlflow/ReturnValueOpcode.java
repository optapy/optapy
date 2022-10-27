package org.optaplanner.jpyinterpreter.opcodes.controlflow;

import java.util.List;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonFunctionType;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.GeneratorImplementor;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;

public class ReturnValueOpcode extends AbstractControlFlowOpcode {

    public ReturnValueOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of();
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of();
    }

    @Override
    public boolean isForcedJump() {
        return true;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        if (functionMetadata.functionType == PythonFunctionType.GENERATOR) {
            GeneratorImplementor.endGenerator(functionMetadata, stackMetadata);
        } else {
            JavaPythonTypeConversionImplementor.returnValue(functionMetadata.methodVisitor, functionMetadata.method,
                    stackMetadata);
        }
    }
}
