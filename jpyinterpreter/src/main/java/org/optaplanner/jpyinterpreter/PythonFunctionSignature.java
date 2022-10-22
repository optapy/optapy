package org.optaplanner.jpyinterpreter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.implementors.CollectionImplementor;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.implementors.StackManipulationImplementor;
import org.optaplanner.jpyinterpreter.types.BoundPythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonIterator;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;

public class PythonFunctionSignature {
    final PythonLikeType returnType;
    final PythonLikeType[] parameterTypes;

    final MethodDescriptor methodDescriptor;

    final List<PythonLikeObject> defaultArgumentList;
    final Map<String, Integer> keywordToArgumentIndexMap;

    final Optional<Integer> extraPositionalArgumentsVariableIndex;
    final Optional<Integer> extraKeywordArgumentsVariableIndex;

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
        this.extraPositionalArgumentsVariableIndex = Optional.empty();
        this.extraKeywordArgumentsVariableIndex = Optional.empty();
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex);

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
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex);
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
        defaultArgumentHolderClass = PythonDefaultArgumentImplementor.createDefaultArgumentFor(methodDescriptor,
                defaultArgumentList, keywordToArgumentIndexMap, extraPositionalArgumentsVariableIndex,
                extraKeywordArgumentsVariableIndex);
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
        if (callParameters.length < minParameters || (extraPositionalArgumentsVariableIndex.isEmpty() &&
                callParameters.length > maxParameters)) {
            return false;
        }

        for (int i = startIndex; i < parameterTypes.length; i++) {
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
        if (argumentCount < parameterTypes.length && defaultArgumentHolderClass == null) {
            throw new IllegalStateException("Cannot call " + this + " because there are not enough arguments");
        }

        if (argumentCount > parameterTypes.length && extraPositionalArgumentsVariableIndex.isEmpty()) {
            throw new IllegalStateException("Cannot call " + this + " because there are too many arguments");
        }

        int[] argumentLocals = new int[parameterTypes.length];

        boolean isVirtual = methodDescriptor.getMethodType() != MethodDescriptor.MethodType.STATIC
                || methodDescriptor.getMethodType() != MethodDescriptor.MethodType.STATIC_AS_VIRTUAL;

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

        if (argumentCount > parameterTypes.length) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeTuple.class),
                    argumentLocals[extraPositionalArgumentsVariableIndex.get()]);
            for (int i = argumentCount; i >= parameterTypes.length; i--) {
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

        for (int i = 0; i < parameterTypes.length; i++) {
            if ((extraPositionalArgumentsVariableIndex.isPresent() && extraPositionalArgumentsVariableIndex.get() == i) ||
                    (extraKeywordArgumentsVariableIndex.isPresent() && extraKeywordArgumentsVariableIndex.get() == i)) {
                continue; // These parameters are set in the previous section
            }
            argumentLocals[parameterTypes.length - i - 1] = localVariableHelper.newLocal();
            localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class),
                    argumentLocals[parameterTypes.length - i - 1]);
        }

        if (isVirtual) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), argumentLocals[i]);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterTypes[i].getJavaTypeInternalName());
        }

        for (int i = 0; i < parameterTypes.length; i++) {
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

    public void callWithoutKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int argumentCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
        callWithKeywords(functionMetadata, stackMetadata, argumentCount);
    }

    public void callWithKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int argumentCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        if (argumentCount < parameterTypes.length && defaultArgumentHolderClass == null) {
            throw new IllegalStateException("Cannot call " + this + " because there are not enough arguments");
        }

        if (argumentCount > parameterTypes.length && extraPositionalArgumentsVariableIndex.isEmpty()
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

    public void callUnpackListAndMap(MethodVisitor methodVisitor) {
        // TOS2 is the function to call, TOS1 is positional arguments, TOS is keyword arguments
        // Create key tuple from TOS key set
        methodVisitor.visitInsn(Opcodes.DUP);
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                "keySet", Type.getMethodDescriptor(Type.getType(Set.class)),
                true);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Iterable.class),
                "iterator", Type.getMethodDescriptor(Type.getType(Iterator.class)),
                true);

        Label noMoreKeys = new Label();
        Label keyLoopStart = new Label();

        methodVisitor.visitLabel(keyLoopStart);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Iterator.class),
                "hasNext", Type.getMethodDescriptor(Type.BOOLEAN_TYPE), true);
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, noMoreKeys);

        // Get the key
        methodVisitor.visitInsn(Opcodes.DUP2); // Stack is ..., keyTuple, iterator, so dup 2 to dupe the keyTuple
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Iterator.class),
                "next", Type.getMethodDescriptor(Type.getType(Object.class)), true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeTuple.class),
                "add", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(PythonLikeObject.class)),
                false);
        methodVisitor.visitInsn(Opcodes.POP); // Pop return value
        methodVisitor.visitJumpInsn(Opcodes.GOTO, keyLoopStart);

        methodVisitor.visitLabel(noMoreKeys);
        methodVisitor.visitInsn(Opcodes.POP2); // Pop off the now empty iterator

        // Stack is method, positionalArgumentList, keywordArguments, keyTuple
        methodVisitor.visitInsn(Opcodes.DUP2_X1);
        methodVisitor.visitInsn(Opcodes.POP2);

        // Stack is now method, keywordArguments, keyTuple, positionalArgumentList
        methodVisitor.visitInsn(Opcodes.DUP_X2);

        // Stack is now method, positionalArgumentList, keywordArguments, keyTuple, positionalArgumentList
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                "size", Type.getMethodDescriptor(Type.INT_TYPE),
                true);

        // Stack is now method, positionalArgumentList, keywordArguments, keyTuple, numberOfPositionalArguments
        // Duplicate key tuple so we can correctly iterate keyword arguments
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now method, positionalArgumentList, keywordArguments, keyTuple, keyTuple, numberOfPositionalArguments

        // Create the default argument holder from the key tuple and number of positional arguments
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(defaultArgumentHolderClass));
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(defaultArgumentHolderClass),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeTuple.class), Type.INT_TYPE),
                false);

        // Stack is now method, positionalArgumentList, keywordArguments, keyTuple, defaults
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is now method, positionalArgumentList, defaults, keywordArguments, keyTuple
        // need to iterate tuple in reverse
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeTuple.class),
                "getReversedIterator", Type.getMethodDescriptor(Type.getType(PythonIterator.class)),
                false);

        // Stack is now method, positionalArgumentList, defaults, keywordArguments, iterator
        Label noMoreKeywordArguments = new Label();
        Label keywordArgumentsLoopStart = new Label();

        methodVisitor.visitLabel(keywordArgumentsLoopStart);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonIterator.class),
                "hasNext", Type.getMethodDescriptor(Type.BOOLEAN_TYPE),
                false);
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, noMoreKeywordArguments);

        // Stack is now method, positionalArgumentList, defaults, keywordArguments, iterator
        methodVisitor.visitInsn(Opcodes.DUP_X2);

        // Stack is now method, positionalArgumentList, iterator, defaults, keywordArguments, iterator
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonIterator.class),
                "nextPythonItem", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class)),
                false);

        // Stack is now method, positionalArgumentList, iterator, defaults, keywordArguments, key
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now method, positionalArgumentList, iterator, defaults, keywordArguments, keywordArguments, key
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                "get", Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)),
                true);

        // Stack is now method, positionalArgumentList, iterator, defaults, keywordArguments, value
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is now method, positionalArgumentList, iterator, keywordArguments, defaults, value
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now method, positionalArgumentList, iterator, keywordArguments, defaults, defaults, value
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(defaultArgumentHolderClass),
                "addArgument", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class)),
                false);

        // Stack is now method, positionalArgumentList, iterator, keywordArguments, defaults
        StackManipulationImplementor.rotateThree(methodVisitor);

        // Stack is now method, positionalArgumentList, defaults, iterator, keywordArguments
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now method, positionalArgumentList, defaults, keywordArguments, iterator
        methodVisitor.visitJumpInsn(Opcodes.GOTO, keywordArgumentsLoopStart);

        methodVisitor.visitLabel(noMoreKeywordArguments);

        methodVisitor.visitInsn(Opcodes.POP2); // Pop off the empty iterator and used keyword arguments

        // Stack is now method, positionalArgumentList, defaults
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now method, defaults, positionalArgumentList

        // Get the number of positional arguments
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "size",
                Type.getMethodDescriptor(Type.INT_TYPE), true);

        // Stack is now method, defaults, positionalArgumentList, size
        Label noMorePositionalArguments = new Label();
        Label positionalArgumentsLoopStart = new Label();

        // We are iterating the list in reverse, since defaults expect the arguments in reverse
        methodVisitor.visitLabel(positionalArgumentsLoopStart);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, noMorePositionalArguments); // if the current index is == 0, then end the loop

        // Subtract 1 from last index to get current index
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.ISUB);

        // Stack is now method, defaults, positionalArgumentList, index
        methodVisitor.visitInsn(Opcodes.DUP2_X1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));

        // Stack is now method, positionalArgumentList, index, default, item
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now method, positionalArgumentList, index, default, default, item
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(defaultArgumentHolderClass),
                "addArgument", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class)),
                false);

        // Stack is now method, positionalArgumentList, index, default
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is now method, default, positionalArgumentList, index
        methodVisitor.visitJumpInsn(Opcodes.GOTO, positionalArgumentsLoopStart);

        methodVisitor.visitLabel(noMorePositionalArguments);

        // Pop off the index and positional argument list
        methodVisitor.visitInsn(Opcodes.POP2);

        // Stack is method, default

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
        for (int i = 0; i < parameterTypes.length; i++) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(defaultArgumentHolderClass),
                    PythonDefaultArgumentImplementor.getArgumentName(i),
                    parameterTypes[i].getJavaTypeDescriptor());
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
