package org.optaplanner.optapy.translator.implementors;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
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

    /**
     * Calls a function. TOS is a tuple containing keyword names.
     * TOS[1]...TOS[len(TOS)] are the keyword arguments to the function (TOS[1] is (TOS)[0], TOS[2] is (TOS)[1], ...).
     * TOS[len(TOS) + 1]...TOS[argc + 1] are the positional arguments (rightmost first).
     * TOS[argc + 2] is the function to call. TOS...TOS[argc + 2] are all popped and
     * the result is pushed onto the stack.
     */
    public static void callFunctionWithKeywords(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        // stack is callable, arg0, arg1, ..., arg(argc - len(keys)), ..., arg(argc - 1), keys
        // We know the total number of arguments, but not the number of individual positional/keyword arguments
        // Since Java Bytecode require consistent stack frames  (i.e. the body of a loop must start with
        // the same number of elements in the stack), we need to add the tuple/map in the same object
        // which will delegate it to either the tuple or the map depending on position and the first item size
        CollectionImplementor.buildCollection(TupleMapPair.class, methodVisitor, instruction.arg + 1);


        // stack is callable, tupleMapPair
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TupleMapPair.class), "tuple", Type.getDescriptor(PythonLikeTuple.class));

        // stack is callable, tupleMapPair, positionalArgs
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TupleMapPair.class), "map", Type.getDescriptor(PythonLikeDict.class));

        // Stack is callable, positionalArgs, keywordArgs
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                                      "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                           Type.getType(List.class),
                                                                           Type.getType(Map.class)),
                                      true);
    }

    public static class TupleMapPair {
        public PythonLikeTuple tuple;
        public PythonLikeDict map;

        List<PythonLikeObject> mapKeyTuple;

        final int totalNumberOfPositionalAndKeywordArguments;

        public TupleMapPair(int itemsToPop) {
            tuple = null; // Tuple is created when we know how many items are in it
            mapKeyTuple = null; // mapKeyTuple is the first item reverseAdded
            map = new PythonLikeDict();
            this.totalNumberOfPositionalAndKeywordArguments = itemsToPop - 1;
        }

        public void reverseAdd(PythonLikeObject object) {
            if (mapKeyTuple == null) {
                mapKeyTuple = (List<PythonLikeObject>) object;
                tuple = new PythonLikeTuple(totalNumberOfPositionalAndKeywordArguments - mapKeyTuple.size());
                return;
            }

            if (map.size() < mapKeyTuple.size()) {
                map.put(mapKeyTuple.get(map.size()), object);
            } else {
                tuple.reverseAdd(object);
            }
        }
    }

}
