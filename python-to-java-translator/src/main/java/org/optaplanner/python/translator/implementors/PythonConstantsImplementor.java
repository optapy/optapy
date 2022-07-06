package org.optaplanner.python.translator.implementors;

import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.CONSTANTS_STATIC_FIELD_NAME;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.NAMES_STATIC_FIELD_NAME;

import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.types.PythonBoolean;
import org.optaplanner.python.translator.types.PythonNone;

/**
 * Implementations of loading Python constants
 */
public class PythonConstantsImplementor {

    /**
     * Pushes None onto the stack. The same instance is pushed on each call.
     */
    public static void loadNone(MethodVisitor methodVisitor) {
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(PythonNone.class), "INSTANCE",
                Type.getDescriptor(PythonNone.class));
    }

    /**
     * Pushes True onto the stack. The same instance is pushed on each call.
     */
    public static void loadTrue(MethodVisitor methodVisitor) {
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(PythonBoolean.class), "TRUE",
                Type.getDescriptor(PythonBoolean.class));
    }

    /**
     * Pushes False onto the stack. The same instance is pushed on each call.
     */
    public static void loadFalse(MethodVisitor methodVisitor) {
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(PythonBoolean.class), "FALSE",
                Type.getDescriptor(PythonBoolean.class));
    }

    /**
     * Gets the {@code constantIndex} constant from the class constant list
     *
     * @param className The class currently being defined by the methodVisitor
     * @param constantIndex The index of the constant to load in the class constant list
     */
    public static void loadConstant(MethodVisitor methodVisitor, String className, int constantIndex) {
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className, CONSTANTS_STATIC_FIELD_NAME, Type.getDescriptor(List.class));
        methodVisitor.visitLdcInsn(constantIndex);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                "get",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                true);
    }

    /**
     * Gets the {@code nameIndex} name from the class name list
     *
     * @param className The class currently being defined by the methodVisitor
     * @param nameIndex The index of the name to load in the class name list
     */
    public static void loadName(MethodVisitor methodVisitor, String className, int nameIndex) {
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className, NAMES_STATIC_FIELD_NAME, Type.getDescriptor(List.class));
        methodVisitor.visitLdcInsn(nameIndex);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                "get",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                true);
    }
}