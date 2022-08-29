package org.optaplanner.python.translator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.implementors.CollectionImplementor;
import org.optaplanner.python.translator.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.python.translator.implementors.StackManipulationImplementor;
import org.optaplanner.python.translator.types.BoundPythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;

public class PythonFunctionSignature {
    final PythonLikeType returnType;
    final PythonLikeType[] parameterTypes;

    final MethodDescriptor methodDescriptor;

    final List<PythonLikeObject> defaultArgumentList;
    final Map<String, Integer> keywordToArgumentIndexMap;

    final Class<?> defaultArgumentHolderClass;

    private static Map<String, Integer> extractKeywordArgument(MethodDescriptor methodDescriptor) {
        Map<String, Integer> out = new HashMap<>();

        int index = 0;
        for (Type parameterType : methodDescriptor.getParameterTypes()) {
            out.put("arg" + index, index);
        }
        return out;
    }

    public static PythonFunctionSignature forMethod(Method method) {
        MethodDescriptor methodDescriptor = new MethodDescriptor(method);
        PythonLikeType returnType = JavaPythonTypeConversionImplementor.getPythonLikeType(method.getReturnType());
        PythonLikeType[] parameterTypes = new PythonLikeType[method.getParameterCount()];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = JavaPythonTypeConversionImplementor.getPythonLikeType(method.getParameterTypes()[i]);
        }
        return new PythonFunctionSignature(methodDescriptor, returnType, parameterTypes);
    }

    public PythonFunctionSignature(MethodDescriptor methodDescriptor,
            PythonLikeType returnType, PythonLikeType... parameterTypes) {
        this(methodDescriptor, List.of(), extractKeywordArgument(methodDescriptor), returnType, parameterTypes);
    }

    public PythonFunctionSignature(MethodDescriptor methodDescriptor,
            PythonLikeType returnType, List<PythonLikeType> parameterTypeList) {
        this(methodDescriptor, List.of(), extractKeywordArgument(methodDescriptor), returnType, parameterTypeList);
    }

    public PythonFunctionSignature(MethodDescriptor methodDescriptor,
            List<PythonLikeObject> defaultArgumentList,
            Map<String, Integer> keywordToArgumentIndexMap,
            PythonLikeType returnType, PythonLikeType... parameterTypes) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.methodDescriptor = methodDescriptor;
        this.defaultArgumentList = defaultArgumentList;
        this.keywordToArgumentIndexMap = keywordToArgumentIndexMap;
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap);

    }

    public PythonFunctionSignature(MethodDescriptor methodDescriptor,
            List<PythonLikeObject> defaultArgumentList,
            Map<String, Integer> keywordToArgumentIndexMap,
            PythonLikeType returnType, List<PythonLikeType> parameterTypesList) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypesList.toArray(new PythonLikeType[0]);
        this.methodDescriptor = methodDescriptor;
        this.defaultArgumentList = defaultArgumentList;
        this.keywordToArgumentIndexMap = keywordToArgumentIndexMap;
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap);
    }

    public PythonLikeType getReturnType() {
        return returnType;
    }

    public PythonLikeType[] getParameterTypes() {
        return parameterTypes;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public boolean matchesParameters(PythonLikeType... callParameters) {
        int minParameters = parameterTypes.length - defaultArgumentList.size();
        int maxParameters = parameterTypes.length;
        int startIndex = 0;

        if (methodDescriptor.methodType == MethodDescriptor.MethodType.STATIC_AS_VIRTUAL) {
            minParameters--;
            maxParameters--;
            startIndex = 1;
        }
        if (callParameters.length < minParameters || callParameters.length > maxParameters) {
            return false;
        }

        for (int i = startIndex; i < callParameters.length; i++) {
            PythonLikeType overloadParameterType = parameterTypes[i];
            PythonLikeType callParameterType = callParameters[i];

            if (!callParameterType.isSubclassOf(overloadParameterType)) {
                return false;
            }
        }
        return true;
    }

    public boolean moreSpecificThan(PythonFunctionSignature other) {
        if (other.parameterTypes.length != parameterTypes.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            PythonLikeType overloadParameterType = parameterTypes[i];
            PythonLikeType otherParameterType = other.parameterTypes[i];

            if (otherParameterType.equals(overloadParameterType)) {
                continue;
            }

            if (otherParameterType.isSubclassOf(overloadParameterType)) {
                return false;
            }
        }
        return true;
    }

    private void unwrapBoundMethod(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper, int posFromTOS) {
        if (methodDescriptor.methodType == MethodDescriptor.MethodType.VIRTUAL ||
                methodDescriptor.methodType == MethodDescriptor.MethodType.INTERFACE) {
            StackManipulationImplementor.duplicateToTOS(methodVisitor, localVariableHelper, posFromTOS);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(BoundPythonLikeFunction.class),
                    "getInstance", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class)),
                    false);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
            StackManipulationImplementor.shiftTOSDownBy(methodVisitor, localVariableHelper, posFromTOS);
        }
    }

    public void callMethod(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper, int argumentCount) {
        if (argumentCount != parameterTypes.length && defaultArgumentHolderClass == null) {
            throw new IllegalStateException("Cannot call " + this + " because there are not enough arguments");
        }

        int[] argumentLocals = new int[argumentCount];

        for (int i = 0; i < argumentCount; i++) {
            argumentLocals[argumentCount - i - 1] = localVariableHelper.newLocal();
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class),
                    argumentLocals[argumentCount - i - 1]);
        }

        if (methodDescriptor.getMethodType() != MethodDescriptor.MethodType.STATIC
                || methodDescriptor.getMethodType() != MethodDescriptor.MethodType.STATIC_AS_VIRTUAL) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
        }

        for (int i = 0; i < argumentCount; i++) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), argumentLocals[i]);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterTypes[i].getJavaTypeInternalName());
        }

        for (int i = 0; i < argumentCount; i++) {
            localVariableHelper.freeLocal();
        }

        for (int i = parameterTypes.length - 1; i >= argumentCount; i--) {
            int defaultIndex = defaultArgumentList.size() - i;
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(defaultArgumentHolderClass),
                    PythonDefaultArgumentImplementor.getConstantName(defaultIndex),
                    Type.getDescriptor(defaultArgumentList.get(defaultIndex).getClass()));
        }

        methodDescriptor.callMethod(methodVisitor);
    }

    public void callWithoutKeywords(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper, int argumentCount) {
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
        callWithKeywords(methodVisitor, localVariableHelper, argumentCount);
    }

    public void callWithKeywords(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper,
            int argumentCount) {
        if (argumentCount != parameterTypes.length && defaultArgumentHolderClass == null) {
            throw new IllegalStateException("Cannot call " + this + " because there are not enough arguments");
        }

        unwrapBoundMethod(methodVisitor, localVariableHelper, argumentCount + 1);

        // TOS is a tuple of keys
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(defaultArgumentHolderClass));
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(defaultArgumentHolderClass),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeTuple.class)),
                false);

        for (int i = 0; i < argumentCount; i++) {
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(defaultArgumentHolderClass),
                    "addArgument", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class)),
                    false);
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(defaultArgumentHolderClass),
                    PythonDefaultArgumentImplementor.getArgumentName(i),
                    "L" + parameterTypes[i].getJavaTypeInternalName() + ";");
            methodVisitor.visitInsn(Opcodes.SWAP);
        }
        methodVisitor.visitInsn(Opcodes.POP);

        methodDescriptor.callMethod(methodVisitor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonFunctionSignature that = (PythonFunctionSignature) o;
        return returnType.equals(that.returnType) && Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(returnType);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    @Override
    public String toString() {
        return methodDescriptor.getMethodName() +
                Arrays.stream(parameterTypes).map(PythonLikeType::toString).collect(Collectors.joining(", ", "(", ") -> ")) +
                returnType;
    }
}
