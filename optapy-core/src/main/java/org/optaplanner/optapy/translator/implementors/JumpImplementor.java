package org.optaplanner.optapy.translator.implementors;

import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;
import org.optaplanner.optapy.translator.PythonUnaryOperator;

/**
 * Implementations of jump opcodes
 */
public class JumpImplementor {

    /**
     * Increment the bytecode counter by the {@code instruction} argument.
     */
    public static void jumpRelative(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.offset + instruction.arg, key -> new Label());
        methodVisitor.visitJumpInsn(Opcodes.GOTO, jumpLocation);
    }

    /**
     * Set the bytecode counter to the {@code instruction} argument.
     */
    public static void jumpAbsolute(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        methodVisitor.visitJumpInsn(Opcodes.GOTO, jumpLocation);
    }

    /**
     * Pops TOS. If TOS is true, set the bytecode counter to the {@code instruction} argument.
     */
    public static void popAndJumpIfTrue(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        PythonConstantsImplementor.loadTrue(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
    }

    /**
     * Pops TOS. If TOS is false, set the bytecode counter to the {@code instruction} argument.
     */
    public static void popAndJumpIfFalse(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        PythonConstantsImplementor.loadFalse(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
    }

    /**
     * If TOS is true, keep TOS on the stack and set the bytecode counter to the {@code instruction} argument.
     * Otherwise, pop TOS.
     */
    public static void jumpIfTrueElsePop(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        methodVisitor.visitInsn(Opcodes.DUP);
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        PythonConstantsImplementor.loadTrue(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
        methodVisitor.visitInsn(Opcodes.POP);
    }

    /**
     * If TOS is false, keep TOS on the stack and set the bytecode counter to the {@code instruction} argument.
     * Otherwise, pop TOS.
     */
    public static void jumpIfFalseElsePop(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        methodVisitor.visitInsn(Opcodes.DUP);
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        PythonConstantsImplementor.loadFalse(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
        methodVisitor.visitInsn(Opcodes.POP);
    }
}
