package org.optaplanner.jpyinterpreter;

import static org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator.ARGUMENT_SPEC_INSTANCE_FIELD_NAME;
import static org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToInstance;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.dag.FlowGraph;
import org.optaplanner.jpyinterpreter.implementors.JavaComparableImplementor;
import org.optaplanner.jpyinterpreter.implementors.JavaEqualsImplementor;
import org.optaplanner.jpyinterpreter.implementors.JavaHashCodeImplementor;
import org.optaplanner.jpyinterpreter.implementors.JavaInterfaceImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;
import org.optaplanner.jpyinterpreter.opcodes.SelfOpcodeWithoutSource;
import org.optaplanner.jpyinterpreter.opcodes.controlflow.ReturnValueOpcode;
import org.optaplanner.jpyinterpreter.opcodes.object.DeleteAttrOpcode;
import org.optaplanner.jpyinterpreter.opcodes.object.LoadAttrOpcode;
import org.optaplanner.jpyinterpreter.opcodes.object.StoreAttrOpcode;
import org.optaplanner.jpyinterpreter.opcodes.variable.LoadFastOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.GeneratedFunctionMethodReference;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;
import org.optaplanner.jpyinterpreter.util.arguments.ArgumentSpec;

public class PythonClassTranslator {
    static Map<FunctionSignature, InterfaceDeclaration> functionSignatureToInterfaceName = new HashMap<>();

    // $ is illegal in variables/methods in Python
    public static String TYPE_FIELD_NAME = "$TYPE";
    public static String CPYTHON_TYPE_FIELD_NAME = "$CPYTHON_TYPE";

    public static PythonLikeType translatePythonClass(PythonCompiledClass pythonCompiledClass) {
        String maybeClassName =
                PythonBytecodeToJavaBytecodeTranslator.USER_PACKAGE_BASE + pythonCompiledClass.getGeneratedClassBaseName();
        int numberOfInstances =
                PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String className = maybeClassName;
        String internalClassName = className.replace('.', '/');

        Set<PythonLikeType> superTypeSet;
        Set<JavaInterfaceImplementor> javaInterfaceImplementorSet = new HashSet<>();

        for (Map.Entry<String, PythonCompiledFunction> instanceMethodEntry : pythonCompiledClass.instanceFunctionNameToPythonBytecode
                .entrySet()) {
            switch (instanceMethodEntry.getKey()) {
                case "__eq__":
                    javaInterfaceImplementorSet.add(new JavaEqualsImplementor(internalClassName));
                    break;

                case "__hash__":
                    javaInterfaceImplementorSet.add(new JavaHashCodeImplementor(internalClassName));
                    break;

                case "__lt__":
                case "__le__":
                case "__gt__":
                case "__ge__":
                    javaInterfaceImplementorSet
                            .add(new JavaComparableImplementor(internalClassName, instanceMethodEntry.getKey()));
                    break;
            }
        }

        if (pythonCompiledClass.superclassList.isEmpty()) {
            superTypeSet = Set.of(CPythonBackedPythonLikeObject.CPYTHON_BACKED_OBJECT_TYPE);
        } else {
            superTypeSet = new LinkedHashSet<>(pythonCompiledClass.superclassList.size());
            for (int i = 0; i < pythonCompiledClass.superclassList.size(); i++) {
                PythonLikeType superType = pythonCompiledClass.superclassList.get(i);
                if (superType == BuiltinTypes.BASE_TYPE || superType == null) {
                    superTypeSet.add(CPythonBackedPythonLikeObject.CPYTHON_BACKED_OBJECT_TYPE);
                } else {
                    superTypeSet.add(superType);
                }
            }
        }

        List<PythonLikeType> superTypeList = new ArrayList<>(superTypeSet);
        PythonLikeType pythonLikeType = new PythonLikeType(pythonCompiledClass.className, internalClassName,
                superTypeList);
        PythonLikeType superClassType = superTypeList.get(0);
        Set<String> instanceAttributeSet = new HashSet<>();
        pythonCompiledClass.instanceFunctionNameToPythonBytecode.values().forEach(instanceMethod -> {
            try {
                instanceAttributeSet.addAll(getReferencedSelfAttributes(instanceMethod));
            } catch (UnsupportedOperationException e) {
                // silently ignore unsupported operations
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("WARNING: Ignoring exception");
                //e.printStackTrace();
                //System.out.println("globals: " + instanceMethod.globalsMap);
                //System.out.println("co_constants: " + instanceMethod.co_constants);
                //System.out.println("co_names: " + instanceMethod.co_names);
                //System.out.println("co_varnames: " + instanceMethod.co_varnames);
                System.out.println("Instructions:");
                System.out.println(instanceMethod.instructionList.stream()
                        .map(PythonBytecodeInstruction::toString)
                        .collect(Collectors.joining("\n")));
            }
        });

        List<JavaInterfaceImplementor> nonObjectInterfaceImplementors = javaInterfaceImplementorSet.stream()
                .filter(implementor -> !Object.class.equals(implementor.getInterfaceClass()))
                .collect(Collectors.toList());
        String[] interfaces = new String[nonObjectInterfaceImplementors.size()];
        for (int i = 0; i < nonObjectInterfaceImplementors.size(); i++) {
            interfaces[i] = Type.getInternalName(nonObjectInterfaceImplementors.get(i).getInterfaceClass());
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, internalClassName, null,
                superClassType.getJavaTypeInternalName(), interfaces);

        pythonCompiledClass.staticAttributeNameToObject.forEach(pythonLikeType::__setAttribute);

        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, TYPE_FIELD_NAME, Type.getDescriptor(PythonLikeType.class),
                null, null);
        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, CPYTHON_TYPE_FIELD_NAME,
                Type.getDescriptor(PythonLikeType.class),
                null, null);

        for (Map.Entry<String, PythonLikeObject> staticAttributeEntry : pythonCompiledClass.staticAttributeNameToObject
                .entrySet()) {
            pythonLikeType.__setAttribute(staticAttributeEntry.getKey(), staticAttributeEntry.getValue());
        }

        Map<String, PythonLikeType> attributeNameToTypeMap = new HashMap<>();
        for (String attributeName : instanceAttributeSet) {
            PythonLikeType type = pythonCompiledClass.typeAnnotations.getOrDefault(attributeName, BuiltinTypes.BASE_TYPE);
            if (type == null) { // null might be in __annotations__
                type = BuiltinTypes.BASE_TYPE;
            }
            String javaFieldTypeDescriptor = 'L' + type.getJavaTypeInternalName() + ';';
            attributeNameToTypeMap.put(attributeName, type);
            classWriter.visitField(Modifier.PUBLIC, getJavaFieldName(attributeName), javaFieldTypeDescriptor, null, null);
            FieldDescriptor fieldDescriptor =
                    new FieldDescriptor(attributeName, getJavaFieldName(attributeName), internalClassName,
                            javaFieldTypeDescriptor, type, true);
            pythonLikeType.addInstanceField(fieldDescriptor);
        }

        // No args constructor for creating instance of this class
        MethodVisitor methodVisitor =
                classWriter.visitMethod(Modifier.PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE),
                        null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalClassName, TYPE_FIELD_NAME,
                Type.getDescriptor(PythonLikeType.class));
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassType.getJavaTypeInternalName(), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeType.class)),
                false);
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        // type arg constructor for creating subclass of this class
        methodVisitor =
                classWriter.visitMethod(Modifier.PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeType.class)),
                        null, null);
        methodVisitor.visitParameter("subclassType", 0);

        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassType.getJavaTypeInternalName(), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeType.class)),
                false);
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        createGetAttribute(classWriter, internalClassName, superClassType.getJavaTypeInternalName(),
                instanceAttributeSet,
                attributeNameToTypeMap);
        createSetAttribute(classWriter, internalClassName, superClassType.getJavaTypeInternalName(),
                instanceAttributeSet,
                attributeNameToTypeMap);
        createDeleteAttribute(classWriter, internalClassName, superClassType.getJavaTypeInternalName(),
                instanceAttributeSet,
                attributeNameToTypeMap);

        for (Map.Entry<String, PythonCompiledFunction> instanceMethodEntry : pythonCompiledClass.instanceFunctionNameToPythonBytecode
                .entrySet()) {
            instanceMethodEntry.getValue().methodKind = PythonMethodKind.VIRTUAL_METHOD;
            createInstanceMethod(pythonLikeType, classWriter, internalClassName, instanceMethodEntry.getKey(),
                    instanceMethodEntry.getValue());
        }

        for (Map.Entry<String, PythonCompiledFunction> staticMethodEntry : pythonCompiledClass.staticFunctionNameToPythonBytecode
                .entrySet()) {
            staticMethodEntry.getValue().methodKind = PythonMethodKind.STATIC_METHOD;
            createStaticMethod(pythonLikeType, classWriter, internalClassName, staticMethodEntry.getKey(),
                    staticMethodEntry.getValue());
        }

        for (Map.Entry<String, PythonCompiledFunction> classMethodEntry : pythonCompiledClass.classFunctionNameToPythonBytecode
                .entrySet()) {
            classMethodEntry.getValue().methodKind = PythonMethodKind.CLASS_METHOD;
            createClassMethod(pythonLikeType, classWriter, internalClassName, classMethodEntry.getKey(),
                    classMethodEntry.getValue());
        }

        boolean isCpythonBacked;

        try {
            isCpythonBacked = CPythonBackedPythonLikeObject.class.isAssignableFrom(superClassType.getJavaClass());
        } catch (ClassNotFoundException e) {
            isCpythonBacked = false;
        }

        if (isCpythonBacked) {
            createCPythonOperationMethods(classWriter, internalClassName, superClassType.getJavaTypeInternalName(),
                    attributeNameToTypeMap);
        }

        javaInterfaceImplementorSet.forEach(implementor -> implementor.implement(classWriter, pythonCompiledClass));

        classWriter.visitEnd();

        PythonBytecodeToJavaBytecodeTranslator.writeClassOutput(BuiltinTypes.classNameToBytecode, className,
                classWriter.toByteArray());

        pythonLikeType.__setAttribute("__name__", PythonString.valueOf(pythonCompiledClass.className));
        pythonLikeType.__setAttribute("__qualname__", PythonString.valueOf(pythonCompiledClass.qualifiedName));
        pythonLikeType.__setAttribute("__module__", PythonString.valueOf(pythonCompiledClass.module));

        PythonLikeDict annotations = new PythonLikeDict();
        pythonCompiledClass.typeAnnotations.forEach((name, type) -> annotations.put(PythonString.valueOf(name), type));
        pythonLikeType.__setAttribute("__annotations__", annotations);

        PythonLikeTuple mro = new PythonLikeTuple();
        mro.addAll(superTypeList);
        pythonLikeType.__setAttribute("__mro__", mro);

        Class<? extends PythonLikeObject> generatedClass;
        try {
            generatedClass = (Class<? extends PythonLikeObject>) BuiltinTypes.asmClassLoader.loadClass(className);
            generatedClass.getField(TYPE_FIELD_NAME).set(null, pythonLikeType);
            generatedClass.getField(CPYTHON_TYPE_FIELD_NAME).set(null, pythonCompiledClass.binaryType);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Impossible State: Unable to load generated class (" +
                    className + ") despite it being just generated.", e);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Impossible State: could not access type static field for generated class ("
                    + className + ").", e);
        }

        Class<?> initFunctionClass = null;
        for (Map.Entry<String, PythonCompiledFunction> instanceMethodEntry : pythonCompiledClass.instanceFunctionNameToPythonBytecode
                .entrySet()) {
            InterfaceDeclaration interfaceDeclaration =
                    getInterfaceForInstancePythonFunction(internalClassName, instanceMethodEntry.getValue());
            Class<?> createdFunctionClass = createBytecodeForMethodAndSetOnClass(className, pythonLikeType,
                    pythonCompiledClass.binaryType, generatedClass,
                    instanceMethodEntry,
                    interfaceDeclaration, PythonMethodKind.VIRTUAL_METHOD);
            if (instanceMethodEntry.getKey().equals("__init__")) {
                initFunctionClass = createdFunctionClass;
            }
        }

        for (Map.Entry<String, PythonCompiledFunction> staticMethodEntry : pythonCompiledClass.staticFunctionNameToPythonBytecode
                .entrySet()) {
            InterfaceDeclaration interfaceDeclaration = getInterfaceForPythonFunction(staticMethodEntry.getValue());
            createBytecodeForMethodAndSetOnClass(className, pythonLikeType, pythonCompiledClass.binaryType, generatedClass,
                    staticMethodEntry,
                    interfaceDeclaration, PythonMethodKind.STATIC_METHOD);
        }

        for (Map.Entry<String, PythonCompiledFunction> classMethodEntry : pythonCompiledClass.classFunctionNameToPythonBytecode
                .entrySet()) {
            InterfaceDeclaration interfaceDeclaration = getInterfaceForClassPythonFunction(classMethodEntry.getValue());
            createBytecodeForMethodAndSetOnClass(className, pythonLikeType, pythonCompiledClass.binaryType, generatedClass,
                    classMethodEntry,
                    interfaceDeclaration, PythonMethodKind.CLASS_METHOD);
        }

        pythonLikeType.setConstructor(createConstructor(internalClassName,
                pythonCompiledClass.instanceFunctionNameToPythonBytecode.get("__init__"),
                generatedClass));

        PythonOverloadImplementor.createDispatchesFor(pythonLikeType);
        return pythonLikeType;
    }

    public static void setSelfStaticInstances(PythonCompiledClass pythonCompiledClass, Class<?> generatedClass,
            PythonLikeType pythonLikeType,
            Map<Number, PythonLikeObject> instanceMap) {
        if (!CPythonBackedPythonLikeObject.class.isAssignableFrom(generatedClass)) {
            return;
        }

        pythonCompiledClass.staticAttributeNameToClassInstance.forEach((attributeName, instancePointer) -> {
            try {
                CPythonBackedPythonLikeObject objectInstance =
                        (CPythonBackedPythonLikeObject) generatedClass.getConstructor().newInstance();
                Number pythonReferenceId = CPythonBackedPythonInterpreter.getPythonReferenceId(instancePointer);
                instanceMap.put(pythonReferenceId, objectInstance);
                objectInstance.$setCPythonReference(instancePointer);
                objectInstance.$setCPythonId(PythonInteger.valueOf(pythonReferenceId.longValue()));
                objectInstance.$setInstanceMap(instanceMap);
                objectInstance.$readFieldsFromCPythonReference();
                pythonLikeType.__setAttribute(attributeName, objectInstance);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException("Unable to construct instance of class (" + generatedClass + ")", e);
            }
        });
    }

    public static String getJavaFieldName(String pythonFieldName) {
        return "$field$" + pythonFieldName;
    }

    public static String getPythonFieldName(String javaFieldName) {
        return javaFieldName.substring("$field$".length());
    }

    public static String getJavaMethodName(String pythonMethodName) {
        return "$method$" + pythonMethodName;
    }

    public static String getPythonMethodName(String javaMethodName) {
        return javaMethodName.substring("$method$".length());
    }

    private static Class<?> createBytecodeForMethodAndSetOnClass(String className, PythonLikeType pythonLikeType,
            PythonLikeType cPythonType,
            Class<? extends PythonLikeObject> generatedClass,
            Map.Entry<String, PythonCompiledFunction> methodEntry,
            InterfaceDeclaration interfaceDeclaration,
            PythonMethodKind pythonMethodKind) {
        Class<?> functionClass;
        Object functionInstance;

        try {
            functionInstance = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToInstance(methodEntry.getValue(),
                    new MethodDescriptor(interfaceDeclaration.interfaceName,
                            MethodDescriptor.MethodType.INTERFACE, "invoke",
                            interfaceDeclaration.methodDescriptor),
                    pythonMethodKind == PythonMethodKind.VIRTUAL_METHOD);
            functionClass = functionInstance.getClass();
            functionClass.getField(PythonBytecodeToJavaBytecodeTranslator.CLASS_CELL_STATIC_FIELD_NAME).set(null,
                    pythonLikeType);
        } catch (Exception e) {
            functionClass = createPythonWrapperMethod(methodEntry.getKey(), methodEntry.getValue(),
                    interfaceDeclaration, pythonMethodKind == PythonMethodKind.VIRTUAL_METHOD);
            try {
                functionInstance = functionClass.getConstructor(PythonLikeType.class).newInstance(cPythonType);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                throw new IllegalStateException(
                        "Cannot create instance of Python native wrapper despite it being just generated", ex);
            }
        }

        try {
            PythonLikeObject translatedPythonMethodWrapper;
            PythonCompiledFunction function = methodEntry.getValue();
            pythonLikeType.clearMethod(methodEntry.getKey());
            String javaMethodDescriptor = Arrays.stream(generatedClass.getDeclaredMethods())
                    .filter(method -> method.getName().equals(getJavaMethodName(methodEntry.getKey())))
                    .map(Type::getMethodDescriptor)
                    .findFirst().orElseThrow();
            ArgumentSpec<?> argumentSpec = function.getArgumentSpecMapper()
                    .apply(function.defaultPositionalArguments, function.defaultKeywordArguments);

            switch (pythonMethodKind) {
                case VIRTUAL_METHOD:
                    translatedPythonMethodWrapper = new GeneratedFunctionMethodReference(functionInstance,
                            functionClass.getMethods()[0],
                            Map.of(),
                            PythonLikeFunction.getFunctionType());
                    pythonLikeType.addMethod(methodEntry.getKey(),
                            argumentSpec.asPythonFunctionSignature(className.replace('.', '/'),
                                    getJavaMethodName(methodEntry.getKey()),
                                    javaMethodDescriptor));
                    break;

                case STATIC_METHOD:
                    translatedPythonMethodWrapper = new GeneratedFunctionMethodReference(functionInstance,
                            functionClass.getMethods()[0],
                            Map.of(),
                            PythonLikeFunction.getStaticFunctionType());
                    pythonLikeType.addMethod(methodEntry.getKey(),
                            argumentSpec.asStaticPythonFunctionSignature(className.replace('.', '/'),
                                    getJavaMethodName(methodEntry.getKey()),
                                    javaMethodDescriptor));
                    break;

                case CLASS_METHOD:
                    translatedPythonMethodWrapper = new GeneratedFunctionMethodReference(functionInstance,
                            functionClass.getMethods()[0],
                            Map.of(),
                            PythonLikeFunction.getClassFunctionType());
                    pythonLikeType.addMethod(methodEntry.getKey(),
                            argumentSpec.asClassPythonFunctionSignature(className.replace('.', '/'),
                                    getJavaMethodName(methodEntry.getKey()),
                                    javaMethodDescriptor));
                    break;

                default:
                    throw new IllegalStateException("Unhandled case: " + pythonMethodKind);
            }

            generatedClass.getField(getJavaMethodName(methodEntry.getKey()))
                    .set(null, functionInstance);
            pythonLikeType.__setAttribute(methodEntry.getKey(), translatedPythonMethodWrapper);
            return functionClass;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException("Impossible State: could not access method (" + methodEntry.getKey()
                    + ") static field for generated class ("
                    + className + ").", e);
        }
    }

    private static Class<?> createPythonWrapperMethod(String methodName, PythonCompiledFunction pythonCompiledFunction,
            InterfaceDeclaration interfaceDeclaration, boolean isVirtual) {
        String maybeClassName = PythonBytecodeToJavaBytecodeTranslator.GENERATED_PACKAGE_BASE
                + pythonCompiledFunction.getGeneratedClassBaseName() + "$$Wrapper";
        int numberOfInstances =
                PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String className = maybeClassName;
        String internalClassName = className.replace('.', '/');

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, internalClassName, null,
                Type.getInternalName(Object.class), new String[] { interfaceDeclaration.interfaceName });

        classWriter.visitField(Modifier.PUBLIC | Modifier.FINAL, "$binaryType", Type.getDescriptor(PythonLikeType.class),
                null, null);

        // Constructor that takes a PythonLikeType
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeType.class)),
                null, null);

        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, "$binaryType",
                Type.getDescriptor(PythonLikeType.class));

        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        // Interface method
        methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "invoke", interfaceDeclaration.methodDescriptor,
                null, null);

        for (int i = 0; i < pythonCompiledFunction.totalArgCount(); i++) {
            methodVisitor.visitParameter("parameter" + i, 0);
        }

        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, internalClassName, "$binaryType",
                Type.getDescriptor(PythonLikeType.class));
        methodVisitor.visitLdcInsn(methodName);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "__getAttributeOrError",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(String.class)),
                true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeFunction.class));

        methodVisitor.visitLdcInsn(pythonCompiledFunction.totalArgCount());
        methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
        for (int i = 0; i < pythonCompiledFunction.totalArgCount(); i++) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, i + 1);
            methodVisitor.visitInsn(Opcodes.AASTORE);
        }

        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Arrays.class), "asList",
                Type.getMethodDescriptor(Type.getType(List.class), Type.getType(Object[].class)),
                false);

        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);

        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class), "$call",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(List.class),
                        Type.getType(Map.class), Type.getType(PythonLikeObject.class)),
                true);

        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                Type.getReturnType(interfaceDeclaration.methodDescriptor).getInternalName());
        methodVisitor.visitInsn(Opcodes.ARETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        classWriter.visitEnd();

        PythonBytecodeToJavaBytecodeTranslator.writeClassOutput(BuiltinTypes.classNameToBytecode, className,
                classWriter.toByteArray());

        try {
            return BuiltinTypes.asmClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load class " + className + " despite it being just generated", e);
        }
    }

    private static PythonLikeFunction createConstructor(String classInternalName,
            PythonCompiledFunction initFunction,
            Class<?> typeGeneratedClass) {
        String maybeClassName = PythonBytecodeToJavaBytecodeTranslator.GENERATED_PACKAGE_BASE
                + classInternalName.replace('/', '.') + "$$Constructor";
        int numberOfInstances =
                PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String constructorClassName = maybeClassName;
        String constructorInternalClassName = constructorClassName.replace('.', '/');

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, constructorInternalClassName, null, Type.getInternalName(Object.class),
                new String[] {
                        Type.getInternalName(PythonLikeFunction.class)
                });

        classWriter.visitField(Modifier.STATIC | Modifier.PUBLIC, ARGUMENT_SPEC_INSTANCE_FIELD_NAME,
                Type.getDescriptor(ArgumentSpec.class),
                null, null);

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

        Type generatedClassType = Type.getType('L' + classInternalName + ';');
        methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "$call",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class), Type.getType(Map.class), Type.getType(PythonLikeObject.class)),
                null, null);

        methodVisitor.visitCode();

        methodVisitor.visitTypeInsn(Opcodes.NEW, classInternalName);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, classInternalName, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        if (initFunction != null) {
            methodVisitor.visitInsn(Opcodes.DUP);

            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, constructorInternalClassName, ARGUMENT_SPEC_INSTANCE_FIELD_NAME,
                    Type.getDescriptor(ArgumentSpec.class));
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);

            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ArgumentSpec.class),
                    "extractArgumentList",
                    Type.getMethodDescriptor(Type.getType(List.class), Type.getType(List.class), Type.getType(Map.class)),
                    false);

            List<PythonLikeType> initParameterTypes = initFunction.getParameterTypes();
            for (int i = 1; i < initParameterTypes.size(); i++) {
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(i - 1);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                        Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                        true);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, initParameterTypes.get(i).getJavaTypeInternalName());
                methodVisitor.visitInsn(Opcodes.SWAP);
            }
            methodVisitor.visitInsn(Opcodes.POP);

            Type[] parameterTypes = new Type[initFunction.totalArgCount() - 1];
            List<PythonLikeType> parameterTypeAnnotations = initFunction.getParameterTypes();
            for (int i = 1; i < parameterTypeAnnotations.size(); i++) {
                parameterTypes[i - 1] = Type.getType('L' + parameterTypeAnnotations.get(i).getJavaTypeInternalName() + ';');
            }

            Type returnType = getVirtualFunctionReturnType(initFunction);

            String initMethodDescriptor = Type.getMethodDescriptor(returnType, parameterTypes);

            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classInternalName, getJavaMethodName("__init__"),
                    initMethodDescriptor, false);

            methodVisitor.visitInsn(Opcodes.POP);
        }

        methodVisitor.visitInsn(Opcodes.ARETURN);

        methodVisitor.visitMaxs(-1, -1);

        methodVisitor.visitEnd();

        classWriter.visitEnd();
        PythonBytecodeToJavaBytecodeTranslator.writeClassOutput(BuiltinTypes.classNameToBytecode, constructorClassName,
                classWriter.toByteArray());

        try {
            @SuppressWarnings("unchecked")
            Class<? extends PythonLikeFunction> generatedClass =
                    (Class<? extends PythonLikeFunction>) BuiltinTypes.asmClassLoader.loadClass(constructorClassName);
            if (initFunction != null) {
                Object method = typeGeneratedClass.getField(getJavaMethodName("__init__")).get(null);
                ArgumentSpec spec =
                        (ArgumentSpec) method.getClass().getField(ARGUMENT_SPEC_INSTANCE_FIELD_NAME).get(method);
                generatedClass.getField(ARGUMENT_SPEC_INSTANCE_FIELD_NAME).set(null, spec);
            }
            return generatedClass.getConstructor().newInstance();
        } catch (ClassNotFoundException | RuntimeException | InstantiationException | NoSuchMethodException
                | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            throw new IllegalStateException("Impossible State: Unable to load generated class (" +
                    constructorClassName + ") despite it being just generated.", e);
        }
    }

    private static void createInstanceMethod(PythonLikeType pythonLikeType, ClassWriter classWriter, String internalClassName,
            String methodName, PythonCompiledFunction function) {
        InterfaceDeclaration interfaceDeclaration = getInterfaceForInstancePythonFunction(internalClassName, function);
        String interfaceDescriptor = 'L' + interfaceDeclaration.interfaceName + ';';
        String javaMethodName = getJavaMethodName(methodName);

        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, javaMethodName, interfaceDescriptor,
                null, null);
        Type returnType = getVirtualFunctionReturnType(function);

        List<PythonLikeType> parameterPythonTypeList = function.getParameterTypes();
        Type[] javaParameterTypes = new Type[Math.max(0, function.totalArgCount() - 1)];

        for (int i = 1; i < function.totalArgCount(); i++) {
            javaParameterTypes[i - 1] = Type.getType('L' + parameterPythonTypeList.get(i).getJavaTypeInternalName() + ';');
        }
        String javaMethodDescriptor = Type.getMethodDescriptor(returnType, javaParameterTypes);
        MethodVisitor methodVisitor =
                classWriter.visitMethod(Modifier.PUBLIC, javaMethodName, javaMethodDescriptor, null, null);

        createMethodBody(internalClassName, javaMethodName, javaParameterTypes, interfaceDeclaration.methodDescriptor, function,
                interfaceDeclaration.interfaceName, interfaceDescriptor, methodVisitor);

        pythonLikeType.addMethod(methodName,
                new PythonFunctionSignature(new MethodDescriptor(internalClassName, MethodDescriptor.MethodType.VIRTUAL,
                        javaMethodName, javaMethodDescriptor),
                        function.getReturnType().orElse(BuiltinTypes.BASE_TYPE),
                        function.totalArgCount() > 0
                                ? function.getParameterTypes().subList(1, function.getParameterTypes().size())
                                : List.of()));
    }

    private static void createStaticMethod(PythonLikeType pythonLikeType, ClassWriter classWriter, String internalClassName,
            String methodName, PythonCompiledFunction function) {
        InterfaceDeclaration interfaceDeclaration = getInterfaceForPythonFunction(function);
        String interfaceDescriptor = 'L' + interfaceDeclaration.interfaceName + ';';
        String javaMethodName = getJavaMethodName(methodName);

        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, javaMethodName, interfaceDescriptor,
                null, null);
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC | Modifier.STATIC, javaMethodName,
                function.getAsmMethodDescriptorString(), null, null);

        List<PythonLikeType> parameterPythonTypeList = function.getParameterTypes();
        Type[] javaParameterTypes = new Type[function.totalArgCount()];

        for (int i = 0; i < function.totalArgCount(); i++) {
            javaParameterTypes[i] = Type.getType('L' + parameterPythonTypeList.get(i).getJavaTypeInternalName() + ';');
        }
        createMethodBody(internalClassName, javaMethodName, javaParameterTypes, interfaceDeclaration.methodDescriptor, function,
                interfaceDeclaration.interfaceName, interfaceDescriptor, methodVisitor);

        pythonLikeType.addMethod(methodName,
                new PythonFunctionSignature(new MethodDescriptor(internalClassName, MethodDescriptor.MethodType.STATIC,
                        javaMethodName, function.getAsmMethodDescriptorString()),
                        function.getReturnType().orElse(BuiltinTypes.BASE_TYPE),
                        function.getParameterTypes()));
    }

    private static void createClassMethod(PythonLikeType pythonLikeType, ClassWriter classWriter, String internalClassName,
            String methodName, PythonCompiledFunction function) {
        InterfaceDeclaration interfaceDeclaration = getInterfaceForClassPythonFunction(function);
        String interfaceDescriptor = 'L' + interfaceDeclaration.interfaceName + ';';
        String javaMethodName = getJavaMethodName(methodName);

        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, javaMethodName, interfaceDescriptor,
                null, null);

        String javaMethodDescriptor = interfaceDeclaration.methodDescriptor;
        MethodVisitor methodVisitor =
                classWriter.visitMethod(Modifier.PUBLIC | Modifier.STATIC, javaMethodName, javaMethodDescriptor, null, null);

        for (int i = 0; i < function.getParameterTypes().size(); i++) {
            methodVisitor.visitParameter("parameter" + i, 0);
        }
        methodVisitor.visitCode();

        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalClassName, javaMethodName, interfaceDescriptor);

        for (int i = 0; i < function.totalArgCount(); i++) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, i);
        }

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, interfaceDeclaration.interfaceName, "invoke",
                interfaceDeclaration.methodDescriptor, true);
        methodVisitor.visitInsn(Opcodes.ARETURN);

        methodVisitor.visitMaxs(-1, -1);

        methodVisitor.visitEnd();

        List<PythonLikeType> parameterTypes = new ArrayList<>(function.getParameterTypes().size());
        parameterTypes.add(BuiltinTypes.TYPE_TYPE);
        parameterTypes.addAll(function.getParameterTypes().subList(1, function.getParameterTypes().size()));

        pythonLikeType.addMethod(methodName,
                new PythonFunctionSignature(new MethodDescriptor(internalClassName, MethodDescriptor.MethodType.CLASS,
                        javaMethodName, interfaceDeclaration.methodDescriptor),
                        function.getReturnType().orElse(BuiltinTypes.BASE_TYPE),
                        parameterTypes));
    }

    private static void createMethodBody(String internalClassName, String javaMethodName, Type[] javaParameterTypes,
            String methodDescriptorString,
            PythonCompiledFunction function, String interfaceInternalName, String interfaceDescriptor,
            MethodVisitor methodVisitor) {
        for (int i = 0; i < javaParameterTypes.length; i++) {
            methodVisitor.visitParameter("parameter" + i, 0);
        }
        methodVisitor.visitCode();

        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalClassName, javaMethodName, interfaceDescriptor);
        for (int i = 0; i < function.totalArgCount(); i++) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, i);
        }

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, interfaceInternalName, "invoke", methodDescriptorString, true);
        methodVisitor.visitInsn(Opcodes.ARETURN);

        methodVisitor.visitMaxs(-1, -1);

        methodVisitor.visitEnd();
    }

    public static Type getVirtualFunctionReturnType(PythonCompiledFunction function) {
        return Type.getType('L' + function.getReturnType().map(PythonLikeType::getJavaTypeInternalName)
                .orElseGet(() -> getPythonReturnTypeOfFunction(function, true).getJavaTypeInternalName()) + ';');
    }

    public static void createGetAttribute(ClassWriter classWriter, String classInternalName, String superInternalName,
            Collection<String> instanceAttributes,
            Map<String, PythonLikeType> fieldToType) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "__getAttributeOrNull",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                null, null);

        methodVisitor.visitParameter("attribute", 0);

        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        BytecodeSwitchImplementor.createStringSwitch(methodVisitor, instanceAttributes, 2, field -> {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, getJavaFieldName(field),
                    'L' + fieldToType.get(field).getJavaTypeInternalName() + ';');
            methodVisitor.visitInsn(Opcodes.ARETURN);
        }, () -> {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superInternalName, "__getAttributeOrNull",
                    Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                            Type.getType(String.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.ARETURN);
        }, true);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    public static void createSetAttribute(ClassWriter classWriter, String classInternalName, String superInternalName,
            Collection<String> instanceAttributes,
            Map<String, PythonLikeType> fieldToType) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "__setAttribute",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(String.class),
                        Type.getType(PythonLikeObject.class)),
                null, null);

        methodVisitor.visitParameter("attribute", 0);
        methodVisitor.visitParameter("value", 0);

        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        BytecodeSwitchImplementor.createStringSwitch(methodVisitor, instanceAttributes, 3, field -> {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldToType.get(field).getJavaTypeInternalName());
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, getJavaFieldName(field),
                    'L' + fieldToType.get(field).getJavaTypeInternalName() + ';');
            methodVisitor.visitInsn(Opcodes.RETURN);
        }, () -> {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superInternalName, "__setAttribute",
                    Type.getMethodDescriptor(Type.VOID_TYPE,
                            Type.getType(String.class),
                            Type.getType(PythonLikeObject.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.RETURN);
        }, true);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    public static void createDeleteAttribute(ClassWriter classWriter, String classInternalName, String superInternalName,
            Collection<String> instanceAttributes,
            Map<String, PythonLikeType> fieldToType) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "__deleteAttribute",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(String.class)),
                null, null);

        methodVisitor.visitParameter("attribute", 0);

        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        BytecodeSwitchImplementor.createStringSwitch(methodVisitor, instanceAttributes, 2, field -> {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, getJavaFieldName(field),
                    'L' + fieldToType.get(field).getJavaTypeInternalName() + ';');
            methodVisitor.visitInsn(Opcodes.RETURN);
        }, () -> {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superInternalName, "__deleteAttribute",
                    Type.getMethodDescriptor(Type.VOID_TYPE,
                            Type.getType(String.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.RETURN);
        }, true);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    public static void createCPythonOperationMethods(ClassWriter classWriter, String internalClassName,
            String superClassInternalName, Map<String, PythonLikeType> attributeNameToType) {
        createReadFromCPythonReference(classWriter, internalClassName, superClassInternalName, attributeNameToType);
        createWriteToCPythonReference(classWriter, internalClassName, superClassInternalName, attributeNameToType);
    }

    public static void createReadFromCPythonReference(ClassWriter classWriter, String internalClassName,
            String superClassInternalName, Map<String, PythonLikeType> attributeNameToType) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "$readFieldsFromCPythonReference",
                Type.getMethodDescriptor(Type.VOID_TYPE), null,
                null);
        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassInternalName,
                "$readFieldsFromCPythonReference",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(CPythonBackedPythonLikeObject.class),
                "$cpythonReference", Type.getDescriptor(OpaquePythonReference.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        Label ifReferenceIsNotNull = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifReferenceIsNotNull);
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitLabel(ifReferenceIsNotNull);
        for (String field : attributeNameToType.keySet()) {
            methodVisitor.visitInsn(Opcodes.DUP2);
            methodVisitor.visitLdcInsn(field);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(CPythonBackedPythonLikeObject.class),
                    "$instanceMap", Type.getDescriptor(Map.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CPythonBackedPythonInterpreter.class),
                    "lookupAttributeOnPythonReference",
                    Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                            Type.getType(OpaquePythonReference.class),
                            Type.getType(String.class),
                            Type.getType(Map.class)),
                    false);

            boolean isAssignableFromNone = false;

            try {
                isAssignableFromNone = attributeNameToType.get(field).getJavaClass().isAssignableFrom(PythonNone.class);
            } catch (ClassNotFoundException e) {
                // do nothing
            }

            Label ifFieldIsNone = new Label();
            Label doneSettingField = new Label();

            if (!isAssignableFromNone) {
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(PythonNone.class),
                        "INSTANCE", Type.getDescriptor(PythonNone.class));

                methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, ifFieldIsNone);
            }

            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, attributeNameToType.get(field).getJavaTypeInternalName());
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, getJavaFieldName(field),
                    "L" + attributeNameToType.get(field).getJavaTypeInternalName() + ";");

            if (!isAssignableFromNone) {
                methodVisitor.visitJumpInsn(Opcodes.GOTO, doneSettingField);

                methodVisitor.visitLabel(ifFieldIsNone);
                methodVisitor.visitInsn(Opcodes.POP);
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, getJavaFieldName(field),
                        "L" + attributeNameToType.get(field).getJavaTypeInternalName() + ";");
                methodVisitor.visitLabel(doneSettingField);
            }
        }
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    public static void createWriteToCPythonReference(ClassWriter classWriter, String internalClassName,
            String superClassInternalName, Map<String, PythonLikeType> attributeNameToType) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "$writeFieldsToCPythonReference",
                Type.getMethodDescriptor(Type.VOID_TYPE), null,
                null);
        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassInternalName,
                "$writeFieldsToCPythonReference",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(CPythonBackedPythonLikeObject.class),
                "$cpythonReference", Type.getDescriptor(OpaquePythonReference.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        Label ifReferenceIsNotNull = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifReferenceIsNotNull);
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitLabel(ifReferenceIsNotNull);

        methodVisitor.visitInsn(Opcodes.SWAP);
        for (String field : attributeNameToType.keySet()) {
            methodVisitor.visitInsn(Opcodes.DUP2);

            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, internalClassName, getJavaFieldName(field),
                    "L" + attributeNameToType.get(field).getJavaTypeInternalName() + ";");
            methodVisitor.visitLdcInsn(field);
            methodVisitor.visitInsn(Opcodes.SWAP);

            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CPythonBackedPythonInterpreter.class),
                    "setAttributeOnPythonReference",
                    Type.getMethodDescriptor(Type.VOID_TYPE,
                            Type.getType(OpaquePythonReference.class),
                            Type.getType(String.class),
                            Type.getType(Object.class)),
                    false);
        }
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    public static InterfaceDeclaration getInterfaceForFunctionSignature(FunctionSignature functionSignature) {
        return functionSignatureToInterfaceName.computeIfAbsent(functionSignature,
                PythonClassTranslator::createInterfaceForFunctionSignature);
    }

    public static InterfaceDeclaration getInterfaceForPythonFunction(PythonCompiledFunction pythonCompiledFunction) {
        String[] parameterTypes = new String[pythonCompiledFunction.totalArgCount()];
        List<PythonLikeType> parameterTypeAnnotations = pythonCompiledFunction.getParameterTypes();
        for (int i = 0; i < parameterTypeAnnotations.size(); i++) {
            parameterTypes[i] = 'L' + parameterTypeAnnotations.get(i).getJavaTypeInternalName() + ';';
        }

        String returnType = 'L' + pythonCompiledFunction.getReturnType()
                .orElseGet(() -> getPythonReturnTypeOfFunction(pythonCompiledFunction, false)).getJavaTypeInternalName() + ';';

        FunctionSignature functionSignature = new FunctionSignature(returnType, parameterTypes);
        return functionSignatureToInterfaceName.computeIfAbsent(functionSignature,
                PythonClassTranslator::createInterfaceForFunctionSignature);
    }

    public static InterfaceDeclaration
            getInterfaceForPythonFunctionIgnoringReturn(PythonCompiledFunction pythonCompiledFunction) {
        String[] parameterTypes = new String[pythonCompiledFunction.totalArgCount()];
        List<PythonLikeType> parameterTypeAnnotations = pythonCompiledFunction.getParameterTypes();

        for (int i = 0; i < parameterTypeAnnotations.size(); i++) {
            parameterTypes[i] = 'L' + parameterTypeAnnotations.get(i).getJavaTypeInternalName() + ';';
        }

        String returnType = 'L' + pythonCompiledFunction.getReturnType()
                .orElse(BuiltinTypes.BASE_TYPE).getJavaTypeInternalName() + ';';

        FunctionSignature functionSignature = new FunctionSignature(returnType, parameterTypes);
        return functionSignatureToInterfaceName.computeIfAbsent(functionSignature,
                PythonClassTranslator::createInterfaceForFunctionSignature);
    }

    public static InterfaceDeclaration getInterfaceForInstancePythonFunction(String instanceInternalClassName,
            PythonCompiledFunction pythonCompiledFunction) {
        List<PythonLikeType> parameterPythonTypeList = pythonCompiledFunction.getParameterTypes();
        String[] pythonParameterTypes = new String[pythonCompiledFunction.totalArgCount()];

        if (pythonParameterTypes.length > 0) {
            pythonParameterTypes[0] = 'L' + instanceInternalClassName + ';';
        }

        for (int i = 1; i < pythonCompiledFunction.totalArgCount(); i++) {
            pythonParameterTypes[i] = 'L' + parameterPythonTypeList.get(i).getJavaTypeInternalName() + ';';
        }
        String returnType = 'L' + pythonCompiledFunction.getReturnType().map(PythonLikeType::getJavaTypeInternalName)
                .orElseGet(() -> getPythonReturnTypeOfFunction(pythonCompiledFunction, true).getJavaTypeInternalName()) + ';';
        FunctionSignature functionSignature = new FunctionSignature(returnType, pythonParameterTypes);
        return functionSignatureToInterfaceName.computeIfAbsent(functionSignature,
                PythonClassTranslator::createInterfaceForFunctionSignature);
    }

    public static InterfaceDeclaration getInterfaceForClassPythonFunction(PythonCompiledFunction pythonCompiledFunction) {
        List<PythonLikeType> parameterPythonTypeList = pythonCompiledFunction.getParameterTypes();
        String[] pythonParameterTypes = new String[pythonCompiledFunction.totalArgCount()];

        if (pythonParameterTypes.length > 0) {
            pythonParameterTypes[0] = Type.getDescriptor(PythonLikeType.class);
        }

        for (int i = 1; i < pythonCompiledFunction.totalArgCount(); i++) {
            pythonParameterTypes[i] = 'L' + parameterPythonTypeList.get(i).getJavaTypeInternalName() + ';';
        }
        String returnType = 'L' + pythonCompiledFunction.getReturnType().map(PythonLikeType::getJavaTypeInternalName)
                .orElseGet(() -> getPythonReturnTypeOfFunction(pythonCompiledFunction, true).getJavaTypeInternalName()) + ';';
        FunctionSignature functionSignature = new FunctionSignature(returnType, pythonParameterTypes);
        return functionSignatureToInterfaceName.computeIfAbsent(functionSignature,
                PythonClassTranslator::createInterfaceForFunctionSignature);
    }

    public static InterfaceDeclaration createInterfaceForFunctionSignature(FunctionSignature functionSignature) {
        String maybeClassName = functionSignature.getClassName();
        int numberOfInstances =
                PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String className = maybeClassName;

        String internalClassName = className.replace('.', '/');

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(Opcodes.V11, Modifier.PUBLIC | Modifier.INTERFACE | Modifier.ABSTRACT, internalClassName, null,
                Type.getInternalName(Object.class), null);

        Type returnType = Type.getType(functionSignature.returnType);
        Type[] parameterTypes = new Type[functionSignature.parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = Type.getType(functionSignature.parameterTypes[i]);
        }

        classWriter.visitMethod(Modifier.PUBLIC | Modifier.ABSTRACT, "invoke",
                Type.getMethodDescriptor(returnType, parameterTypes), null, null);
        classWriter.visitEnd();
        PythonBytecodeToJavaBytecodeTranslator.writeClassOutput(BuiltinTypes.classNameToBytecode, className,
                classWriter.toByteArray());

        return new InterfaceDeclaration(internalClassName, Type.getMethodDescriptor(returnType, parameterTypes));
    }

    public static Class<?> getInterfaceClassForDeclaration(InterfaceDeclaration interfaceDeclaration) {
        try {
            return BuiltinTypes.asmClassLoader.loadClass(interfaceDeclaration.interfaceName.replaceAll("/", "."));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot load " + interfaceDeclaration.interfaceName +
                    " from the classloader; maybe it was not created?", e);
        }
    }

    private static FlowGraph createFlowGraph(PythonCompiledFunction pythonCompiledFunction, boolean isVirtual) {
        InterfaceDeclaration interfaceDeclaration = getInterfaceForPythonFunctionIgnoringReturn(pythonCompiledFunction);
        MethodDescriptor methodDescriptor = new MethodDescriptor(interfaceDeclaration.interfaceName.replace('.', '/'),
                MethodDescriptor.MethodType.INTERFACE,
                "invoke",
                interfaceDeclaration.methodDescriptor);
        LocalVariableHelper localVariableHelper =
                new LocalVariableHelper(methodDescriptor.getParameterTypes(), pythonCompiledFunction);
        List<Opcode> opcodeList = PythonBytecodeToJavaBytecodeTranslator.getOpcodeList(pythonCompiledFunction);
        StackMetadata initialStackMetadata = PythonBytecodeToJavaBytecodeTranslator.getInitialStackMetadata(localVariableHelper,
                methodDescriptor, isVirtual);
        FunctionMetadata functionMetadata = new FunctionMetadata();
        functionMetadata.functionType = PythonBytecodeToJavaBytecodeTranslator.getFunctionType(pythonCompiledFunction);
        functionMetadata.bytecodeCounterToLabelMap = new HashMap<>();
        functionMetadata.bytecodeCounterToCodeArgumenterList = new HashMap<>();
        functionMetadata.method = methodDescriptor;
        functionMetadata.pythonCompiledFunction = pythonCompiledFunction;
        functionMetadata.className = "";
        functionMetadata.methodVisitor = null;

        return FlowGraph.createFlowGraph(functionMetadata, initialStackMetadata, opcodeList);
    }

    public static Set<String> getReferencedSelfAttributes(PythonCompiledFunction pythonCompiledFunction) {
        FlowGraph flowGraph = createFlowGraph(pythonCompiledFunction, true);

        Set<String> referencedSelfAttributeSet = new HashSet<>();

        BiConsumer<AbstractOpcode, StackMetadata> attributeVisitor = (attributeOpcode, stackMetadata) -> {
            Set<Opcode> possibleSourceOpcodeSet = stackMetadata.getTOSValueSource().getPossibleSourceOpcodeSet();
            if (possibleSourceOpcodeSet.stream().anyMatch(opcode -> {
                if (opcode instanceof LoadFastOpcode || opcode instanceof StoreAttrOpcode
                        || opcode instanceof DeleteAttrOpcode) {
                    AbstractOpcode instructionOpcode = (AbstractOpcode) opcode;
                    return instructionOpcode.getInstruction().arg == 0;
                }
                if (opcode instanceof SelfOpcodeWithoutSource) {
                    return true;
                }
                return false;
            })) {
                referencedSelfAttributeSet.add(pythonCompiledFunction.co_names.get(attributeOpcode.getInstruction().arg));
            }
        };

        flowGraph.visitOperations(LoadAttrOpcode.class, attributeVisitor);
        flowGraph.visitOperations(StoreAttrOpcode.class, attributeVisitor);
        flowGraph.visitOperations(DeleteAttrOpcode.class, attributeVisitor);

        return referencedSelfAttributeSet;
    }

    public static PythonLikeType getPythonReturnTypeOfFunction(PythonCompiledFunction pythonCompiledFunction,
            boolean isVirtual) {
        try {
            if (PythonBytecodeToJavaBytecodeTranslator
                    .getFunctionType(pythonCompiledFunction) == PythonFunctionType.GENERATOR) {
                return BuiltinTypes.GENERATOR_TYPE;
            }

            FlowGraph flowGraph = createFlowGraph(pythonCompiledFunction, isVirtual);

            List<PythonLikeType> possibleReturnTypeList = new ArrayList<>();
            flowGraph.visitOperations(ReturnValueOpcode.class, (opcode, stackMetadata) -> {
                possibleReturnTypeList.add(stackMetadata.getTOSType());
            });

            return possibleReturnTypeList.stream()
                    .reduce(PythonLikeType::unifyWith)
                    .orElse(BuiltinTypes.NONE_TYPE);
        } catch (UnsupportedOperationException e) {
            // Return the base type if we encounter any unsupported operations
            return BuiltinTypes.BASE_TYPE;
        } catch (Exception e) {
            System.out.println("WARNING: Ignoring exception");
            //System.out.println("globals: " + pythonCompiledFunction.globalsMap);
            //System.out.println("co_constants: " + pythonCompiledFunction.co_constants);
            //System.out.println("co_names: " + pythonCompiledFunction.co_names);
            //System.out.println("co_varnames: " + pythonCompiledFunction.co_varnames);
            System.out.println("Instructions:");
            System.out.println(pythonCompiledFunction.instructionList.stream()
                    .map(PythonBytecodeInstruction::toString)
                    .collect(Collectors.joining("\n")));
            e.printStackTrace();
            return BuiltinTypes.BASE_TYPE;
        }
    }

    public static class InterfaceDeclaration {
        final String interfaceName;
        final String methodDescriptor;

        public InterfaceDeclaration(String interfaceName, String methodDescriptor) {
            this.interfaceName = interfaceName;
            this.methodDescriptor = methodDescriptor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InterfaceDeclaration that = (InterfaceDeclaration) o;
            return interfaceName.equals(that.interfaceName) && methodDescriptor.equals(that.methodDescriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(interfaceName, methodDescriptor);
        }
    }

    public static class FunctionSignature {
        final String returnType;
        final String[] parameterTypes;

        public FunctionSignature(String returnType, String... parameterTypes) {
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        public String getClassName() {
            return PythonBytecodeToJavaBytecodeTranslator.GENERATED_PACKAGE_BASE + "signature.InterfaceSignature";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FunctionSignature that = (FunctionSignature) o;
            return returnType.equals(that.returnType) && Arrays.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(returnType);
            result = 31 * result + Arrays.hashCode(parameterTypes);
            return result;
        }
    }

    public enum PythonMethodKind {
        VIRTUAL_METHOD,
        STATIC_METHOD,
        CLASS_METHOD;
    }
}
