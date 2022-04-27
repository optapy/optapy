package org.optaplanner.optapy.translator.implementors;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.LocalVariableHelper;
import org.optaplanner.optapy.translator.PythonBinaryOperators;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;
import org.optaplanner.optapy.translator.PythonTernaryOperators;
import org.optaplanner.optapy.translator.PythonUnaryOperator;
import org.optaplanner.optapy.translator.types.PythonLikeList;
import org.optaplanner.optapy.translator.types.PythonLikeTuple;
import org.optaplanner.optapy.translator.types.errors.StopIteration;

/**
 * Implementations of opcodes related to collections (list, tuple, set, dict).
 */
public class CollectionImplementor {

    /**
     * TOS is an iterator; perform TOS' = next(TOS).
     * If TOS is exhausted (which is indicated when it raises a {@link StopIteration} exception),
     * Jump relatively by the instruction argument and pop TOS. Otherwise,
     * leave TOS below TOS' and go to the next instruction.
     *
     * Note: {@link StopIteration} does not fill its stack trace, which make it much more efficient than
     * normal exceptions.
     */
    public static void iterateIterator(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, Map<Integer, Label> bytecodeCounterToLabelMap) {
        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label catchStartLabel = new Label();
        Label catchEndLabel = new Label();
        Label loopEndLabel = bytecodeCounterToLabelMap.computeIfAbsent(instruction.offset + instruction.arg, key -> new Label());

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, catchStartLabel, Type.getInternalName(StopIteration.class));

        methodVisitor.visitLabel(tryStartLabel);
        methodVisitor.visitInsn(Opcodes.DUP);
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
        methodVisitor.visitLabel(tryEndLabel);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, catchEndLabel);

        methodVisitor.visitLabel(catchStartLabel);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, loopEndLabel);
        methodVisitor.visitLabel(catchEndLabel);
    }

    /**
     * TOS is an iterable; push {@code toUnpack} elements from it to the stack
     * (with first item of the iterable as the new TOS). Raise an exception if it does not
     * have exactly {@code toUnpack} elements.
     */
    public static void unpackSequence(MethodVisitor methodVisitor, int toUnpack, LocalVariableHelper localVariableHelper) {
        // Initialize size and unpacked elements local variables
        int sizeLocal = localVariableHelper.newLocal();

        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitVarInsn(Opcodes.ISTORE, sizeLocal);

        int[] unpackedLocals = new int[toUnpack];

        for (int i = 0; i < toUnpack; i++) {
            unpackedLocals[i] = localVariableHelper.newLocal();
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, unpackedLocals[i]);
        }

        // Get the iterator for the iterable
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.ITERATOR);

        // Surround the unpacking code in a try...finally block
        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label finallyStartLabel = new Label();

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, finallyStartLabel, null);

        methodVisitor.visitLabel(tryStartLabel);

        for (int i = 0; i < toUnpack + 1; i++) {
            // Call the next method of the iterator
            methodVisitor.visitInsn(Opcodes.DUP);
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
            if (i < toUnpack) { // Store the unpacked value in a local
                methodVisitor.visitVarInsn(Opcodes.ASTORE, unpackedLocals[i]);
            } else { // Try to get more elements to see if the iterable contain exactly enough
                methodVisitor.visitInsn(Opcodes.POP);
            }
            // increment size
            methodVisitor.visitIincInsn(sizeLocal, 1);
        }
        methodVisitor.visitLabel(tryEndLabel);

        methodVisitor.visitLabel(finallyStartLabel);

        Label toFewElements = new Label();
        Label toManyElements = new Label();
        Label exactNumberOfElements = new Label();

        // Pop off the iterator
        methodVisitor.visitInsn(Opcodes.POP);

        // Check if too few
        methodVisitor.visitVarInsn(Opcodes.ILOAD, sizeLocal);
        methodVisitor.visitLdcInsn(toUnpack);
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, toFewElements);

        // Check if too many
        methodVisitor.visitVarInsn(Opcodes.ILOAD, sizeLocal);
        methodVisitor.visitLdcInsn(toUnpack);
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, toManyElements);

        // Must have exactly enough
        methodVisitor.visitJumpInsn(Opcodes.GOTO, exactNumberOfElements);

        // TODO: Throw ValueError instead
        methodVisitor.visitLabel(toFewElements);
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);

        // Build error message string
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("not enough values to unpack (expected " + toUnpack + ", got ");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class),
                                      "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                                      false);

        methodVisitor.visitVarInsn(Opcodes.ILOAD, sizeLocal);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                                      "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.INT_TYPE),
                                      false);

        methodVisitor.visitLdcInsn(")");
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                                      "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.getType(String.class)),
                                      false);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                                      "toString", Type.getMethodDescriptor(Type.getType(String.class)),
                                      false);

        // Call the constructor of the Error
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                                      "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                                      false);
        // And throw it
        methodVisitor.visitInsn(Opcodes.ATHROW);


        methodVisitor.visitLabel(toManyElements);
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("too many values to unpack (expected " + toUnpack + ")");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                                      "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                                      false);
        methodVisitor.visitInsn(Opcodes.ATHROW);


        methodVisitor.visitLabel(exactNumberOfElements);
        for (int i = toUnpack - 1; i >= 0; i--) {
            // Unlike all other collection operators, UNPACK_SEQUENCE unpacks the result in reverse order
            methodVisitor.visitVarInsn(Opcodes.ALOAD, unpackedLocals[i]);
        }

        localVariableHelper.freeLocal();
        for (int i = 0; i < toUnpack; i++) {
            localVariableHelper.freeLocal();
        }
    }

    /**
     * TOS is an iterable; push {@code toUnpack} elements from it to the stack
     * (with first item of the iterable as the new TOS). Below the elements in the stack
     * is a list containing the remaining elements in the iterable (empty if there are none).
     * Raise an exception if it has less than {@code toUnpack} elements.
     */
    public static void unpackSequenceWithTail(MethodVisitor methodVisitor, int toUnpack, LocalVariableHelper localVariableHelper) {
        // Initialize size, unpacked elements and tail local variables
        int sizeLocal = localVariableHelper.newLocal();

        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitVarInsn(Opcodes.ISTORE, sizeLocal);

        int[] unpackedLocals = new int[toUnpack];

        for (int i = 0; i < toUnpack; i++) {
            unpackedLocals[i] = localVariableHelper.newLocal();
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, unpackedLocals[i]);
        }

        int tailLocal = localVariableHelper.newLocal();
        CollectionImplementor.buildCollection(PythonLikeList.class, methodVisitor, 0);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, tailLocal);

        // Get the iterator for the iterable
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.ITERATOR);

        // Surround the unpacking code in a try...finally block
        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label finallyStartLabel = new Label();

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, finallyStartLabel, null);

        methodVisitor.visitLabel(tryStartLabel);

        for (int i = 0; i < toUnpack; i++) {
            // Call the next method of the iterator
            methodVisitor.visitInsn(Opcodes.DUP);
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
            // Store the unpacked value in a local
            methodVisitor.visitVarInsn(Opcodes.ASTORE, unpackedLocals[i]);
            // increment size
            methodVisitor.visitIincInsn(sizeLocal, 1);
        }

        // Keep iterating through the iterable until StopIteration is raised to get all of its elements
        Label tailLoopStart = new Label();
        methodVisitor.visitLabel(tailLoopStart);

        methodVisitor.visitInsn(Opcodes.DUP);
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, tailLocal);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                                      "add",
                                      Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                                      true);
        methodVisitor.visitInsn(Opcodes.POP); // Pop the return value of add
        methodVisitor.visitJumpInsn(Opcodes.GOTO, tailLoopStart);


        methodVisitor.visitLabel(tryEndLabel);

        methodVisitor.visitLabel(finallyStartLabel);

        Label exactNumberOfElements = new Label();

        // Pop off the iterator
        methodVisitor.visitInsn(Opcodes.POP);

        // Check if too few
        methodVisitor.visitVarInsn(Opcodes.ILOAD, sizeLocal);
        methodVisitor.visitLdcInsn(toUnpack);
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, exactNumberOfElements);

        // TODO: Throw ValueError instead
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);

        // Build error message string
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("not enough values to unpack (expected " + toUnpack + ", got ");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class),
                                      "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                                      false);

        methodVisitor.visitVarInsn(Opcodes.ILOAD, sizeLocal);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                                      "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.INT_TYPE),
                                      false);

        methodVisitor.visitLdcInsn(")");
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                                      "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.getType(String.class)),
                                      false);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                                      "toString", Type.getMethodDescriptor(Type.getType(String.class)),
                                      false);

        // Call the constructor of the Error
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                                      "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                                      false);
        // And throw it
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(exactNumberOfElements);

        // Unlike all other collection operators, UNPACK_SEQUENCE unpacks the result in reverse order
        methodVisitor.visitVarInsn(Opcodes.ALOAD, tailLocal);
        for (int i = toUnpack - 1; i >= 0; i--) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, unpackedLocals[i]);
        }

        localVariableHelper.freeLocal();
        for (int i = 0; i < toUnpack; i++) {
            localVariableHelper.freeLocal();
        }
        localVariableHelper.freeLocal();
    }


    /**
     * Constructs a collection from the top {@code itemCount} on the stack.
     * {@code collectionType} MUST implement PythonLikeObject and define
     * a reverseAdd(PythonLikeObject) method. Basically generate the following code:
     *
     * <code>
     * <pre>
     *     CollectionType collection = new CollectionType(itemCount);
     *     collection.reverseAdd(TOS);
     *     collection.reverseAdd(TOS1);
     *     ...
     *     collection.reverseAdd(TOS(itemCount - 1));
     * </pre>
     * </code>
     * @param collectionType The type of collection to create
     * @param itemCount The number of items to put into collection from the stack
     */
    public static void buildCollection(Class<?> collectionType, MethodVisitor methodVisitor,
                                       int itemCount) {
        String typeInternalName = Type.getInternalName(collectionType);
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeInternalName);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(itemCount);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeInternalName, "<init>",
                                      Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);

        for (int i = 0; i < itemCount; i++) {
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typeInternalName,
                    "reverseAdd",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class)),
                    false);
        }
    }

    /**
     * Convert TOS from a List to a tuple. Basically generates this code
     *
     * <code>
     * <pre>
     *     TOS' = PythonLikeTuple.fromList(TOS);
     * </pre>
     * </code>
     */
    public static void convertListToTuple(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonLikeTuple.class),
                                      "fromList",
                                      Type.getMethodDescriptor(Type.getType(PythonLikeTuple.class), Type.getType(List.class)),
                                      false);
    }

    /**
     * Constructs a map from the top {@code 2 * itemCount} on the stack.
     * {@code mapType} MUST implement PythonLikeObject. Basically generate the following code:
     *
     * <code>
     * <pre>
     *     MapType collection = new MapType(itemCount);
     *     collection.put(TOS1, TOS);
     *     collection.put(TOS3, TOS2);
     *     ...
     *     collection.put(TTOS(2*itemCount - 1), TOS(2*itemCount - 2));
     * </pre>
     * </code>
     * @param mapType The type of map to create
     * @param itemCount The number of key value pairs to put into map from the stack
     */
    public static void buildMap(Class<? extends Map> mapType, MethodVisitor methodVisitor,
                                        int itemCount) {
        String typeInternalName = Type.getInternalName(mapType);
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeInternalName);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeInternalName, "<init>", "()V", false);

        for (int i = 0; i < itemCount; i++) {
            methodVisitor.visitInsn(Opcodes.DUP_X2);
            StackManipulationImplementor.rotateThree(methodVisitor);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                                          "put",
                                          Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)),
                                          true);
            methodVisitor.visitInsn(Opcodes.POP); // pop return value of "put"
        }
    }

    /**
     * Constructs a map from the top {@code itemCount + 1} on the stack.
     * TOS is a tuple containing keys; TOS1-TOS(itemCount) are the values
     * {@code mapType} MUST implement PythonLikeObject. Basically generate the following code:
     *
     * <code>
     * <pre>
     *     MapType collection = new MapType();
     *     collection.put(TOS[0], TOS1);
     *     collection.put(TOS[1], TOS2);
     *     ...
     *     collection.put(TOS[itemCount-1], TOS(itemCount));
     * </pre>
     * </code>
     * @param mapType The type of map to create
     * @param itemCount The number of key value pairs to put into map from the stack
     */
    public static void buildConstKeysMap(Class<? extends Map> mapType, MethodVisitor methodVisitor,
                                int itemCount) {
        String typeInternalName = Type.getInternalName(mapType);
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeInternalName);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeInternalName, "<init>", "()V", false);

        for (int i = 0; i < itemCount; i++) {
            // Stack is value, keyTuple, Map
            methodVisitor.visitInsn(Opcodes.DUP_X2);
            StackManipulationImplementor.rotateThree(methodVisitor);

            // Stack is Map, Map, value, keyTuple
            methodVisitor.visitInsn(Opcodes.DUP_X2);

            //Stack is Map, keyTuple, Map, value, keyTuple
            methodVisitor.visitLdcInsn(itemCount - i - 1); // We are adding in reverse order
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                                          "get",
                                          Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(int.class)),
                                          true);

            // Stack is Map, keyTuple, Map, value, key
            methodVisitor.visitInsn(Opcodes.SWAP);

            // Stack is Map, keyTuple, Map, key, value
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                                          "put",
                                          Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)),
                                          true);
            methodVisitor.visitInsn(Opcodes.POP); // pop return value of "put"

            // Stack is Map, keyTuple
            methodVisitor.visitInsn(Opcodes.SWAP);
        }
        // Stack is keyTuple, Map
        // Pop the keyTuple off the stack
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.POP);
    }

    /**
     * Implements TOS1 in TOS. TOS must be a collection/object that implement the "__contains__" dunder method.
     */
    public static void containsOperator(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        StackManipulationImplementor.swap(methodVisitor);
        DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.CONTAINS);
        // TODO: implement fallback on __getitem__ if __contains__ does not exist
        if (instruction.arg == 1) {
            PythonBuiltinOperatorImplementor.performNotOnTOS(methodVisitor);
        }
    }

    /**
     * Implements TOS1[TOS] = TOS2. TOS1 must be a collection/object that implement the "__setitem__" dunder method.
     */
    public static void setItem(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper) {
        // Stack is TOS2, TOS1, TOS
        StackManipulationImplementor.rotateThree(methodVisitor);
        StackManipulationImplementor.rotateThree(methodVisitor);
        // Stack is TOS1, TOS, TOS2
        DunderOperatorImplementor.ternaryOperator(methodVisitor, PythonTernaryOperators.SET_ITEM, localVariableHelper);
        StackManipulationImplementor.popTOS(methodVisitor);
    }

    /**
     * Calls collection.add(TOS1[i], TOS). TOS1[i] remains on stack; TOS is popped. Used to implement list/set comprehensions.
     */
    public static void collectionAdd(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // instruction.arg is distance from TOS1, so add 1 to get distance from TOS
        StackManipulationImplementor.duplicateToTOS(methodVisitor, localVariableHelper, instruction.arg + 1);
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                                      "add",
                                      Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                                      true);
        StackManipulationImplementor.popTOS(methodVisitor); // pop Collection.add return value
    }

    /**
     * Calls collection.addAll(TOS1[i], TOS). TOS1[i] remains on stack; TOS is popped. Used to build lists/maps.
     */
    public static void collectionAddAll(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // instruction.arg is distance from TOS1, so add 1 to get distance from TOS
        StackManipulationImplementor.duplicateToTOS(methodVisitor, localVariableHelper, instruction.arg + 1);
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                                      "addAll",
                                      Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Collection.class)),
                                      true);
        StackManipulationImplementor.popTOS(methodVisitor); // pop Collection.add return value
    }

    /**
     * Calls map.put(TOS1[i], TOS1, TOS). TOS1[i] remains on stack; TOS and TOS1 are popped. Used to implement map comprehensions.
     */
    public static void mapPut(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // instruction.arg is distance from TOS1, so add 1 to get distance from TOS
        StackManipulationImplementor.duplicateToTOS(methodVisitor, localVariableHelper, instruction.arg + 1);
        StackManipulationImplementor.rotateThree(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                                      "put",
                                      Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)),
                                      true);
        StackManipulationImplementor.popTOS(methodVisitor); // pop Map.put return value
    }

    /**
     * Calls map.putAll(TOS1[i], TOS). TOS1[i] remains on stack; TOS is popped. Used to build maps
     */
    public static void mapPutAll(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // instruction.arg is distance from TOS1, so add 1 to get distance from TOS
        StackManipulationImplementor.duplicateToTOS(methodVisitor, localVariableHelper, instruction.arg + 1);
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                                      "putAll",
                                      Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class)),
                                      true);
    }

    /**
     * Calls map.putAll(TOS1[i], TOS) if TOS does not share any common keys with TOS[i].
     * If TOS shares common keys with TOS[i], an exception is thrown.
     * TOS1[i] remains on stack; TOS is popped. Used to build maps
     */
    public static void mapPutAllOnlyIfAllNewElseThrow(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // instruction.arg is distance from TOS1, so add 1 to get distance from TOS
        StackManipulationImplementor.duplicateToTOS(methodVisitor, localVariableHelper, instruction.arg + 1);
        StackManipulationImplementor.swap(methodVisitor);

        // Duplicate both maps so we can get their key sets
        StackManipulationImplementor.duplicateTOSAndTOS1(methodVisitor);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                                      "keySet",
                                      Type.getMethodDescriptor(Type.getType(Set.class)),
                                      true);

        StackManipulationImplementor.swap(methodVisitor);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                                      "keySet",
                                      Type.getMethodDescriptor(Type.getType(Set.class)),
                                      true);


        // Check if the two key sets are disjoints
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class),
                                      "disjoint",
                                      Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Collection.class), Type.getType(Collection.class)),
                                      false);

        Label performPutAllLabel = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IFNE, performPutAllLabel); // if result == 1 (i.e. true), do the putAll operation

        // else, throw a new exception
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                                      "<init>",
                                      Type.getMethodDescriptor(Type.VOID_TYPE),
                                      false);
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(performPutAllLabel);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                                      "putAll",
                                      Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class)),
                                      true);
    }
}
