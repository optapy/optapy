package org.optaplanner.jpyinterpreter.implementors;

import java.util.Optional;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.FieldDescriptor;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.LocalVariableHelper;
import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonTernaryOperators;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.errors.AttributeError;

/**
 * Implementations of opcodes related to objects
 */
public class ObjectImplementor {

    /**
     * Replaces TOS with getattr(TOS, co_names[instruction.arg])
     */
    public static void getAttribute(FunctionMetadata functionMetadata, MethodVisitor methodVisitor, String className,
            StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        PythonLikeType tosType = stackMetadata.getTOSType();
        String name = functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg);
        Optional<FieldDescriptor> maybeFieldDescriptor = tosType.getInstanceFieldDescriptor(name);

        if (maybeFieldDescriptor.isPresent()) {
            FieldDescriptor fieldDescriptor = maybeFieldDescriptor.get();
            if (fieldDescriptor.isTrueFieldDescriptor()) {
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.getDeclaringClassInternalName());
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, fieldDescriptor.getDeclaringClassInternalName(),
                        fieldDescriptor.getJavaFieldName(),
                        fieldDescriptor.getJavaFieldTypeDescriptor());

                // Check if field is null. If it is null, then it was deleted, so we should raise an AttributeError
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);

                Label ifNotNull = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifNotNull);

                // Throw attribute error
                methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(AttributeError.class));
                methodVisitor.visitInsn(Opcodes.DUP);

                if (fieldDescriptor.getFieldPythonLikeType().isInstance(PythonNone.INSTANCE)) {
                    methodVisitor.visitLdcInsn("'" + tosType.getTypeName() + "' object has no attribute '" + name + "'.");
                } else {
                    // None cannot be assigned to the field, meaning it will delete the attribute instead
                    methodVisitor.visitLdcInsn("'" + tosType.getTypeName() + "' object has no attribute '" + name + "'. " +
                            "It might of been deleted because None cannot be assigned to it; either use " +
                            "hasattr(obj, '" + name + "') or change the typing to allow None (ex: typing.Optional[" +
                            tosType.getTypeName() + "]).");
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(AttributeError.class),
                        "<init>",
                        Type.getMethodDescriptor(Type.getType(void.class), Type.getType(String.class)),
                        false);
                methodVisitor.visitInsn(Opcodes.ATHROW);

                // The attribute was not null
                methodVisitor.visitLabel(ifNotNull);
            } else {
                // It a false field descriptor, which means TOS is a type and this is a field for a method
                // We can call $method$__getattribute__ directly (since type do not override it),
                // which is more efficient then going through the full logic of __getattribute__ dunder method impl.
                PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                        "$method$__getattribute__", Type.getMethodDescriptor(
                                Type.getType(PythonLikeObject.class),
                                Type.getType(PythonString.class)),
                        true);
            }
        } else {
            PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg);
            DunderOperatorImplementor.binaryOperator(methodVisitor,
                    stackMetadata.pushTemp(BuiltinTypes.STRING_TYPE),
                    PythonBinaryOperators.GET_ATTRIBUTE);
        }
    }

    /**
     * Deletes co_names[instruction.arg] of TOS
     */
    public static void deleteAttribute(FunctionMetadata functionMetadata, MethodVisitor methodVisitor, String className,
            StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        PythonLikeType tosType = stackMetadata.getTOSType();
        String name = functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg);
        Optional<FieldDescriptor> maybeFieldDescriptor = tosType.getInstanceFieldDescriptor(name);
        if (maybeFieldDescriptor.isPresent()) {
            FieldDescriptor fieldDescriptor = maybeFieldDescriptor.get();
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.getDeclaringClassInternalName());
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, fieldDescriptor.getDeclaringClassInternalName(),
                    fieldDescriptor.getJavaFieldName(),
                    fieldDescriptor.getJavaFieldTypeDescriptor());
        } else {
            PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg);
            DunderOperatorImplementor.binaryOperator(methodVisitor,
                    stackMetadata.pushTemp(BuiltinTypes.STRING_TYPE),
                    PythonBinaryOperators.DELETE_ATTRIBUTE);
        }
    }

    /**
     * Implement TOS.name = TOS1, where name is co_names[instruction.arg]. TOS and TOS1 are popped.
     */
    public static void setAttribute(FunctionMetadata functionMetadata, MethodVisitor methodVisitor, String className,
            StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        PythonLikeType tosType = stackMetadata.getTOSType();
        String name = functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg);
        Optional<FieldDescriptor> maybeFieldDescriptor = tosType.getInstanceFieldDescriptor(name);
        if (maybeFieldDescriptor.isPresent()) {
            FieldDescriptor fieldDescriptor = maybeFieldDescriptor.get();
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.getDeclaringClassInternalName());
            StackManipulationImplementor.swap(methodVisitor);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.getFieldPythonLikeType().getJavaTypeInternalName());
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, fieldDescriptor.getDeclaringClassInternalName(),
                    fieldDescriptor.getJavaFieldName(),
                    fieldDescriptor.getJavaFieldTypeDescriptor());
        } else {
            StackManipulationImplementor.swap(methodVisitor);
            PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg);
            StackManipulationImplementor.swap(methodVisitor);
            DunderOperatorImplementor.ternaryOperator(functionMetadata, stackMetadata.pop(2)
                    .push(stackMetadata.getValueSourceForStackIndex(0))
                    .pushTemp(BuiltinTypes.STRING_TYPE)
                    .push(stackMetadata.getValueSourceForStackIndex(1)),
                    PythonTernaryOperators.SET_ATTRIBUTE);
        }
    }
}
