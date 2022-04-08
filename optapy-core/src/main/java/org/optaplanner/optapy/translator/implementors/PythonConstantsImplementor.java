package org.optaplanner.optapy.translator.implementors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.translator.types.PythonBoolean;
import org.optaplanner.optapy.translator.types.PythonNone;

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
}
