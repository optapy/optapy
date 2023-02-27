package org.optaplanner.optapy;

import static org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator.writeClassOutput;
import static org.optaplanner.jpyinterpreter.types.BuiltinTypes.asmClassLoader;
import static org.optaplanner.jpyinterpreter.types.BuiltinTypes.classNameToBytecode;

import java.io.PrintStream;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.objectweb.asm.Type;
import org.optaplanner.core.api.domain.entity.PinningFilter;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningEntityProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.domain.valuerange.ValueRange;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.AnchorShadowVariable;
import org.optaplanner.core.api.domain.variable.CustomShadowVariable;
import org.optaplanner.core.api.domain.variable.IndexShadowVariable;
import org.optaplanner.core.api.domain.variable.InverseRelationShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningListVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.optaplanner.core.api.score.calculator.IncrementalScoreCalculator;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.jpyinterpreter.CPythonBackedPythonInterpreter;
import org.optaplanner.jpyinterpreter.PythonClassTranslator;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;
import org.optaplanner.jpyinterpreter.types.wrappers.PythonObjectWrapper;

import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.WhileLoop;

public class PythonWrapperGenerator {
    // These functions are set in Python code
    // Maps a OpaquePythonReference to a unique, numerical id
    static Function<OpaquePythonReference, Number> pythonObjectToId;

    private static Function<OpaquePythonReference, String> pythonObjectToString;

    private static Function<OpaquePythonReference, Class<?>> pythonGetJavaClass;

    // Maps a OpaquePythonReference that represents an array of objects to a list of its values
    private static Function<OpaquePythonReference, List<OpaquePythonReference>> pythonArrayIdToIdArray;

    // Maps a OptaquePythonReference that represents an array of primitive types to their values
    private static Function<OpaquePythonReference, List<Object>> pythonArrayToJavaList;

    // Reads an attribute on a OpaquePythonReference
    private static BiFunction<OpaquePythonReference, String, Object> pythonObjectIdAndAttributeNameToValue;

    // Sets an attribute on a OpaquePythonReference
    public static TriFunction<OpaquePythonReference, String, Object, Object> pythonObjectIdAndAttributeSetter;

    // These functions are used in Python to set fields to the corresponding Python function
    @SuppressWarnings("unused")
    public static void setPythonObjectToString(Function<OpaquePythonReference, String> pythonObjectToString) {
        PythonWrapperGenerator.pythonObjectToString = pythonObjectToString;
    }

    @SuppressWarnings("unused")
    public static void setPythonGetJavaClass(Function<OpaquePythonReference, Class<?>> pythonGetJavaClass) {
        PythonWrapperGenerator.pythonGetJavaClass = pythonGetJavaClass;
    }

    @SuppressWarnings("unused")
    public static void setPythonObjectToId(Function<OpaquePythonReference, Number> pythonObjectToId) {
        PythonWrapperGenerator.pythonObjectToId = pythonObjectToId;
    }

    @SuppressWarnings("unused")
    public static Object getValueFromPythonObject(OpaquePythonReference objectId, String attributeName) {
        return pythonObjectIdAndAttributeNameToValue.apply(objectId, attributeName);
    }

    @SuppressWarnings("unused")
    public static void setValueOnPythonObject(OpaquePythonReference objectId, String attributeName, Object value) {
        pythonObjectIdAndAttributeSetter.apply(objectId, attributeName, value);
    }

    @SuppressWarnings("unused")
    public static void setListValueOnPythonObject(OpaquePythonReference objectId, String attributeName, List javaList,
            Map<Number, Object> idMap, TriFunction updatePythonValue) {
        if (javaList instanceof PythonObject) {
            ((PythonObject) javaList).forceUpdate();
        }
        pythonObjectIdAndAttributeSetter.apply(objectId, attributeName, javaList);
    }

    @SuppressWarnings("unused")
    public static void visitListIdOnPythonObject(OpaquePythonReference objectId, String getterName, List value,
            Map referenceMap) {
        OpaquePythonReference listPythonReference = (OpaquePythonReference) getValueFromPythonObject(objectId, getterName);
        referenceMap.put(CPythonBackedPythonInterpreter.getPythonReferenceId(listPythonReference), value);
    }

    @SuppressWarnings("unused")
    public static void setPythonArrayIdToIdArray(Function<OpaquePythonReference, List<OpaquePythonReference>> function) {
        pythonArrayIdToIdArray = function;
    }

    @SuppressWarnings("unused")
    public static void setPythonArrayToJavaList(Function<OpaquePythonReference, List<Object>> function) {
        pythonArrayToJavaList = function;
    }

    @SuppressWarnings("unused")
    public static void setPythonObjectIdAndAttributeNameToValue(BiFunction<OpaquePythonReference, String, Object> function) {
        pythonObjectIdAndAttributeNameToValue = function;
    }

    @SuppressWarnings("unused")
    public static void setPythonObjectIdAndAttributeSetter(
            TriFunction<OpaquePythonReference, String, Object, Object> setter) {
        pythonObjectIdAndAttributeSetter = setter;
    }

    @SuppressWarnings("unused")
    public static String getPythonObjectString(OpaquePythonReference pythonObject) {
        return pythonObjectToString.apply(pythonObject);
    }

    @SuppressWarnings("unused")
    public static OpaquePythonReference getPythonObject(PythonObject pythonObject) {
        return pythonObject.get__optapy_Id();
    }

    public static OpaquePythonReference getPythonObject(PythonComparable pythonObject) {
        return pythonObject.reference;
    }

    @SuppressWarnings("unused")
    public static ClassLoader getClassLoaderForAliasMap(Map<String, Class<?>> aliasMap) {
        return new ClassLoader() {
            // getName() is an abstract method in Java 11 but not in Java 8
            public String getName() {
                return "OptaPy Alias Map ClassLoader";
            }

            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                if (aliasMap.containsKey(name)) {
                    // Gizmo generated class
                    return aliasMap.get(name);
                } else {
                    // Not a Gizmo generated class; load from parent class loader
                    return asmClassLoader.loadClass(name);
                }
            }
        };
    }

    public static Number getPythonObjectId(PythonObject pythonObject) {
        return pythonObjectToId.apply(pythonObject.get__optapy_Id());
    }

    @SuppressWarnings("unused") // used by variable listener/custom shadow variable on Python side
    public static void updateVariableFromPythonObject(PythonObject object, String variableName)
            throws IllegalAccessException, InvocationTargetException {
        Object newValue;
        try {
            newValue = getValueFromPythonObject(object.get__optapy_Id(), "get_" + variableName);
        } catch (OptaPyException e1) {
            try {
                newValue = getValueFromPythonObject(object.get__optapy_Id(),
                        "get" + Character.toUpperCase(variableName.charAt(0)) + variableName.substring(1));
            } catch (OptaPyException e2) {
                throw new IllegalArgumentException(
                        "Unable to find variable (" + variableName + ") on entity (" + object + ").");
            }
        }
        String javaSetter = "set" + Character.toUpperCase(variableName.charAt(0)) + variableName.substring(1);

        for (Method method : object.getClass().getMethods()) {
            if (method.getName().equals(javaSetter)) {
                // Need to check if (numeric) value is compatible with method parameter
                // (in particular, newValue will be a Long if it is integral, but if the Method
                //  expects an Integer it will throw an exception)
                if (newValue != null && method.getParameterTypes()[0].equals(Integer.class) && !(newValue instanceof Integer)) {
                    newValue = ((Number) newValue).intValue();
                }
                method.invoke(object, newValue);
                return;
            }
        }
        throw new IllegalArgumentException("Unable to find variable (" + variableName + ") on entity (" + object + ").");
    }

    @SuppressWarnings("unused")
    public static Class<?> getJavaClass(OpaquePythonReference object) {
        return pythonGetJavaClass.apply(object);
    }

    @SuppressWarnings("unused")
    public static Boolean wrapBoolean(boolean value) {
        return value;
    }

    @SuppressWarnings("unused")
    public static Byte wrapByte(byte value) {
        return value;
    }

    @SuppressWarnings("unused")
    public static Short wrapShort(short value) {
        return value;
    }

    @SuppressWarnings("unused")
    public static Integer wrapInt(int value) {
        return value;
    }

    @SuppressWarnings("unused")
    public static Long wrapLong(long value) {
        return value;
    }

    // Used in Python to get the array type of a class; used in
    // determining what class a @ProblemFactCollection / @PlanningEntityCollection
    // should be
    @SuppressWarnings("unused")
    public static Class<?> getArrayClass(Class<?> elementClass) {
        return Array.newInstance(elementClass, 0).getClass();
    }

    @SuppressWarnings("unused")
    public static <T> String getCollectionSignature(Class<?> collectionClass, Class<T> elementClass) {
        StringBuilder out = new StringBuilder();
        out.append('L').append(Type.getInternalName(collectionClass)); // Return is of class Collection
        out.append("<"); // Collection is of generic type...
        out.append('L').append(Type.getInternalName(elementClass)); // The collection type
        out.append(";>;"); // end of signature
        String result = out.toString();
        return result;
    }

    // Holds the OpaquePythonReference
    static final String PYTHON_BINDING_FIELD_NAME = "__optaplannerPythonValue";
    static final String REFERENCE_MAP_FIELD_NAME = "__optaplannerReferenceMap";

    static final String PYTHON_SETTER_FIELD_NAME = "_optaplannerPythonSetter";

    static final String PYTHON_LIKE_VALUE_MAP_FIELD_NAME = "__optaplannerPythonLikeValueCacheMap";
    static final String PYTHON_LIKE_TYPE_FIELD_NAME = "$TYPE";

    static final TriFunction<OpaquePythonReference, String, Object, Object> NONE_PYTHON_SETTER = (a, b, c) -> null;

    private static <T> T wrapArray(Class<T> javaClass, OpaquePythonReference object, Number id, Map<Number, Object> map,
            TriFunction<OpaquePythonReference, String, Object, Object> pythonSetter) {
        // If the class is an array, we need to extract
        // its elements from the OpaquePythonReference
        if (Comparable.class.isAssignableFrom(javaClass.getComponentType()) ||
                Number.class.isAssignableFrom(javaClass.getComponentType())) {
            List<Object> items = pythonArrayToJavaList.apply(object);
            int length = items.size();
            Object out = Array.newInstance(javaClass.getComponentType(), length);

            // Put the array into the python id to java instance map
            map.put(id, out);

            // Set the elements of the array to the wrapped python items
            for (int i = 0; i < length; i++) {
                Object item = items.get(i);
                if (javaClass.getComponentType().equals(Integer.class) && item instanceof Long) {
                    item = ((Long) item).intValue();
                }
                Array.set(out, i, item);
            }
            return (T) out;
        }
        List<OpaquePythonReference> itemIds = pythonArrayIdToIdArray.apply(object);
        int length = itemIds.size();
        Object out = Array.newInstance(javaClass.getComponentType(), length);

        // Put the array into the python id to java instance map
        map.put(id, out);

        // Set the elements of the array to the wrapped python items
        for (int i = 0; i < length; i++) {
            Array.set(out, i, wrap(javaClass.getComponentType(), itemIds.get(i), map, pythonSetter));
        }
        return (T) out;
    }

    public static <T> T wrapCollection(OpaquePythonReference object, Number id, Map<Number, Object> map,
            TriFunction<OpaquePythonReference, String, Object, Object> pythonSetter) {
        PythonList out = new PythonList(object, id, map, pythonSetter);
        map.put(id, out);
        return (T) out;
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrap(Class<T> javaClass, OpaquePythonReference object, Map<Number, Object> map,
            TriFunction<OpaquePythonReference, String, Object, Object> pythonSetter) {
        if (object == null) {
            return null;
        }

        // Check to see if we already created the object
        Number id = pythonObjectToId.apply(object);
        if (map.containsKey(id)) {
            return (T) map.get(id);
        }

        try {
            if (javaClass.isArray()) {
                return wrapArray(javaClass, object, id, map, pythonSetter);
            } else if (javaClass.isAssignableFrom(List.class)) {
                return wrapCollection(object, id, map, pythonSetter);
            } else if (javaClass.isAssignableFrom(OpaquePythonReference.class)) {
                // Don't wrap OpaquePythonReference if it is a pointer to an OpaquePythonReference
                return (T) object;
            } else {
                // Create a new instance of the Java Class. Its constructor will put the instance into the map
                return javaClass.getConstructor(OpaquePythonReference.class, Number.class, Map.class,
                        TriFunction.class).newInstance(object, id, map, pythonSetter);
            }
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new IllegalStateException("Error occurred when wrapping object (" + getPythonObjectString(object) + ")", e);
        }
    }

    private static ClassOutput getClassOutput(AtomicReference<byte[]> bytesReference) {
        return (path, byteCode) -> {
            bytesReference.set(byteCode);
        };
    }

    /**
     * Creates a class that looks like this:
     *
     * class JavaWrapper implements NaryFunction<A0,A1,A2,...,AN> {
     * public static NaryFunction<A0,A1,A2,...,AN> delegate;
     *
     * #64;Override
     * public AN apply(A0 arg0, A1 arg1, ..., A(N-1) finalArg) {
     * return delegate.apply(arg0,arg1,...,finalArg);
     * }
     * }
     *
     * @param className The simple name of the generated class
     * @param baseInterface the base interface
     * @param delegate The Python function to delegate to
     * @return never null
     */
    @SuppressWarnings({ "unused", "unchecked" })
    public static <A> Class<? extends A> defineWrapperFunction(String className, Class<A> baseInterface,
            Object delegate) {
        Method[] interfaceMethods = baseInterface.getMethods();
        if (interfaceMethods.length != 1) {
            throw new IllegalArgumentException("Can only call this function for functional interfaces (only 1 method)");
        }
        if (classNameToBytecode.containsKey(className)) {
            try {
                return (Class<? extends A>) asmClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = getClassOutput(classBytecodeHolder);

        // holds the delegate (static; same one is reused; should be stateless)
        FieldDescriptor delegateField;
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(baseInterface)
                .classOutput(classOutput)
                .build()) {
            delegateField = classCreator.getFieldCreator("delegate", baseInterface)
                    .setModifiers(Modifier.STATIC | Modifier.PUBLIC)
                    .getFieldDescriptor();
            MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(interfaceMethods[0]));

            ResultHandle pythonProxy = methodCreator.readStaticField(delegateField);
            ResultHandle[] args = new ResultHandle[interfaceMethods[0].getParameterCount()];
            for (int i = 0; i < args.length; i++) {
                args[i] = methodCreator.getMethodParam(i);
            }
            ResultHandle constraints = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(interfaceMethods[0]),
                    pythonProxy, args);
            methodCreator.returnValue(constraints);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        writeClassOutput(classNameToBytecode, className, classBytecodeHolder.get());
        try {
            // Now that the class created, we need to set it static field to the delegate function
            Class<? extends A> out = (Class<? extends A>) asmClassLoader.loadClass(className);
            out.getField(delegateField.getName()).set(null, delegate);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible State: the class (" + className + ") should exists since it was just created");
        }
    }

    public static ValueRange getValueRangeProxy(Object proxy) {
        return (ValueRange) Proxy.newProxyInstance(proxy.getClass().getClassLoader(),
                new Class[] { ValueRange.class },
                Proxy.getInvocationHandler(proxy));
    }

    public static CountableValueRange getCountableValueRangeProxy(Object proxy) {
        return (CountableValueRange) Proxy.newProxyInstance(proxy.getClass().getClassLoader(),
                new Class[] { CountableValueRange.class },
                Proxy.getInvocationHandler(proxy));
    }

    /**
     * Creates a class that looks like this:
     *
     * class JavaWrapper implements SomeInterface {
     * public static Supplier&lt;SomeInterface&gt; supplier;
     *
     * private SomeInterface delegate;
     *
     * public JavaWrapper() {
     * delegate = supplier.get(); classNameToBytecode.put(className, classBytecodeHolder.get());
     * }
     *
     * #64;Override
     * public Result interfaceMethod1(A0 arg0, A1 arg1, ..., A(N-1) finalArg) {
     * return delegate.interfaceMethod1(arg0,arg1,...,finalArg);
     * }
     *
     * #64;Override
     * public Result interfaceMethod2(A0 arg0, A1 arg1, ..., A(N-1) finalArg) {
     * return delegate.interfaceMethod2(arg0,arg1,...,finalArg);
     * }
     * }
     *
     * @param className The simple name of the generated class
     * @param baseInterface the base interface
     * @param delegateSupplier The Python class to delegate to
     * @return never null
     */
    @SuppressWarnings({ "unused", "unchecked" })
    public static <A> Class<? extends A> defineWrapperClass(String className, Class<? extends A> baseInterface,
            Supplier<? extends A> delegateSupplier) {
        Method[] interfaceMethods = baseInterface.getMethods();
        if (classNameToBytecode.containsKey(className)) {
            try {
                return (Class<? extends A>) asmClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = getClassOutput(classBytecodeHolder);

        // holds the supplier of the delegate (static)
        FieldDescriptor supplierField;

        // holds the delegate (instance; new one created for each instance)
        FieldDescriptor delegateField;
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(baseInterface)
                .classOutput(classOutput)
                .build()) {
            supplierField = classCreator.getFieldCreator("delegateSupplier", Supplier.class)
                    .setModifiers(Modifier.STATIC | Modifier.PUBLIC)
                    .getFieldDescriptor();
            delegateField = classCreator.getFieldCreator("delegate", baseInterface)
                    .setModifiers(Modifier.PUBLIC | Modifier.FINAL)
                    .getFieldDescriptor();

            MethodCreator constructorCreator =
                    classCreator.getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName()));
            constructorCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), constructorCreator.getThis());
            constructorCreator.writeInstanceField(delegateField, constructorCreator.getThis(),
                    constructorCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Supplier.class, "get", Object.class),
                            constructorCreator.readStaticField(supplierField)));
            constructorCreator.returnValue(constructorCreator.getThis());

            for (Method method : interfaceMethods) {
                MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(method));
                ResultHandle pythonProxy = methodCreator.readInstanceField(delegateField, methodCreator.getThis());
                ResultHandle[] args = new ResultHandle[method.getParameterCount()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = methodCreator.getMethodParam(i);
                }
                ResultHandle result = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(method),
                        pythonProxy, args);
                methodCreator.returnValue(result);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        writeClassOutput(classNameToBytecode, className, classBytecodeHolder.get());
        try {
            // Now that the class created, we need to set it static field to the supplier of the delegate
            Class<? extends A> out = (Class<? extends A>) asmClassLoader.loadClass(className);
            out.getField(supplierField.getName()).set(null, delegateSupplier);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible State: the class (" + className + ") should exists since it was just created");
        }
    }

    /**
     * Creates a class that looks like this:
     *
     * class PythonConstraintProvider implements ConstraintProvider {
     * public static Function<ConstraintFactory, Constraint[]> defineConstraintsImpl;
     *
     * &#64;Override
     * public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
     * return defineConstraintsImpl.apply(constraintFactory);
     * }
     * }
     *
     * @param className The simple name of the generated class
     * @param defineConstraintsImpl The Python function that return the list of constraints
     * @return never null
     */
    @SuppressWarnings("unused")
    public static Class<?> defineConstraintProviderClass(String className,
            ConstraintProvider defineConstraintsImpl) {
        return defineWrapperFunction(className, ConstraintProvider.class, defineConstraintsImpl);
    }

    /**
     * Creates a class that looks like this:
     *
     * class PythonEasyScoreCalculator implements EasyScoreCalculator {
     * public static EasyScoreCalculator easyScoreCalculatorImpl;
     *
     * &#64;Override
     * public Score calculateScore(Solution solution) {
     * return easyScoreCalculatorImpl.calculateScore(solution);
     * }
     * }
     *
     * @param className The simple name of the generated class
     * @param easyScoreCalculatorImpl The Python function that return the score for the solution
     * @return never null
     */
    @SuppressWarnings("unused")
    public static Class<?> defineEasyScoreCalculatorClass(String className,
            EasyScoreCalculator easyScoreCalculatorImpl) {
        return defineWrapperFunction(className, EasyScoreCalculator.class, easyScoreCalculatorImpl);
    }

    /**
     * Creates a class that looks like this:
     *
     * class PythonIncrementalScoreCalculator implements IncrementalScoreCalculator {
     * public static Supplier&lt;IncrementalScoreCalculator&gt; supplier;
     * public final IncrementalScoreCalculator delegate;
     *
     * public PythonIncrementalScoreCalculator() {
     * delegate = supplier.get();
     * }
     *
     * &#64;Override
     * public Score calculateScore(Solution solution) {
     * return delegate.calculateScore(solution);
     * }
     *
     * ...
     * }
     *
     * @param className The simple name of the generated class
     * @param incrementalScoreCalculatorSupplier A supplier that returns a new instance of the incremental score calculator on
     *        each call
     * @return never null
     */
    @SuppressWarnings("unused")
    public static Class<?> defineIncrementalScoreCalculatorClass(String className,
            Supplier<? extends IncrementalScoreCalculator> incrementalScoreCalculatorSupplier,
            boolean constraintMatchAware) {
        if (constraintMatchAware) {
            return defineWrapperClass(className, ConstraintMatchAwareIncrementalScoreCalculator.class,
                    (Supplier<ConstraintMatchAwareIncrementalScoreCalculator>) incrementalScoreCalculatorSupplier);
        }
        return defineWrapperClass(className, IncrementalScoreCalculator.class, incrementalScoreCalculatorSupplier);
    }

    private static FieldDescriptor getInheritedFieldDescriptor(ClassCreator classCreator, Class<?> parentClass,
            String fieldName, Class<?> fieldClass) {
        try {
            Field field = parentClass.getField(fieldName);
            return FieldDescriptor.of(field);
        } catch (NoSuchFieldException e) {
            return classCreator.getFieldCreator(fieldName, fieldClass).setModifiers(Modifier.PUBLIC).getFieldDescriptor();
        }
    }

    /**
     * Creates a class that looks like this:
     *
     * class PythonVariableListener implements VariableListener {
     * public static Supplier&lt;VariableListener&gt; supplier;
     * public final VariableListener delegate;
     *
     * public PythonVariableListener() {
     * delegate = supplier.get();
     * }
     *
     * public void afterVariableChange(scoreDirector, entity) {
     * delegate.afterVariableChange(scoreDirector, entity);
     * }
     * ...
     * }
     *
     * @param className The simple name of the generated class
     * @param variableListenerSupplier A supplier that returns a new instance of the variable listener on
     *        each call
     * @return never null
     */
    @SuppressWarnings("unused")
    public static Class<?> defineVariableListenerClass(String className,
            Supplier<? extends VariableListener> variableListenerSupplier) {
        return defineWrapperClass(className, VariableListener.class, variableListenerSupplier);
    }

    /*
     * The Planning Entity, Problem Fact, and Planning Solution classes look similar, with the only
     * difference being their top-level annotation. They all look like this:
     *
     * (none or @PlanningEntity or @PlanningSolution)
     * public class PojoForPythonObject implements PythonObject {
     * OpaquePythonReference __optaplannerPythonValue;
     * String string$field;
     * AnotherPojoForPythonObject otherObject$field;
     *
     * public PojoForPythonObject(OpaquePythonReference reference, Number id, Map<Number, PythonObject>
     * pythonIdToPythonObjectMap) {
     * this.__optaplannerPythonValue = reference;
     * pythonIdToPythonObjectMap.put(id, this);
     * string$field = PythonWrapperGenerator.getValueFromPythonObject(reference, "string");
     * OpaquePythonReference otherObjectReference = PythonWrapperGenerator.getValueFromPythonObject(reference, "otherObject");
     * otherObject$field = PythonWrapperGenerator.wrap(otherObjectReference, id, pythonIdToPythonObjectMap);
     * }
     *
     * public OpaquePythonReference get__optapy_Id() {
     * return __optaplannerPythonValue;
     * }
     *
     * public String getStringField() {
     * return string$field;
     * }
     *
     * public void setStringField(String val) {
     * PythonWrapperGenerator.setValueOnPythonObject(__optaplannerPythonValue, "string", val);
     * this.string$field = val;
     * }
     * // Repeat for otherObject
     * }
     */
    @SuppressWarnings("unused")
    public static Class<?> definePlanningEntityClass(String className, Class<?> parentClass,
            boolean defineEqualsAndHashcode,
            List<List<Object>> optaplannerMethodAnnotations,
            Map<String, Object> planningEntityAnnotations) {
        if (classNameToBytecode.containsKey(className)) {
            try {
                return asmClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = getClassOutput(classBytecodeHolder);
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .superClass(parentClass != null ? parentClass : Object.class)
                .interfaces(PythonObject.class)
                .classOutput(classOutput)
                .build()) {
            AnnotationCreator annotationCreator = classCreator.addAnnotation(PlanningEntity.class);
            Object pinningFilter = planningEntityAnnotations.get("pinningFilter");
            if (pinningFilter != null) {
                Class<? extends PinningFilter> pinningFilterClass = defineWrapperFunction(className + "PinningFilter",
                        PinningFilter.class, pinningFilter);
                annotationCreator.addValue("pinningFilter", pinningFilterClass);
            }

            FieldDescriptor valueField = getInheritedFieldDescriptor(classCreator, parentClass, PYTHON_BINDING_FIELD_NAME,
                    OpaquePythonReference.class);
            FieldDescriptor referenceMapField = classCreator.getFieldCreator(REFERENCE_MAP_FIELD_NAME, Map.class)
                    .setModifiers(Modifier.PUBLIC).getFieldDescriptor();
            FieldDescriptor pythonLikeValueMapField = getInheritedFieldDescriptor(classCreator, parentClass,
                    PYTHON_LIKE_VALUE_MAP_FIELD_NAME,
                    Map.class);
            FieldDescriptor pythonSetterField = getInheritedFieldDescriptor(classCreator, parentClass,
                    PYTHON_SETTER_FIELD_NAME, TriFunction.class);
            FieldDescriptor pythonLikeTypeField =
                    classCreator.getFieldCreator(PYTHON_LIKE_TYPE_FIELD_NAME, PythonLikeType.class)
                            .setModifiers(Modifier.PUBLIC | Modifier.STATIC).getFieldDescriptor();
            generateWrapperMethods(classCreator, parentClass, GeneratedClassType.PLANNING_ENTITY,
                    defineEqualsAndHashcode, valueField, referenceMapField,
                    pythonLikeValueMapField,
                    pythonSetterField,
                    pythonLikeTypeField,
                    optaplannerMethodAnnotations);
        }
        writeClassOutput(classNameToBytecode, className, classBytecodeHolder.get());
        return createAndInitializeClass(className);
    }

    @SuppressWarnings("unused")
    public static Class<?> defineProblemFactClass(String className, Class<?> parentClass,
            boolean defineEqualsAndHashcode,
            List<List<Object>> optaplannerMethodAnnotations) {
        if (classNameToBytecode.containsKey(className)) {
            try {
                return asmClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = getClassOutput(classBytecodeHolder);
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .superClass(parentClass != null ? parentClass : Object.class)
                .interfaces(PythonObject.class)
                .classOutput(classOutput)
                .build()) {
            FieldDescriptor valueField = getInheritedFieldDescriptor(classCreator, parentClass, PYTHON_BINDING_FIELD_NAME,
                    OpaquePythonReference.class);
            FieldDescriptor referenceMapField = classCreator.getFieldCreator(REFERENCE_MAP_FIELD_NAME, Map.class)
                    .setModifiers(Modifier.PUBLIC).getFieldDescriptor();
            FieldDescriptor pythonLikeValueMapField = getInheritedFieldDescriptor(classCreator, parentClass,
                    PYTHON_LIKE_VALUE_MAP_FIELD_NAME,
                    Map.class);
            FieldDescriptor pythonSetterField = getInheritedFieldDescriptor(classCreator, parentClass,
                    PYTHON_SETTER_FIELD_NAME, TriFunction.class);
            FieldDescriptor pythonLikeTypeField =
                    classCreator.getFieldCreator(PYTHON_LIKE_TYPE_FIELD_NAME, PythonLikeType.class)
                            .setModifiers(Modifier.PUBLIC | Modifier.STATIC).getFieldDescriptor();
            generateWrapperMethods(classCreator, parentClass, GeneratedClassType.PROBLEM_FACT, defineEqualsAndHashcode,
                    valueField, referenceMapField,
                    pythonLikeValueMapField, pythonSetterField, pythonLikeTypeField,
                    optaplannerMethodAnnotations);
        }
        writeClassOutput(classNameToBytecode, className, classBytecodeHolder.get());
        return createAndInitializeClass(className);
    }

    @SuppressWarnings("unused")
    public static Class<?> definePlanningSolutionClass(String className, Class<?> parentClass,
            boolean defineEqualsAndHashcode,
            List<List<Object>> optaplannerMethodAnnotations) {
        if (classNameToBytecode.containsKey(className)) {
            try {
                return asmClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = getClassOutput(classBytecodeHolder);
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .superClass(parentClass)
                .interfaces(PythonObject.class)
                .classOutput(classOutput)
                .build()) {
            classCreator.addAnnotation(PlanningSolution.class)
                    .addValue("solutionCloner", Type.getType(PythonPlanningSolutionCloner.class));
            FieldDescriptor valueField = getInheritedFieldDescriptor(classCreator, parentClass, PYTHON_BINDING_FIELD_NAME,
                    OpaquePythonReference.class);
            FieldDescriptor referenceMapField = classCreator.getFieldCreator(REFERENCE_MAP_FIELD_NAME, Map.class)
                    .setModifiers(Modifier.PUBLIC).getFieldDescriptor();
            FieldDescriptor pythonLikeValueMapField = classCreator.getFieldCreator(PYTHON_LIKE_VALUE_MAP_FIELD_NAME, Map.class)
                    .setModifiers(Modifier.PUBLIC).getFieldDescriptor();
            FieldDescriptor pythonSetterField = classCreator.getFieldCreator(PYTHON_SETTER_FIELD_NAME, TriFunction.class)
                    .setModifiers(Modifier.PUBLIC).getFieldDescriptor();
            FieldDescriptor pythonLikeTypeField =
                    classCreator.getFieldCreator(PYTHON_LIKE_TYPE_FIELD_NAME, PythonLikeType.class)
                            .setModifiers(Modifier.PUBLIC | Modifier.STATIC).getFieldDescriptor();
            generateWrapperMethods(classCreator, parentClass, GeneratedClassType.PLANNING_SOLUTION,
                    defineEqualsAndHashcode, valueField, referenceMapField,
                    pythonLikeValueMapField, pythonSetterField, pythonLikeTypeField,
                    optaplannerMethodAnnotations);
        }
        writeClassOutput(classNameToBytecode, className, classBytecodeHolder.get());
        return createAndInitializeClass(className);
    }

    // Used for debugging; prints a result handle
    private static void print(BytecodeCreator methodCreator, ResultHandle toPrint) {
        ResultHandle out = methodCreator.readStaticField(FieldDescriptor.of(System.class, "out", PrintStream.class));
        methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(PrintStream.class, "println", void.class, Object.class),
                out, toPrint);
    }

    // Generate PythonObject interface methods
    private static void generateAsPointer(ClassCreator classCreator, FieldDescriptor valueField,
            FieldDescriptor referenceMapField, FieldDescriptor pythonLikeObjectValueField,
            FieldDescriptor typeField) {
        MethodCreator methodCreator = classCreator.getMethodCreator("get__optapy_Id", OpaquePythonReference.class);
        ResultHandle valueResultHandle = methodCreator.readInstanceField(valueField, methodCreator.getThis());
        methodCreator.returnValue(valueResultHandle);

        methodCreator = classCreator.getMethodCreator("get__optapy_reference_map", Map.class);
        ResultHandle referenceMapResultHandle = methodCreator.readInstanceField(referenceMapField, methodCreator.getThis());
        methodCreator.returnValue(referenceMapResultHandle);
    }

    private static void generateForceUpdate(ClassCreator classCreator, GeneratedClassType generatedClassType,
            Class<?> parentClass,
            FieldDescriptor valueField, FieldDescriptor pythonSetterField,
            List<FieldDescriptor> planningEntityPropertyFieldList,
            List<FieldDescriptor> planningEntityCollectionFieldList,
            List<FieldDescriptor> planningVariableFieldList, List<String> planningVariableSetterNameList,
            List<FieldDescriptor> planningListVariableFieldList, List<String> planningListVariableSetterNameList,
            List<FieldDescriptor> planningScoreFieldList,
            List<String> planningScoreSetterNameList) {
        MethodCreator methodCreator = classCreator.getMethodCreator("forceUpdate", void.class);

        switch (generatedClassType) {
            case PROBLEM_FACT: {
                break; // Do nothing
            }
            case PLANNING_ENTITY: {
                boolean parentHasForceUpdate = false;
                try {
                    if (parentClass != null) {
                        Method method = parentClass.getMethod("forceUpdate", void.class);
                        parentHasForceUpdate = !Modifier.isAbstract(method.getModifiers());
                    }
                } catch (NoSuchMethodException e) {
                    // ignore
                }
                if (parentHasForceUpdate) {
                    methodCreator.invokeSpecialInterfaceMethod(MethodDescriptor.ofMethod(PythonObject.class, "forceUpdate",
                            void.class),
                            methodCreator.getThis());
                }
                ResultHandle thisObj = methodCreator.getThis();
                ResultHandle opaquePythonReference = methodCreator.readInstanceField(valueField, methodCreator.getThis());
                for (int i = 0; i < planningVariableFieldList.size(); i++) {
                    FieldDescriptor planningVariableField = planningVariableFieldList.get(i);
                    String setterName = planningVariableSetterNameList.get(i);

                    methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "setValueOnPythonObject", void.class,
                                    OpaquePythonReference.class, String.class, Object.class),
                            opaquePythonReference,
                            methodCreator.load(setterName),
                            methodCreator.readInstanceField(planningVariableField, thisObj));
                }

                for (int i = 0; i < planningListVariableFieldList.size(); i++) {
                    FieldDescriptor planningListVariableField = planningListVariableFieldList.get(i);
                    String setterName = planningListVariableSetterNameList.get(i);

                    methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "setListValueOnPythonObject", void.class,
                                    OpaquePythonReference.class, String.class, List.class, Map.class, TriFunction.class),
                            opaquePythonReference,
                            methodCreator.load(setterName),
                            methodCreator.readInstanceField(planningListVariableField, thisObj),
                            methodCreator.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "get__optapy_reference_map", Map.class),
                                    thisObj),
                            methodCreator.readInstanceField(pythonSetterField, thisObj));
                }
                break;
            }
            case PLANNING_SOLUTION: {
                ResultHandle thisObject = methodCreator.getThis();
                for (int i = 0; i < planningScoreFieldList.size(); i++) {
                    FieldDescriptor planningScoreField = planningScoreFieldList.get(i);
                    String setterName = planningScoreSetterNameList.get(i);

                    methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "setValueOnPythonObject", void.class,
                                    OpaquePythonReference.class, String.class, Object.class),
                            methodCreator.readInstanceField(valueField, methodCreator.getThis()),
                            methodCreator.load(setterName),
                            methodCreator.readInstanceField(planningScoreField, thisObject));
                }
                for (FieldDescriptor planningEntityField : planningEntityPropertyFieldList) {
                    methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PythonObject.class, "forceUpdate", void.class),
                            methodCreator.readInstanceField(planningEntityField, thisObject));
                }
                for (FieldDescriptor planningEntityCollectionField : planningEntityCollectionFieldList) {
                    if (planningEntityCollectionField.getType().endsWith("[]")) {
                        // Array
                        AssignableResultHandle arrayIndex = methodCreator.createVariable(int.class);
                        methodCreator.assign(arrayIndex, methodCreator.load(0));

                        ResultHandle array = methodCreator.readInstanceField(planningEntityCollectionField, thisObject);
                        ResultHandle arrayLength = methodCreator.arrayLength(array);

                        WhileLoop arrayLoop =
                                methodCreator.whileLoop(condition -> condition.ifIntegerLessThan(arrayIndex, arrayLength));
                        try (BytecodeCreator arrayLoopBlock = arrayLoop.block()) {
                            ResultHandle arrayElement = arrayLoopBlock.readArrayValue(array, arrayIndex);
                            arrayLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "forceUpdate", void.class), arrayElement);
                            arrayLoopBlock.assign(arrayIndex, methodCreator.increment(arrayIndex));
                        }
                    } else {
                        // Collection
                        ResultHandle collection = methodCreator.readInstanceField(planningEntityCollectionField, thisObject);
                        ResultHandle iterator = methodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(Collection.class, "iterator",
                                        Iterator.class),
                                collection);
                        WhileLoop iteratorLoop = methodCreator.whileLoop(condition -> condition.ifTrue(condition
                                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class),
                                        iterator)));
                        try (BytecodeCreator iteratorLoopBlock = iteratorLoop.block()) {
                            ResultHandle element = iteratorLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Iterator.class, "next", Object.class),
                                    iterator);
                            iteratorLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "forceUpdate", void.class), element);
                        }
                    }
                }
                break;
            }
            default:
                throw new IllegalStateException("Unhandled GeneratedClassType (" + generatedClassType + ")");
        }
        methodCreator.returnValue(null);
    }

    private static void generateReadFromPythonObject(ClassCreator classCreator, GeneratedClassType generatedClassType,
            Class<?> parentClass,
            List<FieldDescriptor> planningEntityPropertyFieldList,
            List<FieldDescriptor> planningEntityCollectionFieldList,
            List<FieldDescriptor> problemFactPropertyFieldList,
            List<FieldDescriptor> problemFactCollectionFieldList,
            List<FieldDescriptor> planningVariableFieldList,
            List<String> planningVariableSetterNameList) {
        MethodCreator methodCreator = classCreator.getMethodCreator("readFromPythonObject", void.class, Set.class, Map.class);

        ResultHandle doneSet = methodCreator.getMethodParam(0);
        ResultHandle referenceMap = methodCreator.getMethodParam(1);
        ResultHandle alreadyHandled = methodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Collection.class, "contains", boolean.class, Object.class),
                methodCreator.getMethodParam(0), methodCreator.getThis());
        methodCreator.ifTrue(alreadyHandled).trueBranch().returnValue(null);
        methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Collection.class, "add", boolean.class, Object.class),
                methodCreator.getMethodParam(0), methodCreator.getThis());
        methodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(CPythonBackedPythonLikeObject.class, "$setInstanceMap", void.class, Map.class),
                methodCreator.getThis(), methodCreator.getMethodParam(1));

        switch (generatedClassType) {
            case PROBLEM_FACT:
            case PLANNING_ENTITY: {
                break;
            }
            case PLANNING_SOLUTION: {
                ResultHandle thisObject = methodCreator.getThis();
                // planning entities
                for (FieldDescriptor planningEntityField : planningEntityPropertyFieldList) {
                    methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PythonObject.class, "readFromPythonObject", void.class, Set.class,
                                    Map.class),
                            methodCreator.readInstanceField(planningEntityField, thisObject), doneSet, referenceMap);
                }
                for (FieldDescriptor planningEntityCollectionField : planningEntityCollectionFieldList) {
                    if (planningEntityCollectionField.getType().endsWith("[]")) {
                        // Array
                        AssignableResultHandle arrayIndex = methodCreator.createVariable(int.class);
                        methodCreator.assign(arrayIndex, methodCreator.load(0));

                        ResultHandle array = methodCreator.readInstanceField(planningEntityCollectionField, thisObject);
                        ResultHandle arrayLength = methodCreator.arrayLength(array);

                        WhileLoop arrayLoop =
                                methodCreator.whileLoop(condition -> condition.ifIntegerLessThan(arrayIndex, arrayLength));
                        try (BytecodeCreator arrayLoopBlock = arrayLoop.block()) {
                            ResultHandle arrayElement = arrayLoopBlock.readArrayValue(array, arrayIndex);
                            arrayLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "readFromPythonObject", void.class, Set.class,
                                            Map.class),
                                    arrayElement, doneSet, referenceMap);
                            arrayLoopBlock.assign(arrayIndex, methodCreator.increment(arrayIndex));
                        }
                    } else {
                        // Collection
                        ResultHandle iterator = methodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(Collection.class, "iterator",
                                        Iterator.class),
                                methodCreator.readInstanceField(planningEntityCollectionField, thisObject));
                        WhileLoop iteratorLoop = methodCreator.whileLoop(condition -> condition.ifTrue(condition
                                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class),
                                        iterator)));
                        try (BytecodeCreator iteratorLoopBlock = iteratorLoop.block()) {
                            ResultHandle element = iteratorLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Iterator.class, "next", Object.class),
                                    iterator);
                            iteratorLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "readFromPythonObject", void.class, Set.class,
                                            Map.class),
                                    element, doneSet, referenceMap);
                        }
                    }
                }

                // problem facts
                for (FieldDescriptor problemFactField : problemFactPropertyFieldList) {
                    ResultHandle fieldValue = methodCreator.readInstanceField(problemFactField, thisObject);
                    ResultHandle isInstanceOfPythonObject = methodCreator.instanceOf(fieldValue, PythonObject.class);
                    methodCreator.ifTrue(isInstanceOfPythonObject)
                            .trueBranch()
                            .invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "readFromPythonObject", void.class, Set.class,
                                            Map.class),
                                    fieldValue, doneSet, referenceMap);
                }

                for (FieldDescriptor problemFactCollectionField : problemFactCollectionFieldList) {
                    if (problemFactCollectionField.getType().endsWith("[]")) {
                        // Array
                        AssignableResultHandle arrayIndex = methodCreator.createVariable(int.class);
                        methodCreator.assign(arrayIndex, methodCreator.load(0));

                        ResultHandle array = methodCreator.readInstanceField(problemFactCollectionField, thisObject);
                        ResultHandle arrayLength = methodCreator.arrayLength(array);

                        WhileLoop arrayLoop =
                                methodCreator.whileLoop(condition -> condition.ifIntegerLessThan(arrayIndex, arrayLength));
                        try (BytecodeCreator arrayLoopBlock = arrayLoop.block()) {
                            ResultHandle arrayElement = arrayLoopBlock.readArrayValue(array, arrayIndex);
                            ResultHandle isInstanceOfPythonObject = arrayLoopBlock.instanceOf(arrayElement, PythonObject.class);
                            arrayLoopBlock.ifTrue(isInstanceOfPythonObject)
                                    .trueBranch()
                                    .invokeInterfaceMethod(
                                            MethodDescriptor.ofMethod(PythonObject.class, "readFromPythonObject", void.class,
                                                    Set.class, Map.class),
                                            arrayElement, doneSet, referenceMap);
                            arrayLoopBlock.assign(arrayIndex, methodCreator.increment(arrayIndex));
                        }
                    } else {
                        // Collection
                        ResultHandle iterator = methodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(Collection.class, "iterator",
                                        Iterator.class),
                                methodCreator.readInstanceField(problemFactCollectionField, thisObject));
                        WhileLoop iteratorLoop = methodCreator.whileLoop(condition -> condition.ifTrue(condition
                                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class),
                                        iterator)));
                        try (BytecodeCreator iteratorLoopBlock = iteratorLoop.block()) {
                            ResultHandle element = iteratorLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Iterator.class, "next", Object.class),
                                    iterator);
                            ResultHandle isInstanceOfPythonObject = iteratorLoopBlock.instanceOf(element, PythonObject.class);
                            iteratorLoopBlock.ifTrue(isInstanceOfPythonObject)
                                    .trueBranch()
                                    .invokeInterfaceMethod(
                                            MethodDescriptor.ofMethod(PythonObject.class, "readFromPythonObject", void.class,
                                                    Set.class, Map.class),
                                            element, doneSet, referenceMap);
                        }
                    }
                }
                break;
            }
            default:
                throw new IllegalStateException("Unhandled GeneratedClassType (" + generatedClassType + ")");
        }
        methodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(CPythonBackedPythonLikeObject.class, "$readFieldsFromCPythonReference", void.class),
                methodCreator.getThis());
        methodCreator.returnValue(null);
    }

    private static void generateVisitIds(ClassCreator classCreator, GeneratedClassType generatedClassType,
            Class<?> parentClass, FieldDescriptor valueField,
            List<FieldDescriptor> planningEntityPropertyFieldList,
            List<FieldDescriptor> planningEntityCollectionFieldList, List<String> planningEntityCollectionGetterList,
            List<FieldDescriptor> problemFactPropertyFieldList,
            List<FieldDescriptor> problemFactCollectionFieldList, List<String> problemFactCollectionGetterList,
            List<FieldDescriptor> planningVariableFieldList, List<String> planningVariableSetterNameList,
            List<FieldDescriptor> planningListVariableFieldList, List<String> planningListVariableSetterNameList) {
        MethodCreator methodCreator = classCreator.getMethodCreator("visitIds", void.class, Map.class);

        ResultHandle referenceMap = methodCreator.getMethodParam(0);
        methodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                referenceMap,
                methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "getPythonObjectId", Number.class,
                                PythonObject.class),
                        methodCreator.getThis()),
                methodCreator.getThis());

        switch (generatedClassType) {
            case PROBLEM_FACT: {
                break;
            }
            case PLANNING_ENTITY: {
                ResultHandle thisObj = methodCreator.getThis();
                ResultHandle opaquePythonReference = methodCreator.readInstanceField(valueField, methodCreator.getThis());

                for (int i = 0; i < planningListVariableFieldList.size(); i++) {
                    FieldDescriptor planningListVariableField = planningListVariableFieldList.get(i);
                    String setterName = planningListVariableSetterNameList.get(i);

                    methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "visitListIdOnPythonObject", void.class,
                                    OpaquePythonReference.class, String.class, List.class, Map.class),
                            opaquePythonReference,
                            methodCreator.load("get" + setterName.substring(3)),
                            methodCreator.readInstanceField(planningListVariableField, thisObj),
                            referenceMap);
                }
                break;
            }
            case PLANNING_SOLUTION: {
                ResultHandle thisObject = methodCreator.getThis();
                ResultHandle opaquePythonReference = methodCreator.readInstanceField(valueField, methodCreator.getThis());

                // planning entities
                for (FieldDescriptor planningEntityField : planningEntityPropertyFieldList) {
                    methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PythonObject.class, "visitIds", void.class, Map.class),
                            methodCreator.readInstanceField(planningEntityField, thisObject), referenceMap);
                }
                for (int i = 0; i < planningEntityCollectionFieldList.size(); i++) {
                    FieldDescriptor planningEntityCollectionField = planningEntityCollectionFieldList.get(i);
                    if (planningEntityCollectionField.getType().endsWith("[]")) {
                        // Array
                        AssignableResultHandle arrayIndex = methodCreator.createVariable(int.class);
                        methodCreator.assign(arrayIndex, methodCreator.load(0));

                        ResultHandle array = methodCreator.readInstanceField(planningEntityCollectionField, thisObject);
                        ResultHandle arrayLength = methodCreator.arrayLength(array);

                        WhileLoop arrayLoop =
                                methodCreator.whileLoop(condition -> condition.ifIntegerLessThan(arrayIndex, arrayLength));
                        try (BytecodeCreator arrayLoopBlock = arrayLoop.block()) {
                            ResultHandle arrayElement = arrayLoopBlock.readArrayValue(array, arrayIndex);
                            arrayLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "visitIds", void.class, Map.class),
                                    arrayElement, referenceMap);
                            arrayLoopBlock.assign(arrayIndex, methodCreator.increment(arrayIndex));
                        }
                    } else {
                        // Collection
                        methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "visitListIdOnPythonObject", void.class,
                                        OpaquePythonReference.class, String.class, List.class, Map.class),
                                opaquePythonReference,
                                methodCreator.load(planningEntityCollectionGetterList.get(i)),
                                methodCreator.readInstanceField(planningEntityCollectionField, thisObject),
                                referenceMap);

                        ResultHandle iterator = methodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(Collection.class, "iterator",
                                        Iterator.class),
                                methodCreator.readInstanceField(planningEntityCollectionField, thisObject));
                        WhileLoop iteratorLoop = methodCreator.whileLoop(condition -> condition.ifTrue(condition
                                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class),
                                        iterator)));
                        try (BytecodeCreator iteratorLoopBlock = iteratorLoop.block()) {
                            ResultHandle element = iteratorLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Iterator.class, "next", Object.class),
                                    iterator);
                            iteratorLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "visitIds", void.class, Map.class), element,
                                    referenceMap);
                        }
                    }
                }

                // problem facts
                for (FieldDescriptor problemFactField : problemFactPropertyFieldList) {
                    ResultHandle fieldValue = methodCreator.readInstanceField(problemFactField, thisObject);
                    ResultHandle isInstanceOfPythonObject = methodCreator.instanceOf(fieldValue, PythonObject.class);
                    methodCreator.ifTrue(isInstanceOfPythonObject)
                            .trueBranch()
                            .invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(PythonObject.class, "visitIds", void.class, Map.class),
                                    fieldValue, referenceMap);
                }

                for (int i = 0; i < problemFactCollectionFieldList.size(); i++) {
                    FieldDescriptor problemFactCollectionField = problemFactCollectionFieldList.get(i);
                    if (problemFactCollectionField.getType().endsWith("[]")) {
                        // Array
                        AssignableResultHandle arrayIndex = methodCreator.createVariable(int.class);
                        methodCreator.assign(arrayIndex, methodCreator.load(0));

                        ResultHandle array = methodCreator.readInstanceField(problemFactCollectionField, thisObject);
                        ResultHandle arrayLength = methodCreator.arrayLength(array);

                        WhileLoop arrayLoop =
                                methodCreator.whileLoop(condition -> condition.ifIntegerLessThan(arrayIndex, arrayLength));
                        try (BytecodeCreator arrayLoopBlock = arrayLoop.block()) {
                            ResultHandle arrayElement = arrayLoopBlock.readArrayValue(array, arrayIndex);
                            ResultHandle isInstanceOfPythonObject = arrayLoopBlock.instanceOf(arrayElement, PythonObject.class);
                            arrayLoopBlock.ifTrue(isInstanceOfPythonObject)
                                    .trueBranch()
                                    .invokeInterfaceMethod(
                                            MethodDescriptor.ofMethod(PythonObject.class, "visitIds", void.class, Map.class),
                                            arrayElement, referenceMap);
                            arrayLoopBlock.assign(arrayIndex, methodCreator.increment(arrayIndex));
                        }
                    } else {
                        // Collection
                        methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "visitListIdOnPythonObject", void.class,
                                        OpaquePythonReference.class, String.class, List.class, Map.class),
                                opaquePythonReference,
                                methodCreator.load(problemFactCollectionGetterList.get(i)),
                                methodCreator.readInstanceField(problemFactCollectionField, thisObject),
                                referenceMap);

                        ResultHandle iterator = methodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(Collection.class, "iterator",
                                        Iterator.class),
                                methodCreator.readInstanceField(problemFactCollectionField, thisObject));
                        WhileLoop iteratorLoop = methodCreator.whileLoop(condition -> condition.ifTrue(condition
                                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class),
                                        iterator)));
                        try (BytecodeCreator iteratorLoopBlock = iteratorLoop.block()) {
                            ResultHandle element = iteratorLoopBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Iterator.class, "next", Object.class),
                                    iterator);
                            ResultHandle isInstanceOfPythonObject = iteratorLoopBlock.instanceOf(element, PythonObject.class);
                            iteratorLoopBlock.ifTrue(isInstanceOfPythonObject)
                                    .trueBranch()
                                    .invokeInterfaceMethod(
                                            MethodDescriptor.ofMethod(PythonObject.class, "visitIds", void.class, Map.class),
                                            element, referenceMap);
                        }
                    }
                }
                break;
            }
            default:
                throw new IllegalStateException("Unhandled GeneratedClassType (" + generatedClassType + ")");
        }
        methodCreator.returnValue(null);
    }

    public enum GeneratedClassType {
        PROBLEM_FACT,
        PLANNING_ENTITY,
        PLANNING_SOLUTION
    }

    public static Class<?> createAndInitializeClass(String className) {
        Class<?> clazz;
        try {
            clazz = asmClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        PythonLikeType parentType = null;
        boolean hasParent = PythonLikeObject.class.isAssignableFrom(clazz.getSuperclass())
                && !clazz.getSuperclass().equals(PythonObjectWrapper.class);

        if (hasParent) {
            try {
                parentType = (PythonLikeType) clazz.getSuperclass().getField(PYTHON_LIKE_TYPE_FIELD_NAME).get(null);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }

        PythonLikeType typeField;
        if (parentType != null) {
            typeField = new PythonLikeType(clazz.getName(), (Class<? extends PythonLikeObject>) clazz, List.of(parentType));
        } else {
            typeField = new PythonLikeType(clazz.getName(), (Class<? extends PythonLikeObject>) clazz);
        }

        try {
            clazz.getField(PYTHON_LIKE_TYPE_FIELD_NAME).set(null, typeField);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        return clazz;
    }

    // Create all methods in the class
    @SuppressWarnings("unchecked")
    private static void generateWrapperMethods(ClassCreator classCreator, Class<?> parentClass,
            GeneratedClassType generatedClassType,
            boolean defineEqualsAndHashcode, FieldDescriptor valueField,
            FieldDescriptor referenceMapField,
            FieldDescriptor pythonLikeValueMapField,
            FieldDescriptor pythonSetterField,
            FieldDescriptor typeField,
            List<List<Object>> optaplannerMethodAnnotations) {
        boolean hasOptaPyParentClass = false;
        try {
            parentClass.getMethod("get__optapy_Id", OpaquePythonReference.class);
            hasOptaPyParentClass = true;
        } catch (NoSuchMethodException e) {
            // Do nothing
        }

        if (!hasOptaPyParentClass) {
            generateAsPointer(classCreator, valueField, referenceMapField, pythonLikeValueMapField, typeField);
        }

        // We only need to create methods/fields for methods with OptaPlanner annotations
        // optaplannerMethodAnnotations: list of tuples (methodName, returnType, annotationList)
        // (Each annotation is represented by a Map)
        List<FieldDescriptor> fieldDescriptorList = new ArrayList<>(optaplannerMethodAnnotations.size());
        List<Object> returnTypeList = new ArrayList<>(optaplannerMethodAnnotations.size());
        List<FieldDescriptor> planningEntityFieldList = new ArrayList<>();
        List<FieldDescriptor> planningEntityCollectionFieldList = new ArrayList<>();
        List<String> planningEntityCollectionGetterList = new ArrayList<>();
        List<FieldDescriptor> problemFactFieldList = new ArrayList<>();
        List<FieldDescriptor> problemFactCollectionFieldList = new ArrayList<>();
        List<String> problemFactCollectionGetterList = new ArrayList<>();
        List<FieldDescriptor> planningVariableFieldList = new ArrayList<>();
        List<String> planningVariableSetterNameList = new ArrayList<>();
        List<FieldDescriptor> planningListVariableFieldList = new ArrayList<>();
        List<String> planningListVariableSetterNameList = new ArrayList<>();
        List<FieldDescriptor> planningScoreFieldList = new ArrayList<>();
        List<String> planningScoreSetterNameList = new ArrayList<>();

        for (List<Object> optaplannerMethodAnnotation : optaplannerMethodAnnotations) {
            String methodName = (String) (optaplannerMethodAnnotation.get(0));
            Class<?> returnType = (Class<?>) (optaplannerMethodAnnotation.get(1));
            String signature = (String) (optaplannerMethodAnnotation.get(2));
            if (returnType == null) {
                returnType = Object.class;
            }
            List<Map<String, Object>> annotations = (List<Map<String, Object>>) optaplannerMethodAnnotation.get(3);
            fieldDescriptorList
                    .add(generateWrapperMethod(classCreator, parentClass, valueField, pythonLikeValueMapField,
                            pythonSetterField,
                            methodName, returnType, signature, annotations, returnTypeList,
                            planningEntityFieldList,
                            planningEntityCollectionFieldList, planningEntityCollectionGetterList,
                            problemFactFieldList,
                            problemFactCollectionFieldList, problemFactCollectionGetterList,
                            planningVariableFieldList, planningVariableSetterNameList,
                            planningListVariableFieldList, planningListVariableSetterNameList,
                            planningScoreFieldList, planningScoreSetterNameList));
        }
        createConstructor(classCreator, valueField, referenceMapField, pythonLikeValueMapField, pythonSetterField,
                parentClass, fieldDescriptorList, returnTypeList);

        generateForceUpdate(classCreator, generatedClassType, parentClass, valueField, pythonSetterField,
                planningEntityFieldList, planningEntityCollectionFieldList,
                planningVariableFieldList, planningVariableSetterNameList,
                planningListVariableFieldList, planningListVariableSetterNameList,
                planningScoreFieldList, planningScoreSetterNameList);

        generateReadFromPythonObject(classCreator, generatedClassType, parentClass,
                planningEntityFieldList, planningEntityCollectionFieldList,
                problemFactFieldList, problemFactCollectionFieldList,
                planningVariableFieldList,
                planningVariableSetterNameList);

        generateVisitIds(classCreator, generatedClassType, parentClass, valueField,
                planningEntityFieldList,
                planningEntityCollectionFieldList, planningEntityCollectionGetterList,
                problemFactFieldList,
                problemFactCollectionFieldList, problemFactCollectionGetterList,
                planningVariableFieldList, planningVariableSetterNameList,
                planningListVariableFieldList, planningListVariableSetterNameList);

        if (!hasOptaPyParentClass) {
            createToString(classCreator, valueField);
        }

        if (defineEqualsAndHashcode) {
            createEqualsAndHashcode(classCreator, valueField);
        }
    }

    private static void createToString(ClassCreator classCreator, FieldDescriptor valueField) {
        MethodCreator methodCreator =
                classCreator.getMethodCreator(MethodDescriptor.ofMethod(classCreator.getClassName(), "toString", String.class));
        methodCreator.returnValue(methodCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "getPythonObjectString", String.class,
                        OpaquePythonReference.class),
                methodCreator.readInstanceField(valueField, methodCreator.getThis())));
    }

    private static void createEqualsAndHashcode(ClassCreator classCreator, FieldDescriptor valueField) {
        // equals
        MethodCreator methodCreator =
                classCreator.getMethodCreator(
                        MethodDescriptor.ofMethod(classCreator.getClassName(), "equals", boolean.class, Object.class));
        ResultHandle parameter = methodCreator.getMethodParam(0);
        ResultHandle isInstance = methodCreator.instanceOf(parameter, classCreator.getClassName());
        BranchResult branchResult = methodCreator.ifTrue(isInstance);
        BytecodeCreator bytecodeCreator = branchResult.trueBranch();
        bytecodeCreator.returnValue(bytecodeCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(PythonComparable.class, "isPythonObjectEqualToOther", boolean.class,
                        OpaquePythonReference.class, OpaquePythonReference.class),
                bytecodeCreator.readInstanceField(valueField, methodCreator.getThis()),
                bytecodeCreator.readInstanceField(valueField, parameter)));
        bytecodeCreator = branchResult.falseBranch();
        bytecodeCreator.returnValue(bytecodeCreator.load(false));

        // hashCode
        methodCreator =
                classCreator.getMethodCreator(MethodDescriptor.ofMethod(classCreator.getClassName(), "hashCode", int.class));
        methodCreator.returnValue(methodCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(PythonComparable.class, "getPythonObjectHash", int.class,
                        OpaquePythonReference.class),
                methodCreator.readInstanceField(valueField, methodCreator.getThis())));
    }

    private static void createConstructor(ClassCreator classCreator, FieldDescriptor valueField,
            FieldDescriptor referenceMapField, FieldDescriptor pythonLikeValueMapField, FieldDescriptor pythonSetterField,
            Class<?> parentClass, List<FieldDescriptor> fieldDescriptorList, List<Object> returnTypeList) {
        // Entity(PythonLikeType) constructor, for subclasses
        if (!PythonObjectWrapper.class.isAssignableFrom(parentClass)) {
            // Entity(PythonLikeType) constructor, for subclasses
            MethodCreator methodCreator = classCreator
                    .getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName(), PythonLikeType.class));
            methodCreator.setModifiers(Modifier.PUBLIC);
            methodCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(parentClass, PythonLikeType.class),
                    methodCreator.getThis(),
                    methodCreator.getMethodParam(0));
            methodCreator.returnValue(methodCreator.getThis());
        } else {
            // Entity(OpaquePythonReference) constructor, for subclasses
            MethodCreator methodCreator = classCreator
                    .getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName(), OpaquePythonReference.class));
            methodCreator.setModifiers(Modifier.PUBLIC);
            methodCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(parentClass, OpaquePythonReference.class),
                    methodCreator.getThis(),
                    methodCreator.getMethodParam(0));
            methodCreator.returnValue(methodCreator.getThis());
        }

        // Entity(OpaquePythonReference, Number, Map, TriFunction) constructor, for cloning
        MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName(),
                OpaquePythonReference.class, Number.class, Map.class, TriFunction.class));
        methodCreator.setModifiers(Modifier.PUBLIC);

        if (PythonObjectWrapper.class.isAssignableFrom(parentClass)) {
            methodCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(parentClass, OpaquePythonReference.class),
                    methodCreator.getThis(),
                    methodCreator.getMethodParam(0));
        } else {
            methodCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(parentClass, PythonLikeType.class),
                    methodCreator.getThis(),
                    methodCreator.readStaticField(FieldDescriptor.of(classCreator.getClassName(),
                            "$TYPE",
                            PythonLikeType.class)));
        }
        try {
            Method initMethod = parentClass.getMethod("$init", void.class,
                    OpaquePythonReference.class, Number.class, Map.class,
                    TriFunction.class);
            methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(initMethod), methodCreator.getThis(),
                    methodCreator.getMethodParam(0),
                    methodCreator.getMethodParam(1),
                    methodCreator.getMethodParam(2),
                    methodCreator.getMethodParam(3));
        } catch (NoSuchMethodException e) {
            createInitMethod(classCreator, valueField, referenceMapField, pythonLikeValueMapField, pythonSetterField);
            methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(classCreator.getClassName(), "$init", void.class,
                    OpaquePythonReference.class, Number.class, Map.class,
                    TriFunction.class),
                    methodCreator.getThis(),
                    methodCreator.getMethodParam(0),
                    methodCreator.getMethodParam(1),
                    methodCreator.getMethodParam(2),
                    methodCreator.getMethodParam(3));
        }
        createSetFields(classCreator, valueField, referenceMapField, pythonLikeValueMapField, pythonSetterField,
                parentClass, fieldDescriptorList, returnTypeList);
        methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(classCreator.getClassName(), "$setFields", void.class,
                OpaquePythonReference.class, Number.class, Map.class, TriFunction.class),
                methodCreator.getThis(),
                methodCreator.getMethodParam(0), methodCreator.getMethodParam(1), methodCreator.getMethodParam(2),
                methodCreator.getMethodParam(3));
        methodCreator.returnValue(methodCreator.getThis());
    }

    private static void createInitMethod(ClassCreator classCreator, FieldDescriptor valueField,
            FieldDescriptor referenceMapField, FieldDescriptor pythonLikeValueMapField, FieldDescriptor pythonSetterField) {
        MethodCreator initCreator = classCreator.getMethodCreator("$init", void.class,
                OpaquePythonReference.class, Number.class, Map.class,
                TriFunction.class);
        initCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                initCreator.getMethodParam(2), initCreator.getMethodParam(1), initCreator.getThis());
        initCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(CPythonBackedPythonLikeObject.class,
                "$setCPythonReference",
                void.class,
                OpaquePythonReference.class),
                initCreator.getThis(), initCreator.getMethodParam(0));
        initCreator.writeInstanceField(valueField, initCreator.getThis(), initCreator.getMethodParam(0));
        initCreator.writeInstanceField(referenceMapField, initCreator.getThis(), initCreator.getMethodParam(2));
        initCreator.writeInstanceField(pythonSetterField, initCreator.getThis(), initCreator.getMethodParam(3));
        initCreator.writeInstanceField(pythonLikeValueMapField, initCreator.getThis(),
                initCreator.newInstance(MethodDescriptor.ofConstructor(HashMap.class)));
        initCreator.returnValue(null);
    }

    private static void createSetFields(ClassCreator classCreator, FieldDescriptor valueField,
            FieldDescriptor referenceMapField, FieldDescriptor pythonLikeValueMapField, FieldDescriptor pythonSetterField,
            Class<?> parentClass, List<FieldDescriptor> fieldDescriptorList, List<Object> returnTypeList) {
        MethodCreator methodCreator =
                classCreator.getMethodCreator(MethodDescriptor.ofMethod(classCreator.getClassName(), "$setFields", void.class,
                        OpaquePythonReference.class, Number.class, Map.class, TriFunction.class));
        methodCreator.setModifiers(Modifier.PUBLIC);

        try {
            Method parentSetFields = parentClass.getMethod("$setFields", OpaquePythonReference.class, Number.class, Map.class,
                    TriFunction.class);
            methodCreator.invokeSpecialMethod(MethodDescriptor.ofMethod(parentSetFields), methodCreator.getThis(),
                    methodCreator.getMethodParam(0), methodCreator.getMethodParam(1),
                    methodCreator.getMethodParam(2), methodCreator.getMethodParam(3));
        } catch (NoSuchMethodException e) {
            // Do nothing; don't need to call parent $setFields as it does not have one
        }

        ResultHandle value = methodCreator.getMethodParam(0);

        for (int i = 0; i < fieldDescriptorList.size(); i++) {
            FieldDescriptor fieldDescriptor = fieldDescriptorList.get(i);
            Object returnType = returnTypeList.get(i);
            String methodName = fieldDescriptor.getName().substring(0, fieldDescriptor.getName().length() - 6);

            if (returnType instanceof Class) {
                Class<?> returnTypeClass = (Class<?>) returnType;
                if (returnTypeClass.equals(OpaquePythonReference.class)) {
                    ResultHandle outResultHandle = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(CPythonBackedPythonInterpreter.class,
                                    "lookupPointerForAttributeOnPythonReference", OpaquePythonReference.class,
                                    OpaquePythonReference.class, String.class),
                            value, methodCreator.load(methodName));
                    methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(), outResultHandle);
                    continue;
                } else if (returnTypeClass.isArray()
                        && returnTypeClass.getComponentType().equals(OpaquePythonReference.class)) {
                    ResultHandle outResultHandle = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(CPythonBackedPythonInterpreter.class,
                                    "lookupPointerArrayForAttributeOnPythonReference", OpaquePythonReference[].class,
                                    OpaquePythonReference.class, String.class),
                            value, methodCreator.load(methodName));
                    methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(), outResultHandle);
                    continue;
                }
            }

            ResultHandle outResultHandle = methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "getValueFromPythonObject", Object.class,
                            OpaquePythonReference.class, String.class),
                    value, methodCreator.load(methodName));

            if (returnType instanceof Class) {
                Class<?> returnTypeClass = (Class<?>) returnType;
                if ((Comparable.class.isAssignableFrom(returnTypeClass) &&
                        returnTypeClass.getClassLoader() != asmClassLoader) ||
                        Number.class.isAssignableFrom(returnTypeClass) ||
                        ValueRange.class.isAssignableFrom(returnTypeClass) ||
                        OpaquePythonReference.class.isAssignableFrom(returnTypeClass)) {
                    // It is a number/String, so it already translated to the corresponding Java type
                    if (Integer.class.equals(returnTypeClass)) {
                        ResultHandle isLong = methodCreator.instanceOf(outResultHandle, Long.class);
                        BranchResult ifLongBranchResult = methodCreator.ifTrue(isLong);
                        BytecodeCreator bytecodeCreator = ifLongBranchResult.trueBranch();
                        bytecodeCreator.writeInstanceField(fieldDescriptor, bytecodeCreator.getThis(),
                                bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Long.class, "intValue",
                                        int.class), outResultHandle));
                        bytecodeCreator = ifLongBranchResult.falseBranch();
                        bytecodeCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(), outResultHandle);
                    } else if (CountableValueRange.class.isAssignableFrom(returnTypeClass)) {
                        ResultHandle proxy = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "getCountableValueRangeProxy",
                                        CountableValueRange.class,
                                        Object.class),
                                outResultHandle);
                        methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(), proxy);
                    } else if (ValueRange.class.isAssignableFrom(returnTypeClass)) {
                        ResultHandle proxy = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "getValueRangeProxy", ValueRange.class,
                                        Object.class),
                                outResultHandle);
                        methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(), proxy);
                    } else {
                        methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(), outResultHandle);
                    }
                } else {
                    // We need to wrap it
                    ResultHandle actualClass;

                    if (returnTypeClass.isArray()) {
                        actualClass = methodCreator.loadClass(returnTypeClass);
                    } else if (Collection.class.isAssignableFrom(returnTypeClass)) {
                        actualClass = methodCreator.loadClass(List.class);
                    } else {
                        actualClass = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(PythonWrapperGenerator.class, "getJavaClass", Class.class,
                                        OpaquePythonReference.class),
                                outResultHandle);
                    }
                    methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(),
                            methodCreator.invokeStaticMethod(MethodDescriptor.ofMethod(PythonWrapperGenerator.class,
                                    "wrap", Object.class, Class.class, OpaquePythonReference.class, Map.class,
                                    TriFunction.class),
                                    actualClass, outResultHandle, methodCreator.getMethodParam(2),
                                    methodCreator.getMethodParam(3)));
                }
            } else {
                // It a reference to the current class; need to be wrapped
                ResultHandle actualClass = methodCreator.loadClass(classCreator.getClassName());
                methodCreator.writeInstanceField(fieldDescriptor, methodCreator.getThis(),
                        methodCreator.invokeStaticMethod(MethodDescriptor.ofMethod(PythonWrapperGenerator.class,
                                "wrap", Object.class, Class.class, OpaquePythonReference.class, Map.class, TriFunction.class),
                                actualClass, outResultHandle, methodCreator.getMethodParam(2),
                                methodCreator.getMethodParam(3)));
            }
        }

        for (FieldDescriptor fieldDescriptor : fieldDescriptorList) {
            String methodName = fieldDescriptor.getName().substring(0, fieldDescriptor.getName().length() - 6);

            // Put it into the map used by the python interpreter
            AssignableResultHandle valueAsPythonLikeObject = methodCreator.createVariable(PythonLikeObject.class);
            methodCreator.assign(valueAsPythonLikeObject, methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(JavaPythonTypeConversionImplementor.class, "wrapJavaObject",
                            PythonLikeObject.class, Object.class),
                    methodCreator.readInstanceField(fieldDescriptor, methodCreator.getThis())));

            // If it a string, it will be assignable from PythonLikeObject (used for self)
            BranchResult branchResult = methodCreator.ifReferencesEqual(valueAsPythonLikeObject,
                    methodCreator.readStaticField(FieldDescriptor.of(
                            PythonNone.class, "INSTANCE", PythonNone.class)));
            BytecodeCreator currentBranch = branchResult.trueBranch();
            currentBranch.assign(valueAsPythonLikeObject, currentBranch.loadNull());

            String fieldName;
            if (methodName.startsWith("get_")) {
                fieldName = methodName.substring(4);
            } else {
                fieldName = methodName.substring(3);
            }
            methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.class, "put", Object.class,
                    Object.class, Object.class),
                    methodCreator.readInstanceField(pythonLikeValueMapField, methodCreator.getThis()),
                    methodCreator.load(fieldName),
                    valueAsPythonLikeObject);
            try {
                Method setterMethod =
                        lookupMethod(parentClass, PythonClassTranslator.getJavaMethodName("s" + methodName.substring(1)));
                methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(setterMethod), methodCreator.getThis(),
                        valueAsPythonLikeObject);
            } catch (NoSuchMethodException e) {
                // Do nothing; no setter
            }
        }
        methodCreator.returnValue(null);
    }

    private static FieldDescriptor generateWrapperMethod(ClassCreator classCreator, Class<?> parentClass,
            FieldDescriptor valueField,
            FieldDescriptor pythonLikeAttributeField, FieldDescriptor pythonSetterField,
            String methodName, Class<?> returnType, String signature,
            List<Map<String, Object>> annotations, List<Object> returnTypeList,
            List<FieldDescriptor> planningEntityFieldList,
            List<FieldDescriptor> planningEntityCollectionFieldList, List<String> planningEntityCollectionGetterList,
            List<FieldDescriptor> problemFactFieldList,
            List<FieldDescriptor> problemFactCollectionFieldList, List<String> problemFactCollectionGetterList,
            List<FieldDescriptor> planningVariableFieldList, List<String> planningVariableSetterNameList,
            List<FieldDescriptor> planningListVariableFieldList, List<String> planningListVariableSetterNameList,
            List<FieldDescriptor> planningScoreFieldDescriptor,
            List<String> planningScoreFieldDescriptorName) {
        // Python types are not required, so we need to discover them. If the type is unknown, we default to Object,
        // but some annotations need something more specific than Object
        Object actualReturnType = returnType;
        for (Map<String, Object> annotation : annotations) {
            Class<?> annotationType = (Class<?>) annotation.get("annotationType");
            if (PlanningId.class.isAssignableFrom(annotationType)
                    && !Comparable.class.isAssignableFrom(returnType)) {
                // A PlanningId MUST be comparable
                actualReturnType = Comparable.class;
            } else if ((ProblemFactCollectionProperty.class.isAssignableFrom(annotationType)
                    || PlanningEntityCollectionProperty.class.isAssignableFrom(annotationType))
                    && !(Collection.class.isAssignableFrom(returnType) || returnType.isArray())) {
                // A ProblemFactCollection/PlanningEntityCollection MUST be a collection or array
                actualReturnType = List.class;
            } else if (ValueRangeProvider.class.isAssignableFrom(annotationType) &&
                    !(Collection.class.isAssignableFrom(returnType) ||
                            ValueRange.class.isAssignableFrom(returnType) ||
                            returnType.isArray())) {
                // A Value Range must be a Collection, ValueRange or Array
                actualReturnType = List.class;
            } else if (SelfType.class.equals(returnType)) {
                actualReturnType = classCreator.getClassName();
            }
        }
        returnTypeList.add(actualReturnType);
        FieldDescriptor fieldDescriptor =
                classCreator.getFieldCreator(methodName + "$field", actualReturnType).getFieldDescriptor();

        for (Map<String, Object> annotation : annotations) {
            Class<?> annotationType = (Class<?>) annotation.get("annotationType");

            if (PlanningEntityProperty.class.isAssignableFrom(annotationType)) {
                planningEntityFieldList.add(fieldDescriptor);
            } else if (PlanningEntityCollectionProperty.class.isAssignableFrom(annotationType)) {
                planningEntityCollectionFieldList.add(fieldDescriptor);
            } else if (PlanningVariable.class.isAssignableFrom(annotationType) ||
                    (InverseRelationShadowVariable.class.isAssignableFrom(annotationType) &&
                            actualReturnType instanceof Class &&
                            !Collection.class.isAssignableFrom((Class<?>) actualReturnType))
                    ||
                    IndexShadowVariable.class.isAssignableFrom(annotationType) ||
                    AnchorShadowVariable.class.isAssignableFrom(annotationType) ||
                    CustomShadowVariable.class.isAssignableFrom(annotationType)) {
                planningVariableFieldList.add(fieldDescriptor);
            } else if (PlanningListVariable.class.isAssignableFrom(annotationType) ||
                    (InverseRelationShadowVariable.class.isAssignableFrom(annotationType) &&
                            actualReturnType instanceof Class &&
                            Collection.class.isAssignableFrom((Class<?>) actualReturnType))) {
                planningListVariableFieldList.add(fieldDescriptor);
            } else if (PlanningScore.class.isAssignableFrom(annotationType)) {
                planningScoreFieldDescriptor.add(fieldDescriptor);
            } else if (ProblemFactProperty.class.isAssignableFrom(annotationType)) {
                problemFactFieldList.add(fieldDescriptor);
            } else if (ProblemFactCollectionProperty.class.isAssignableFrom(annotationType)) {
                problemFactCollectionFieldList.add(fieldDescriptor);
            }
        }

        String javaMethodName = methodName;
        if (javaMethodName.startsWith("get_") && javaMethodName.length() >= 5) {
            javaMethodName = "get" + Character.toUpperCase(javaMethodName.charAt(4)) + javaMethodName.substring(5);
        }

        MethodCreator methodCreator = classCreator.getMethodCreator(javaMethodName, actualReturnType);
        if (signature != null) {
            methodCreator.setSignature("()" + signature);
        }

        // Create method annotations for each annotation in the list
        for (Map<String, Object> annotation : annotations) {
            // The class representing the annotation is in the annotationType parameter.
            AnnotationCreator annotationCreator = methodCreator.addAnnotation((Class<?>) annotation.get("annotationType"));
            createAnnotation(annotationCreator, annotation);
        }

        // Getter is simply reading the generated field
        methodCreator.returnValue(methodCreator.readInstanceField(fieldDescriptor, methodCreator.getThis()));

        // Assumption: all getters have a setter
        if (methodName.startsWith("get")) {
            String setterMethodName = "set" + methodName.substring(3);
            String javaSetterMethodName = "set" + javaMethodName.substring(3);
            MethodCreator setterMethodCreator =
                    classCreator.getMethodCreator(javaSetterMethodName, void.class, actualReturnType);
            if (signature != null) {
                setterMethodCreator.setSignature("(" + signature + ")V;");
            }

            for (Map<String, Object> annotation : annotations) {
                Class<?> annotationType = (Class<?>) annotation.get("annotationType");
                if (PlanningVariable.class.isAssignableFrom(annotationType) ||
                        (InverseRelationShadowVariable.class.isAssignableFrom(annotationType) &&
                                actualReturnType instanceof Class &&
                                !Collection.class.isAssignableFrom((Class<?>) actualReturnType))
                        ||
                        IndexShadowVariable.class.isAssignableFrom(annotationType) ||
                        AnchorShadowVariable.class.isAssignableFrom(annotationType) ||
                        CustomShadowVariable.class.isAssignableFrom(annotationType)) {
                    planningVariableSetterNameList.add(setterMethodName);
                } else if (PlanningListVariable.class.isAssignableFrom(annotationType) ||
                        (InverseRelationShadowVariable.class.isAssignableFrom(annotationType) &&
                                actualReturnType instanceof Class &&
                                Collection.class.isAssignableFrom((Class<?>) actualReturnType))) {
                    planningListVariableSetterNameList.add(setterMethodName);
                } else if (PlanningScore.class.isAssignableFrom(annotationType)) {
                    planningScoreFieldDescriptorName.add(setterMethodName);
                } else if (PlanningEntityCollectionProperty.class.isAssignableFrom(annotationType)) {
                    planningEntityCollectionGetterList.add(methodName);
                } else if (ProblemFactCollectionProperty.class.isAssignableFrom(annotationType)) {
                    problemFactCollectionGetterList.add(methodName);
                }
            }

            // Use pythonSetterField to set the value on the Python Object (or ignore if score calculation was completely translated to Java)
            ResultHandle setterTriFunction =
                    setterMethodCreator.readInstanceField(pythonSetterField, setterMethodCreator.getThis());
            setterMethodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(TriFunction.class, "apply", Object.class,
                            Object.class, Object.class, Object.class),
                    setterTriFunction,
                    setterMethodCreator.readInstanceField(valueField, setterMethodCreator.getThis()),
                    setterMethodCreator.load(setterMethodName),
                    setterMethodCreator.getMethodParam(0));
            // Update the field on the Pojo
            setterMethodCreator.writeInstanceField(fieldDescriptor, setterMethodCreator.getThis(),
                    setterMethodCreator.getMethodParam(0));
            String fieldName = methodName.substring(3);
            if (fieldName.startsWith("_")) {
                fieldName = fieldName.substring(1);
            }
            ResultHandle valueAsPythonLikeObject;
            if (actualReturnType instanceof Class && PythonLikeObject.class.isAssignableFrom((Class<?>) actualReturnType)) {
                valueAsPythonLikeObject = setterMethodCreator.getMethodParam(0);
            } else {
                valueAsPythonLikeObject = setterMethodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(JavaPythonTypeConversionImplementor.class, "wrapJavaObject",
                                PythonLikeObject.class, Object.class),
                        setterMethodCreator.getMethodParam(0));
            }
            setterMethodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.class, "put", Object.class,
                    Object.class, Object.class),
                    setterMethodCreator.readInstanceField(pythonLikeAttributeField, setterMethodCreator.getThis()),
                    setterMethodCreator.load(fieldName), valueAsPythonLikeObject);

            try {
                Method setterMethod = lookupMethod(parentClass, PythonClassTranslator.getJavaMethodName(setterMethodName));

                Class<?> fieldType;
                try {
                    fieldType = parentClass.getField(PythonClassTranslator.getJavaFieldName(fieldName)).getType();
                } catch (NoSuchFieldException e) {
                    fieldType = null;
                }
                if ((fieldType == null || fieldType.isAssignableFrom(PythonNone.class))
                        && setterMethod.getParameterTypes()[0].isAssignableFrom(PythonNone.class)) {
                    // valueAsPythonLikeObject might be null if the getter returns a PythonLikeObject
                    BranchResult isNullBranchResult = setterMethodCreator.ifNull(valueAsPythonLikeObject);
                    BytecodeCreator currentBranch = isNullBranchResult.trueBranch();
                    currentBranch.invokeSpecialMethod(MethodDescriptor.ofMethod(setterMethod),
                            currentBranch.getThis(),
                            currentBranch.readStaticField(FieldDescriptor.of(PythonNone.class, "INSTANCE", PythonNone.class)));

                    currentBranch = isNullBranchResult.falseBranch();
                    currentBranch.invokeSpecialMethod(MethodDescriptor.ofMethod(setterMethod),
                            currentBranch.getThis(),
                            valueAsPythonLikeObject);
                } else {
                    // Cannot assign None due to typing, so use null
                    BranchResult isNullBranchResult = setterMethodCreator.ifNull(setterMethodCreator.getMethodParam(0));
                    BytecodeCreator currentBranch = isNullBranchResult.falseBranch();

                    BranchResult isNoneBranchResult = currentBranch.ifTrue(
                            currentBranch.instanceOf(currentBranch.getMethodParam(0), PythonNone.class));

                    currentBranch = isNoneBranchResult.falseBranch();
                    currentBranch.invokeSpecialMethod(MethodDescriptor.ofMethod(setterMethod), currentBranch.getThis(),
                            valueAsPythonLikeObject);

                    currentBranch = isNoneBranchResult.trueBranch();
                    currentBranch.invokeSpecialMethod(MethodDescriptor.ofMethod(setterMethod), currentBranch.getThis(),
                            currentBranch.loadNull());

                    currentBranch = isNullBranchResult.trueBranch();
                    currentBranch.invokeSpecialMethod(MethodDescriptor.ofMethod(setterMethod), currentBranch.getThis(),
                            currentBranch.loadNull());
                }
            } catch (NoSuchMethodException e) {
                // Do nothing; no setter
            }
            setterMethodCreator.returnValue(null);
        }
        return fieldDescriptor;
    }

    private static Method lookupMethod(Class<?> declaringClass, String methodName) throws NoSuchMethodException {
        for (Method method : declaringClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException();
    }

    private static void createAnnotation(AnnotationCreator annotationCreator, Map<String, Object> annotation) {
        Class<?> annotationType = (Class<?>) annotation.get("annotationType");
        for (Method method : annotationType.getMethods()) {
            if (method.getParameterCount() != 0
                    || !method.getDeclaringClass().equals(annotation.get("annotationType"))) {
                // skip if the parameter is not from the actual annotation (toString, hashCode, etc.)
                continue;
            }
            Object annotationValue = convertAnnotationValue(annotation.get(method.getName()));

            if (annotationValue != null) {
                annotationCreator.addValue(method.getName(), annotationValue);
            }
        }
    }

    private static Object convertAnnotationValue(Object annotationValue) {
        if (annotationValue == null) {
            return null;
        }
        if (annotationValue.getClass().isArray()) {
            int arrayLength = Array.getLength(annotationValue);
            Object[] out = new Object[arrayLength];
            for (int i = 0; i < out.length; i++) {
                out[i] = convertAnnotationValue(Array.get(annotationValue, i));
            }
            return out;
        }
        if (annotationValue instanceof List) {
            List<?> annotationValueList = (List<?>) annotationValue;
            Object[] out = new Object[annotationValueList.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = convertAnnotationValue(annotationValueList.get(i));
            }
            return out;
        } else if (annotationValue instanceof Map) {
            Map<String, Object> nestedAnnotation = (Map<String, Object>) annotationValue;
            Class<?> nestedAnnotationClass = (Class<?>) nestedAnnotation.get("annotationType");
            AnnotationCreator nestedAnnotationValue =
                    AnnotationCreator.of(nestedAnnotationClass.getName(), RetentionPolicy.RUNTIME);
            createAnnotation(nestedAnnotationValue, nestedAnnotation);
            return nestedAnnotationValue;
        } else {
            return annotationValue;
        }
    }
}
