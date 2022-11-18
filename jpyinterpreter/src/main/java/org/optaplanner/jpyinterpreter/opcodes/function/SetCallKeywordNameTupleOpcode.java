package org.optaplanner.jpyinterpreter.opcodes.function;

import java.util.List;
import java.util.stream.Collectors;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.implementors.FunctionImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.PythonString;

public class SetCallKeywordNameTupleOpcode extends AbstractOpcode {

    public SetCallKeywordNameTupleOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.setCallKeywordNameList(
                ((List<PythonString>) functionMetadata.pythonCompiledFunction.co_constants.get(instruction.arg))
                        .stream().map(PythonString::getValue).collect(Collectors.toList()));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.setCallKeywordNameTuple(functionMetadata, stackMetadata, instruction.arg);
    }
}
