package org.optaplanner.optapy;

import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.objectweb.asm.Type;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class PythonWrapperGenerator {
    /**
     * The Gizmo generated bytecode. Used by
     * gizmoClassLoader when not run in Quarkus
     * in order to create an instance of the Member
     * Accessor
     */
    private static final Map<String, byte[]> classNameToBytecode = new HashMap<>();
    private static Function<Number, List<Number>> pythonArrayIdToIdArray;
    private static BiFunction<Number, String, Object> pythonObjectIdAndAttributeNameToValue;
    private static TriFunction<Number, String, Object, Object> pythonObjectIdAndAttributeSetter;

    public static Object getValueFromPythonObject(Number objectId, String attributeName) {
        return pythonObjectIdAndAttributeNameToValue.apply(objectId, attributeName);
    }

    public static void setValueOnPythonObject(Number objectId, String attributeName, Object value) {
        pythonObjectIdAndAttributeSetter.apply(objectId, attributeName, value);
    }

    public static void setPythonArrayIdToIdArray(Function<Number, List<Number>> function) {
        pythonArrayIdToIdArray = function;
    }

    public static void setPythonObjectIdAndAttributeNameToValue(BiFunction<Number, String, Object> function) {
        pythonObjectIdAndAttributeNameToValue = function;
    }

    public static void setPythonObjectIdAndAttributeSetter(
            TriFunction<Number, String, Object, Object> setter) {
        pythonObjectIdAndAttributeSetter = setter;
    }

    public static Number getPythonObjectId(PythonObject pythonObject) {
        Number out = pythonObject.get__optapy_Id();
        return out;
    }

    public static PythonObject getPythonObject(PythonObject parent, Number id) {
        return parent.get__optapy_ObjectMap().get(id);
    }

    static final String pythonBindingFieldName = "__optaplannerPythonValue";
    static final String valueToInstanceMapFieldName = "__optaplannerPythonValueToInstanceMap";

    /**
     * A custom classloader that looks for the class in
     * classNameToBytecode
     */
    static ClassLoader gizmoClassLoader = new ClassLoader() {
        // getName() is an abstract method in Java 11 but not in Java 8
        public String getName() {
            return "OptaPlanner Gizmo Python Wrapper ClassLoader";
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            if (classNameToBytecode.containsKey(name)) {
                // Gizmo generated class
                byte[] byteCode = classNameToBytecode.get(name);
                return defineClass(name, byteCode, 0, byteCode.length);
            } else {
                // Not a Gizmo generated class; load from parent class loader
                return PythonWrapperGenerator.class.getClassLoader().loadClass(name);
            }
        }
    };

    public static Class<?> getArrayClass(Class<?> elementClass) {
        return Array.newInstance(elementClass, 0).getClass();
    }

    public static <T> T wrap(Class<T> javaClass, Number object) {
        return wrap(javaClass, object, new HashMap<>());
    }

    public static <T> T wrap(Class<T> javaClass, Number object, Map<Number, Object> valueObjectMap) {
        if (object == null) {
            return null;
        }
        if (valueObjectMap.containsKey(object)) {
            return (T) valueObjectMap.get(object);
        }
        try {
            if (javaClass.isArray()) {
                List<Number> itemIds = pythonArrayIdToIdArray.apply(object);
                int length = itemIds.size();
                Object out = Array.newInstance(javaClass.getComponentType(), length);
                for (int i = 0; i < length; i++) {
                    Array.set(out, i, wrap(javaClass.getComponentType(), itemIds.get(i), valueObjectMap));
                }
                return (T) out;
            } else {
                T out = (T) javaClass.getConstructor(Number.class, Map.class).newInstance(object, valueObjectMap);
                valueObjectMap.put(object, out);
                return out;
            }
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Supplier<Object> wrapObject(Class<?> javaClass, Number object) {
        Object out = wrap(javaClass, object);
        return () -> out;
    }

    public static Class<?> defineConstraintProviderClass(String className, Function<ConstraintProvider, Constraint[]> defineConstraintsImpl) {
        className = "org.optaplanner.optapy.generated." + className + ".GeneratedClass";
        if (classNameToBytecode.containsKey(className)) {
            try {
                return gizmoClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = (path, byteCode) -> {
            classBytecodeHolder.set(byteCode);
        };
        FieldDescriptor valueField;
        try(ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(ConstraintProvider.class)
                .classOutput(classOutput)
                .build()) {
            valueField = classCreator.getFieldCreator("__defineConstraintsImpl", Function.class)
                    .setModifiers(Modifier.STATIC | Modifier.PUBLIC)
                    .getFieldDescriptor();
            MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(ConstraintProvider.class,
                    "defineConstraints", Constraint[].class, ConstraintFactory.class));

            ResultHandle pythonProxy = methodCreator.readStaticField(valueField);
            ResultHandle constraints = methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Function.class, "apply", Object.class, Object.class),
                    pythonProxy, methodCreator.getMethodParam(0));
            methodCreator.returnValue(constraints);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        classNameToBytecode.put(className, classBytecodeHolder.get());
        try {
            Class<?> out = gizmoClassLoader.loadClass(className);
            out.getField(valueField.getName()).set(null, defineConstraintsImpl);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible State: the class (" + className + ") should exists since it was just created");
        }
    }

    public static Class<?> definePlanningEntityClass(String className, List<List<Object>> optaplannerMethodAnnotations) {
        className = "org.optaplanner.optapy.generated." + className + ".GeneratedClass";
        if (classNameToBytecode.containsKey(className)) {
            try {
                return gizmoClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = (path, byteCode) -> {
            classBytecodeHolder.set(byteCode);
        };
        try(ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(PythonObject.class)
                .classOutput(classOutput)
                .build()) {
            classCreator.addAnnotation(PlanningEntity.class);
            FieldDescriptor valueField = classCreator.getFieldCreator(pythonBindingFieldName, Number.class)
                    .setModifiers(Modifier.PUBLIC).getFieldDescriptor();
            generateWrapperMethods(classCreator, valueField, optaplannerMethodAnnotations);
        }
        classNameToBytecode.put(className, classBytecodeHolder.get());
        try {
            return gizmoClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Impossible State: the class (" + className + ") should exists since it was just created");
        }
    }

    public static Class<?> defineProblemFactClass(String className, List<List<Object>> optaplannerMethodAnnotations) {
        className = "org.optaplanner.optapy.generated." + className + ".GeneratedClass";
        if (classNameToBytecode.containsKey(className)) {
            try {
                return gizmoClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = (path, byteCode) -> {
            classBytecodeHolder.set(byteCode);
        };
        try(ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(PythonObject.class)
                .classOutput(classOutput)
                .build()) {
            FieldDescriptor valueField = classCreator.getFieldCreator(pythonBindingFieldName, Number.class)
                    .setModifiers(Modifier.PUBLIC).getFieldDescriptor();
            generateWrapperMethods(classCreator, valueField, optaplannerMethodAnnotations);
        }
        classNameToBytecode.put(className, classBytecodeHolder.get());
        try {
            return gizmoClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Impossible State: the class (" + className + ") should exists since it was just created");
        }
    }

    public static Class<?> definePlanningSolutionClass(String className, List<List<Object>> optaplannerMethodAnnotations) {
        className = "org.optaplanner.optapy.generated." + className + ".GeneratedClass";
        if (classNameToBytecode.containsKey(className)) {
            try {
                return gizmoClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = (path, byteCode) -> {
            classBytecodeHolder.set(byteCode);
        };
        try(ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(PythonObject.class)
                .classOutput(classOutput)
                .build()) {
            classCreator.addAnnotation(PlanningSolution.class)
                .addValue("solutionCloner", Type.getType(PythonPlanningSolutionCloner.class));
            FieldDescriptor valueField = classCreator.getFieldCreator(pythonBindingFieldName, Number.class)
                    .setModifiers(Modifier.PUBLIC).getFieldDescriptor();
            generateWrapperMethods(classCreator, valueField, optaplannerMethodAnnotations);
        }
        classNameToBytecode.put(className, classBytecodeHolder.get());
        try {
            return gizmoClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Impossible State: the class (" + className + ") should exists since it was just created");
        }
    }

    private static void print(MethodCreator methodCreator, ResultHandle toPrint) {
        ResultHandle out = methodCreator.readStaticField(FieldDescriptor.of(System.class, "out", PrintStream.class));
        methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(PrintStream.class, "println", void.class, Object.class),
                out, toPrint);
    }

    private static void generateAsPointer(ClassCreator classCreator, FieldDescriptor valueField) {
        MethodCreator methodCreator = classCreator.getMethodCreator("get__optapy_Id", Number.class);
        ResultHandle valueResultHandle = methodCreator.readInstanceField(valueField, methodCreator.getThis());
        methodCreator.returnValue(valueResultHandle);

        methodCreator = classCreator.getMethodCreator("get__optapy_ObjectMap", Map.class);
        valueResultHandle = methodCreator.readInstanceField(FieldDescriptor.of(classCreator.getClassName(), valueToInstanceMapFieldName, Map.class), methodCreator.getThis());
        methodCreator.returnValue(valueResultHandle);
    }

    private static void generateWrapperMethods(ClassCreator classCreator, FieldDescriptor valueField, List<List<Object>> optaplannerMethodAnnotations) {
        generateAsPointer(classCreator, valueField);

        List<FieldDescriptor> fieldDescriptorList = new ArrayList<>(optaplannerMethodAnnotations.size());
        List<Class<?>> returnTypeList = new ArrayList<>(optaplannerMethodAnnotations.size());
        for (int i = 0; i < optaplannerMethodAnnotations.size(); i++) {
            String methodName = (String) (optaplannerMethodAnnotations.get(i).get(0));
            Class<?> returnType = (Class<?>) (optaplannerMethodAnnotations.get(i).get(1));
            if (returnType == null) {
                returnType = Object.class;
            }
            List<Map<String, Object>> annotations = (List<Map<String, Object>>) optaplannerMethodAnnotations.get(i).get(2);
            fieldDescriptorList.add(generateWrapperMethod(classCreator, valueField, methodName, returnType, annotations, returnTypeList));
        }
        createConstructor(classCreator, valueField, fieldDescriptorList, returnTypeList);
    }

    private static void createConstructor(ClassCreator classCreator, FieldDescriptor valueField, List<FieldDescriptor> fieldDescriptorList,
            List<Class<?>> returnTypeList) {
        FieldDescriptor mapField = classCreator.getFieldCreator(valueToInstanceMapFieldName, Map.class).getFieldDescriptor();

        MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName(), Number.class, Map.class));
        methodCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), methodCreator.getThis());
        ResultHandle value = methodCreator.getMethodParam(0);
        methodCreator.writeInstanceField(valueField, methodCreator.getThis(), value);
        ResultHandle map = methodCreator.getMethodParam(1);
        methodCreator.writeInstanceField(mapField, methodCreator.getThis(), map);

        for (int i = 0; i < fieldDescriptorList.size(); i++) {
            FieldDescriptor fieldDescriptor = fieldDescriptorList.get(i);
            Class returnType = returnTypeList.get(i);
            String methodName = fieldDescriptor.getName().substring(0, fieldDescriptor.getName().length() - 6);

            ResultHandle outResultHandle = methodCreator.invokeStaticMethod(MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "getValueFromPythonObject", Object.class, Number.class, String.class),
                    value, methodCreator.load(methodName));
            if ( Comparable.class.isAssignableFrom(returnType) ) {
                methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(), outResultHandle);
            } else {
                methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(),
                        methodCreator.invokeStaticMethod(MethodDescriptor.ofMethod(PythonWrapperGenerator.class,
                            "wrap", Object.class, Class.class, Number.class, Map.class),
                            methodCreator.loadClass(returnType), outResultHandle,
                            map));
            }
        }
        methodCreator.returnValue(methodCreator.getThis());
    }

    private static FieldDescriptor generateWrapperMethod(ClassCreator classCreator, FieldDescriptor valueField, String methodName, Class<?> returnType, List<Map<String, Object>> annotations,
            List<Class<?>> returnTypeList) {
        for (Map<String,Object> annotation : annotations) {
            if (PlanningId.class.isAssignableFrom((Class<?>) annotation.get("annotationType")) && !Comparable.class.isAssignableFrom(returnType)) {
                returnType = Comparable.class;
            } else if ((ProblemFactCollectionProperty.class.isAssignableFrom((Class<?>) annotation.get("annotationType")) || PlanningEntityCollectionProperty.class.isAssignableFrom((Class<?>) annotation.get("annotationType"))) && !(Collection.class.isAssignableFrom(returnType) || returnType.isArray())) {
                returnType = Object[].class;
            }
        }
        returnTypeList.add(returnType);
        FieldDescriptor fieldDescriptor = classCreator.getFieldCreator(methodName + "$field", returnType).getFieldDescriptor();
        MethodCreator methodCreator = classCreator.getMethodCreator(methodName, returnType);
        for (Map<String,Object> annotation : annotations) {
            AnnotationCreator annotationCreator = methodCreator.addAnnotation((Class<?>) annotation.get("annotationType"));
            for (Method method : ((Class<?>) annotation.get("annotationType")).getMethods()) {
                if (method.getParameterCount() != 0 || !method.getDeclaringClass().equals((Class<?>) annotation.get("annotationType"))) {
                    continue;
                }
                Object annotationValue = annotation.get(method.getName());
                if (annotationValue != null) {
                    annotationCreator.addValue(method.getName(), annotationValue);
                }
            }
        }
        methodCreator.returnValue(methodCreator.readInstanceField(fieldDescriptor, methodCreator.getThis()));

        if (methodName.startsWith("get")) {
            String setterMethodName = "set" + methodName.substring(3);
            MethodCreator setterMethodCreator = classCreator.getMethodCreator(setterMethodName, void.class, returnType);

            setterMethodCreator.invokeStaticMethod(MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "setValueOnPythonObject", void.class, Number.class, String.class, Object.class),
                    setterMethodCreator.readInstanceField(valueField, setterMethodCreator.getThis()),
                    setterMethodCreator.load(setterMethodName),
                    setterMethodCreator.getMethodParam(0));
            setterMethodCreator.writeInstanceField(fieldDescriptor, setterMethodCreator.getThis(), setterMethodCreator.getMethodParam(0));
            setterMethodCreator.returnValue(null);
        }
        return fieldDescriptor;
    }
}
