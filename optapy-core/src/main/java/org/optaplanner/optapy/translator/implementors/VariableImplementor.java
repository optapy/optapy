package org.optaplanner.optapy.translator.implementors;

import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.LocalVariableHelper;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;
import org.optaplanner.optapy.translator.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.optapy.translator.types.PythonCell;
import org.optaplanner.optapy.translator.types.PythonLikeTuple;

/**
 * Implementations of local variable manipulation opcodes.
 * See https://tenthousandmeters.com/blog/python-behind-the-scenes-5-how-variables-are-implemented-in-cpython/
 * for a detailed explanation of the differences between LOAD_FAST, LOAD_GLOBAL, LOAD_DEREF, etc.
 */
public class VariableImplementor {

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

    /**
     * Deletes the local variable or parameter indicated by the {@code instruction} argument.
     */
    public static void deleteLocalVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // Deleting is implemented as setting the value to null
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, localVariableHelper.getPythonLocalVariableSlot(instruction.arg));
    }

    /**
     * Loads the cell indicated by the {@code instruction} argument onto the stack.
     * This is used by {@link PythonBytecodeInstruction.OpCode#LOAD_CLOSURE} when creating a closure
     * for a dependent function.
     */
    public static void createCell(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper, int cellIndex) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonCell.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonCell.class), "<init>",
                                      Type.getMethodDescriptor(Type.VOID_TYPE), false);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, localVariableHelper.getPythonCellOrFreeVariableSlot(cellIndex));
    }

    /**
     * Moves the {@code cellIndex} free variable (stored in the
     * {@link PythonBytecodeToJavaBytecodeTranslator#CELLS_INSTANCE_FIELD_NAME} field
     * to its corresponding local variable.
     */
    public static void setupFreeVariableCell(MethodVisitor methodVisitor, String internalClassName,
                                  LocalVariableHelper localVariableHelper, int cellIndex) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, internalClassName, PythonBytecodeToJavaBytecodeTranslator.CELLS_INSTANCE_FIELD_NAME,
                                     Type.getDescriptor(PythonLikeTuple.class));
        methodVisitor.visitLdcInsn(cellIndex);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                                      Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(int.class)),
                                      true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonCell.class));
        methodVisitor.visitVarInsn(Opcodes.ASTORE,
                                   localVariableHelper.pythonFreeVariablesStart + cellIndex);
    }

    /**
     * Loads the cell indicated by the {@code instruction} argument onto the stack.
     * This is used by {@link PythonBytecodeInstruction.OpCode#LOAD_CLOSURE} when creating a closure
     * for a dependent function.
     */
    public static void loadCell(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getPythonCellOrFreeVariableSlot(instruction.arg));
    }

    /**
     * Loads the cell variable/free variable indicated by the {@code instruction} argument onto the stack.
     * (which is an {@link PythonCell}, so it can see changes from the parent function).
     */
    public static void loadCellVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        loadCell(methodVisitor, instruction, localVariableHelper);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(PythonCell.class), "cellValue",
                                     Type.getDescriptor(PythonLikeObject.class));
    }

    /**
     * Stores TOS into the cell variable or parameter indicated by the {@code instruction} argument
     * (which is an {@link PythonCell}, so changes in the parent function affect the variable in dependent functions).
     */
    public static void storeInCellVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        loadCell(methodVisitor, instruction, localVariableHelper);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(PythonCell.class), "cellValue",
                                     Type.getDescriptor(PythonLikeObject.class));
    }

    /**
     * Deletes the cell variable or parameter indicated by the {@code instruction} argument
     * (which is an {@link PythonCell}, so changes in the parent function affect the variable in dependent functions).
     */
    public static void deleteCellVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // Deleting is implemented as setting the value to null
        loadCell(methodVisitor, instruction, localVariableHelper);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(PythonCell.class), "cellValue",
                                     Type.getDescriptor(PythonLikeObject.class));
    }
}
