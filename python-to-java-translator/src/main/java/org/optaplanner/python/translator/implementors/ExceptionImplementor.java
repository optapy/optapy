package org.optaplanner.python.translator.implementors;

import java.util.Map;
import java.util.function.BiConsumer;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.LocalVariableHelper;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.errors.PythonAssertionError;
import org.optaplanner.python.translator.types.errors.PythonBaseException;
import org.optaplanner.python.translator.types.errors.PythonTraceback;

/**
 * Implementations of opcodes related to exceptions
 */
public class ExceptionImplementor {

    /**
     * Creates an AssertionError and pushes it to the stack.
     */
    public static void createAssertionError(MethodVisitor methodVisitor) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonAssertionError.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonAssertionError.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE),
                false);
    }

    /**
     * Reraise the last exception (stored in the exception local variable slot)
     */
    public static void reraiseLast(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getCurrentExceptionVariableSlot());
        methodVisitor.visitInsn(Opcodes.ATHROW);
    }

    /**
     * TOS is an exception. Reraise it (i.e. throw it).
     */
    public static void reraise(MethodVisitor methodVisitor) {
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Throwable.class));
        methodVisitor.visitInsn(Opcodes.ATHROW);
    }

    /**
     * TOS is an exception; TOS1 is a type or an exception instance. Raise
     * TOS1 with TOS as its cause.
     */
    public static void raiseWithCause(MethodVisitor methodVisitor) {
        StackManipulationImplementor.swap(methodVisitor);

        Label ifExceptionIsInstanceStart = new Label();

        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(PythonLikeType.class));
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, ifExceptionIsInstanceStart);

        // Exception is type: turn it to instance via its constructor
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.arg = 0;
        FunctionImplementor.callGenericFunction(methodVisitor, instruction); // a type is callable; calling it results in calling its constructor

        methodVisitor.visitLabel(ifExceptionIsInstanceStart);
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Throwable.class),
                "initCause", Type.getMethodDescriptor(Type.getType(Throwable.class),
                        Type.getType(Throwable.class)),
                false);
        reraise(methodVisitor);
    }

    /**
     * Raise an exception, with the stack and effect varying depending on {@code instruction.arg}:
     *
     * instruction.arg = 0: Stack is empty. Reraise the last exception.
     * instruction.arg = 1: TOS is an exception; raise it.
     * instruction.arg = 2: TOS1 is an exception/exception type, and TOS is the cause. Raise TOS1 with TOS as the cause.
     */
    public static void raiseWithOptionalExceptionAndCause(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            LocalVariableHelper localVariableHelper) {
        switch (instruction.arg) {
            case 0:
                reraiseLast(methodVisitor, localVariableHelper);
                break;
            case 1:
                reraise(methodVisitor);
                break;
            case 2:
                raiseWithCause(methodVisitor);
                break;
            default:
                throw new IllegalStateException("Impossible argc value (" + instruction.arg + ") for RAISE_VARARGS.");
        }
    }

    /**
     * Creates a try...finally block. Python also treat catch blocks as finally blocks, which
     * are handled via the {@link PythonBytecodeInstruction.OpcodeIdentifier#JUMP_IF_NOT_EXC_MATCH} instruction.
     * {@code instruction.arg} is the difference in bytecode offset to the first catch/finally block.
     */
    public static void createTryFinallyBlock(MethodVisitor methodVisitor, String className,
            PythonBytecodeInstruction instruction,
            LocalVariableHelper localVariableHelper,
            Map<Integer, Label> bytecodeCounterToLabelMap,
            BiConsumer<Integer, Runnable> bytecodeCounterCodeArgumentConsumer) {
        Label finallyStart =
                bytecodeCounterToLabelMap.computeIfAbsent(instruction.offset + instruction.arg, key -> new Label());
        Label tryStart = new Label();
        methodVisitor.visitLabel(tryStart);

        methodVisitor.visitTryCatchBlock(tryStart, finallyStart, finallyStart, Type.getInternalName(PythonBaseException.class));

        // At finallyStart, stack is expected to be:
        // [tb, exception, exception_class] ; where:
        // tb = stack trace
        // exception = exception instance
        // exception_class = the exception class
        bytecodeCounterCodeArgumentConsumer.accept(instruction.offset + instruction.arg, () -> {
            // Stack is exception
            // Duplicate exception to the current exception variable slot so we can reraise it if needed
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, localVariableHelper.getCurrentExceptionVariableSlot());

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className,
                    PythonBytecodeToJavaBytecodeTranslator.INTERPRETER_INSTANCE_FIELD_NAME,
                    Type.getDescriptor(PythonInterpreter.class));

            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonInterpreter.class),
                    "getTraceback", Type.getMethodDescriptor(Type.getType(PythonTraceback.class)),
                    true);
            methodVisitor.visitInsn(Opcodes.SWAP);

            // Stack is traceback, exception
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                    "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                    true);

            // Stack is traceback, exception, exception_class
        });
    }

    public static void endExcept(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper) {
        // Clear the exception since it was handled
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, localVariableHelper.getCurrentExceptionVariableSlot());
    }
}
