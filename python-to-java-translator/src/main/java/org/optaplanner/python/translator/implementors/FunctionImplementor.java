package org.optaplanner.python.translator.implementors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.LocalVariableHelper;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.types.PythonCode;
import org.optaplanner.python.translator.types.PythonKnownFunctionType;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeGenericType;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.collections.PythonLikeDict;
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;

/**
 * Implements opcodes related to functions
 */
public class FunctionImplementor {

    /**
     * Loads a method named co_names[namei] from the TOS object. TOS is popped. This bytecode distinguishes two cases:
     * if TOS has a method with the correct name, the bytecode pushes the unbound method and TOS.
     * TOS will be used as the first argument (self) by CALL_METHOD when calling the unbound method.
     * Otherwise, NULL and the object return by the attribute lookup are pushed.
     */
    public static void loadMethod(FunctionMetadata functionMetadata, MethodVisitor methodVisitor, String className,
            PythonCompiledFunction function,
            StackMetadata stackMetadata, PythonBytecodeInstruction instruction) {
        PythonLikeType stackTosType = stackMetadata.getTOSType();
        PythonLikeType tosType;
        boolean isTosType;
        if (stackTosType instanceof PythonLikeGenericType) {
            tosType = ((PythonLikeGenericType) stackTosType).getOrigin();
            isTosType = true;
        } else {
            tosType = stackTosType;
            isTosType = false;
        }
        tosType.getMethodType(functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg)).ifPresentOrElse(
                knownFunctionType -> {
                    if (isTosType && knownFunctionType.isStatic()) {
                        methodVisitor.visitLdcInsn(function.co_names.get(instruction.arg));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "__getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                    } else if (!isTosType && knownFunctionType.isStatic()) {
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                true);
                        methodVisitor.visitLdcInsn(function.co_names.get(instruction.arg));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "__getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                    } else if (isTosType) {
                        methodVisitor.visitInsn(Opcodes.DUP);
                        methodVisitor.visitLdcInsn(function.co_names.get(instruction.arg));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "__getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                    } else {
                        methodVisitor.visitInsn(Opcodes.DUP);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                true);
                        methodVisitor.visitLdcInsn(function.co_names.get(instruction.arg));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "__getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                    }
                },
                () -> loadGenericMethod(functionMetadata, methodVisitor, className, function, stackMetadata, instruction));
    }

    /**
     * Loads a method named co_names[namei] from the TOS object. TOS is popped. This bytecode distinguishes two cases:
     * if TOS has a method with the correct name, the bytecode pushes the unbound method and TOS.
     * TOS will be used as the first argument (self) by CALL_METHOD when calling the unbound method.
     * Otherwise, NULL and the object return by the attribute lookup are pushed.
     */
    private static void loadGenericMethod(FunctionMetadata functionMetadata, MethodVisitor methodVisitor, String className,
            PythonCompiledFunction function,
            StackMetadata stackMetadata, PythonBytecodeInstruction instruction) {

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(function.co_names.get(instruction.arg));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        Label blockEnd = new Label();

        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, blockEnd);

        // TOS is null; type does not have attribute; do normal attribute lookup
        // Stack is object, null
        methodVisitor.visitInsn(Opcodes.POP);
        ObjectImplementor.getAttribute(functionMetadata, methodVisitor, className, stackMetadata, instruction);

        // Stack is method
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitInsn(Opcodes.SWAP);

        methodVisitor.visitLabel(blockEnd);

        // Stack is either:
        // object, method if it was in type
        // null, method if it was not in type
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now:
        // method, object if it was in type
        // method, null if it was not in type
    }

    /**
     * Calls a method. argc is the number of positional arguments. Keyword arguments are not supported.
     * This opcode is designed to be used with LOAD_METHOD. Positional arguments are on top of the stack.
     * Below them, the two items described in LOAD_METHOD are on the stack
     * (either self and an unbound method object or NULL and an arbitrary callable).
     * All of them are popped and the return value is pushed.
     */
    public static void callMethod(MethodVisitor methodVisitor, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg + 1);
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            PythonLikeType[] parameterTypes =
                    new PythonLikeType[knownFunctionType.isStatic() ? instruction.arg : instruction.arg + 1];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameterTypes[parameterTypes.length - i - 1] = stackMetadata.getTypeAtStackIndex(i);
            }
            knownFunctionType.getFunctionForParameters(parameterTypes)
                    .ifPresentOrElse(functionSignature -> {
                        functionSignature.callMethod(methodVisitor, localVariableHelper, instruction.arg);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitInsn(Opcodes.POP);
                        if (knownFunctionType.isStatic()) {
                            methodVisitor.visitInsn(Opcodes.SWAP);
                            methodVisitor.visitInsn(Opcodes.POP);
                        }
                    }, () -> callGenericMethod(methodVisitor, instruction, localVariableHelper));
        } else {
            callGenericMethod(methodVisitor, instruction, localVariableHelper);
        }
    }

    private static void callGenericMethod(MethodVisitor methodVisitor,
            PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        // Stack is method, (obj or null), arg0, ..., arg(argc - 1)
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, instruction.arg);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is method, argList, (obj or null)
        Label ifNullStart = new Label();
        Label blockEnd = new Label();

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, ifNullStart);

        // Stack is method, argList, obj
        StackManipulationImplementor.duplicateToTOS(methodVisitor, localVariableHelper, 1);
        StackManipulationImplementor.swap(methodVisitor);

        // Stack is method, argList, argList, obj
        methodVisitor.visitInsn(Opcodes.ICONST_0);

        // Stack is method, argList, argList, obj, index
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is method, argList, argList, index, obj
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                "add",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(Object.class)),
                true);

        // Stack is method, argList
        methodVisitor.visitJumpInsn(Opcodes.GOTO, blockEnd);

        methodVisitor.visitLabel(ifNullStart);
        // Stack is method, argList, null
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is method, argList
        methodVisitor.visitLabel(blockEnd);

        // Stack is method, argList
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        // Stack is callable, argument_list, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                true);
    }

    /**
     * Calls a function. TOS...TOS[argc - 1] are the arguments to the function.
     * TOS[argc] is the function to call. TOS...TOS[argc] are all popped and
     * the result is pushed onto the stack.
     */
    public static void callFunction(MethodVisitor methodVisitor, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg);
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            knownFunctionType.getDefaultFunctionSignature()
                    .ifPresentOrElse(functionSignature -> {
                        functionSignature.callWithoutKeywords(methodVisitor, stackMetadata.localVariableHelper,
                                instruction.arg);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitInsn(Opcodes.POP);
                    }, () -> callGenericFunction(methodVisitor, instruction));
        } else {
            callGenericFunction(methodVisitor, instruction);
        }
    }

    public static void callGenericFunction(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        callGenericFunction(methodVisitor, instruction.arg);
    }

    public static void callGenericFunction(MethodVisitor methodVisitor, int argCount) {
        // stack is callable, arg0, arg1, ..., arg(argc - 1)
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, argCount);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        // Stack is callable, argument_list, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                true);
    }

    /**
     * Calls a function. TOS is a tuple containing keyword names.
     * TOS[1]...TOS[len(TOS)] are the keyword arguments to the function (TOS[1] is (TOS)[0], TOS[2] is (TOS)[1], ...).
     * TOS[len(TOS) + 1]...TOS[argc + 1] are the positional arguments (rightmost first).
     * TOS[argc + 2] is the function to call. TOS...TOS[argc + 2] are all popped and
     * the result is pushed onto the stack.
     */
    public static void callFunctionWithKeywords(MethodVisitor methodVisitor, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg + 1);
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            knownFunctionType.getDefaultFunctionSignature()
                    .ifPresentOrElse(functionSignature -> {
                        functionSignature.callWithKeywords(methodVisitor, stackMetadata.localVariableHelper,
                                instruction.arg);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitInsn(Opcodes.POP);
                    }, () -> callGenericFunction(methodVisitor, instruction));
        } else {
            callGenericFunctionWithKeywords(methodVisitor, instruction);
        }
    }

    /**
     * Calls a function. TOS is a tuple containing keyword names.
     * TOS[1]...TOS[len(TOS)] are the keyword arguments to the function (TOS[1] is (TOS)[0], TOS[2] is (TOS)[1], ...).
     * TOS[len(TOS) + 1]...TOS[argc + 1] are the positional arguments (rightmost first).
     * TOS[argc + 2] is the function to call. TOS...TOS[argc + 2] are all popped and
     * the result is pushed onto the stack.
     */
    public static void callGenericFunctionWithKeywords(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        // stack is callable, arg0, arg1, ..., arg(argc - len(keys)), ..., arg(argc - 1), keys
        // We know the total number of arguments, but not the number of individual positional/keyword arguments
        // Since Java Bytecode require consistent stack frames  (i.e. the body of a loop must start with
        // the same number of elements in the stack), we need to add the tuple/map in the same object
        // which will delegate it to either the tuple or the map depending on position and the first item size
        CollectionImplementor.buildCollection(TupleMapPair.class, methodVisitor, instruction.arg + 1);

        // stack is callable, tupleMapPair
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TupleMapPair.class), "tuple",
                Type.getDescriptor(PythonLikeTuple.class));

        // stack is callable, tupleMapPair, positionalArgs
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TupleMapPair.class), "map",
                Type.getDescriptor(PythonLikeDict.class));

        // Stack is callable, positionalArgs, keywordArgs
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                true);
    }

    /**
     * Calls a function. If the lowest bit of instruction.arg is set, TOS is a mapping object containing keyword
     * arguments, TOS[1] is an iterable containing positional arguments and TOS[2] is callable. Otherwise,
     * TOS is an iterable containing positional arguments and TOS[1] is callable.
     */
    public static void callFunctionUnpack(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        if ((instruction.arg & 1) == 1) {
            callFunctionUnpackMapAndIterable(methodVisitor);
        } else {
            callFunctionUnpackIterable(methodVisitor);
        }
    }

    public static void callFunctionUnpackMapAndIterable(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                true);
    }

    public static void callFunctionUnpackIterable(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "__call__", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                true);
    }

    /**
     * Creates a function. The stack depends on {@code instruction.arg}:
     *
     * - If (arg & 1) == 1, a tuple of default values for positional-only and positional-or-keyword parameters in positional
     * order
     * - If (arg & 2) == 2, a dictionary of keyword-only parameters’ default values
     * - If (arg & 4) == 4, an annotation dictionary
     * - If (arg & 8) == 8, a tuple containing cells for free variables
     *
     * The stack will contain the following items, in the given order:
     *
     * TOP
     * [Mandatory] Function Name
     * [Mandatory] Class of the PythonLikeFunction to create
     * [Optional, flag = 0x8] A tuple containing the cells for free variables
     * [Optional, flag = 0x4] A tuple containing key,value pairs for the annotation directory
     * [Optional, flag = 0x2] A dictionary of keyword-only parameters’ default values
     * [Optional, flag = 0x1] A tuple of default values for positional-only and positional-or-keyword parameters in positional
     * order
     * BOTTOM
     *
     * All arguments are popped. A new instance of Class is created with the arguments and pushed to the stack.
     */
    public static void createFunction(MethodVisitor methodVisitor, String className, PythonBytecodeInstruction instruction,
            LocalVariableHelper localVariableHelper) {
        int providedOptionalArgs = Integer.bitCount(instruction.arg);

        // If the argument present, decrement providedOptionalArgs to keep argument shifting logic the same
        // Ex: present, missing, present, present -> need to shift default for missing down by 4 = 2 + (3 - 1)
        // Ex: present, present, missing, present -> need to shift default for missing down by 3 = 2 + (3 - 2)
        // Ex: present, missing1, missing2, present -> need to shift default for missing1 down by 3 = 2 + (2 - 1),
        //                                             need to shift default for missing2 down by 3 = 2 + (2 - 1)
        if ((instruction.arg & 1) != 1) {
            CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
            StackManipulationImplementor.shiftTOSDownBy(methodVisitor, localVariableHelper, 2 + providedOptionalArgs);
        } else {
            providedOptionalArgs--;
        }

        if ((instruction.arg & 2) != 2) {
            CollectionImplementor.buildMap(PythonLikeDict.class, methodVisitor, 0);
            StackManipulationImplementor.shiftTOSDownBy(methodVisitor, localVariableHelper, 2 + providedOptionalArgs);
        } else {
            providedOptionalArgs--;
        }

        if ((instruction.arg & 4) != 4) {
            CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
            StackManipulationImplementor.shiftTOSDownBy(methodVisitor, localVariableHelper, 2 + providedOptionalArgs);
        } else {
            providedOptionalArgs--;
        }

        if ((instruction.arg & 8) != 8) {
            CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
            StackManipulationImplementor.shiftTOSDownBy(methodVisitor, localVariableHelper, 2 + providedOptionalArgs);
        }

        // Stack is now:
        // default positional args, default keyword args, annotation directory tuple, cell tuple, function class, function name

        // Do type casts for name string and code object
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonCode.class));

        // Pass the current function's interpreter to the new function instance
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className,
                PythonBytecodeToJavaBytecodeTranslator.INTERPRETER_INSTANCE_FIELD_NAME,
                Type.getDescriptor(PythonInterpreter.class));

        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(FunctionImplementor.class),
                "createInstance", Type.getMethodDescriptor(Type.getType(PythonLikeFunction.class),
                        Type.getType(PythonLikeTuple.class),
                        Type.getType(PythonLikeDict.class),
                        Type.getType(PythonLikeTuple.class),
                        Type.getType(PythonLikeTuple.class),
                        Type.getType(PythonString.class),
                        Type.getType(PythonCode.class),
                        Type.getType(PythonInterpreter.class)),
                false);

    }

    @SuppressWarnings("unused")
    public static List<PythonLikeObject> extractArguments(int totalArgCount,
            int keywordOnlyArgCount,
            List<PythonString> variableNameList,
            List<PythonLikeObject> defaultPositionalArguments,
            Map<PythonString, PythonLikeObject> defaultNamedArguments,
            List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments,
            boolean isGeneric) {
        if (positionalArguments == null) {
            positionalArguments = List.of();
        }

        if (namedArguments == null) {
            namedArguments = Map.of();
        }

        if (positionalArguments.size() + namedArguments.size() + defaultPositionalArguments.size()
                + defaultNamedArguments.size() < totalArgCount) {
            // TODO: explain missing positional/named arguments
            System.out.println(positionalArguments);
            throw new IllegalArgumentException();
        }

        int positionalArgumentEnd = totalArgCount - keywordOnlyArgCount;
        List<PythonLikeObject> out = new ArrayList<>(totalArgCount);

        out.addAll(positionalArguments);
        if (out.size() < positionalArgumentEnd) {
            int defaultPositionalArgumentsStart = positionalArgumentEnd - defaultPositionalArguments.size() - out.size();
            if (defaultPositionalArgumentsStart < 0) {
                // TODO: explain missing positional/named arguments
                throw new IllegalArgumentException("missing " + -defaultPositionalArgumentsStart + " required arguments");
            }
            out.addAll(defaultPositionalArguments.subList(defaultPositionalArgumentsStart, defaultPositionalArguments.size()));
        }

        for (int i = out.size(); i < totalArgCount; i++) {
            out.add(null);
        }

        for (PythonString key : defaultNamedArguments.keySet()) {
            int index = variableNameList.indexOf(key);
            out.set(index, namedArguments.get(key));
        }

        Set<PythonString> toRemoveNamedArgs = new HashSet<>();
        for (PythonString key : namedArguments.keySet()) {
            int index = variableNameList.indexOf(key);
            if (index == -1 || index >= totalArgCount) {
                if (!isGeneric) {
                    // TODO: explain invalid named arguments
                    throw new IllegalArgumentException("Function is not generic and got extra keyword arg " + key);
                } else {
                    // remove unused element from out
                    out.remove(out.size() - 1);
                }
            } else {
                out.set(index, namedArguments.get(key));
                toRemoveNamedArgs.add(key);
            }
        }
        toRemoveNamedArgs.forEach(namedArguments::remove);

        if (out.size() != totalArgCount) {
            // TODO: explain invalid argument count
            throw new IllegalArgumentException("Mismatch arg counts: actual " + out.size() + " expected " + totalArgCount +
                    " args: " + positionalArguments + "; names: " + namedArguments);
        }

        return out;
    }

    @SuppressWarnings("unused")
    public static PythonLikeFunction createInstance(PythonLikeTuple defaultPositionalArgs,
            PythonLikeDict defaultKeywordArgs,
            PythonLikeTuple annotationTuple,
            PythonLikeTuple closure,
            PythonString functionName,
            PythonCode code,
            PythonInterpreter pythonInterpreter) {
        return createInstance(defaultPositionalArgs, defaultKeywordArgs, annotationTuple, closure, functionName,
                code.functionClass, pythonInterpreter);
    }

    public static <T> T createInstance(PythonLikeTuple defaultPositionalArgs,
            PythonLikeDict defaultKeywordArgs,
            PythonLikeTuple annotationTuple,
            PythonLikeTuple closure,
            PythonString functionName,
            Class<T> functionClass,
            PythonInterpreter pythonInterpreter) {
        PythonLikeDict annotationDirectory = new PythonLikeDict();
        for (int i = 0; i < annotationTuple.size(); i++) {
            annotationDirectory.put(annotationTuple.get(i * 2), annotationTuple.get(i * 2 + 1));
        }

        try {
            Constructor<T> constructor = functionClass.getConstructor(PythonLikeTuple.class,
                    PythonLikeDict.class,
                    PythonLikeDict.class,
                    PythonLikeTuple.class,
                    PythonString.class,
                    PythonInterpreter.class);
            return constructor.newInstance(defaultPositionalArgs, defaultKeywordArgs, annotationDirectory, closure,
                    functionName, pythonInterpreter);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TupleMapPair {
        public PythonLikeTuple tuple;
        public PythonLikeDict map;

        List<PythonLikeObject> mapKeyTuple;

        final int totalNumberOfPositionalAndKeywordArguments;

        public TupleMapPair(int itemsToPop) {
            tuple = null; // Tuple is created when we know how many items are in it
            mapKeyTuple = null; // mapKeyTuple is the first item reverseAdded
            map = new PythonLikeDict();
            this.totalNumberOfPositionalAndKeywordArguments = itemsToPop - 1;
        }

        public void reverseAdd(PythonLikeObject object) {
            if (mapKeyTuple == null) {
                mapKeyTuple = (List<PythonLikeObject>) object;
                tuple = new PythonLikeTuple(totalNumberOfPositionalAndKeywordArguments - mapKeyTuple.size());
                return;
            }

            if (map.size() < mapKeyTuple.size()) {
                map.put(mapKeyTuple.get(mapKeyTuple.size() - map.size() - 1), object);
            } else {
                tuple.reverseAdd(object);
            }
        }
    }

}
