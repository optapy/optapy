package org.optaplanner.optapy.translator.implementors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.optaplanner.optapy.translator.LocalVariableHelper;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;

/**
 * Implementations of local variable manipulation opcodes.
 */
public class LocalVariableImplementor {

    /**
     * Loads the local variable or parameter indicated by the {@code instruction} argument onto the stack.
     */
    public static void loadLocalVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getPythonLocalVariableSlot(instruction.arg));
    }

    /**
     * Stores TOS into the local variable or parameter indicated by the {@code instruction} argument.
     */
    public static void storeInLocalVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        methodVisitor.visitVarInsn(Opcodes.ASTORE, localVariableHelper.getPythonLocalVariableSlot(instruction.arg));
    }
}
