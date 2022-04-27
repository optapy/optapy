package org.optaplanner.python.translator.implementors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.ObjectBuiltinOperations;
import org.optaplanner.python.translator.types.PythonString;

public class StringImplementor {

    /**
     * Constructs a string from the top {@code itemCount} on the stack.
     * Basically generate the following code:
     *
     * <code>
     * <pre>
     *     StringBuilder builder = new StringBuilder();
     *     builder.insert(0, TOS);
     *     builder.insert(0, TOS1);
     *     ...
     *     builder.insert(0, TOS(itemCount - 1));
     *     TOS' = PythonString.valueOf(builder.toString())
     * </pre>
     * </code>
     * 
     * @param itemCount The number of items to put into collection from the stack
     */
    public static void buildString(MethodVisitor methodVisitor,
            int itemCount) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        for (int i = 0; i < itemCount; i++) {
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitInsn(Opcodes.SWAP);

            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "insert",
                    Type.getMethodDescriptor(Type.getType(StringBuilder.class),
                            Type.INT_TYPE,
                            Type.getType(Object.class)),
                    false);
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                "toString",
                Type.getMethodDescriptor(Type.getType(String.class)), false);

        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonString.class),
                "valueOf",
                Type.getMethodDescriptor(Type.getType(PythonString.class),
                        Type.getType(String.class)),
                false);
    }

    /**
     * TOS1 is a value and TOS is an optional format string (either PythonNone or PythonString).
     * Depending on {@code instruction.arg}, does one of several things to TOS1 before formatting it
     * (as specified by {@link PythonBytecodeInstruction.OpCode#FORMAT_VALUE}:
     *
     * arg & 3 == 0: Do nothing
     * arg & 3 == 1: Call str() on value before formatting it
     * arg & 3 == 2: Call repr() on value before formatting it
     * arg & 3 == 3: Call ascii() on value before formatting it
     *
     * if arg & 4 == 0, TOS is the value to format, so push PythonNone before calling format
     * if arg & 4 == 4, TOS is a format string, use it in the call
     */
    public static void formatValue(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        if ((instruction.arg & 4) == 0) {
            // No format string on stack; push None
            PythonConstantsImplementor.loadNone(methodVisitor);
        }

        switch (instruction.arg & 3) {
            case 0: // Do Nothing
                break;
            case 1: // Call str()
                StackManipulationImplementor.swap(methodVisitor);
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_STRING);
                StackManipulationImplementor.swap(methodVisitor);
                break;
            case 2: // Call repr()
                StackManipulationImplementor.swap(methodVisitor);
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.REPRESENTATION);
                StackManipulationImplementor.swap(methodVisitor);
                break;
            case 3: // Call ascii()
                StackManipulationImplementor.swap(methodVisitor);
                // TODO: Create method that calls repr and convert non-ascii character to ascii and call it
                StackManipulationImplementor.swap(methodVisitor);
                break;
            default:
                throw new IllegalStateException("Invalid flag: " + instruction.arg +
                        "; & did not produce a value in range 0-3: " + (instruction.arg & 3));
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ObjectBuiltinOperations.class),
                "format",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class)),
                false);
    }

    /**
     * TOS is an PythonLikeObject to be printed. Pop TOS off the stack and print it.
     */
    public static void print(MethodVisitor methodVisitor, String className) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className,
                PythonBytecodeToJavaBytecodeTranslator.INTERPRETER_INSTANCE_FIELD_NAME,
                Type.getDescriptor(PythonInterpreter.class));
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonInterpreter.class),
                "print",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class)),
                true);
    }
}
