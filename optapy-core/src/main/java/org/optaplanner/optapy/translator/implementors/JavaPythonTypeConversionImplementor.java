package org.optaplanner.optapy.translator.implementors;

import java.lang.reflect.Method;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.translator.LocalVariableHelper;
import org.optaplanner.optapy.translator.types.PythonBoolean;
import org.optaplanner.optapy.translator.types.PythonFloat;
import org.optaplanner.optapy.translator.types.PythonInteger;
import org.optaplanner.optapy.translator.types.PythonNumber;

/**
 * Implementations of opcodes and operations that require Java to Python or Python to Java conversions.
 */
public class JavaPythonTypeConversionImplementor {

    /**
     * Convert the Java {@code constant} to its Python equivalent and push it onto the stack.
     *
     * @param constant The Java constant to load
     */
    public static void loadConstant(MethodVisitor methodVisitor, Object constant) {
        if (constant == null) {
            PythonConstantsImplementor.loadNone(methodVisitor);
            return;
        }

        if (constant instanceof Number || constant instanceof String) {
            if (constant instanceof Byte || constant instanceof Short || constant instanceof Integer) {
                constant = ((Number) constant).longValue();
            } else if (constant instanceof Float) {
                constant = ((Number) constant).doubleValue();
            }
            if ( constant instanceof Long) {
                methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonInteger.class));
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(constant);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonInteger.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE), false);
            } else if (constant instanceof Double) {
                methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonFloat.class));
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(constant);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonFloat.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.DOUBLE_TYPE), false);
            }
            return;
        }

        if (constant instanceof Boolean) {
            if (Boolean.TRUE.equals(constant)) {
                PythonConstantsImplementor.loadTrue(methodVisitor);
            } else {
                PythonConstantsImplementor.loadFalse(methodVisitor);
            }
            return;
        }
        throw new UnsupportedOperationException("Cannot load constant (" + constant + ").");
    }

    /**
     * If {@code method} return type is not void, convert TOS into its Java equivalent and return it.
     * If {@code method} return type is void, immediately return.
     *
     * @param method The method that is being implemented.
     */
    public static void returnValue(MethodVisitor methodVisitor, Method method) {
        Class<?> returnType = method.getReturnType();

        if (void.class.equals(returnType)) {
            methodVisitor.visitInsn(Opcodes.RETURN);
            return;
        }

        if (!returnType.isPrimitive()) {
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return;
        }

        String wrapperClassName;
        String methodName;
        String methodDescriptor;
        int returnOpcode;

        if (byte.class.isAssignableFrom(returnType) ||
            short.class.isAssignableFrom(returnType) ||
            int.class.isAssignableFrom(returnType) ||
            long.class.isAssignableFrom(returnType) ||
            float.class.isAssignableFrom(returnType) ||
            double.class.isAssignableFrom(returnType)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonNumber.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                          Type.getInternalName(PythonNumber.class),
                                          "getValue",
                                          Type.getMethodDescriptor(Type.getType(Number.class)),
                                          true);
        }

        if (boolean.class.equals(returnType)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonBoolean.class));
            wrapperClassName = Type.getInternalName(PythonBoolean.class);
            methodName = "getValue";
            methodDescriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE);
            returnOpcode = Opcodes.IRETURN;
        } else if (char.class.equals(returnType)) {
            throw new IllegalStateException("Unhandled case for primitive type (" + returnType + ").");
            // returnOpcode = Opcodes.IRETURN;
        } else if (byte.class.equals(returnType)) {
            wrapperClassName = Type.getInternalName(Number.class);
            methodName = "byteValue";
            methodDescriptor = Type.getMethodDescriptor(Type.BYTE_TYPE);
            returnOpcode = Opcodes.IRETURN;
        } else if (short.class.equals(returnType)) {
            wrapperClassName = Type.getInternalName(Number.class);
            methodName = "shortValue";
            methodDescriptor = Type.getMethodDescriptor(Type.SHORT_TYPE);
            returnOpcode = Opcodes.IRETURN;
        } else if (int.class.equals(returnType)) {
            wrapperClassName = Type.getInternalName(Number.class);
            methodName = "intValue";
            methodDescriptor = Type.getMethodDescriptor(Type.INT_TYPE);
            returnOpcode = Opcodes.IRETURN;
        } else if (float.class.equals(returnType)) {
            wrapperClassName = Type.getInternalName(Number.class);
            methodName = "floatValue";
            methodDescriptor = Type.getMethodDescriptor(Type.FLOAT_TYPE);
            returnOpcode = Opcodes.FRETURN;
        } else if (long.class.equals(returnType)) {
            wrapperClassName = Type.getInternalName(Number.class);
            methodName = "longValue";
            methodDescriptor = Type.getMethodDescriptor(Type.LONG_TYPE);
            returnOpcode = Opcodes.LRETURN;
        } else if (double.class.equals(returnType)) {
            wrapperClassName = Type.getInternalName(Number.class);
            methodName = "doubleValue";
            methodDescriptor = Type.getMethodDescriptor(Type.DOUBLE_TYPE);
            returnOpcode = Opcodes.DRETURN;
        } else {
            throw new IllegalStateException("Unhandled case for primitive type (" + returnType + ").");
        }

        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                wrapperClassName, methodName, methodDescriptor,
                false);
        methodVisitor.visitInsn(returnOpcode);
    }

    /**
     * Convert the {@code parameterIndex} Java parameter to its Python equivalent and store it into
     * the corresponding Python parameter local variable slot.
     */
    public static void copyParameter(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper,
            int parameterIndex) {
        Class<?> parameterClass = localVariableHelper.parameters[parameterIndex].getType();
        if (parameterClass.isPrimitive() || Number.class.isAssignableFrom(parameterClass)) {
            int loadOpcode;
            String valueOfOwner;
            String valueOfDescriptor;

            if (boolean.class.equals(parameterClass)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonBoolean.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonBoolean.class), Type.getType(boolean.class));
            } else if (char.class.equals(parameterClass)) {
                loadOpcode = Opcodes.ILOAD;
                throw new IllegalStateException("Unhandled case for primitive type (" + parameterClass + ").");
            } else if (byte.class.equals(parameterClass)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(byte.class));
            } else if (short.class.equals(parameterClass)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(short.class));
            } else if (int.class.equals(parameterClass)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(int.class));
            } else if (float.class.equals(parameterClass)) {
                loadOpcode = Opcodes.FLOAD;
                valueOfOwner = Type.getInternalName(PythonFloat.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonFloat.class), Type.getType(float.class));
            } else if (long.class.equals(parameterClass)) {
                loadOpcode = Opcodes.LLOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(long.class));
            } else if (double.class.equals(parameterClass)) {
                loadOpcode = Opcodes.DLOAD;
                valueOfOwner = Type.getInternalName(PythonFloat.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonFloat.class), Type.getType(double.class));
            } else {
                throw new IllegalStateException("Unhandled case for primitive type (" + parameterClass + ").");
            }

            methodVisitor.visitVarInsn(loadOpcode, localVariableHelper.getParameterSlot(parameterIndex));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, valueOfOwner, "valueOf",
                    valueOfDescriptor, false);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, localVariableHelper.getPythonLocalVariableSlot(parameterIndex));
        } else {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getParameterSlot(parameterIndex));

            // Need to convert numeric types to wrappers
            Label integerWrap = new Label();
            Label floatWrap = new Label();
            Label storeResult = new Label();

            // This is basically an instanceof chain:
            // if (x instanceof Byte || x instanceof Short || x instanceof Integer || x instanceof Long) {
            //    wrapInteger(x);
            // } else if (x instanceof Float || x instanceof Double) {
            //    wrapFloat(x);
            // }

            // if (x instanceof Byte || x instanceof Short || x instanceof Integer || x instanceof Long)
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(Byte.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, integerWrap);
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(Short.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, integerWrap);
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(Integer.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, integerWrap);
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(Long.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, integerWrap);

            // else if (x instanceof Float || x instanceof Double)
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(Float.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, floatWrap);
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(Double.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, floatWrap);

            methodVisitor.visitJumpInsn(Opcodes.GOTO, storeResult);

            // wrapInteger(x);
            methodVisitor.visitLabel(integerWrap);
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonInteger.class));
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Number.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Number.class), "longValue", Type.getMethodDescriptor(Type.LONG_TYPE), false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonInteger.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE), false);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, storeResult);

            // wrapFloat(x);
            methodVisitor.visitLabel(floatWrap);
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonFloat.class));
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Number.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Number.class), "doubleValue", Type.getMethodDescriptor(Type.DOUBLE_TYPE), false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonFloat.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.DOUBLE_TYPE), false);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, storeResult);

            methodVisitor.visitLabel(storeResult);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, localVariableHelper.getPythonLocalVariableSlot(parameterIndex));
        }
    }
}
