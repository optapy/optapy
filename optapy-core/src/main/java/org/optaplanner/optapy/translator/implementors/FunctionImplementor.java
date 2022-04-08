package org.optaplanner.optapy.translator.implementors;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.LocalVariableHelper;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;
import org.optaplanner.optapy.translator.types.PythonLikeDict;
import org.optaplanner.optapy.translator.types.PythonLikeFunction;
import org.optaplanner.optapy.translator.types.PythonLikeTuple;

/**
 * Implements opcodes related to functions
 */
public class FunctionImplementor {

    /**
     * Calls a function. TOS...TOS[argc - 1] are the arguments to the function.
     * TOS[argc] is the function to call. TOS...TOS[argc] are all popped and
     * the result is pushed onto the stack.
     */
    public static void callFunction(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        // stack is callable, arg0, arg1, ..., arg(argc - 1)
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, instruction.arg);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        // Stack is callable, argument_list, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                                      "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                           Type.getType(List.class),
                                                                           Type.getType(Map.class)),
                                      true);
    }

}
