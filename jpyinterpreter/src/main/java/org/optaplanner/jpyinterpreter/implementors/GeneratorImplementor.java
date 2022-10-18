package org.optaplanner.jpyinterpreter.implementors;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonGeneratorTranslator;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.types.PythonGenerator;
import org.optaplanner.jpyinterpreter.types.errors.StopIteration;

public class GeneratorImplementor {

    public static void restoreGeneratorState(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STACK,
                Type.getDescriptor(List.class));

        for (int i = stackMetadata.getStackSize() - 1; i >= 0; i--) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, stackMetadata.getTypeAtStackIndex(i).getJavaTypeInternalName());
            methodVisitor.visitInsn(Opcodes.SWAP);
        }
        methodVisitor.visitInsn(Opcodes.POP);
    }

    private static void saveGeneratorState(PythonBytecodeInstruction instruction, FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // Store stack in generatorStack
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(ArrayList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(stackMetadata.getStackSize());
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
    }

    public static void yieldValue(PythonBytecodeInstruction instruction, FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // First, store TOS in yieldedValue
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.YIELDED_VALUE,
                Type.getDescriptor(PythonLikeObject.class));

        // Next, save stack and generator position
        saveGeneratorState(instruction, functionMetadata, stackMetadata);

        // return control to the caller
        methodVisitor.visitInsn(Opcodes.RETURN);
    }

    public static void yieldFrom(PythonBytecodeInstruction instruction, FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // TODO: Find out what TOS, which is usually None, is used for

        // Pop TOS (Unknown what is used for)
        methodVisitor.visitInsn(Opcodes.POP);

        // Store the subiterator into yieldFromIterator
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className,
                PythonGeneratorTranslator.YIELD_FROM_ITERATOR,
                Type.getDescriptor(PythonLikeObject.class));

        // Save stack and position
        // Store stack in both locals and fields, just in case the iterator stops iteration immediately
        int[] storedStack = StackManipulationImplementor.storeStack(methodVisitor, stackMetadata.pop(2));
        saveGeneratorState(instruction, functionMetadata, stackMetadata.pop(2));

        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label catchStartLabel = new Label();
        Label catchEndLabel = new Label();

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, catchStartLabel,
                Type.getInternalName(StopIteration.class));

        methodVisitor.visitLabel(tryStartLabel);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, functionMetadata.className,
                PythonGeneratorTranslator.YIELD_FROM_ITERATOR,
                Type.getDescriptor(PythonLikeObject.class));
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.YIELDED_VALUE,
                Type.getDescriptor(PythonLikeObject.class));
        methodVisitor.visitInsn(Opcodes.RETURN); // subiterator yielded something; return control to caller

        methodVisitor.visitLabel(tryEndLabel);

        methodVisitor.visitLabel(catchStartLabel);
        methodVisitor.visitInsn(Opcodes.POP); // pop the StopIteration exception
        methodVisitor.visitLabel(catchEndLabel);

        // Set yieldFromIterator to null since it is finished
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className,
                PythonGeneratorTranslator.YIELD_FROM_ITERATOR,
                Type.getDescriptor(PythonLikeObject.class));

        // Restore the stack, since subiterator was empty, and resume execution
        StackManipulationImplementor.restoreStack(methodVisitor, stackMetadata.pop(2), storedStack);

        // Since the subiterator was empty, push None to TOS
        PythonConstantsImplementor.loadNone(methodVisitor);
    }

    public static void getYieldFromIter(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        Label isGeneratorOrCoroutine = new Label();

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(PythonGenerator.class));
        methodVisitor.visitJumpInsn(Opcodes.IFNE, isGeneratorOrCoroutine);

        // not a generator/coroutine
        DunderOperatorImplementor.unaryOperator(methodVisitor, stackMetadata, PythonUnaryOperator.ITERATOR);

        methodVisitor.visitLabel(isGeneratorOrCoroutine);
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

    public static void generatorStart(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        methodVisitor.visitInsn(Opcodes.POP); // Despite stackMetadata says it empty, the stack actually has
                                              // one item: the first sent item, which MUST BE None
    }
}
