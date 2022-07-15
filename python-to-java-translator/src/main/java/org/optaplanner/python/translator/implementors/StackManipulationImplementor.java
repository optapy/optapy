package org.optaplanner.python.translator.implementors;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.optaplanner.python.translator.LocalVariableHelper;
import org.optaplanner.python.translator.StackMetadata;

/**
 * Implementations of stack manipulation opcodes (rotations, pop, duplication, etc.)
 */
public class StackManipulationImplementor {

    /**
     * Swaps TOS and TOS1:
     *
     * (i.e. ..., TOS1, TOS -> ..., TOS, TOS1)
     */
    public static void swap(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.SWAP);
    }

    /**
     * Move TOS down two places, and pushes TOS1 and TOS2 up one:
     *
     * (i.e. ..., TOS2, TOS1, TOS -> ..., TOS, TOS2, TOS1)
     */
    public static void rotateThree(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
    }

    /**
     * Move TOS down three places, and pushes TOS1, TOS2 and TOS3 up one:
     *
     * (i.e. ..., TOS3, TOS2, TOS1, TOS -> ..., TOS, TOS3, TOS2, TOS1)
     */
    public static void rotateFour(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper) {
        int secondFromStack = localVariableHelper.newLocal();
        int thirdFromStack = localVariableHelper.newLocal();

        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        methodVisitor.visitVarInsn(Opcodes.ASTORE, secondFromStack);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, thirdFromStack);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, thirdFromStack);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, secondFromStack);

        localVariableHelper.freeLocal();
        localVariableHelper.freeLocal();
    }

    /**
     * Pops TOS.
     *
     * (i.e. ..., TOS -> ...)
     */
    public static void popTOS(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.POP);
    }

    /**
     * Duplicates TOS.
     *
     * (i.e. ..., TOS -> ..., TOS, TOS)
     */
    public static void duplicateTOS(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.DUP);
    }

    /**
     * Duplicates TOS and TOS1.
     *
     * (i.e. ..., TOS1, TOS -> ..., TOS1, TOS, TOS1, TOS)
     */
    public static void duplicateTOSAndTOS1(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.DUP2);
    }

    /**
     * Copies TOS[posFromTOS] to TOS, leaving other stack elements in their original place
     *
     * (i.e. ..., TOS[posFromTOS], ..., TOS2, TOS1, TOS -> ..., TOS[posFromTOS], ..., TOS2, TOS1, TOS, TOS[posFromTOS])
     */
    public static void duplicateToTOS(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper, int posFromTOS) {
        List<Integer> localList = new ArrayList<>(posFromTOS);

        // Store TOS...TOS[posFromTOS - 1] into local variables
        for (int i = 0; i < posFromTOS; i++) {
            int local = localVariableHelper.newLocal();
            localList.add(local);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, local);
        }

        // Duplicate TOS[posFromTOS]
        methodVisitor.visitInsn(Opcodes.DUP);

        // Restore TOS...TOS[posFromTOS - 1] from local variables, swaping the duplicated value to keep it on TOS
        for (int i = posFromTOS - 1; i >= 0; i--) {
            int local = localList.get(i);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, local);
            methodVisitor.visitInsn(Opcodes.SWAP);
            localVariableHelper.freeLocal();
        }
    }

    /**
     * Copies TOS to TOS[posFromTOS], moving other stack elements up by one
     *
     * (i.e. ..., TOS[posFromTOS], ..., TOS2, TOS1, TOS -> ..., TOS, TOS[posFromTOS] ..., TOS2, TOS1)
     */
    public static void shiftTOSDownBy(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper, int posFromTOS) {
        List<Integer> localList = new ArrayList<>(posFromTOS + 1);

        if (posFromTOS == 0) {
            // A rotation of 0 is a no-op
            return;
        }

        // Store TOS...TOS[posFromTOS - 1] into local variables
        for (int i = 0; i < posFromTOS + 1; i++) {
            int local = localVariableHelper.newLocal();
            localList.add(local);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, local);
        }

        // Copy TOS to this position
        methodVisitor.visitVarInsn(Opcodes.ALOAD, localList.get(0));

        // Restore TOS[1]...TOS[posFromTOS] from local variables
        for (int i = posFromTOS; i > 0; i--) {
            int local = localList.get(i);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, local);
            localVariableHelper.freeLocal();
        }
    }

    public static int[] storeStack(MethodVisitor methodVisitor, StackMetadata stackMetadata) {
        int[] stackLocalVariables = new int[stackMetadata.getStackSize()];
        for (int i = stackLocalVariables.length - 1; i >= 0; i--) {
            stackLocalVariables[i] = stackMetadata.localVariableHelper.newLocal();
            methodVisitor.visitVarInsn(Opcodes.ASTORE, i);
        }
        for (int i = 0; i < stackLocalVariables.length; i++) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, i);
        }

        return stackLocalVariables;
    }

    public static void restoreStack(MethodVisitor methodVisitor, int[] stackLocalVariables) {
        for (int i = 0; i < stackLocalVariables.length; i++) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, i);
        }
    }
}
