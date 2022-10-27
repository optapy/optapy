package org.optaplanner.jpyinterpreter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.implementors.CollectionImplementor;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.implementors.StackManipulationImplementor;
import org.optaplanner.jpyinterpreter.types.BoundPythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.util.arguments.ArgumentSpec;

public class PythonFunctionSignature {
    final PythonLikeType returnType;
    final PythonLikeType[] parameterTypes;

    final MethodDescriptor methodDescriptor;

    final List<PythonLikeObject> defaultArgumentList;
    final Map<String, Integer> keywordToArgumentIndexMap;

    final Optional<Integer> extraPositionalArgumentsVariableIndex;
    final Optional<Integer> extraKeywordArgumentsVariableIndex;

    final Class<?> defaultArgumentHolderClass;
    final boolean isFromArgumentSpec;

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
        this.extraPositionalArgumentsVariableIndex = Optional.empty();
        this.extraKeywordArgumentsVariableIndex = Optional.empty();
        isFromArgumentSpec = false;
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, getArgumentSpec());

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
        this.extraPositionalArgumentsVariableIndex = Optional.empty();
        this.extraKeywordArgumentsVariableIndex = Optional.empty();
        isFromArgumentSpec = false;
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, getArgumentSpec());
    }

    public PythonFunctionSignature(MethodDescriptor methodDescriptor,
            List<PythonLikeObject> defaultArgumentList,
            Map<String, Integer> keywordToArgumentIndexMap,
            PythonLikeType returnType, List<PythonLikeType> parameterTypesList,
            Optional<Integer> extraPositionalArgumentsVariableIndex,
            Optional<Integer> extraKeywordArgumentsVariableIndex) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypesList.toArray(new PythonLikeType[0]);
        this.methodDescriptor = methodDescriptor;
        this.defaultArgumentList = defaultArgumentList;
        this.keywordToArgumentIndexMap = keywordToArgumentIndexMap;
        this.extraPositionalArgumentsVariableIndex = extraPositionalArgumentsVariableIndex;
        this.extraKeywordArgumentsVariableIndex = extraKeywordArgumentsVariableIndex;
        isFromArgumentSpec = false;
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, getArgumentSpec());
    }

    public PythonFunctionSignature(MethodDescriptor methodDescriptor,
            List<PythonLikeObject> defaultArgumentList,
            Map<String, Integer> keywordToArgumentIndexMap,
            PythonLikeType returnType, List<PythonLikeType> parameterTypesList,
            Optional<Integer> extraPositionalArgumentsVariableIndex,
            Optional<Integer> extraKeywordArgumentsVariableIndex,
            ArgumentSpec<?> argumentSpec) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypesList.toArray(new PythonLikeType[0]);
        this.methodDescriptor = methodDescriptor;
        this.defaultArgumentList = defaultArgumentList;
        this.keywordToArgumentIndexMap = keywordToArgumentIndexMap;
        this.extraPositionalArgumentsVariableIndex = extraPositionalArgumentsVariableIndex;
        this.extraKeywordArgumentsVariableIndex = extraKeywordArgumentsVariableIndex;
        isFromArgumentSpec = true;
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, argumentSpec);
    }

    private ArgumentSpec<?> getArgumentSpec() {
        try {
            ArgumentSpec<?> argumentSpec = ArgumentSpec.forFunctionReturning(methodDescriptor.getMethodName(),
                    (Class<? extends PythonLikeObject>) returnType.getJavaClass());
            for (int i = 0; i < parameterTypes.length - defaultArgumentList.size(); i++) {
                if (extraPositionalArgumentsVariableIndex.isPresent() && extraPositionalArgumentsVariableIndex.get() == i) {
                    continue;
                }

                if (extraKeywordArgumentsVariableIndex.isPresent() && extraKeywordArgumentsVariableIndex.get() == i) {
                    continue;
                }

                final int argIndex = i;
                Optional<String> argumentName = keywordToArgumentIndexMap.entrySet()
                        .stream().filter(e -> e.getValue().equals(argIndex))
                        .map(Map.Entry::getKey)
                        .findAny();

                if (argumentName.isEmpty()) {
                    argumentSpec = argumentSpec.addArgument("$arg" + i,
                            (Class<? extends PythonLikeObject>) parameterTypes[i].getJavaClass());
                } else {
                    argumentSpec = argumentSpec.addArgument(argumentName.get(),
                            (Class<? extends PythonLikeObject>) parameterTypes[i].getJavaClass());
                }
            }

            for (int i = parameterTypes.length - defaultArgumentList.size(); i < parameterTypes.length; i++) {
                if (extraPositionalArgumentsVariableIndex.isPresent() && extraPositionalArgumentsVariableIndex.get() == i) {
                    continue;
                }

                if (extraKeywordArgumentsVariableIndex.isPresent() && extraKeywordArgumentsVariableIndex.get() == i) {
                    continue;
                }

                PythonLikeObject defaultValue =
                        defaultArgumentList.get(defaultArgumentList.size() - (parameterTypes.length - i));

                final int argIndex = i;
                Optional<String> argumentName = keywordToArgumentIndexMap.entrySet()
                        .stream().filter(e -> e.getValue().equals(argIndex))
                        .map(Map.Entry::getKey)
                        .findAny();

                if (argumentName.isEmpty()) {
                    argumentSpec = argumentSpec.addArgument("$arg" + i,
                            (Class<PythonLikeObject>) parameterTypes[i].getJavaClass(),
                            defaultValue);
                } else {
                    argumentSpec = argumentSpec.addArgument(argumentName.get(),
                            (Class<PythonLikeObject>) parameterTypes[i].getJavaClass(),
                            defaultValue);
                }
            }

            if (extraPositionalArgumentsVariableIndex.isPresent()) {
                argumentSpec = argumentSpec.addExtraPositionalVarArgument("*vargs");
            }

            if (extraKeywordArgumentsVariableIndex.isPresent()) {
                argumentSpec = argumentSpec.addExtraKeywordVarArgument("**kwargs");
            }
            return argumentSpec;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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

    public boolean isFromArgumentSpec() {
        return isFromArgumentSpec;
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
        if (callParameters.length < minParameters || (extraPositionalArgumentsVariableIndex.isEmpty() &&
                callParameters.length > maxParameters)) {
            return false;
        }

        for (int i = startIndex; i < Math.min(parameterTypes.length, callParameters.length); i++) {
            if (extraPositionalArgumentsVariableIndex.isPresent() && i == extraPositionalArgumentsVariableIndex.get()) {
                continue;
            }
            if (extraKeywordArgumentsVariableIndex.isPresent() && i == extraKeywordArgumentsVariableIndex.get()) {
                continue;
            }

            PythonLikeType overloadParameterType = parameterTypes[i];
            PythonLikeType callParameterType = callParameters[i];

            if (!callParameterType.isSubclassOf(overloadParameterType)) {
                return false;
            }
        }
        return true;
    }

    public boolean moreSpecificThan(PythonFunctionSignature other) {
        if (other.parameterTypes.length < parameterTypes.length &&
                (other.extraPositionalArgumentsVariableIndex.isPresent() ||
                        other.extraKeywordArgumentsVariableIndex.isPresent())) {
            return true;
        }

        if (other.parameterTypes.length > parameterTypes.length &&
                (extraPositionalArgumentsVariableIndex.isPresent() ||
                        extraKeywordArgumentsVariableIndex.isPresent())) {
            return false;
        }

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

    void unwrapBoundMethod(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int posFromTOS) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        if (methodDescriptor.methodType == MethodDescriptor.MethodType.VIRTUAL ||
                methodDescriptor.methodType == MethodDescriptor.MethodType.INTERFACE) {
            StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, posFromTOS);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(BoundPythonLikeFunction.class),
                    "getInstance", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class)),
                    false);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
            StackManipulationImplementor.shiftTOSDownBy(functionMetadata, stackMetadata, posFromTOS);
        }
    }

    public void callMethod(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper, int argumentCount) {
        Type[] descriptorParameterTypes = methodDescriptor.getParameterTypes();
        if (argumentCount < descriptorParameterTypes.length && defaultArgumentHolderClass == null) {
            throw new IllegalStateException("Cannot call " + this + " because there are not enough arguments");
        }

        if (argumentCount > descriptorParameterTypes.length && extraPositionalArgumentsVariableIndex.isEmpty()) {
            throw new IllegalStateException("Cannot call " + this + " because there are too many arguments");
        }

        int[] argumentLocals = new int[descriptorParameterTypes.length];

        boolean isVirtual = methodDescriptor.getMethodType() != MethodDescriptor.MethodType.STATIC
                || methodDescriptor.getMethodType() != MethodDescriptor.MethodType.STATIC_AS_VIRTUAL;
        final int offset = (methodDescriptor.getMethodType() == MethodDescriptor.MethodType.CLASS) ? 1 : 0;

        if (extraPositionalArgumentsVariableIndex.isPresent()) {
            argumentLocals[extraPositionalArgumentsVariableIndex.get()] = localVariableHelper.newLocal();
            CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeTuple.class),
                    argumentLocals[extraPositionalArgumentsVariableIndex.get()]);
        }

        if (extraKeywordArgumentsVariableIndex.isPresent()) {
            argumentLocals[extraKeywordArgumentsVariableIndex.get()] = localVariableHelper.newLocal();
            CollectionImplementor.buildMap(PythonLikeDict.class, methodVisitor, 0);
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeTuple.class),
                    argumentLocals[extraKeywordArgumentsVariableIndex.get()]);
        }

        if (argumentCount > descriptorParameterTypes.length) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeTuple.class),
                    argumentLocals[extraPositionalArgumentsVariableIndex.get()]);
            for (int i = argumentCount; i >= descriptorParameterTypes.length; i--) {
                methodVisitor.visitInsn(Opcodes.DUP_X1);
                methodVisitor.visitInsn(Opcodes.SWAP);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitInsn(Opcodes.SWAP);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeTuple.class), "add",
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(PythonLikeObject.class)),
                        false);
            }
            methodVisitor.visitInsn(Opcodes.POP);
        }

        int readArguments = Math.min(argumentCount, descriptorParameterTypes.length - offset);

        for (int i = 0; i < readArguments; i++) {
            if ((extraPositionalArgumentsVariableIndex.isPresent() && extraPositionalArgumentsVariableIndex.get() == i + offset)
                    ||
                    (extraKeywordArgumentsVariableIndex.isPresent()
                            && extraKeywordArgumentsVariableIndex.get() == i + offset)) {
                continue; // These parameters are set last
            }
            argumentLocals[descriptorParameterTypes.length - i - 1] = localVariableHelper.newLocal();
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class),
                    argumentLocals[descriptorParameterTypes.length - i - 1]);
        }
        if (isVirtual) {
            if (methodDescriptor.getMethodType() == MethodDescriptor.MethodType.CLASS) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                        "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                        true);
            } else {
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
            }
        }

        for (int i = 0; i < readArguments; i++) {
            if ((extraPositionalArgumentsVariableIndex.isPresent() && extraPositionalArgumentsVariableIndex.get() == i) ||
                    (extraKeywordArgumentsVariableIndex.isPresent() && extraKeywordArgumentsVariableIndex.get() == i)) {
                continue; // These parameters are set in the previous section
            }

            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), argumentLocals[i + offset]);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, descriptorParameterTypes[i + offset].getInternalName());
        }

        for (int i = readArguments; i < descriptorParameterTypes.length - offset; i++) {
            int defaultIndex = i - readArguments;

            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(defaultArgumentHolderClass),
                    PythonDefaultArgumentImplementor.getConstantName(defaultIndex),
                    descriptorParameterTypes[i + offset].getDescriptor());
        }

        if (extraPositionalArgumentsVariableIndex.isPresent()) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeTuple.class),
                    argumentLocals[extraPositionalArgumentsVariableIndex.get()]);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                    descriptorParameterTypes[extraPositionalArgumentsVariableIndex.get()].getInternalName());
        }

        if (extraKeywordArgumentsVariableIndex.isPresent()) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeDict.class),
                    argumentLocals[extraKeywordArgumentsVariableIndex.get()]);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                    descriptorParameterTypes[extraKeywordArgumentsVariableIndex.get()].getInternalName());
        }

        for (int i = 0; i < descriptorParameterTypes.length; i++) {
            localVariableHelper.freeLocal();
        }

        methodDescriptor.callMethod(methodVisitor);
    }

    public void callWithoutKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int argumentCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
        callWithKeywords(functionMetadata, stackMetadata, argumentCount);
    }

    public void callWithKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int argumentCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Type[] descriptorParameterTypes = methodDescriptor.getParameterTypes();

        if (argumentCount < descriptorParameterTypes.length && defaultArgumentHolderClass == null) {
            throw new IllegalStateException("Cannot call " + this + " because there are not enough arguments");
        }

        if (argumentCount > descriptorParameterTypes.length && extraPositionalArgumentsVariableIndex.isEmpty()
                && extraKeywordArgumentsVariableIndex.isEmpty()) {
            throw new IllegalStateException("Cannot call " + this + " because there are too many arguments");
        }

        unwrapBoundMethod(functionMetadata, stackMetadata, argumentCount + 1);

        // TOS is a tuple of keys
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(defaultArgumentHolderClass));
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is defaults (uninitialized), keys

        // Get position of last positional arg (= argumentCount - len(keys) - 1 )
        methodVisitor.visitInsn(Opcodes.DUP); // dup keys
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeTuple.class), "size",
                Type.getMethodDescriptor(Type.INT_TYPE), false);
        methodVisitor.visitLdcInsn(argumentCount);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.ISUB);
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.ISUB);

        // Stack is defaults (uninitialized), keys, positional arguments
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(defaultArgumentHolderClass),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeTuple.class), Type.INT_TYPE),
                false);

        for (int i = 0; i < argumentCount; i++) {
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(defaultArgumentHolderClass),
                    "addArgument", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class)),
                    false);
        }

        for (int i = 0; i < descriptorParameterTypes.length; i++) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(defaultArgumentHolderClass),
                    PythonDefaultArgumentImplementor.getArgumentName(i),
                    descriptorParameterTypes[i].getDescriptor());
            methodVisitor.visitInsn(Opcodes.SWAP);
        }
        methodVisitor.visitInsn(Opcodes.POP);

        methodDescriptor.callMethod(methodVisitor);
    }

    public void callUnpackListAndMap(MethodVisitor methodVisitor) {
        Type[] descriptorParameterTypes = methodDescriptor.getParameterTypes();

        // TOS2 is the function to call, TOS1 is positional arguments, TOS is keyword arguments
        if (methodDescriptor.methodType == MethodDescriptor.MethodType.CLASS) {
            // stack is bound-method, pos, keywords
            StackManipulationImplementor.rotateThree(methodVisitor);
            // stack is keywords, bound-method, pos
            StackManipulationImplementor.swap(methodVisitor);

            // stack is keywords, pos, bound-method
            methodVisitor.visitInsn(Opcodes.DUP_X2);

            // stack is bound-method, keywords, pos, bound-method
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(BoundPythonLikeFunction.class),
                    "getInstance", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class)),
                    false);

            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeType.class));
            // stack is bound-method, keywords, pos, type

            methodVisitor.visitInsn(Opcodes.DUP2);

            // stack is bound-method, keywords, pos, type, pos, type
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "add",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(Object.class)),
                    true);
            // stack is bound-method, keywords, pos, type
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.SWAP);

            // stack is bound-method, pos, keywords
        }

        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(defaultArgumentHolderClass),
                PythonDefaultArgumentImplementor.ARGUMENT_SPEC_STATIC_FIELD_NAME,
                Type.getDescriptor(ArgumentSpec.class));

        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ArgumentSpec.class),
                "extractArgumentList", Type.getMethodDescriptor(Type.getType(List.class),
                        Type.getType(List.class), Type.getType(Map.class)),
                false);

        // Stack is function to call, argument list
        // Unwrap the bound method
        if (methodDescriptor.methodType == MethodDescriptor.MethodType.VIRTUAL ||
                methodDescriptor.methodType == MethodDescriptor.MethodType.INTERFACE) {
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(BoundPythonLikeFunction.class),
                    "getInstance", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class)),
                    false);

            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
            methodVisitor.visitInsn(Opcodes.SWAP);
        }

        // Stack is method, boundedInstance?, default

        // Read the parameters
        for (int i = 0; i < descriptorParameterTypes.length; i++) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                    "get", Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                    true);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, descriptorParameterTypes[i].getInternalName());
            methodVisitor.visitInsn(Opcodes.SWAP);
        }
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is method, boundedInstance?, arg0, arg1, ...

        methodDescriptor.callMethod(methodVisitor);

        // Stack is method, result
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
        return returnType.equals(that.returnType) && Arrays.equals(parameterTypes, that.parameterTypes) &&
                extraPositionalArgumentsVariableIndex.equals(that.extraPositionalArgumentsVariableIndex) &&
                extraKeywordArgumentsVariableIndex.equals(that.extraKeywordArgumentsVariableIndex);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(returnType, extraPositionalArgumentsVariableIndex, extraKeywordArgumentsVariableIndex);
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
