package org.optaplanner.optapy.translator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.optapy.translator.implementors.CollectionImplementor;
import org.optaplanner.optapy.translator.implementors.DunderOperatorImplementor;
import org.optaplanner.optapy.translator.implementors.FunctionImplementor;
import org.optaplanner.optapy.translator.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.optapy.translator.implementors.JumpImplementor;
import org.optaplanner.optapy.translator.implementors.LocalVariableImplementor;
import org.optaplanner.optapy.translator.implementors.ObjectImplementor;
import org.optaplanner.optapy.translator.implementors.PythonBuiltinOperatorImplementor;
import org.optaplanner.optapy.translator.implementors.PythonConstantsImplementor;
import org.optaplanner.optapy.translator.implementors.StackManipulationImplementor;
import org.optaplanner.optapy.translator.types.PythonLikeDict;
import org.optaplanner.optapy.translator.types.PythonLikeList;
import org.optaplanner.optapy.translator.types.PythonLikeSet;
import org.optaplanner.optapy.translator.types.PythonLikeTuple;
import org.optaplanner.optapy.translator.types.PythonString;

import static org.optaplanner.optapy.PythonWrapperGenerator.writeClassOutput;

public class PythonBytecodeToJavaBytecodeTranslator {
    /**
     * The ASM generated bytecode. Used by
     * asmClassLoader to create the Java versions of Python methods
     */
    private static final Map<String, byte[]> classNameToBytecode = new HashMap<>();

    public static final String CONSTANTS_STATIC_FIELD_NAME = "co_consts";

    public static final String NAMES_STATIC_FIELD_NAME = "co_names";
    private static long generatedClassId = 0L;

    /**
     * A custom classloader that looks for the class in
     * classNameToBytecode
     */
    static ClassLoader asmClassLoader = new ClassLoader() {
        // getName() is an abstract method in Java 11 but not in Java 8
        public String getName() {
            return "OptaPlanner Gizmo Python Bytecode ClassLoader";
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            if (classNameToBytecode.containsKey(name)) {
                // Gizmo generated class
                byte[] byteCode = classNameToBytecode.get(name);
                return defineClass(name, byteCode, 0, byteCode.length);
            } else {
                // Not a Gizmo generated class; load from parent class loader
                return PythonBytecodeToJavaBytecodeTranslator.class.getClassLoader().loadClass(name);
            }
        }
    };

    private static Method getFunctionalInterfaceMethod(Class<?> interfaceClass) {
        List<Method> candidateList = new ArrayList<>();
        for (Method method : interfaceClass.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                candidateList.add(method);
            }
        }

        if (candidateList.isEmpty()) {
            throw new IllegalArgumentException("Class (" + interfaceClass.getName() + ") is not a functional interface: " +
                    "it has no abstract methods.");
        }

        if (candidateList.size() > 1) {
            throw new IllegalArgumentException("Class (" + interfaceClass.getName() + ") is not a functional interface: " +
                    "it has multiple abstract methods (" + candidateList + ").");
        }

        return candidateList.get(0);
    }

    @SuppressWarnings({ "unused", "unchecked" })
    public static <T> T translatePythonBytecode(PythonCompiledFunction pythonCompiledFunction,
            Class<T> javaFunctionalInterfaceType) {
        Method functionalMethod = getFunctionalInterfaceMethod(javaFunctionalInterfaceType);
        String className = "org.optaplanner.optapy.generated." + "function" + generatedClassId + ".GeneratedFunction";
        generatedClassId++;

        String internalClassName = className.replace('.', '/');
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, internalClassName, null, Type.getInternalName(Object.class),
                new String[] { Type.getInternalName(javaFunctionalInterfaceType) });
        Class<?>[] exceptionTypes = functionalMethod.getExceptionTypes();
        String[] exceptionNames = new String[exceptionTypes.length];
        for (int i = 0; i < exceptionTypes.length; i++) {
            exceptionNames[i] = Type.getInternalName(exceptionTypes[i]);
        }
        createConstructor(classWriter);
        createConstantsStaticField(classWriter);
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC,
                functionalMethod.getName(),
                Type.getMethodDescriptor(functionalMethod),
                null,
                exceptionNames);

        translatePythonBytecodeToMethod(functionalMethod, internalClassName, methodVisitor, pythonCompiledFunction);
        classWriter.visitEnd();

        writeClassOutput(classNameToBytecode, className, classWriter.toByteArray());

        try {
            Class<?> compiledClass = asmClassLoader.loadClass(className);
            setConstantsStaticField(compiledClass, pythonCompiledFunction);
            return (T) compiledClass.getConstructor().newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException
                | NoSuchMethodException e) {
            throw new IllegalStateException("Impossible State: Unable to create instance of generated class (" +
                    className + ") despite it being just generated.", e);
        }
    }

    private static void createConstructor(ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "<init>", "()V",
                null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>",
                "()V", false);
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    private static void createConstantsStaticField(ClassWriter classWriter) {
        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC,
                               CONSTANTS_STATIC_FIELD_NAME, Type.getDescriptor(List.class), null, null);
        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC,
                               NAMES_STATIC_FIELD_NAME, Type.getDescriptor(List.class), null, null);
    }

    private static void setConstantsStaticField(Class<?> compiledClass, PythonCompiledFunction pythonCompiledFunction) {
        try {
            compiledClass.getField(CONSTANTS_STATIC_FIELD_NAME).set(null, pythonCompiledFunction.co_constants);

            // Need to convert co_names to python strings (used in __getattribute__)
            List<PythonString> pythonNameList = new ArrayList<>(pythonCompiledFunction.co_names.size());
            for (String name : pythonCompiledFunction.co_names) {
                pythonNameList.add(PythonString.valueOf(name));
            }
            compiledClass.getField(NAMES_STATIC_FIELD_NAME).set(null, pythonNameList);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException("Impossible state: generated class (" + compiledClass +
                                                    ") does not have static field \"" + CONSTANTS_STATIC_FIELD_NAME + "\"", e);
        }
    }

    private static void translatePythonBytecodeToMethod(Method method, String className, MethodVisitor methodVisitor,
            PythonCompiledFunction pythonCompiledFunction) {
        for (Parameter parameter : method.getParameters()) {
            methodVisitor.visitParameter(parameter.getName(), 0);
        }
        methodVisitor.visitCode();

        Map<Integer, Label> bytecodeCounterToLabelMap = new HashMap<>();
        LocalVariableHelper localVariableHelper = new LocalVariableHelper(method.getParameters(), pythonCompiledFunction);

        for (int i = 0; i < localVariableHelper.parameters.length; i++) {
            JavaPythonTypeConversionImplementor.copyParameter(methodVisitor, localVariableHelper, i);
        }

        for (PythonBytecodeInstruction instruction : pythonCompiledFunction.instructionList) {
            translatePythonBytecodeInstruction(method, className, methodVisitor, pythonCompiledFunction, instruction,
                    bytecodeCounterToLabelMap, localVariableHelper);
        }
        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    private static void translatePythonBytecodeInstruction(Method method,
            String className,
            MethodVisitor methodVisitor,
            PythonCompiledFunction pythonCompiledFunction,
            PythonBytecodeInstruction instruction,
            Map<Integer, Label> bytecodeCounterToLabelMap,
            LocalVariableHelper localVariableHelper) {
        if (instruction.isJumpTarget) {
            Label label = bytecodeCounterToLabelMap.computeIfAbsent(instruction.offset, offset -> new Label());
            methodVisitor.visitLabel(label);
        }
        switch (instruction.opcode) {
            case NOP: { // use brackets to scope local variables
                methodVisitor.visitInsn(Opcodes.NOP);
                break;
            }
            case POP_TOP: {
                StackManipulationImplementor.popTOS(methodVisitor);
                break;
            }
            case ROT_TWO: {
                StackManipulationImplementor.swap(methodVisitor);
                break;
            }
            case ROT_THREE: {
                StackManipulationImplementor.rotateThree(methodVisitor);
                break;
            }
            case ROT_FOUR: {
                StackManipulationImplementor.rotateFour(methodVisitor, localVariableHelper);
                break;
            }
            case DUP_TOP: {
                StackManipulationImplementor.duplicateTOS(methodVisitor);
                break;
            }
            case DUP_TOP_TWO: {
                StackManipulationImplementor.duplicateTOSAndTOS1(methodVisitor);
                break;
            }
            case UNARY_POSITIVE:  {
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.POSITIVE);
                break;
            }
            case UNARY_NEGATIVE:  {
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEGATIVE);
                break;
            }
            case UNARY_NOT: {
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_BOOLEAN);
                PythonBuiltinOperatorImplementor.performNotOnTOS(methodVisitor);
                break;
            }
            case UNARY_INVERT: {
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.INVERT);
                break;
            }
            case GET_ITER: {
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.ITERATOR);
                break;
            }
            case GET_YIELD_FROM_ITER:
                break;
            case BINARY_POWER: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.POWER);
                break;
            }
            case BINARY_MULTIPLY: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.MULTIPLY);
                break;
            }
            case BINARY_MATRIX_MULTIPLY: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.MATRIX_MULTIPLY);
                break;
            }
            case BINARY_FLOOR_DIVIDE: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.FLOOR_DIVIDE);
                break;
            }
            case BINARY_TRUE_DIVIDE: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.TRUE_DIVIDE);
                break;
            }
            case BINARY_MODULO: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.MODULO);
                break;
            }
            case BINARY_ADD: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.ADD);
                break;
            }
            case BINARY_SUBTRACT: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.SUBTRACT);
                break;
            }
            case BINARY_SUBSCR: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.GET_ITEM);
                break;
            }
            case BINARY_LSHIFT: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.LSHIFT);
                break;
            }
            case BINARY_RSHIFT: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.RSHIFT);
                break;
            }
            case BINARY_AND: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.AND);
                break;
            }
            case BINARY_XOR: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.XOR);
                break;
            }
            case BINARY_OR: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.OR);
                break;
            }
            case INPLACE_POWER: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_POWER);
                break;
            }
            case INPLACE_MULTIPLY: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_MULTIPLY);
                break;
            }
            case INPLACE_MATRIX_MULTIPLY: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_MATRIX_MULTIPLY);
                break;
            }
            case INPLACE_FLOOR_DIVIDE: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_FLOOR_DIVIDE);
                break;
            }
            case INPLACE_TRUE_DIVIDE: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_TRUE_DIVIDE);
                break;
            }
            case INPLACE_MODULO: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_MODULO);
                break;
            }
            case INPLACE_ADD: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_ADD);
                break;
            }
            case INPLACE_SUBTRACT: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_SUBTRACT);
                break;
            }
            case INPLACE_LSHIFT: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_LSHIFT);
                break;
            }
            case INPLACE_RSHIFT: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_RSHIFT);
                break;
            }
            case INPLACE_AND: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_AND);
                break;
            }
            case INPLACE_XOR: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_XOR);
                break;
            }
            case INPLACE_OR: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.INPLACE_OR);
                break;
            }
            case STORE_SUBSCR: {
                CollectionImplementor.setItem(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case DEL_SUBSCR: {
                DunderOperatorImplementor.binaryOperator(methodVisitor, PythonBinaryOperators.DELETE_ITEM);
                break;
            }
            case GET_AWAITABLE:
                break;
            case GET_AITER:
                break;
            case GET_ANEXT:
                break;
            case END_ASYNC_FOR:
                break;
            case BEFORE_ASYNC_WITH:
                break;
            case SETUP_ASYNC_WITH:
                break;
            case PRINT_EXPR:
                break;
            case SET_ADD:
            case LIST_APPEND: {
                // SET_ADD and LIST_APPEND have the same bytecode
                CollectionImplementor.collectionAdd(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case MAP_ADD: {
                CollectionImplementor.mapPut(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case RETURN_VALUE: {
                JavaPythonTypeConversionImplementor.returnValue(methodVisitor, method);
                break;
            }
            case YIELD_VALUE:
                break;
            case YIELD_FROM:
                break;
            case SETUP_ANNOTATIONS:
                break;
            case IMPORT_STAR:
                break;
            case POP_BLOCK:
                break;
            case POP_EXCEPT:
                break;
            case RERAISE:
                break;
            case WITH_EXCEPT_START:
                break;
            case LOAD_ASSERTION_ERROR:
                break;
            case LOAD_BUILD_CLASS:
                break;
            case SETUP_WITH:
                break;
            case UNPACK_SEQUENCE:
                break;
            case UNPACK_EX:
                break;
            case LOAD_ATTR: {
                ObjectImplementor.getAttribute(methodVisitor, className, instruction);
                break;
            }
            case STORE_ATTR: {
                ObjectImplementor.setAttribute(methodVisitor, className, instruction, localVariableHelper);
                break;
            }
            case DELETE_ATTR: {
                ObjectImplementor.deleteAttribute(methodVisitor, className, instruction);
                break;
            }
            case LOAD_CONST: {
                PythonConstantsImplementor.loadConstant(methodVisitor, className, instruction.arg);
                break;
            }
            case BUILD_TUPLE: {
                CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, instruction.arg);
                break;
            }
            case BUILD_LIST: {
                CollectionImplementor.buildCollection(PythonLikeList.class, methodVisitor, instruction.arg);
                break;
            }
            case BUILD_SET: {
                CollectionImplementor.buildCollection(PythonLikeSet.class, methodVisitor, instruction.arg);
                break;
            }
            case BUILD_MAP: {
                CollectionImplementor.buildMap(PythonLikeDict.class, methodVisitor, instruction.arg);
                break;
            }
            case BUILD_CONST_KEY_MAP: {
                CollectionImplementor.buildConstKeysMap(PythonLikeDict.class, methodVisitor, instruction.arg);
                break;
            }
            case BUILD_STRING: {
                CollectionImplementor.buildString(methodVisitor, instruction.arg);
                break;
            }
            case LIST_TO_TUPLE: {
                CollectionImplementor.convertListToTuple(methodVisitor);
                break;
            }
            case LIST_EXTEND:
            case SET_UPDATE: {
                // LIST_EXTEND and SET_UPDATE have the same bytecode
                CollectionImplementor.collectionAddAll(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case DICT_UPDATE: {
                CollectionImplementor.mapPutAll(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case DICT_MERGE: {
                CollectionImplementor.mapPutAllOnlyIfAllNewElseThrow(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case COMPARE_OP: {
                DunderOperatorImplementor.compareValues(methodVisitor, CompareOp.getOp(instruction.arg));
                break;
            }
            case IS_OP: {
                PythonBuiltinOperatorImplementor.isOperator(methodVisitor, instruction);
                break;
            }
            case CONTAINS_OP: {
                CollectionImplementor.containsOperator(methodVisitor, instruction);
                break;
            }
            case IMPORT_NAME:
                break;
            case IMPORT_FROM:
                break;
            case JUMP_FORWARD: {
                JumpImplementor.jumpRelative(methodVisitor, instruction, bytecodeCounterToLabelMap);
                break;
            }
            case POP_JUMP_IF_TRUE: {
                JumpImplementor.popAndJumpIfTrue(methodVisitor, instruction, bytecodeCounterToLabelMap);
                break;
            }
            case POP_JUMP_IF_FALSE: {
                JumpImplementor.popAndJumpIfFalse(methodVisitor, instruction, bytecodeCounterToLabelMap);
                break;
            }
            case JUMP_IF_NOT_EXC_MATCH:
                break;
            case JUMP_IF_TRUE_OR_POP: {
                JumpImplementor.jumpIfTrueElsePop(methodVisitor, instruction, bytecodeCounterToLabelMap);
                break;
            }
            case JUMP_IF_FALSE_OR_POP: {
                JumpImplementor.jumpIfFalseElsePop(methodVisitor, instruction, bytecodeCounterToLabelMap);
                break;
            }
            case JUMP_ABSOLUTE: {
                JumpImplementor.jumpAbsolute(methodVisitor, instruction, bytecodeCounterToLabelMap);
                break;
            }
            case FOR_ITER: {
                CollectionImplementor.iterateIterator(methodVisitor, instruction, bytecodeCounterToLabelMap);
                break;
            }
            case SETUP_FINALLY:
                break;

            case LOAD_NAME:
                break;
            case STORE_NAME:
                break;
            case DELETE_NAME:
                break;

            case LOAD_GLOBAL:
                break;
            case STORE_GLOBAL:
                break;
            case DELETE_GLOBAL:
                break;

            case LOAD_FAST: {
                LocalVariableImplementor.loadLocalVariable(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case STORE_FAST: {
                LocalVariableImplementor.storeInLocalVariable(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case DELETE_FAST:
                break;

            case LOAD_CLOSURE:
                break;

            case LOAD_DEREF:
                break;
            case STORE_DEREF:
                break;
            case DELETE_DEREF:
                break;

            case LOAD_CLASSDEREF:
                break;

            case RAISE_VARARGS:
                break;
            case CALL_FUNCTION: {
                FunctionImplementor.callFunction(methodVisitor, instruction);
                break;
            }
            case CALL_FUNCTION_KW: {
                FunctionImplementor.callFunctionWithKeywords(methodVisitor, instruction);
                break;
            }
            case CALL_FUNCTION_EX: {
                FunctionImplementor.callFunctionUnpack(methodVisitor, instruction);
                break;
            }
            case LOAD_METHOD: {
                FunctionImplementor.loadMethod(methodVisitor, className, pythonCompiledFunction, instruction);
                break;
            }
            case CALL_METHOD: {
                FunctionImplementor.callMethod(methodVisitor, instruction, localVariableHelper);
                break;
            }
            case MAKE_FUNCTION:
                break;
            case BUILD_SLICE:
                break;
            case EXTENDED_ARG:
                break;
            case FORMAT_VALUE:
                break;
            default:
                throw new UnsupportedOperationException("Opcode not implemented: " + instruction.opname);
        }
    }
}
