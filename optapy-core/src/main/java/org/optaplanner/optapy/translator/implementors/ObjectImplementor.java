package org.optaplanner.optapy.translator.implementors;

import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;
import org.optaplanner.optapy.translator.PythonCompiledFunction;

/**
 * Implementations of opcodes related to objects
 */
public class ObjectImplementor {

    /**
     * Replaces TOS with getattr(TOS, co_names[instruction.arg])
     */
    public static void getAttribute(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, PythonCompiledFunction function) {
        methodVisitor.visitLdcInsn(function.co_names.get(instruction.arg));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                      "__getAttributeOrError",
                                      Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(String.class)),
                                      true);
    }

    /**
     * Implement TOS.name = TOS1, where name is co_names[instruction.arg]. TOS and TOS1 are popped.
     */
    public static void setAttribute(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, PythonCompiledFunction function) {
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitLdcInsn(function.co_names.get(instruction.arg));
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                      "__setAttribute",
                                      Type.getMethodDescriptor(Type.VOID_TYPE,
                                                               Type.getType(String.class),
                                                               Type.getType(PythonLikeObject.class)),
                                      true);
    }
}
