package org.optaplanner.optapy.translator.implementors;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.CompareOp;
import org.optaplanner.optapy.translator.PythonBinaryOperators;
import org.optaplanner.optapy.translator.PythonUnaryOperator;
import org.optaplanner.optapy.translator.types.PythonLikeFunction;
import org.optaplanner.optapy.translator.types.PythonLikeList;
import org.optaplanner.optapy.translator.types.PythonLikeType;

/**
 * Implementations of opcodes that delegate to dunder/magic methods.
 */
public class DunderOperatorImplementor {

    /**
     * Performs a unary dunder operation on TOS. Generate codes that look like this:
     *
     * <code>
     * <pre>
     *    BiFunction[List, Map, Result] operand_method = TOS.__type__().__getattribute__(operator.getDunderMethod());
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
                                      "__type__", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                      true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                      "__getattribute__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                                   Type.getType(String.class)),
                                      true);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.POP);

        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeList.class),
                                      "reverseAdd",
                                      Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class)),
                                      false);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                                      "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                           Type.getType(List.class),
                                                                           Type.getType(Map.class)),
                                      true);
    }

    /**
     * Performs a binary dunder operation on TOS and TOS1. Generate codes that look like this:
     *
     * <code>
     * <pre>
     *    BiFunction[List, Map, Result] operand_method = TOS1.__type__().__getattribute__(operator.getDunderMethod());
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
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                      "__type__", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                      true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                      "__getattribute__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                                   Type.getType(String.class)),
                                      true);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                                      "add",
                                      Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                                      true);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                                      "add",
                                      Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                                      true);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                                      "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                                Type.getType(List.class),
                                                                                Type.getType(Map.class)),
                                      true);
    }

    /**
     * Compares TOS and TOS1 via their dunder methods. {@code CompareOp} indicates the operation
     * to perform.
     */
    public static void compareValues(MethodVisitor methodVisitor, CompareOp op) {
        switch (op) {
            case LESS_THAN:
                binaryOperator(methodVisitor, PythonBinaryOperators.LESS_THAN);
                break;
            case LESS_THAN_OR_EQUALS:
                binaryOperator(methodVisitor, PythonBinaryOperators.LESS_THAN_OR_EQUAL);
                break;
            case EQUALS:
                binaryOperator(methodVisitor, PythonBinaryOperators.EQUAL);
                break;
            case NOT_EQUALS:
                binaryOperator(methodVisitor, PythonBinaryOperators.NOT_EQUAL);
                break;
            case GREATER_THAN:
                binaryOperator(methodVisitor, PythonBinaryOperators.GREATER_THAN);
                break;
            case GREATER_THAN_OR_EQUALS:
                binaryOperator(methodVisitor, PythonBinaryOperators.GREATER_THAN_OR_EQUAL);
                break;
            default:
                throw new IllegalStateException("Unhandled branch: " + op);
        }
    }
}
