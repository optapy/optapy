package org.optaplanner.python.translator;

import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.GENERATED_PACKAGE_BASE;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.writeClassOutput;
import static org.optaplanner.python.translator.types.BuiltinTypes.asmClassLoader;
import static org.optaplanner.python.translator.types.BuiltinTypes.classNameToBytecode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.python.translator.types.PythonKnownFunctionType;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;

public class PythonOverloadImplementor {
    public static Comparator<PythonLikeType> TYPE_DEPTH_COMPARATOR = Comparator.comparingInt(PythonLikeType::getDepth)
            .thenComparing(PythonLikeType::getTypeName)
            .thenComparing(PythonLikeType::getJavaTypeInternalName)
            .reversed();

    private final static List<DeferredRunner> deferredRunnerList = new ArrayList<>();

    public interface DeferredRunner {
        PythonLikeType run() throws NoSuchMethodException;
    }

    public static void deferDispatchesFor(DeferredRunner runner) {
        try {
            createDispatchesFor(runner.run());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        // deferredRunnerList.add(runner);
    }

    public static void createDeferredDispatches() {
        while (!deferredRunnerList.isEmpty()) {
            List<DeferredRunner> deferredRunnables = new ArrayList<>(deferredRunnerList);

            List<PythonLikeType> deferredTypes = new ArrayList<>(deferredRunnables.size());
            deferredRunnables.forEach(runner -> {
                try {
                    deferredTypes.add(runner.run());
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            });
            deferredTypes.forEach(PythonOverloadImplementor::createDispatchesFor);
            deferredRunnerList.subList(0, deferredRunnables.size()).clear();
        }
    }

    public static void createDispatchesFor(PythonLikeType pythonLikeType) {
        for (String methodName : pythonLikeType.getKnownMethods()) {
            PythonLikeFunction overloadDispatch =
                    createDispatchForMethod(pythonLikeType, methodName, pythonLikeType.getMethodType(methodName).orElseThrow());
            pythonLikeType.__setAttribute(methodName, overloadDispatch);
        }
    }

    private static PythonLikeFunction createDispatchForMethod(PythonLikeType pythonLikeType,
            String methodName,
            PythonKnownFunctionType knownFunctionType) {
        String maybeClassName = GENERATED_PACKAGE_BASE + pythonLikeType.getJavaTypeInternalName().replace('/', '.') + "."
                + methodName + "$$Dispatcher";
        int numberOfInstances = classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String className = maybeClassName;
        String internalClassName = className.replace('.', '/');

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, internalClassName, null,
                Type.getInternalName(Object.class), new String[] {
                        Type.getInternalName(PythonLikeFunction.class)
                });

        // No args constructor for creating instance of this class
        MethodVisitor methodVisitor =
                classWriter.visitMethod(Modifier.PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE),
                        null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        createDispatchFunction(pythonLikeType, knownFunctionType, classWriter);

        classWriter.visitEnd();
        writeClassOutput(classNameToBytecode, className, classWriter.toByteArray());

        try {
            Class<? extends PythonLikeFunction> generatedClass =
                    (Class<? extends PythonLikeFunction>) asmClassLoader.loadClass(className);
            return generatedClass.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Impossible State: Unable to load generated class (" +
                    className + ") despite it being just generated.", e);
        } catch (InvocationTargetException | InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Impossible State: Unable to invoke constructor for generated class (" +
                    className + ").", e);
        }
    }

    private static void createDispatchFunction(PythonLikeType type, PythonKnownFunctionType knownFunctionType,
            ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "__call__",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class)),
                null,
                null);

        methodVisitor.visitParameter("positionalArguments", 0);
        methodVisitor.visitParameter("namedArguments", 0);
        methodVisitor.visitCode();

        List<PythonFunctionSignature> overloadList = knownFunctionType.getOverloadFunctionSignatureList();

        Map<Integer, List<PythonFunctionSignature>> pythonFunctionSignatureByArgumentLength = overloadList.stream()
                .collect(Collectors.groupingBy(sig -> sig.getParameterTypes().length));

        if (pythonFunctionSignatureByArgumentLength.size() == 1) {
            Map.Entry<Integer, List<PythonFunctionSignature>> argCountOverloadPair = pythonFunctionSignatureByArgumentLength
                    .entrySet().iterator().next();
            createDispatchForArgCount(methodVisitor, argCountOverloadPair.getKey(), type, argCountOverloadPair.getValue());
        } else {
            int[] argCounts = pythonFunctionSignatureByArgumentLength.keySet().stream().sorted().mapToInt(i -> i).toArray();
            Label[] argCountLabel = new Label[argCounts.length];
            Label defaultCase = new Label();

            for (int i = 0; i < argCountLabel.length; i++) {
                argCountLabel[i] = new Label();
            }

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "size",
                    Type.getMethodDescriptor(Type.INT_TYPE), true);
            if (!overloadList.get(0).getMethodDescriptor().methodType.isStatic()) {
                methodVisitor.visitInsn(Opcodes.ICONST_M1);
                methodVisitor.visitInsn(Opcodes.IADD);
            }
            methodVisitor.visitLookupSwitchInsn(defaultCase, argCounts, argCountLabel);

            for (int i = 0; i < argCounts.length; i++) {
                methodVisitor.visitLabel(argCountLabel[i]);
                createDispatchForArgCount(methodVisitor, argCounts[i], type,
                        pythonFunctionSignatureByArgumentLength.get(argCounts[i]));
            }
            methodVisitor.visitLabel(defaultCase);

            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn("No overload has the given argcount. Possible overload(s) are: " +
                    knownFunctionType.getOverloadFunctionSignatureList().stream().map(PythonFunctionSignature::toString)
                            .collect(Collectors.joining(",\n")));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                    "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.ATHROW);
        }

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    private static void createDispatchForArgCount(MethodVisitor methodVisitor, int argCount,
            PythonLikeType type, List<PythonFunctionSignature> functionSignatureList) {
        final int MATCHING_OVERLOAD_SET_VARIABLE_INDEX = 3; // 0 = this; 1 = posArguments; 2 = namedArguments
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(HashSet.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(functionSignatureList.size());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(HashSet.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);
        for (int i = 0; i < functionSignatureList.size(); i++) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
                    Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE), false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "add",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                    true);
            methodVisitor.visitInsn(Opcodes.POP);
        }
        methodVisitor.visitVarInsn(Opcodes.ASTORE, MATCHING_OVERLOAD_SET_VARIABLE_INDEX);

        int startIndex = 0;
        if (!functionSignatureList.get(0).getMethodDescriptor().methodType.isStatic()) {
            startIndex = 1;
        }

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);

        // At the start of each iteration, stack = arg_1, arg_2, ..., arg_(i-1), pos_args_list
        for (int i = 0; i < argCount; i++) {
            methodVisitor.visitInsn(Opcodes.DUP);

            // Get the ith positional argument
            methodVisitor.visitLdcInsn(i + startIndex);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);

            SortedMap<PythonLikeType, List<PythonFunctionSignature>> typeToPossibleSignatures =
                    getTypeForParameter(functionSignatureList, i);

            Label endOfInstanceOfIfs = new Label();
            for (PythonLikeType pythonLikeType : typeToPossibleSignatures.keySet()) {
                Label nextIf = new Label();
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, pythonLikeType.getJavaTypeInternalName());
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, nextIf);

                // pythonLikeType matches argument type
                List<PythonFunctionSignature> matchingOverloadList = typeToPossibleSignatures.get(pythonLikeType);

                if (matchingOverloadList.size() != functionSignatureList.size()) {
                    // Remove overloads that do not match from the matching overload set
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, MATCHING_OVERLOAD_SET_VARIABLE_INDEX);
                    for (int sigIndex = 0; sigIndex < functionSignatureList.size(); sigIndex++) {
                        if (!matchingOverloadList.contains(functionSignatureList.get(sigIndex))) {
                            methodVisitor.visitInsn(Opcodes.DUP);
                            methodVisitor.visitLdcInsn(sigIndex);
                            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
                                    Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE), false);
                            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                                    "remove",
                                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                                    true);
                            methodVisitor.visitInsn(Opcodes.POP);
                        }
                    }
                    methodVisitor.visitInsn(Opcodes.POP);
                    methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfInstanceOfIfs);
                } else {
                    methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfInstanceOfIfs);
                }
                methodVisitor.visitLabel(nextIf);
            }
            // This is an else at the end of the instanceof if's, which clear the set as no overloads match
            methodVisitor.visitVarInsn(Opcodes.ALOAD, MATCHING_OVERLOAD_SET_VARIABLE_INDEX);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "clear",
                    Type.getMethodDescriptor(Type.VOID_TYPE), true);

            // end of instance of ifs
            methodVisitor.visitLabel(endOfInstanceOfIfs);
            methodVisitor.visitInsn(Opcodes.POP); // remove argument (need to typecast it later)
        }
        methodVisitor.visitInsn(Opcodes.POP); // Remove list

        // Stack is arg_1, arg_2, ..., arg_(argCount)
        methodVisitor.visitVarInsn(Opcodes.ALOAD, MATCHING_OVERLOAD_SET_VARIABLE_INDEX);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "size",
                Type.getMethodDescriptor(Type.INT_TYPE), true);

        Label setIsNotEmpty = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IFNE, setIsNotEmpty);

        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("No overload match the given arguments. Possible overload(s) for " + argCount
                + " arguments are: " +
                functionSignatureList.stream().map(PythonFunctionSignature::toString).collect(Collectors.joining(",\n")));
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(setIsNotEmpty);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Iterable.class), "iterator",
                Type.getMethodDescriptor(Type.getType(Iterator.class)), true);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Iterator.class), "next",
                Type.getMethodDescriptor(Type.getType(Object.class)), true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Integer.class));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue",
                Type.getMethodDescriptor(Type.INT_TYPE), false);

        Label defaultHandler = new Label();
        Label[] signatureIndexToDispatch = new Label[functionSignatureList.size()];
        for (int i = 0; i < functionSignatureList.size(); i++) {
            signatureIndexToDispatch[i] = new Label();
        }

        methodVisitor.visitTableSwitchInsn(0, functionSignatureList.size() - 1, defaultHandler, signatureIndexToDispatch);

        for (int i = 0; i < functionSignatureList.size(); i++) {
            methodVisitor.visitLabel(signatureIndexToDispatch[i]);
            PythonFunctionSignature matchingSignature = functionSignatureList.get(i);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);

            if (startIndex != 0) {
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(0);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                        Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                        matchingSignature.getMethodDescriptor().getDeclaringClassInternalName());
                methodVisitor.visitInsn(Opcodes.SWAP);
            }

            for (int argIndex = 0; argIndex < argCount; argIndex++) {
                PythonLikeType parameterType = matchingSignature.getParameterTypes()[argIndex];
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(argIndex + startIndex);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                        Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterType.getJavaTypeInternalName());
                methodVisitor.visitInsn(Opcodes.SWAP);
            }
            methodVisitor.visitInsn(Opcodes.POP);
            matchingSignature.getMethodDescriptor().callMethod(methodVisitor);
            methodVisitor.visitInsn(Opcodes.ARETURN);
        }

        methodVisitor.visitLabel(defaultHandler);
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalStateException.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("Return signature index is out of bounds");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalStateException.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ATHROW);
    }

    private static SortedMap<PythonLikeType, List<PythonFunctionSignature>>
            getTypeForParameter(List<PythonFunctionSignature> functionSignatureList, int parameter) {
        SortedMap<PythonLikeType, List<PythonFunctionSignature>> out = new TreeMap<>(TYPE_DEPTH_COMPARATOR);
        for (PythonFunctionSignature functionSignature : functionSignatureList) {
            out.computeIfAbsent(functionSignature.getParameterTypes()[parameter], type -> new ArrayList<>())
                    .add(functionSignature);
        }
        return out;
    }

}