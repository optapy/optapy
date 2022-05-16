package org.optaplanner.python.translator.opcodes.variable;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.implementors.VariableImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeType;

public class LoadGlobalOpcode extends AbstractOpcode {

    public LoadGlobalOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.push(PythonLikeType.getBaseType());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.loadGlobalVariable(functionMetadata.methodVisitor, functionMetadata.className,
                functionMetadata.pythonCompiledFunction, instruction);
    }
}
