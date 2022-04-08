package org.optaplanner.optapy.translator.implementors;

import java.lang.reflect.Method;
import java.util.Iterator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.LocalVariableHelper;
import org.optaplanner.optapy.translator.types.JavaObjectWrapper;
import org.optaplanner.optapy.translator.types.PythonBoolean;
import org.optaplanner.optapy.translator.types.PythonFloat;
import org.optaplanner.optapy.translator.types.PythonInteger;
import org.optaplanner.optapy.translator.types.PythonIterator;
import org.optaplanner.optapy.translator.types.PythonNone;
import org.optaplanner.optapy.translator.types.PythonNumber;

/**
 * Implementations of opcodes and operations that require Java to Python or Python to Java conversions.
 */
public class JavaPythonTypeConversionImplementor {

    /**
     * Wraps {@code object} to a PythonLikeObject.
     */
    public static PythonLikeObject wrapJavaObject(Object object) {
        if (object == null) {
            return PythonNone.INSTANCE;
        }

        if (object instanceof PythonLikeObject) {
            // Object already a PythonLikeObject; need to do nothing
            return (PythonLikeObject) object;
        }

        if (object instanceof Byte || object instanceof Short || object instanceof Integer || object instanceof Long) {
            return PythonInteger.valueOf(((Number) object).longValue());
        }

        if (object instanceof Float || object instanceof Double) {
            return PythonFloat.valueOf(((Number) object).doubleValue());
        }

        if (object instanceof Boolean) {
            return PythonBoolean.valueOf((Boolean) object);
        }

        if (object instanceof Iterator) {
            return new PythonIterator((Iterator) object);
        }

        // TODO: List, Map, Set, String

        // Default: return a JavaObjectWrapper
        return new JavaObjectWrapper(object);
    }

    /**
     * Converts a {@code PythonLikeObject} to the given {@code type}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertPythonObjectToJavaType(Class<? extends T> type, PythonLikeObject object) {
        if (object instanceof PythonNone) {
            return null;
        }

        if (object instanceof JavaObjectWrapper) {
            JavaObjectWrapper wrappedObject = (JavaObjectWrapper) object;
            Object javaObject = wrappedObject.getWrappedObject();
            if (!type.isAssignableFrom(javaObject.getClass())) {
                throw new IllegalArgumentException("Cannot convert from (" + javaObject.getClass() + ") to (" + type + ").");
            }
            return (T) javaObject;
        }

        if (type.isAssignableFrom(object.getClass())) {
            // Can directly assign; no modification needed
            return (T) object;
        }

        if (type.equals(byte.class) || type.equals(short.class) || type.equals(int.class) || type.equals(long.class) ||
                type.equals(float.class) || type.equals(double.class) || Number.class.isAssignableFrom(type)) {
            if (!(object instanceof PythonNumber)) {
                throw new IllegalArgumentException("Cannot convert from (" + object.getClass() + ") to (" + type + ").");
            }
            PythonNumber pythonNumber = (PythonNumber) object;
            Number value = pythonNumber.getValue();

            if (type.equals(byte.class) || type.equals(Byte.class)) {
                return (T) (Byte) value.byteValue();
            }

            if (type.equals(short.class) || type.equals(Short.class)) {
                return (T) (Short) value.shortValue();
            }

            if (type.equals(int.class) || type.equals(Integer.class)) {
                return (T) (Integer) value.intValue();
            }

            if (type.equals(long.class) || type.equals(Long.class)) {
                return (T) (Long) value.longValue();
            }

            if (type.equals(float.class) || type.equals(Float.class)) {
                return (T) (Float) value.floatValue();
            }

            if (type.equals(double.class) || type.equals(Double.class)) {
                return (T) (Double) value.doubleValue();
            }
        }

        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            if (!(object instanceof PythonBoolean)) {
                throw new IllegalArgumentException("Cannot convert from (" + object.getClass() + ") to (" + type + ").");
            }
            PythonBoolean pythonBoolean = (PythonBoolean) object;
            return (T) (Boolean) pythonBoolean.getValue();
        }

        // TODO: List, Map, Set, String

        throw new IllegalStateException("Cannot convert from (" + object.getClass() + ") to (" + type + ").");
    }

    /**
     * Convert the Java {@code constant} to its Python equivalent and push it onto the stack.
     *
     * @param constant The Java constant to load
     */
    public static void loadConstant(MethodVisitor methodVisitor, Object constant) {
        // Cannot use wrapJavaObject to wrap primitives types with ldc (null, int, long, float, boolean)
        // since that will load the unwrapped type instead of the wrap type
        if (constant instanceof Number) {
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

        if (constant == null) {
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        } else {
            methodVisitor.visitLdcInsn(constant);
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                                      "wrapJavaObject",
                                      Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(Object.class)),
                                      false);
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
            methodVisitor.visitLdcInsn(Type.getType(method.getReturnType()));
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                                          "convertPythonObjectToJavaType",
                                          Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Class.class),
                                                                   Type.getType(PythonLikeObject.class)),
                                          false);
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
        if (parameterClass.isPrimitive()) {
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
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                                          "wrapJavaObject",
                                          Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(Object.class)),
                                          false);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, localVariableHelper.getPythonLocalVariableSlot(parameterIndex));
        }
    }
}
