package org.optaplanner.python.translator.implementors;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.CompareOp;
import org.optaplanner.python.translator.LocalVariableHelper;
import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonTernaryOperators;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.types.PythonKnownFunctionType;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeList;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Implementations of opcodes that delegate to dunder/magic methods.
 */
public class DunderOperatorImplementor {

    public static void unaryOperator(MethodVisitor methodVisitor, StackMetadata stackMetadata, PythonUnaryOperator operator) {
        PythonLikeType operand = Optional.ofNullable(stackMetadata.getTOSType()).orElseGet(PythonLikeType::getBaseType);

        Optional<PythonKnownFunctionType> maybeKnownFunctionType = operand.getMethodType(operator.getDunderMethod());
        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature = knownFunctionType.getFunctionForParameters();
            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                MethodDescriptor methodDescriptor = functionSignature.getMethodDescriptor();
                if (methodDescriptor.getParameterTypes().length < 1) {
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
                } else {
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getParameterTypes()[0].getInternalName());
                }
                functionSignature.getMethodDescriptor().callMethod(methodVisitor);
            } else {
                unaryOperator(methodVisitor, operator);
            }
        } else {
            unaryOperator(methodVisitor, operator);
        }
    }

    /**
     * Performs a unary dunder operation on TOS. Generate codes that look like this:
     *
     * <code>
     * <pre>
     *    BiFunction[List, Map, Result] operand_method = TOS.__getType().__getAttributeOrError(operator.getDunderMethod());
     *    List args = new ArrayList(1);
     *    args.set(0) = TOS
     *    pop TOS
     *    TOS' = operand_method.apply(args, null)
     * </pre>
     * </code>
     *
     */
    public static void unaryOperator(MethodVisitor methodVisitor, PythonUnaryOperator operator) {
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getAttributeOrError", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);

        // Stack is now TOS, method
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is now method, TOS
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        // Stack is now method, TOS, argList
        pushArgumentIntoList(methodVisitor);

        // Stack is now method, argList
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                true);
    }

    public static void binaryOperator(MethodVisitor methodVisitor, StackMetadata stackMetadata,
            PythonBinaryOperators operator) {
        PythonLikeType leftOperand =
                Optional.ofNullable(stackMetadata.getTypeAtStackIndex(1)).orElseGet(PythonLikeType::getBaseType);
        PythonLikeType rightOperand =
                Optional.ofNullable(stackMetadata.getTypeAtStackIndex(0)).orElseGet(PythonLikeType::getBaseType);

        Optional<PythonKnownFunctionType> maybeKnownFunctionType = leftOperand.getMethodType(operator.getDunderMethod());
        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature = knownFunctionType.getFunctionForParameters(rightOperand);
            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                MethodDescriptor methodDescriptor = functionSignature.getMethodDescriptor();

                if (methodDescriptor.getParameterTypes().length < 2) {
                    methodVisitor.visitInsn(Opcodes.SWAP);
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
                    methodVisitor.visitInsn(Opcodes.SWAP);
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getParameterTypes()[0].getInternalName());
                } else {
                    methodVisitor.visitInsn(Opcodes.SWAP);
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getParameterTypes()[0].getInternalName());
                    methodVisitor.visitInsn(Opcodes.SWAP);
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getParameterTypes()[1].getInternalName());
                }
                functionSignature.getMethodDescriptor().callMethod(methodVisitor);
            } else {
                binaryOperator(methodVisitor, operator);
            }
        } else {
            binaryOperator(methodVisitor, operator);
        }
    }

    /**
     * Performs a binary dunder operation on TOS and TOS1. Generate codes that look like this:
     *
     * <code>
     * <pre>
     *    BiFunction[List, Map, Result] operand_method = TOS1.__getType().__getAttributeOrError(operator.getDunderMethod());
     *    List args = new ArrayList(2);
     *    args.set(0) = TOS1
     *    args.set(1) = TOS
     *    pop TOS, TOS1
     *    TOS' = operand_method.apply(args, null)
     * </pre>
     * </code>
     *
     */
    public static void binaryOperator(MethodVisitor methodVisitor, PythonBinaryOperators operator) {
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now TOS, TOS1
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getAttributeOrError", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);

        // Stack is now TOS, TOS1, method
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is now method, TOS, TOS1
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        // Stack is now method, TOS, TOS1, argList
        pushArgumentIntoList(methodVisitor);
        pushArgumentIntoList(methodVisitor);

        // Stack is now method, argList
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                true);
    }

    /**
     * Performs a ternary dunder operation on TOS, TOS1 and TOS2. Generate codes that look like this:
     *
     * <code>
     * <pre>
     *    BiFunction[List, Map, Result] operand_method = TOS2.__getType().__getAttributeOrError(operator.getDunderMethod());
     *    List args = new ArrayList(2);
     *    args.set(0) = TOS2
     *    args.set(1) = TOS1
     *    args.set(2) = TOS
     *    pop TOS, TOS1, TOS2
     *    TOS' = operand_method.apply(args, null)
     * </pre>
     * </code>
     *
     */
    public static void ternaryOperator(MethodVisitor methodVisitor, PythonTernaryOperators operator,
            LocalVariableHelper localVariableHelper) {
        StackManipulationImplementor.rotateThree(methodVisitor);
        methodVisitor.visitInsn(Opcodes.SWAP);
        // Stack is now TOS, TOS1, TOS2
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getAttributeOrError", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);
        // Stack is now TOS, TOS1, TOS2, method
        StackManipulationImplementor.rotateFour(methodVisitor, localVariableHelper);

        // Stack is now method, TOS, TOS1, TOS2
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        // Stack is now method, TOS, TOS1, TOS2, argList
        pushArgumentIntoList(methodVisitor);
        pushArgumentIntoList(methodVisitor);
        pushArgumentIntoList(methodVisitor);

        // Stack is now method, argList
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                true);
    }

    /**
     * TOS is a list and TOS1 is an argument. Pushes TOS1 into TOS, and leave TOS on the stack (pops TOS1).
     */
    private static void pushArgumentIntoList(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                "add",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                true);
        methodVisitor.visitInsn(Opcodes.POP);
    }

    /**
     * Compares TOS and TOS1 via their dunder methods. {@code CompareOp} indicates the operation
     * to perform.
     */
    public static void compareValues(MethodVisitor methodVisitor, StackMetadata stackMetadata, CompareOp op) {
        switch (op) {
            case LESS_THAN:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperators.LESS_THAN);
                break;
            case LESS_THAN_OR_EQUALS:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperators.LESS_THAN_OR_EQUAL);
                break;
            case EQUALS:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperators.EQUAL);
                break;
            case NOT_EQUALS:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperators.NOT_EQUAL);
                break;
            case GREATER_THAN:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperators.GREATER_THAN);
                break;
            case GREATER_THAN_OR_EQUALS:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperators.GREATER_THAN_OR_EQUAL);
                break;
            default:
                throw new IllegalStateException("Unhandled branch: " + op);
        }
    }
}
