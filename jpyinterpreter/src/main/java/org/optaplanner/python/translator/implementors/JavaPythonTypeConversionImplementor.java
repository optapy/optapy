package org.optaplanner.python.translator.implementors;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.BOOLEAN_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.BYTES_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.BYTE_ARRAY_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.DICT_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.FLOAT_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.FROZEN_SET_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.INT_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.ITERATOR_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.LIST_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.NONE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.NUMBER_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.SET_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.STRING_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.TUPLE_TYPE;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.LocalVariableHelper;
import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonClassTranslator;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.types.BuiltinTypes;
import org.optaplanner.python.translator.types.PythonByteArray;
import org.optaplanner.python.translator.types.PythonBytes;
import org.optaplanner.python.translator.types.PythonCode;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.collections.PythonIterator;
import org.optaplanner.python.translator.types.collections.PythonLikeDict;
import org.optaplanner.python.translator.types.collections.PythonLikeFrozenSet;
import org.optaplanner.python.translator.types.collections.PythonLikeList;
import org.optaplanner.python.translator.types.collections.PythonLikeSet;
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;
import org.optaplanner.python.translator.types.errors.TypeError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonFloat;
import org.optaplanner.python.translator.types.numeric.PythonInteger;
import org.optaplanner.python.translator.types.numeric.PythonNumber;
import org.optaplanner.python.translator.types.wrappers.JavaObjectWrapper;
import org.optaplanner.python.translator.types.wrappers.OpaqueJavaReference;
import org.optaplanner.python.translator.types.wrappers.OpaquePythonReference;
import org.optaplanner.python.translator.types.wrappers.PythonObjectWrapper;

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

        if (object instanceof OpaqueJavaReference) {
            return ((OpaqueJavaReference) object).proxy();
        }

        if (object instanceof PythonLikeObject) {
            // Object already a PythonLikeObject; need to do nothing
            return (PythonLikeObject) object;
        }

        if (object instanceof Byte || object instanceof Short || object instanceof Integer || object instanceof Long) {
            return PythonInteger.valueOf(((Number) object).longValue());
        }

        if (object instanceof BigInteger) {
            return PythonInteger.valueOf((BigInteger) object);
        }

        if (object instanceof Float || object instanceof Double) {
            return PythonFloat.valueOf(((Number) object).doubleValue());
        }

        if (object instanceof Boolean) {
            return PythonBoolean.valueOf((Boolean) object);
        }

        if (object instanceof String) {
            return PythonString.valueOf((String) object);
        }

        if (object instanceof Iterator) {
            return new PythonIterator((Iterator) object);
        }

        if (object instanceof List) {
            PythonLikeList out = new PythonLikeList();
            for (Object item : (List) object) {
                out.add(wrapJavaObject(item));
            }
            return out;
        }

        if (object instanceof Set) {
            PythonLikeSet out = new PythonLikeSet();
            for (Object item : (Set) object) {
                out.add(wrapJavaObject(item));
            }
            return out;
        }

        if (object instanceof Map) {
            PythonLikeDict out = new PythonLikeDict();
            Set<Map.Entry<?, ?>> entrySet = ((Map) object).entrySet();
            for (Map.Entry<?, ?> entry : entrySet) {
                out.put(wrapJavaObject(entry.getKey()), wrapJavaObject(entry.getValue()));
            }
            return out;
        }

        if (object instanceof Class) {
            Class<?> maybeFunctionClass = (Class<?>) object;
            if (Set.of(maybeFunctionClass.getInterfaces()).contains(PythonLikeFunction.class)) {
                return new PythonCode((Class<? extends PythonLikeFunction>) maybeFunctionClass);
            }
        }

        if (object instanceof OpaquePythonReference) {
            return new PythonObjectWrapper((OpaquePythonReference) object);
        }

        // Default: return a JavaObjectWrapper
        return new JavaObjectWrapper(object);
    }

    /**
     * Get the {@link PythonLikeType} of a java {@link Class}.
     */
    public static PythonLikeType getPythonLikeType(Class<?> javaClass) {
        if (PythonNone.class.equals(javaClass)) {
            return NONE_TYPE;
        }

        if (PythonLikeObject.class.equals(javaClass)) {
            return BASE_TYPE;
        }

        if (byte.class.equals(javaClass) || short.class.equals(javaClass) || int.class.equals(javaClass)
                || long.class.equals(javaClass) ||
                Byte.class.equals(javaClass) || Short.class.equals(javaClass) || Integer.class.equals(javaClass)
                || Long.class.equals(javaClass) || BigInteger.class.equals(javaClass) ||
                PythonInteger.class.equals(javaClass)) {
            return INT_TYPE;
        }

        if (float.class.equals(javaClass) || double.class.equals(javaClass) ||
                Float.class.equals(javaClass) || Double.class.equals(javaClass) ||
                PythonFloat.class.equals(javaClass)) {
            return FLOAT_TYPE;
        }

        if (PythonNumber.class.equals(javaClass)) {
            return NUMBER_TYPE;
        }

        if (boolean.class.equals(javaClass) ||
                Boolean.class.equals(javaClass) ||
                PythonBoolean.class.equals(javaClass)) {
            return BOOLEAN_TYPE;
        }

        if (String.class.equals(javaClass) ||
                PythonString.class.equals(javaClass)) {
            return STRING_TYPE;
        }

        if (PythonBytes.class.equals(javaClass)) {
            return BYTES_TYPE;
        }

        if (PythonByteArray.class.equals(javaClass)) {
            return BYTE_ARRAY_TYPE;
        }

        if (Iterator.class.equals(javaClass) ||
                PythonIterator.class.equals(javaClass)) {
            return ITERATOR_TYPE;
        }

        if (List.class.equals(javaClass) ||
                PythonLikeList.class.equals(javaClass)) {
            return LIST_TYPE;
        }

        if (PythonLikeTuple.class.equals(javaClass)) {
            return TUPLE_TYPE;
        }

        if (Set.class.equals(javaClass) ||
                PythonLikeSet.class.equals(javaClass)) {
            return SET_TYPE;
        }

        if (PythonLikeFrozenSet.class.equals(javaClass)) {
            return FROZEN_SET_TYPE;
        }

        if (Map.class.equals(javaClass) ||
                PythonLikeDict.class.equals(javaClass)) {
            return DICT_TYPE;
        }

        try {
            Field typeField = javaClass.getField(PythonClassTranslator.TYPE_FIELD_NAME);
            Object maybeType = typeField.get(null);
            if (maybeType instanceof PythonLikeType) {
                return (PythonLikeType) maybeType;
            }
            if (PythonLikeFunction.class.isAssignableFrom(javaClass)) {
                return PythonLikeFunction.getFunctionType();
            }
            return JavaObjectWrapper.getPythonTypeForClass(javaClass);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            if (PythonLikeFunction.class.isAssignableFrom(javaClass)) {
                return PythonLikeFunction.getFunctionType();
            }
            return JavaObjectWrapper.getPythonTypeForClass(javaClass);
        }
    }

    /**
     * Converts a {@code PythonLikeObject} to the given {@code type}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertPythonObjectToJavaType(Class<? extends T> type, PythonLikeObject object) {
        if (object == null || type.isAssignableFrom(object.getClass())) {
            // Can directly assign; no modification needed
            return (T) object;
        }

        if (object instanceof PythonNone) {
            return null;
        }

        if (object instanceof JavaObjectWrapper) {
            JavaObjectWrapper wrappedObject = (JavaObjectWrapper) object;
            Object javaObject = wrappedObject.getWrappedObject();
            if (!type.isAssignableFrom(javaObject.getClass())) {
                throw new TypeError("Cannot convert from (" + javaObject.getClass() + ") to (" + type + ").");
            }
            return (T) javaObject;
        }

        if (type.equals(byte.class) || type.equals(short.class) || type.equals(int.class) || type.equals(long.class) ||
                type.equals(float.class) || type.equals(double.class) || Number.class.isAssignableFrom(type)) {
            if (!(object instanceof PythonNumber)) {
                throw new TypeError("Cannot convert from (" + object.getClass() + ") to (" + type + ").");
            }
            PythonNumber pythonNumber = (PythonNumber) object;
            Number value = pythonNumber.getValue();

            if (type.equals(BigInteger.class)) {
                return (T) value;
            }

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
                throw new TypeError("Cannot convert from (" + object.getClass() + ") to (" + type + ").");
            }
            PythonBoolean pythonBoolean = (PythonBoolean) object;
            return (T) (Boolean) pythonBoolean.getBooleanValue();
        }

        if (type.equals(String.class)) {
            PythonString pythonString = (PythonString) object;
            return (T) pythonString.getValue();
        }

        // TODO: List, Map, Set

        throw new TypeError("Cannot convert from (" + object.getClass() + ") to (" + type + ").");
    }

    /**
     * Loads a String and push it onto the stack
     *
     * @param name The name to load
     */
    public static void loadName(MethodVisitor methodVisitor, String name) {
        methodVisitor.visitLdcInsn(name);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonString.class),
                "valueOf",
                Type.getMethodDescriptor(Type.getType(PythonString.class), Type.getType(PythonString.class)),
                false);
    }

    /**
     * If {@code method} return type is not void, convert TOS into its Java equivalent and return it.
     * If {@code method} return type is void, immediately return.
     *
     * @param method The method that is being implemented.
     */
    public static void returnValue(MethodVisitor methodVisitor, MethodDescriptor method, StackMetadata stackMetadata) {
        Type returnAsmType = method.getReturnType();

        if (Type.VOID_TYPE.equals(returnAsmType)) {
            methodVisitor.visitInsn(Opcodes.RETURN);
            return;
        }

        if (Type.BYTE_TYPE.equals(returnAsmType) ||
                Type.CHAR_TYPE.equals(returnAsmType) ||
                Type.SHORT_TYPE.equals(returnAsmType) ||
                Type.INT_TYPE.equals(returnAsmType) ||
                Type.LONG_TYPE.equals(returnAsmType) ||
                Type.FLOAT_TYPE.equals(returnAsmType) ||
                Type.DOUBLE_TYPE.equals(returnAsmType)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonNumber.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                    Type.getInternalName(PythonNumber.class),
                    "getValue",
                    Type.getMethodDescriptor(Type.getType(Number.class)),
                    true);
            String wrapperClassName = null;
            String methodName = null;
            String methodDescriptor = null;
            int returnOpcode = 0;

            if (Type.BYTE_TYPE.equals(returnAsmType)) {
                wrapperClassName = Type.getInternalName(Number.class);
                methodName = "byteValue";
                methodDescriptor = Type.getMethodDescriptor(Type.BYTE_TYPE);
                returnOpcode = Opcodes.IRETURN;
            } else if (Type.CHAR_TYPE.equals(returnAsmType)) {
                throw new IllegalStateException("Unhandled case for primitive type (char).");
                // returnOpcode = Opcodes.IRETURN;
            } else if (Type.SHORT_TYPE.equals(returnAsmType)) {
                wrapperClassName = Type.getInternalName(Number.class);
                methodName = "shortValue";
                methodDescriptor = Type.getMethodDescriptor(Type.SHORT_TYPE);
                returnOpcode = Opcodes.IRETURN;
            } else if (Type.INT_TYPE.equals(returnAsmType)) {
                wrapperClassName = Type.getInternalName(Number.class);
                methodName = "intValue";
                methodDescriptor = Type.getMethodDescriptor(Type.INT_TYPE);
                returnOpcode = Opcodes.IRETURN;
            } else if (Type.FLOAT_TYPE.equals(returnAsmType)) {
                wrapperClassName = Type.getInternalName(Number.class);
                methodName = "floatValue";
                methodDescriptor = Type.getMethodDescriptor(Type.FLOAT_TYPE);
                returnOpcode = Opcodes.FRETURN;
            } else if (Type.LONG_TYPE.equals(returnAsmType)) {
                wrapperClassName = Type.getInternalName(Number.class);
                methodName = "longValue";
                methodDescriptor = Type.getMethodDescriptor(Type.LONG_TYPE);
                returnOpcode = Opcodes.LRETURN;
            } else if (Type.DOUBLE_TYPE.equals(returnAsmType)) {
                wrapperClassName = Type.getInternalName(Number.class);
                methodName = "doubleValue";
                methodDescriptor = Type.getMethodDescriptor(Type.DOUBLE_TYPE);
                returnOpcode = Opcodes.DRETURN;
            }
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    wrapperClassName, methodName, methodDescriptor,
                    false);
            methodVisitor.visitInsn(returnOpcode);
            return;
        }

        if (Type.BOOLEAN_TYPE.equals(returnAsmType)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonBoolean.class));
            String wrapperClassName = Type.getInternalName(PythonBoolean.class);
            String methodName = "getBooleanValue";
            String methodDescriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE);
            int returnOpcode = Opcodes.IRETURN;
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    wrapperClassName, methodName, methodDescriptor,
                    false);
            methodVisitor.visitInsn(returnOpcode);
            return;
        }

        try {
            Class<?> returnTypeClass =
                    Class.forName(returnAsmType.getClassName(), true, BuiltinTypes.asmClassLoader);

            if (stackMetadata.getTOSType() == null) {
                throw new IllegalStateException("Cannot return a deleted or undefined value");
            }
            Class<?> tosTypeClass = stackMetadata.getTOSType().getJavaClass();
            if (returnTypeClass.isAssignableFrom(tosTypeClass)) {
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, returnAsmType.getInternalName());
                methodVisitor.visitInsn(Opcodes.ARETURN);
                return;
            }
        } catch (ClassNotFoundException e) {
            // Do nothing; default case is below
        }

        methodVisitor.visitLdcInsn(returnAsmType);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                "convertPythonObjectToJavaType",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Class.class),
                        Type.getType(PythonLikeObject.class)),
                false);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, returnAsmType.getInternalName());
        methodVisitor.visitInsn(Opcodes.ARETURN);
    }

    /**
     * Convert the {@code parameterIndex} Java parameter to its Python equivalent and store it into
     * the corresponding Python parameter local variable slot.
     */
    public static void copyParameter(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper,
            int parameterIndex) {
        Type parameterType = localVariableHelper.parameters[parameterIndex];
        if (parameterType.getSort() != Type.OBJECT && parameterType.getSort() != Type.ARRAY) {
            int loadOpcode;
            String valueOfOwner;
            String valueOfDescriptor;

            if (Type.BOOLEAN_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonBoolean.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonBoolean.class), Type.getType(boolean.class));
            } else if (Type.CHAR_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                throw new IllegalStateException("Unhandled case for primitive type (" + parameterType + ").");
            } else if (Type.BYTE_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(byte.class));
            } else if (Type.SHORT_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(short.class));
            } else if (Type.INT_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(int.class));
            } else if (Type.FLOAT_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.FLOAD;
                valueOfOwner = Type.getInternalName(PythonFloat.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonFloat.class), Type.getType(float.class));
            } else if (Type.LONG_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.LLOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(long.class));
            } else if (Type.DOUBLE_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.DLOAD;
                valueOfOwner = Type.getInternalName(PythonFloat.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonFloat.class), Type.getType(double.class));
            } else {
                throw new IllegalStateException("Unhandled case for primitive type (" + parameterType + ").");
            }

            methodVisitor.visitVarInsn(loadOpcode, localVariableHelper.getParameterSlot(parameterIndex));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, valueOfOwner, "valueOf",
                    valueOfDescriptor, false);
            localVariableHelper.writeLocal(methodVisitor, parameterIndex);
        } else {
            try {
                Class<?> typeClass = Class.forName(parameterType.getClassName(), false,
                        BuiltinTypes.asmClassLoader);
                if (!PythonLikeObject.class.isAssignableFrom(typeClass)) {
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getParameterSlot(parameterIndex));
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                            Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                            "wrapJavaObject",
                            Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(Object.class)),
                            false);
                    localVariableHelper.writeLocal(methodVisitor, parameterIndex);
                } else {
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getParameterSlot(parameterIndex));
                    localVariableHelper.writeLocal(methodVisitor, parameterIndex);
                }
            } catch (ClassNotFoundException e) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getParameterSlot(parameterIndex));
                localVariableHelper.writeLocal(methodVisitor, parameterIndex);
            }
        }
    }
}
