package org.optaplanner.python.translator.implementors;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonGeneratorTranslator;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.StackMetadata;

public class GeneratorImplementor {
    public static void yieldValue(PythonBytecodeInstruction instruction, FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // First, store TOS in yieldedValue
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.YIELDED_VALUE,
                Type.getDescriptor(PythonLikeObject.class));

        // Next, store stack in generatorStack
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(ArrayList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(stackMetadata.getStackSize() - 1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STACK,
                Type.getDescriptor(List.class));

        for (int i = 0; i < stackMetadata.getStackSize() - 1; i++) {
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "add",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)), true);
            methodVisitor.visitInsn(Opcodes.POP); // Do not use return value of add
        }
        methodVisitor.visitInsn(Opcodes.POP);

        // Set the generator state
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitLdcInsn(instruction.offset + 1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STATE,
                Type.INT_TYPE.getDescriptor());

        methodVisitor.visitInsn(Opcodes.RETURN);
    }

    public static void endGenerator(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // First, store TOS in yieldedValue
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.YIELDED_VALUE,
                Type.getDescriptor(PythonLikeObject.class));

        // Next, set generatorStack to null
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STACK,
                Type.getDescriptor(List.class));

        // Set the generator state
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitLdcInsn(-1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STATE,
                Type.INT_TYPE.getDescriptor());

        methodVisitor.visitInsn(Opcodes.RETURN);
    }
}
