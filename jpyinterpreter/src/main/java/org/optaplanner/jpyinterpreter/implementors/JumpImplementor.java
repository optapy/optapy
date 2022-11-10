package org.optaplanner.jpyinterpreter.implementors;

import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

/**
 * Implementations of jump opcodes
 */
public class JumpImplementor {
    /**
     * Set the bytecode counter to the {@code instruction} argument.
     */
    public static void jumpAbsolute(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Map<Integer, Label> bytecodeCounterToLabelMap = functionMetadata.bytecodeCounterToLabelMap;

        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget, key -> new Label());
        methodVisitor.visitJumpInsn(Opcodes.GOTO, jumpLocation);
    }

    /**
     * Pops TOS. If TOS is true, set the bytecode counter to the {@code instruction} argument.
     */
    public static void popAndJumpIfTrue(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Map<Integer, Label> bytecodeCounterToLabelMap = functionMetadata.bytecodeCounterToLabelMap;

        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget, key -> new Label());
        if (stackMetadata.getTOSType() != BuiltinTypes.BOOLEAN_TYPE) {
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        }
        PythonConstantsImplementor.loadTrue(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
    }

    /**
     * Pops TOS. If TOS is false, set the bytecode counter to the {@code instruction} argument.
     */
    public static void popAndJumpIfFalse(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Map<Integer, Label> bytecodeCounterToLabelMap = functionMetadata.bytecodeCounterToLabelMap;

        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget, key -> new Label());
        if (stackMetadata.getTOSType() != BuiltinTypes.BOOLEAN_TYPE) {
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        }
        PythonConstantsImplementor.loadFalse(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
    }

    /**
     * Pops TOS. If TOS is not None, set the bytecode counter to {@code jumpTarget}.
     */
    public static void popAndJumpIfIsNotNone(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Map<Integer, Label> bytecodeCounterToLabelMap = functionMetadata.bytecodeCounterToLabelMap;

        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget, key -> new Label());
        PythonConstantsImplementor.loadNone(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, jumpLocation);
    }

    /**
     * Pops TOS. If TOS is None, set the bytecode counter to {@code jumpTarget}.
     */
    public static void popAndJumpIfIsNone(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Map<Integer, Label> bytecodeCounterToLabelMap = functionMetadata.bytecodeCounterToLabelMap;

        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget, key -> new Label());
        PythonConstantsImplementor.loadNone(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
    }

    /**
     * TOS and TOS1 are an exception types. If TOS1 is not an instance of TOS, set the bytecode counter to the
     * {@code instruction} argument.
     * Pop TOS and TOS1 off the stack.
     */
    public static void popAndJumpIfExceptionDoesNotMatch(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Map<Integer, Label> bytecodeCounterToLabelMap = functionMetadata.bytecodeCounterToLabelMap;
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget, key -> new Label());

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
    public static void jumpIfTrueElsePop(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Map<Integer, Label> bytecodeCounterToLabelMap = functionMetadata.bytecodeCounterToLabelMap;
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget, key -> new Label());
        methodVisitor.visitInsn(Opcodes.DUP);
        if (stackMetadata.getTOSType() != BuiltinTypes.BOOLEAN_TYPE) {
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
    public static void jumpIfFalseElsePop(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Map<Integer, Label> bytecodeCounterToLabelMap = functionMetadata.bytecodeCounterToLabelMap;
        Label jumpLocation = bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget, key -> new Label());
        methodVisitor.visitInsn(Opcodes.DUP);
        if (stackMetadata.getTOSType() != BuiltinTypes.BOOLEAN_TYPE) {
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
        }
        PythonConstantsImplementor.loadFalse(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jumpLocation);
        methodVisitor.visitInsn(Opcodes.POP);
    }
}
