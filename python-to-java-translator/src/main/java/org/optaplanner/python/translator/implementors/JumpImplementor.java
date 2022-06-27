package org.optaplanner.python.translator.implementors;

import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.types.PythonBoolean;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Implementations of jump opcodes
 */
public class JumpImplementor {

    /**
     * Increment the bytecode counter by the {@code instruction} argument.
     */
    public static void jumpRelative(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation =
                bytecodeCounterToLabelMap.computeIfAbsent(instruction.offset + instruction.arg, key -> new Label());
        methodVisitor.visitJumpInsn(Opcodes.GOTO, jumpLocation);
    }

    /**
     * Set the bytecode counter to the {@code instruction} argument.
     */
    public static void jumpAbsolute(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        methodVisitor.visitJumpInsn(Opcodes.GOTO, jumpLocation);
    }

    /**
     * Pops TOS. If TOS is true, set the bytecode counter to the {@code instruction} argument.
     */
    public static void popAndJumpIfTrue(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            StackMetadata stackMetadata, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        if (stackMetadata.getTOSType() != PythonBoolean.BOOLEAN_TYPE) {
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        }
        PythonConstantsImplementor.loadTrue(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
    }

    /**
     * Pops TOS. If TOS is false, set the bytecode counter to the {@code instruction} argument.
     */
    public static void popAndJumpIfFalse(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            StackMetadata stackMetadata, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        if (stackMetadata.getTOSType() != PythonBoolean.BOOLEAN_TYPE) {
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        }
        PythonConstantsImplementor.loadFalse(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
    }

    /**
     * TOS and TOS1 are an exception types. If TOS1 is not an instance of TOS, set the bytecode counter to the
     * {@code instruction} argument.
     * Pop TOS and TOS1 off the stack.
     */
    public static void popAndJumpIfExceptionDoesNotMatch(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());

        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeType.class));
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeType.class),
                "isSubclassOf", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(PythonLikeType.class)),
                false);
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, jumpLocation);
    }

    /**
     * If TOS is true, keep TOS on the stack and set the bytecode counter to the {@code instruction} argument.
     * Otherwise, pop TOS.
     */
    public static void jumpIfTrueElsePop(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            StackMetadata stackMetadata, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        methodVisitor.visitInsn(Opcodes.DUP);
        if (stackMetadata.getTOSType() != PythonBoolean.BOOLEAN_TYPE) {
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        }
        PythonConstantsImplementor.loadTrue(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
        methodVisitor.visitInsn(Opcodes.POP);
    }

    /**
     * If TOS is false, keep TOS on the stack and set the bytecode counter to the {@code instruction} argument.
     * Otherwise, pop TOS.
     */
    public static void jumpIfFalseElsePop(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            StackMetadata stackMetadata, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(instruction.arg, key -> new Label());
        methodVisitor.visitInsn(Opcodes.DUP);
        if (stackMetadata.getTOSType() != PythonBoolean.BOOLEAN_TYPE) {
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        }
        PythonConstantsImplementor.loadFalse(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
        methodVisitor.visitInsn(Opcodes.POP);
    }
}
