package org.optaplanner.jpyinterpreter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.implementors.CollectionImplementor;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.implementors.StackManipulationImplementor;
import org.optaplanner.jpyinterpreter.types.BoundPythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
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
    final ArgumentSpec<?> argumentSpec;
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
        argumentSpec = computeArgumentSpec();
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, argumentSpec);

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
        argumentSpec = computeArgumentSpec();
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, argumentSpec);
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
        argumentSpec = computeArgumentSpec();
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, argumentSpec);
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
        this.argumentSpec = argumentSpec;
        isFromArgumentSpec = true;
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, argumentSpec);
    }

    private ArgumentSpec<?> computeArgumentSpec() {
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

    public ArgumentSpec<?> getArgumentSpec() {
        return argumentSpec;
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

    public boolean isVirtualMethod() {
        switch (methodDescriptor.methodType) {
            case VIRTUAL:
            case INTERFACE:
            case CONSTRUCTOR:
                return true;
            default:
                return false;

        }
    }

    public boolean isClassMethod() {
        return methodDescriptor.methodType == MethodDescriptor.MethodType.CLASS;
    }

    public boolean isStaticMethod() {
        return methodDescriptor.methodType == MethodDescriptor.MethodType.STATIC;
    }

    public boolean matchesParameters(PythonLikeType... callParameters) {
        if (methodDescriptor.methodType == MethodDescriptor.MethodType.CLASS) {
            List<PythonLikeType> actualCallParameters = new ArrayList<>();
            actualCallParameters.add(BuiltinTypes.TYPE_TYPE);
            actualCallParameters.addAll(List.of(callParameters));
            return argumentSpec.verifyMatchesCallSignature(callParameters.length + 1, List.of(),
                    actualCallParameters);
        } else {
            return argumentSpec.verifyMatchesCallSignature(callParameters.length, List.of(),
                    Arrays.asList(callParameters));
        }
    }

    public boolean matchesParameters(int positionalArgumentCount, List<String> keywordArgumentNameList,
            List<PythonLikeType> callStackTypeList) {
        if (methodDescriptor.methodType == MethodDescriptor.MethodType.CLASS) {
            List<PythonLikeType> actualCallParameters = new ArrayList<>();
            actualCallParameters.add(BuiltinTypes.TYPE_TYPE);
            actualCallParameters.addAll(callStackTypeList);
            return argumentSpec.verifyMatchesCallSignature(positionalArgumentCount + 1, keywordArgumentNameList,
                    actualCallParameters);
        } else {
            return argumentSpec.verifyMatchesCallSignature(positionalArgumentCount, keywordArgumentNameList,
                    callStackTypeList);
        }
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
        if (isClassMethod()) {
            // Class methods will also have their type/instance on the stack, but it not in argumentCount
            argumentCount++;
        }

        int specPositionalArgumentCount = argumentSpec.getAllowPositionalArgumentCount();
        int missingValues = Math.max(0, specPositionalArgumentCount - argumentCount);

        int[] argumentLocals = new int[specPositionalArgumentCount];
        int capturedExtraPositionalArgumentsLocal = localVariableHelper.newLocal();

        // Create temporary variables for each argument
        for (int i = 0; i < argumentLocals.length; i++) {
            argumentLocals[i] = localVariableHelper.newLocal();
        }

        if (argumentSpec.hasExtraPositionalArgumentsCapture()) {
            CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor,
                    Math.max(0, argumentCount - specPositionalArgumentCount));
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeTuple.class),
                    capturedExtraPositionalArgumentsLocal);
        } else if (argumentCount > specPositionalArgumentCount) {
            throw new IllegalStateException("Too many positional arguments given for argument spec " + argumentSpec);
        }

        // Call stack is in reverse, so TOS = argument (specPositionalArgumentCount - missingValues - 1)
        // First store the variables into temporary local variables since we need to typecast them all
        for (int i = specPositionalArgumentCount - missingValues - 1; i >= 0; i--) {
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class),
                    argumentLocals[i]);
        }

        if (isVirtualMethod()) {
            // If it is a virtual method, there will be self here, which we need to cast to the declaring class
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
        }

        if (isClassMethod()) {
            // If it is a class method, argument 0 need to be converted to a type if it not a type
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class),
                    argumentLocals[0]);
            methodVisitor.visitInsn(Opcodes.DUP);
            Label ifIsBoundFunction = new Label();
            Label doneGettingType = new Label();
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(BoundPythonLikeFunction.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, ifIsBoundFunction);
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(PythonLikeType.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, doneGettingType);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                    "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                    true);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, doneGettingType);
            methodVisitor.visitLabel(ifIsBoundFunction);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(BoundPythonLikeFunction.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(BoundPythonLikeFunction.class),
                    "getInstance", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class)),
                    false);
            methodVisitor.visitLabel(doneGettingType);
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), argumentLocals[0]);
        }

        // Now load and typecheck the local variables
        for (int i = 0; i < Math.min(specPositionalArgumentCount, argumentCount); i++) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), argumentLocals[i]);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(argumentSpec.getArgumentType(i)));
        }

        // Load any arguments missing values
        for (int i = specPositionalArgumentCount - missingValues; i < specPositionalArgumentCount; i++) {
            if (argumentSpec.isArgumentNullable(i)) {
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            } else {
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(defaultArgumentHolderClass),
                        PythonDefaultArgumentImplementor.getConstantName(i),
                        Type.getDescriptor(argumentSpec.getArgumentType(i)));
            }
        }

        // Load *vargs and **kwargs if the function has them
        if (argumentSpec.hasExtraPositionalArgumentsCapture()) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeTuple.class),
                    capturedExtraPositionalArgumentsLocal);
        }

        if (argumentSpec.hasExtraKeywordArgumentsCapture()) {
            // No kwargs for call method, so just load an empty map
            CollectionImplementor.buildMap(PythonLikeDict.class, methodVisitor, 0);
        }

        // Call the method
        methodDescriptor.callMethod(methodVisitor);

        // Free temporary locals for arguments
        for (int i = 0; i < argumentLocals.length; i++) {
            localVariableHelper.freeLocal();
        }
        // Free temporary local for vargs
        localVariableHelper.freeLocal();
    }

    public void callPython311andAbove(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int argumentCount,
            List<String> keywordArgumentNameList) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        LocalVariableHelper localVariableHelper = stackMetadata.localVariableHelper;

        int specTotalArgumentCount = argumentSpec.getTotalArgumentCount();
        int positionalArgumentCount = argumentCount - keywordArgumentNameList.size();
        int[] argumentLocals = new int[specTotalArgumentCount];

        // Create temporary variables for each argument
        for (int i = 0; i < argumentLocals.length; i++) {
            argumentLocals[i] = localVariableHelper.newLocal();
        }
        int extraKeywordArgumentsLocal = (argumentSpec.getExtraKeywordsArgumentIndex().isPresent())
                ? argumentLocals[argumentSpec.getExtraKeywordsArgumentIndex().get()]
                : -1;
        int extraPositionalArgumentsLocal = (argumentSpec.getExtraPositionalsArgumentIndex().isPresent())
                ? argumentLocals[argumentSpec.getExtraPositionalsArgumentIndex().get()]
                : -1;

        // Read keyword arguments
        if (extraKeywordArgumentsLocal != -1) {
            CollectionImplementor.buildMap(PythonLikeDict.class, methodVisitor, 0);
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeDict.class),
                    extraKeywordArgumentsLocal);
        }

        // Read positional arguments
        int positionalArgumentStart = (isClassMethod()) ? 1 : 0;

        for (int keywordArgumentNameIndex =
                keywordArgumentNameList.size() - 1; keywordArgumentNameIndex >= 0; keywordArgumentNameIndex--) {
            // Need to iterate keyword name tuple in reverse (since last element of the tuple correspond to TOS)
            String keywordArgument = keywordArgumentNameList.get(keywordArgumentNameIndex);
            int argumentIndex = argumentSpec.getArgumentIndex(keywordArgument);
            if (argumentIndex == -1) {
                // Unknown keyword argument; put it into the extraKeywordArguments dict
                localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeDict.class),
                        extraKeywordArgumentsLocal);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeDict.class));
                methodVisitor.visitInsn(Opcodes.SWAP);
                methodVisitor.visitLdcInsn(keywordArgument);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonString.class),
                        "valueOf", Type.getMethodDescriptor(Type.getType(PythonString.class),
                                Type.getType(String.class)),
                        false);
                methodVisitor.visitInsn(Opcodes.SWAP);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeDict.class),
                        "put", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                Type.getType(PythonLikeObject.class),
                                Type.getType(PythonLikeObject.class)),
                        false);
            } else {
                localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class),
                        argumentLocals[argumentIndex]);
            }
        }

        if (extraPositionalArgumentsLocal != -1) {
            CollectionImplementor.buildCollection(PythonLikeTuple.class,
                    methodVisitor,
                    Math.max(0, positionalArgumentCount - argumentSpec.getAllowPositionalArgumentCount()
                            + positionalArgumentStart));
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeTuple.class),
                    extraPositionalArgumentsLocal);
        }

        for (int i = Math.min(positionalArgumentCount + positionalArgumentStart, argumentSpec.getAllowPositionalArgumentCount())
                - 1; i >= positionalArgumentStart; i--) {
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class),
                    argumentLocals[i]);
        }

        // Load missing arguments with default values
        int defaultOffset = argumentSpec.getTotalArgumentCount() - defaultArgumentList.size();
        for (int argumentIndex : argumentSpec.getUnspecifiedArgumentSet(positionalArgumentCount + positionalArgumentStart,
                keywordArgumentNameList)) {
            if (argumentSpec.isArgumentNullable(argumentIndex)) {
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            } else {
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(defaultArgumentHolderClass),
                        PythonDefaultArgumentImplementor.getConstantName(argumentIndex - defaultOffset),
                        Type.getDescriptor(argumentSpec.getArgumentType(argumentIndex)));
            }
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class),
                    argumentLocals[argumentIndex]);
        }

        if (isVirtualMethod()) {
            // If it is a virtual method, there will be self here, which we need to cast to the declaring class
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
        }

        if (isClassMethod()) {
            // If it is a class method, argument 0 need to be converted to a type if it not a type
            methodVisitor.visitInsn(Opcodes.DUP);
            Label ifIsBoundFunction = new Label();
            Label doneGettingType = new Label();
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(BoundPythonLikeFunction.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, ifIsBoundFunction);
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(PythonLikeType.class));
            methodVisitor.visitJumpInsn(Opcodes.IFNE, doneGettingType);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                    "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                    true);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, doneGettingType);
            methodVisitor.visitLabel(ifIsBoundFunction);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(BoundPythonLikeFunction.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(BoundPythonLikeFunction.class),
                    "getInstance", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class)),
                    false);
            methodVisitor.visitLabel(doneGettingType);
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), argumentLocals[0]);
        }

        // Load arguments in proper order and typecast them
        for (int i = 0; i < specTotalArgumentCount; i++) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), argumentLocals[i]);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(argumentSpec.getArgumentType(i)));
        }

        methodDescriptor.callMethod(methodVisitor);

        // If it not a CLASS method, pop off the function object
        // CLASS method consume the function object; Static and Virtual do not
        if (!isClassMethod()) {
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitInsn(Opcodes.POP);
        }

        // Pop off NULL if it on the stack
        if (stackMetadata.getTypeAtStackIndex(argumentCount + 1) == BuiltinTypes.NULL_TYPE) {
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitInsn(Opcodes.POP);
        }

        // Free temporary locals for arguments
        for (int i = 0; i < argumentLocals.length; i++) {
            localVariableHelper.freeLocal();
        }
    }

    public void callWithoutKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int argumentCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
        callWithKeywordsAndUnwrapSelf(functionMetadata, stackMetadata, argumentCount);
    }

    public void callWithKeywordsAndUnwrapSelf(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int argumentCount) {
        callWithKeywords(functionMetadata, stackMetadata, argumentCount, true);
    }

    public void callWithKeywordsNoUnwrap(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int argumentCount) {
        callWithKeywords(functionMetadata, stackMetadata, argumentCount, false);
    }

    private void callWithKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int argumentCount, boolean unwrapSelf) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        Type[] descriptorParameterTypes = methodDescriptor.getParameterTypes();

        if (argumentCount < descriptorParameterTypes.length && defaultArgumentHolderClass == null) {
            throw new IllegalStateException("Cannot call " + this + " because there are not enough arguments");
        }

        if (argumentCount > descriptorParameterTypes.length && extraPositionalArgumentsVariableIndex.isEmpty()
                && extraKeywordArgumentsVariableIndex.isEmpty()) {
            throw new IllegalStateException("Cannot call " + this + " because there are too many arguments");
        }

        if (unwrapSelf) {
            unwrapBoundMethod(functionMetadata, stackMetadata, argumentCount + 1);
        }

        if (!unwrapSelf && isClassMethod()) {
            argumentCount++;
        }

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
            if (!unwrapSelf && isClassMethod() && i == argumentCount - 1) {
                methodVisitor.visitInsn(Opcodes.DUP);
                Label ifIsBoundFunction = new Label();
                Label doneGettingType = new Label();
                methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(BoundPythonLikeFunction.class));
                methodVisitor.visitJumpInsn(Opcodes.IFNE, ifIsBoundFunction);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(PythonLikeType.class));
                methodVisitor.visitJumpInsn(Opcodes.IFNE, doneGettingType);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                        "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                        true);
                methodVisitor.visitJumpInsn(Opcodes.GOTO, doneGettingType);
                methodVisitor.visitLabel(ifIsBoundFunction);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(BoundPythonLikeFunction.class));
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(BoundPythonLikeFunction.class),
                        "getInstance", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class)),
                        false);
                methodVisitor.visitLabel(doneGettingType);
            }
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
