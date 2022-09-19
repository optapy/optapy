package org.optaplanner.python.translator;

import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.GENERATED_PACKAGE_BASE;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.writeClassOutput;
import static org.optaplanner.python.translator.types.BuiltinTypes.asmClassLoader;
import static org.optaplanner.python.translator.types.BuiltinTypes.classNameToBytecode;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;

/**
 * Implement classes that hold static constants used for default arguments when calling
 */
public class PythonDefaultArgumentImplementor {
    public static final String ARGUMENT_PREFIX = "argument_";
    public static final String CONSTANT_PREFIX = "DEFAULT_VALUE_";

    public static final String KEY_TUPLE_FIELD_NAME = "keyword_args";

    public static final String REMAINING_KEY_ARGUMENTS_FIELD_NAME = "remaining_keys";

    public static final String POSITIONAL_INDEX = "positional_index";

    public static String getArgumentName(int argumentIndex) {
        return ARGUMENT_PREFIX + argumentIndex;
    }

    public static String getConstantName(int defaultIndex) {
        return CONSTANT_PREFIX + defaultIndex;
    }

    public static Class<?> createDefaultArgumentFor(MethodDescriptor methodDescriptor,
            List<PythonLikeObject> defaultArgumentList,
            Map<String, Integer> argumentNameToIndexMap) {
        String maybeClassName = GENERATED_PACKAGE_BASE +
                methodDescriptor.declaringClassInternalName.replace('/', '.') +
                "."
                + methodDescriptor.methodName + "$$Defaults";
        int numberOfInstances = classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String className = maybeClassName;
        String internalClassName = className.replace('.', '/');

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, internalClassName, null,
                Type.getInternalName(Object.class), new String[] {
                        Type.getInternalName(PythonLikeFunction.class)
                });

        // static constants
        for (int i = 0; i < defaultArgumentList.size(); i++) {
            PythonLikeObject value = defaultArgumentList.get(i);
            String fieldName = getConstantName(i);
            classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, fieldName,
                    Type.getDescriptor(value.getClass()),
                    null,
                    null);
        }

        // instance fields (representing actual arguments)
        classWriter.visitField(Modifier.PRIVATE | Modifier.FINAL, KEY_TUPLE_FIELD_NAME,
                Type.getDescriptor(PythonLikeTuple.class), null, null);
        classWriter.visitField(Modifier.PRIVATE, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class), null, null);
        classWriter.visitField(Modifier.PRIVATE, POSITIONAL_INDEX,
                Type.getDescriptor(int.class), null, null);
        for (int i = 0; i < methodDescriptor.getParameterTypes().length; i++) {
            String fieldName = getArgumentName(i);
            classWriter.visitField(Modifier.PUBLIC, fieldName,
                    methodDescriptor.getParameterTypes()[i].getDescriptor(),
                    null,
                    null);
        }

        // public constructor; an instance is created for keyword function calls, since we need consistent stack frames
        MethodVisitor methodVisitor =
                classWriter.visitMethod(Modifier.PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeTuple.class)),
                        null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, KEY_TUPLE_FIELD_NAME,
                Type.getDescriptor(PythonLikeTuple.class));
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "size",
                Type.getMethodDescriptor(Type.INT_TYPE), true);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, POSITIONAL_INDEX, Type.getDescriptor(int.class));

        for (int i = 0; i < defaultArgumentList.size(); i++) {
            int argumentIndex = i + (methodDescriptor.getParameterTypes().length - defaultArgumentList.size());
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalClassName,
                    getConstantName(i), Type.getDescriptor(defaultArgumentList.get(i).getClass()));
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName,
                    getArgumentName(argumentIndex),
                    methodDescriptor.getParameterTypes()[argumentIndex].getDescriptor());
        }
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        createAddArgumentMethod(classWriter, internalClassName, methodDescriptor, argumentNameToIndexMap);

        classWriter.visitEnd();
        writeClassOutput(classNameToBytecode, className, classWriter.toByteArray());

        try {
            Class<?> compiledClass = asmClassLoader.loadClass(className);
            for (int i = 0; i < defaultArgumentList.size(); i++) {
                PythonLikeObject value = defaultArgumentList.get(i);
                String fieldName = getConstantName(i);
                compiledClass.getField(fieldName).set(null, value);
            }
            return compiledClass;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Impossible State: Unable to load generated class (" +
                    className + ") despite it being just generated.", e);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Impossible State: Unable to set field in generated class (" +
                    className + ").", e);
        }
    }

    /**
     * Create code that look like this:
     *
     * <pre>
     * void addArgument(PythonLikeObject argument) {
     *     if (remainingKeywords > 0) {
     *         String keyword = keywordTuple.get(remainingKeywords - 1).getValue();
     *         switch (keyword) {
     *             case "key1":
     *                 argument_0 = (Argument0Type) argument;
     *                 break;
     *             case "key2":
     *                 argument_1 = (Argument1Type) argument;
     *                 break;
     *             ...
     *         }
     *         remainingKeywords--;
     *         return;
     *     } else {
     *         switch (positionalIndex) {
     *             case 0:
     *                 argument_0 = (Argument0Type) argument;
     *                 break;
     *             case 1:
     *                 argument_1 = (Argument1Type) argument;
     *                 break;
     *             ...
     *         }
     *         positionalIndex++;
     *         return;
     *     }
     * }
     * </pre>
     * 
     * @param classVisitor
     * @param classInternalName
     * @param methodDescriptor
     * @param argumentNameToIndexMap
     */
    private static void createAddArgumentMethod(ClassVisitor classVisitor, String classInternalName,
            MethodDescriptor methodDescriptor,
            Map<String, Integer> argumentNameToIndexMap) {
        MethodVisitor methodVisitor = classVisitor.visitMethod(Modifier.PUBLIC, "addArgument",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeObject.class)),
                null, null);

        methodVisitor.visitParameter("argument", 0);

        methodVisitor.visitCode();

        Label noMoreKeywordArguments = new Label();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, noMoreKeywordArguments);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, KEY_TUPLE_FIELD_NAME,
                Type.getDescriptor(PythonLikeTuple.class));

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));

        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.ISUB);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                true);

        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));

        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonString.class), "getValue",
                Type.getMethodDescriptor(Type.getType(String.class)),
                false);

        BytecodeSwitchImplementor.createStringSwitch(methodVisitor, argumentNameToIndexMap.keySet(),
                2, key -> {
                    int index = argumentNameToIndexMap.get(key);
                    Type parameterType = methodDescriptor.getParameterTypes()[index];
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterType.getInternalName());
                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, getArgumentName(index),
                            parameterType.getDescriptor());
                },
                () -> {
                    methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
                    methodVisitor.visitInsn(Opcodes.DUP);
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                            "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
                    methodVisitor.visitInsn(Opcodes.ATHROW);
                },
                false);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.ISUB);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));
        methodVisitor.visitInsn(Opcodes.RETURN);

        // No more keyword arguments
        methodVisitor.visitLabel(noMoreKeywordArguments);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, POSITIONAL_INDEX, Type.getDescriptor(int.class));

        BytecodeSwitchImplementor.createIntSwitch(methodVisitor, argumentNameToIndexMap.values(),
                index -> {
                    Type parameterType = methodDescriptor.getParameterTypes()[index];
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterType.getInternalName());
                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, getArgumentName(index),
                            parameterType.getDescriptor());
                },
                () -> {
                    methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
                    methodVisitor.visitInsn(Opcodes.DUP);
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                            "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
                    methodVisitor.visitInsn(Opcodes.ATHROW);
                },
                false);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, POSITIONAL_INDEX, Type.getDescriptor(int.class));
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.IADD);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, POSITIONAL_INDEX, Type.getDescriptor(int.class));

        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }
}
