package org.optaplanner.jpyinterpreter.implementors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.ExceptionBlock;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.LocalVariableHelper;
import org.optaplanner.jpyinterpreter.OpcodeIdentifier;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.jpyinterpreter.PythonInterpreter;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.opcodes.OpcodeWithoutSource;
import org.optaplanner.jpyinterpreter.types.BoundPythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.types.errors.PythonAssertionError;
import org.optaplanner.jpyinterpreter.types.errors.PythonBaseException;
import org.optaplanner.jpyinterpreter.types.errors.PythonTraceback;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;

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
        localVariableHelper.readCurrentException(methodVisitor);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Throwable.class));
        methodVisitor.visitInsn(Opcodes.ATHROW);
    }

    /**
     * TOS is an exception or an exception type. Reraise it (i.e. throw it).
     */
    public static void reraise(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(Throwable.class));

        Label ifNotThrowable = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, ifNotThrowable);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Throwable.class));
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(ifNotThrowable);

        // Construct an instance of the type and throw it
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
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

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(PythonLikeType.class));
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, ifExceptionIsInstanceStart);

        // Exception is type: turn it to instance via its constructor
        FunctionImplementor.callGenericFunction(methodVisitor, 0); // a type is callable; calling it results in calling its constructor

        methodVisitor.visitLabel(ifExceptionIsInstanceStart);

        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Throwable.class));
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Throwable.class));
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
     * instruction.arg = 1: TOS is an exception or exception type; raise it.
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
     * are handled via the {@link OpcodeIdentifier#JUMP_IF_NOT_EXC_MATCH} instruction.
     * {@code instruction.arg} is the difference in bytecode offset to the first catch/finally block.
     */
    public static void createTryFinallyBlock(MethodVisitor methodVisitor, String className,
            int handlerLocation,
            StackMetadata stackMetadata,
            Map<Integer, Label> bytecodeCounterToLabelMap,
            BiConsumer<Integer, Runnable> bytecodeCounterCodeArgumentConsumer) {
        // Store the stack in local variables so the except block has access to them
        int[] stackLocals = StackManipulationImplementor.storeStack(methodVisitor, stackMetadata);

        Label finallyStart =
                bytecodeCounterToLabelMap.computeIfAbsent(handlerLocation,
                        key -> new Label());
        Label tryStart = new Label();

        methodVisitor.visitTryCatchBlock(tryStart, finallyStart, finallyStart, Type.getInternalName(PythonBaseException.class));

        methodVisitor.visitLabel(tryStart);

        // At finallyStart, stack is expected to be:
        // [(stack-before-try), instruction, level, label, tb, exception, exception_class] ; where:
        // (stack-before-try) = the stack state before the try statement
        // (see https://github.com/python/cpython/blob/b6558d768f19584ad724be23030603280f9e6361/Python/compile.c#L3241-L3268 )
        // instruction =  instruction that created the block
        // level = stack depth at the time the block was created
        // label = label to go to for exception
        // (see https://stackoverflow.com/a/66720684)
        // tb = stack trace
        // exception = exception instance
        // exception_class = the exception class
        bytecodeCounterCodeArgumentConsumer.accept(handlerLocation, () -> {
            // Stack is exception
            // Duplicate exception to the current exception variable slot so we can reraise it if needed
            stackMetadata.localVariableHelper.writeCurrentException(methodVisitor);
            StackManipulationImplementor.restoreStack(methodVisitor, stackMetadata, stackLocals);

            // Stack is (stack-before-try)

            // Instruction
            PythonConstantsImplementor.loadNone(methodVisitor); // We don't use it

            // Stack is (stack-before-try), instruction

            // Stack Size
            methodVisitor.visitLdcInsn(stackMetadata.getStackSize());
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonInteger.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.INT_TYPE),
                    false);

            // Stack is (stack-before-try), instruction, stack-size, exception

            // Label
            PythonConstantsImplementor.loadNone(methodVisitor); // We don't use it

            // Stack is (stack-before-try), instruction, stack-size, label

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, className); // needed cast; type confusion on this?
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className,
                    PythonBytecodeToJavaBytecodeTranslator.INTERPRETER_INSTANCE_FIELD_NAME,
                    Type.getDescriptor(PythonInterpreter.class));

            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonInterpreter.class),
                    "getTraceback", Type.getMethodDescriptor(Type.getType(PythonTraceback.class)),
                    true);

            // Stack is (stack-before-try), instruction, stack-size, label, traceback

            // Load exception
            stackMetadata.localVariableHelper.readCurrentException(methodVisitor);

            // Stack is (stack-before-try), instruction, stack-size, label, traceback, exception

            // Get exception class
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                    "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                    true);

            // Stack is (stack-before-try), instruction, stack-size, label, traceback, exception, exception_class
        });
    }

    public static void startExceptOrFinally(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        LocalVariableHelper localVariableHelper = stackMetadata.localVariableHelper;

        // Clear the exception since it was handled
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        localVariableHelper.writeCurrentException(methodVisitor);

        // Pop off the block
        if (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) {
            methodVisitor.visitInsn(Opcodes.POP);
        } else {
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.POP);
        }
    }

    public static void startWith(int jumpTarget, FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        methodVisitor.visitInsn(Opcodes.DUP); // duplicate context_manager twice; need one for __enter__, two for __exit__
        methodVisitor.visitInsn(Opcodes.DUP);

        // First load the method __exit__
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn("__exit__");
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getAttributeOrError",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(String.class)),
                true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeFunction.class));

        // bind it to the context_manager
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(BoundPythonLikeFunction.class));
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(BoundPythonLikeFunction.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeFunction.class)),
                false);

        // Swap __exit__ method with duplicated context_manager
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Call __enter__
        DunderOperatorImplementor.unaryOperator(methodVisitor, stackMetadata, PythonUnaryOperator.ENTER);

        int enterResult = stackMetadata.localVariableHelper.newLocal();

        // store enter result in temp, so it does not get saved in try block
        stackMetadata.localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), enterResult);

        // Create a try...finally block pointing to delta
        StackMetadata currentStackMetadata = stackMetadata
                .pop()
                .push(ValueSourceInfo.of(new OpcodeWithoutSource(), PythonLikeFunction.getFunctionType(),
                        stackMetadata.getTOSValueSource()));

        createTryFinallyBlock(methodVisitor, functionMetadata.className, jumpTarget,
                currentStackMetadata,
                functionMetadata.bytecodeCounterToLabelMap,
                (bytecodeIndex, runnable) -> {
                    functionMetadata.bytecodeCounterToCodeArgumenterList
                            .computeIfAbsent(bytecodeIndex, key -> new ArrayList<>()).add(runnable);
                });

        // Push enter result back to the stack
        stackMetadata.localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), enterResult);
        // cannot free, since try block store stack in locals => freeing enterResult messes up indexing of locals
    }

    public static void startExceptBlock(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            ExceptionBlock exceptionBlock) {
        // In Python 3.11 and above, the stack here is
        // [(stack-before-try), exception]

        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // Stack is exception
        // Duplicate exception to the current exception variable slot so we can reraise it if needed
        stackMetadata.localVariableHelper.writeCurrentException(methodVisitor);
        StackManipulationImplementor.restoreExceptionTableStack(functionMetadata, stackMetadata, exceptionBlock);

        // Stack is (stack-before-try)
        if (exceptionBlock.isPushLastIndex()) {
            // Load 0 for last index since we don't use it
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(PythonInteger.class), "ZERO",
                    Type.getDescriptor(PythonInteger.class));
        }

        // Load exception
        stackMetadata.localVariableHelper.readCurrentException(methodVisitor);
        // Stack is (stack-before-try), index?, exception
    }

    public static void pushExcInfo(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        LocalVariableHelper localVariableHelper = stackMetadata.localVariableHelper;

        localVariableHelper.readCurrentException(methodVisitor);
        methodVisitor.visitInsn(Opcodes.SWAP);
    }

    public static void checkExcMatch(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        methodVisitor.visitInsn(Opcodes.DUP2);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeType.class));
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeType.class), "isInstance",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(PythonLikeObject.class)),
                false);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonBoolean.class), "valueOf",
                Type.getMethodDescriptor(Type.getType(PythonBoolean.class), Type.BOOLEAN_TYPE),
                false);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.POP);
    }

    public static void handleExceptionInWith(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        LocalVariableHelper localVariableHelper = stackMetadata.localVariableHelper;

        // First, store the top 7 items in the stack to be restored later
        int exceptionType = localVariableHelper.newLocal();
        int exception = localVariableHelper.newLocal();
        int traceback = localVariableHelper.newLocal();
        int label = localVariableHelper.newLocal();
        int stackSize = localVariableHelper.newLocal();
        int instruction = localVariableHelper.newLocal();
        int exitFunction = localVariableHelper.newLocal();

        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), exceptionType);
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), exception);
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), traceback);
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), label);
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), stackSize);
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), instruction);
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), exitFunction);

        // load exitFunction
        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), exitFunction);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeFunction.class));

        // create the argument list
        // (exc_type, exc_value, traceback)
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(ArrayList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(3);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(ArrayList.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
                false);

        methodVisitor.visitInsn(Opcodes.DUP);
        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), exceptionType);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                "add", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                true);
        methodVisitor.visitInsn(Opcodes.POP);

        methodVisitor.visitInsn(Opcodes.DUP);
        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), exception);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                "add", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                true);
        methodVisitor.visitInsn(Opcodes.POP);

        methodVisitor.visitInsn(Opcodes.DUP);
        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), traceback);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                "add", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                true);
        methodVisitor.visitInsn(Opcodes.POP);

        // Use null for keywords
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        // Call the exit function
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);

        // Restore the stack, raising the returned value to the top of the stack

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), exitFunction);
        methodVisitor.visitInsn(Opcodes.SWAP);

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), instruction);
        methodVisitor.visitInsn(Opcodes.SWAP);

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), stackSize);
        methodVisitor.visitInsn(Opcodes.SWAP);

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), label);
        methodVisitor.visitInsn(Opcodes.SWAP);

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), traceback);
        methodVisitor.visitInsn(Opcodes.SWAP);

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), exception);
        methodVisitor.visitInsn(Opcodes.SWAP);

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), exceptionType);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Free the 7 temps
        localVariableHelper.freeLocal();
        localVariableHelper.freeLocal();
        localVariableHelper.freeLocal();

        localVariableHelper.freeLocal();
        localVariableHelper.freeLocal();
        localVariableHelper.freeLocal();

        localVariableHelper.freeLocal();
    }
}
