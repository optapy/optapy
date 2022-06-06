package org.optaplanner.python.translator.opcodes.variable;

import org.objectweb.asm.Opcodes;
import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.PythonConstantsImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeType;

public class LoadConstantOpcode extends AbstractOpcode {

    public LoadConstantOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject constant = functionMetadata.pythonCompiledFunction.co_constants.get(instruction.arg);
        PythonLikeType constantType = constant.__getType();
        return stackMetadata.push(ValueSourceInfo.of(this, constantType));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject constant = functionMetadata.pythonCompiledFunction.co_constants.get(instruction.arg);
        PythonLikeType constantType = constant.__getType();

        PythonConstantsImplementor.loadConstant(functionMetadata.methodVisitor, functionMetadata.className,
                instruction.arg);
        functionMetadata.methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, constantType.getJavaTypeInternalName());
    }
}
