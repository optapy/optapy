package org.optaplanner.optapy.translator.implementors;

import java.util.Collection;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.PythonBinaryOperators;
import org.optaplanner.optapy.translator.PythonBytecodeInstruction;
import org.optaplanner.optapy.translator.PythonUnaryOperator;
import org.optaplanner.optapy.translator.types.StopIteration;

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
    public static void buildCollection(Class<? extends Collection> collectionType, MethodVisitor methodVisitor,
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
     * Constructs a map from the top {@code itemCount} on the stack.
     * {@code mapType} MUST implement PythonLikeObject. Basically generate the following code:
     *
     * <code>
     * <pre>
     *     MapType collection = new MapType(itemCount);
     *     collection.put(TOS1, TOS);
     *     collection.put(TOS3, TOS2);
     *     ...
     *     collection.reverseAdd(TTOS(2*itemCount - 1), TOS(2*itemCount - 2));
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
            methodVisitor.visitInsn(Opcodes.DUP_X2);
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                                          "put",
                                          Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)),
                                          true);
            methodVisitor.visitInsn(Opcodes.POP);
        }
    }

    /**
     * Implements TOS1 in TOS. TOS must be a collection/object that implement the "__contains__" dunder method.
     * @param methodVisitor
     * @param instruction
     */
    public static void containsOperator(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        StackManipulationImplementor.swap(methodVisitor);
        DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.CONTAINS);
        // TODO: implement fallback on __getitem__ if __contains__ does not exist
        if (instruction.arg == 1) {
            PythonBuiltinOperatorImplementor.performNotOnTOS(methodVisitor);
        }
    }
}
