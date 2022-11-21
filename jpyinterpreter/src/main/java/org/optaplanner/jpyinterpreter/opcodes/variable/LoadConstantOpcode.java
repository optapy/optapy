package org.optaplanner.jpyinterpreter.opcodes.variable;

import org.objectweb.asm.Opcodes;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.PythonConstantsImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class LoadConstantOpcode extends AbstractOpcode {

    public LoadConstantOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject constant = functionMetadata.pythonCompiledFunction.co_constants.get(instruction.arg);
        PythonLikeType constantType = constant.__getGenericType();
        return stackMetadata.push(ValueSourceInfo.of(this, constantType));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject constant = functionMetadata.pythonCompiledFunction.co_constants.get(instruction.arg);
        PythonLikeType constantType = constant.__getGenericType();

        PythonConstantsImplementor.loadConstant(functionMetadata.methodVisitor, functionMetadata.className,
                instruction.arg);
        functionMetadata.methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, constantType.getJavaTypeInternalName());
    }
}
