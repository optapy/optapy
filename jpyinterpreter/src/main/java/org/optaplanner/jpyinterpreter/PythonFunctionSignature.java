package org.optaplanner.jpyinterpreter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.util.arguments.ArgumentSpec;

public class PythonFunctionSignature {
    private final PythonLikeType returnType;
    private final PythonLikeType[] parameterTypes;

    private final MethodDescriptor methodDescriptor;

    private final List<PythonLikeObject> defaultArgumentList;
    private final Map<String, Integer> keywordToArgumentIndexMap;

    private final Optional<Integer> extraPositionalArgumentsVariableIndex;
    private final Optional<Integer> extraKeywordArgumentsVariableIndex;

    private final Class<?> defaultArgumentHolderClass;
    private final ArgumentSpec<?> argumentSpec;
    private final boolean isFromArgumentSpec;

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
                defaultArgumentList, keywordToArgumentIndexMap, getExtraPositionalArgumentsVariableIndex(),
                getExtraKeywordArgumentsVariableIndex(), getArgumentSpec());

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
                defaultArgumentList, keywordToArgumentIndexMap, getExtraPositionalArgumentsVariableIndex(),
                getExtraKeywordArgumentsVariableIndex(), getArgumentSpec());
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
        this.argumentSpec = argumentSpec;
        isFromArgumentSpec = true;
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex, argumentSpec);
    }

    private ArgumentSpec<?> computeArgumentSpec() {
        try {
            ArgumentSpec<?> argumentSpec = ArgumentSpec.forFunctionReturning(getMethodDescriptor().getMethodName(),
                    (Class<? extends PythonLikeObject>) getReturnType().getJavaClass());
            for (int i = 0; i < getParameterTypes().length - getDefaultArgumentList().size(); i++) {
                if (getExtraPositionalArgumentsVariableIndex().isPresent()
                        && getExtraPositionalArgumentsVariableIndex().get() == i) {
                    continue;
                }

                if (getExtraKeywordArgumentsVariableIndex().isPresent() && getExtraKeywordArgumentsVariableIndex().get() == i) {
                    continue;
                }

                final int argIndex = i;
                Optional<String> argumentName = getKeywordToArgumentIndexMap().entrySet()
                        .stream().filter(e -> e.getValue().equals(argIndex))
                        .map(Map.Entry::getKey)
                        .findAny();

                if (argumentName.isEmpty()) {
                    argumentSpec = argumentSpec.addArgument("$arg" + i,
                            (Class<? extends PythonLikeObject>) getParameterTypes()[i].getJavaClass());
                } else {
                    argumentSpec = argumentSpec.addArgument(argumentName.get(),
                            (Class<? extends PythonLikeObject>) getParameterTypes()[i].getJavaClass());
                }
            }

            for (int i = getParameterTypes().length - getDefaultArgumentList().size(); i < getParameterTypes().length; i++) {
                if (getExtraPositionalArgumentsVariableIndex().isPresent()
                        && getExtraPositionalArgumentsVariableIndex().get() == i) {
                    continue;
                }

                if (getExtraKeywordArgumentsVariableIndex().isPresent() && getExtraKeywordArgumentsVariableIndex().get() == i) {
                    continue;
                }

                PythonLikeObject defaultValue =
                        getDefaultArgumentList().get(getDefaultArgumentList().size() - (getParameterTypes().length - i));

                final int argIndex = i;
                Optional<String> argumentName = getKeywordToArgumentIndexMap().entrySet()
                        .stream().filter(e -> e.getValue().equals(argIndex))
                        .map(Map.Entry::getKey)
                        .findAny();

                if (argumentName.isEmpty()) {
                    argumentSpec = argumentSpec.addArgument("$arg" + i,
                            (Class<PythonLikeObject>) getParameterTypes()[i].getJavaClass(),
                            defaultValue);
                } else {
                    argumentSpec = argumentSpec.addArgument(argumentName.get(),
                            (Class<PythonLikeObject>) getParameterTypes()[i].getJavaClass(),
                            defaultValue);
                }
            }

            if (getExtraPositionalArgumentsVariableIndex().isPresent()) {
                argumentSpec = argumentSpec.addExtraPositionalVarArgument("*vargs");
            }

            if (getExtraKeywordArgumentsVariableIndex().isPresent()) {
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

    public List<PythonLikeObject> getDefaultArgumentList() {
        return defaultArgumentList;
    }

    public Map<String, Integer> getKeywordToArgumentIndexMap() {
        return keywordToArgumentIndexMap;
    }

    public Optional<Integer> getExtraPositionalArgumentsVariableIndex() {
        return extraPositionalArgumentsVariableIndex;
    }

    public Optional<Integer> getExtraKeywordArgumentsVariableIndex() {
        return extraKeywordArgumentsVariableIndex;
    }

    public Class<?> getDefaultArgumentHolderClass() {
        return defaultArgumentHolderClass;
    }

    public boolean isVirtualMethod() {
        switch (getMethodDescriptor().getMethodType()) {
            case VIRTUAL:
            case INTERFACE:
            case CONSTRUCTOR:
                return true;
            default:
                return false;

        }
    }

    public boolean isClassMethod() {
        return getMethodDescriptor().getMethodType() == MethodDescriptor.MethodType.CLASS;
    }

    public boolean isStaticMethod() {
        return getMethodDescriptor().getMethodType() == MethodDescriptor.MethodType.STATIC;
    }

    private int getPositionalParameterCount(int originalPositionalParameterCount) {
        if (getMethodDescriptor().getMethodType() == MethodDescriptor.MethodType.CLASS) {
            return originalPositionalParameterCount + 1;
        } else {
            return originalPositionalParameterCount;
        }
    }

    private List<PythonLikeType> getCallParameterList(List<PythonLikeType> callStackTypeList) {
        if (getMethodDescriptor().getMethodType() == MethodDescriptor.MethodType.CLASS) {
            List<PythonLikeType> actualCallParameters = new ArrayList<>();
            actualCallParameters.add(BuiltinTypes.TYPE_TYPE);
            actualCallParameters.addAll(callStackTypeList);
            return actualCallParameters;
        } else {
            return callStackTypeList;
        }
    }

    public boolean matchesParameters(PythonLikeType... callParameters) {
        return getArgumentSpec().verifyMatchesCallSignature(getPositionalParameterCount(callParameters.length),
                Collections.emptyList(),
                getCallParameterList(List.of(callParameters)));
    }

    public boolean matchesParameters(int positionalArgumentCount, List<String> keywordArgumentNameList,
            List<PythonLikeType> callStackTypeList) {

        return getArgumentSpec().verifyMatchesCallSignature(getPositionalParameterCount(positionalArgumentCount),
                keywordArgumentNameList,
                getCallParameterList(callStackTypeList));
    }

    public boolean moreSpecificThan(PythonFunctionSignature other) {
        if (other.getParameterTypes().length < getParameterTypes().length &&
                (other.getExtraPositionalArgumentsVariableIndex().isPresent() ||
                        other.getExtraKeywordArgumentsVariableIndex().isPresent())) {
            return true;
        }

        if (other.getParameterTypes().length > getParameterTypes().length &&
                (getExtraPositionalArgumentsVariableIndex().isPresent() ||
                        getExtraKeywordArgumentsVariableIndex().isPresent())) {
            return false;
        }

        if (other.getParameterTypes().length != getParameterTypes().length) {
            return false;
        }

        for (int i = 0; i < getParameterTypes().length; i++) {
            PythonLikeType overloadParameterType = getParameterTypes()[i];
            PythonLikeType otherParameterType = other.getParameterTypes()[i];

            if (otherParameterType.equals(overloadParameterType)) {
                continue;
            }

            if (otherParameterType.isSubclassOf(overloadParameterType)) {
                return false;
            }
        }
        return true;
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
        return getReturnType().equals(that.getReturnType()) && Arrays.equals(getParameterTypes(), that.getParameterTypes()) &&
                getExtraPositionalArgumentsVariableIndex().equals(that.getExtraPositionalArgumentsVariableIndex()) &&
                getExtraKeywordArgumentsVariableIndex().equals(that.getExtraKeywordArgumentsVariableIndex());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getReturnType(), getExtraPositionalArgumentsVariableIndex(),
                getExtraKeywordArgumentsVariableIndex());
        result = 31 * result + Arrays.hashCode(getParameterTypes());
        return result;
    }

    @Override
    public String toString() {
        return getMethodDescriptor().getMethodName() +
                Arrays.stream(getParameterTypes()).map(PythonLikeType::toString).collect(Collectors.joining(", ", "(", ") -> "))
                +
                getReturnType();
    }
}
