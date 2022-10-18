package org.optaplanner.jpyinterpreter.implementors;

import java.util.Optional;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.optaplanner.jpyinterpreter.FieldDescriptor;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.LocalVariableHelper;
import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonTernaryOperators;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

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
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.getDeclaringClassInternalName());
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, fieldDescriptor.getDeclaringClassInternalName(),
                    fieldDescriptor.getJavaFieldName(),
                    fieldDescriptor.getJavaFieldTypeDescriptor());
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
