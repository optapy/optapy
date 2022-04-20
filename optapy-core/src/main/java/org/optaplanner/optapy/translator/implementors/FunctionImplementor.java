package org.optaplanner.optapy.translator.implementors;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.LocalVariableHelper;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;
import org.optaplanner.optapy.translator.PythonCompiledFunction;
import org.optaplanner.optapy.translator.types.PythonLikeDict;
import org.optaplanner.optapy.translator.types.PythonLikeFunction;
import org.optaplanner.optapy.translator.types.PythonLikeTuple;
import org.optaplanner.optapy.translator.types.PythonLikeType;

/**
 * Implements opcodes related to functions
 */
public class FunctionImplementor {

    /**
     * Loads a method named co_names[namei] from the TOS object. TOS is popped. This bytecode distinguishes two cases:
     * if TOS has a method with the correct name, the bytecode pushes the unbound method and TOS.
     * TOS will be used as the first argument (self) by CALL_METHOD when calling the unbound method.
     * Otherwise, NULL and the object return by the attribute lookup are pushed.
     */
    public static void loadMethod(MethodVisitor methodVisitor, String className, PythonCompiledFunction function,
                                  PythonBytecodeInstruction instruction) {
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                      "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                      true);
        methodVisitor.visitLdcInsn(function.co_names.get(instruction.arg));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                      "__getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                           Type.getType(String.class)),
                                      true);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        Label blockEnd = new Label();

        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, blockEnd);

        // TOS is null; type does not have attribute; do normal attribute lookup
        // Stack is object, null
        methodVisitor.visitInsn(Opcodes.POP);
        ObjectImplementor.getAttribute(methodVisitor, className, instruction);

        // Stack is method
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitInsn(Opcodes.SWAP);

        methodVisitor.visitLabel(blockEnd);

        // Stack is either:
        // object, method if it was in type
        // null, method if it was not in type
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now:
        // method, object if it was in type
        // method, null if it was not in type
    }

    /**
     * Calls a method. argc is the number of positional arguments. Keyword arguments are not supported.
     * This opcode is designed to be used with LOAD_METHOD. Positional arguments are on top of the stack.
     * Below them, the two items described in LOAD_METHOD are on the stack
     * (either self and an unbound method object or NULL and an arbitrary callable).
     * All of them are popped and the return value is pushed.
     */
    public static void callMethod(MethodVisitor methodVisitor,
                                  PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // Stack is method, (obj or null), arg0, ..., arg(argc - 1)
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, instruction.arg);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is method, argList, (obj or null)
        Label ifNullStart = new Label();
        Label blockEnd = new Label();

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, ifNullStart);

        // Stack is method, argList, obj
        StackManipulationImplementor.duplicateToTOS(methodVisitor, localVariableHelper, 1);
        StackManipulationImplementor.swap(methodVisitor);

        // Stack is method, argList, argList, obj
        methodVisitor.visitInsn(Opcodes.ICONST_0);

        // Stack is method, argList, argList, obj, index
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is method, argList, argList, index, obj
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                                      "add",
                                      Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(Object.class)),
                                      true);

        // Stack is method, argList
        methodVisitor.visitJumpInsn(Opcodes.GOTO, blockEnd);

        methodVisitor.visitLabel(ifNullStart);
        // Stack is method, argList, null
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is method, argList
        methodVisitor.visitLabel(blockEnd);

        // Stack is method, argList
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        // Stack is callable, argument_list, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                                      "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                           Type.getType(List.class),
                                                                           Type.getType(Map.class)),
                                      true);
    }

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

    /**
     * Calls a function. If the lowest bit of instruction.arg is set, TOS is a mapping object containing keyword
     * arguments, TOS[1] is an iterable containing positional arguments and TOS[2] is callable. Otherwise,
     * TOS is an iterable containing positional arguments and TOS[1] is callable.
     */
    public static void callFunctionUnpack(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        if ((instruction.arg & 1) == 1) {
            callFunctionUnpackMapAndIterable(methodVisitor);
        } else {
            callFunctionUnpackIterable(methodVisitor);
        }
    }

    public static void callFunctionUnpackMapAndIterable(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                                      "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                                                           Type.getType(List.class),
                                                                           Type.getType(Map.class)),
                                      true);
    }

    public static void callFunctionUnpackIterable(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
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
                map.put(mapKeyTuple.get(mapKeyTuple.size() - map.size() - 1), object);
            } else {
                tuple.reverseAdd(object);
            }
        }
    }

}
