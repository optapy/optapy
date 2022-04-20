package org.optaplanner.optapy.translator.implementors;

import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.LocalVariableHelper;
import org.optaplanner.optapy.translator.PythonBinaryOperators;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;
import org.optaplanner.optapy.translator.PythonCompiledFunction;
import org.optaplanner.optapy.translator.PythonTernaryOperators;

/**
 * Implementations of opcodes related to objects
 */
public class ObjectImplementor {

    /**
     * Replaces TOS with getattr(TOS, co_names[instruction.arg])
     */
    public static void getAttribute(MethodVisitor methodVisitor, String className, PythonBytecodeInstruction instruction) {
        PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg);
        DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.GET_ATTRIBUTE);
    }

    /**
     * Deletes co_names[instruction.arg] of TOS
     */
    public static void deleteAttribute(MethodVisitor methodVisitor, String className, PythonBytecodeInstruction instruction) {
        PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg);
        DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.DELETE_ATTRIBUTE);
    }

    /**
     * Implement TOS.name = TOS1, where name is co_names[instruction.arg]. TOS and TOS1 are popped.
     */
    public static void setAttribute(MethodVisitor methodVisitor, String className, PythonBytecodeInstruction instruction,
                                    LocalVariableHelper localVariableHelper) {
        StackManipulationImplementor.swap(methodVisitor);
        PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg);
        StackManipulationImplementor.swap(methodVisitor);
        DunderOperatorImplementor.ternaryOperator(methodVisitor, PythonTernaryOperators.SET_ATTRIBUTE, localVariableHelper);
    }
}
