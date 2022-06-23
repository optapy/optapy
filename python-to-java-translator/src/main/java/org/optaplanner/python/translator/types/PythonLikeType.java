package org.optaplanner.python.translator.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.objectweb.asm.Type;
import org.optaplanner.python.translator.FieldDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonTernaryOperators;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.ObjectBuiltinOperations;

public class PythonLikeType implements PythonLikeObject,
        PythonLikeFunction {
    private static PythonLikeType BASE_TYPE; // Initialized in {@link #getBaseType()}
    public final static PythonLikeType TYPE_TYPE = getTypeType();
    public final Map<String, PythonLikeObject> __dir__;

    private final String TYPE_NAME;

    private final String JAVA_TYPE_INTERNAL_NAME;
    private final List<PythonLikeType> PARENT_TYPES;

    private final Map<String, PythonKnownFunctionType> functionNameToKnownFunctionType;

    private final Map<String, FieldDescriptor> instanceFieldToFieldDescriptorMap;

    private PythonLikeFunction constructor;

    public PythonLikeType(String typeName, Class<? extends PythonLikeObject> javaClass) {
        this(typeName, javaClass, List.of(getBaseType()));
    }

    public PythonLikeType(String typeName, Class<? extends PythonLikeObject> javaClass, List<PythonLikeType> parents) {
        TYPE_NAME = typeName;
        JAVA_TYPE_INTERNAL_NAME = Type.getInternalName(javaClass);
        PARENT_TYPES = parents;
        constructor = (positional, keywords) -> {
            throw new UnsupportedOperationException("Cannot create instance of type (" + TYPE_NAME + ").");
        };
        __dir__ = new HashMap<>();
        functionNameToKnownFunctionType = new HashMap<>();
        instanceFieldToFieldDescriptorMap = new HashMap<>();
    }

    public PythonLikeType(String typeName, String javaTypeInternalName, List<PythonLikeType> parents) {
        TYPE_NAME = typeName;
        JAVA_TYPE_INTERNAL_NAME = javaTypeInternalName;
        PARENT_TYPES = parents;
        constructor = (positional, keywords) -> {
            throw new UnsupportedOperationException("Cannot create instance of type (" + TYPE_NAME + ").");
        };
        __dir__ = new HashMap<>();
        functionNameToKnownFunctionType = new HashMap<>();
        instanceFieldToFieldDescriptorMap = new HashMap<>();
    }

    public PythonLikeType(String typeName, Class<? extends PythonLikeObject> javaClass, Consumer<PythonLikeType> initializer) {
        this(typeName, javaClass, List.of(getBaseType()));
        initializer.accept(this);
    }

    public boolean isInstance(PythonLikeObject object) {
        PythonLikeType objectType = object.__getType();
        return objectType.isSubclassOf(this);
    }

    public static PythonLikeType getBaseType() {
        if (BASE_TYPE == null) {
            BASE_TYPE = new PythonLikeType("base-object", PythonLikeObject.class, Collections.emptyList());
            try {
                BASE_TYPE.__dir__.put(PythonBinaryOperators.GET_ATTRIBUTE.getDunderMethod(),
                        new JavaMethodReference(
                                ObjectBuiltinOperations.class.getMethod("getAttribute", PythonLikeObject.class, String.class),
                                Map.of("self", 0, "name", 1)));
                BASE_TYPE.__dir__.put(PythonTernaryOperators.SET_ATTRIBUTE.getDunderMethod(),
                        new JavaMethodReference(
                                ObjectBuiltinOperations.class.getMethod("setAttribute", PythonLikeObject.class, String.class,
                                        PythonLikeObject.class),
                                Map.of("self", 0, "name", 1, "value", 2)));
                BASE_TYPE.__dir__.put(PythonBinaryOperators.DELETE_ATTRIBUTE.getDunderMethod(),
                        new JavaMethodReference(
                                ObjectBuiltinOperations.class.getMethod("deleteAttribute", PythonLikeObject.class,
                                        String.class),
                                Map.of("self", 0, "name", 1)));
                BASE_TYPE.__dir__.put(PythonBinaryOperators.FORMAT.getDunderMethod(),
                        new JavaMethodReference(
                                ObjectBuiltinOperations.class.getMethod("formatPythonObject", PythonLikeObject.class,
                                        PythonLikeObject.class),
                                Map.of("self", 0, "format", 1)));
                BASE_TYPE.__dir__.put(PythonUnaryOperator.AS_STRING.getDunderMethod(),
                        new JavaMethodReference(Object.class.getMethod("toString"), Map.of()));
                BASE_TYPE.__dir__.put(PythonBinaryOperators.EQUAL.getDunderMethod(),
                        new JavaMethodReference(Object.class.getMethod("equals", Object.class),
                                Map.of()));
                BASE_TYPE.__dir__.put(PythonBinaryOperators.NOT_EQUAL.getDunderMethod(),
                        new BinaryLambdaReference((a, b) -> ((PythonBoolean) (((PythonLikeFunction) (a.__getType()
                                .__getAttributeOrError("__eq__")))
                                        .__call__(List.of(a, b), Map.of()))).not(),
                                Map.of()));
                BASE_TYPE.__dir__.put(PythonUnaryOperator.HASH.getDunderMethod(),
                        new JavaMethodReference(Object.class.getMethod("hashCode"), Map.of()));
                BASE_TYPE.setConstructor((vargs, kwargs) -> new AbstractPythonLikeObject(BASE_TYPE) {
                });
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        return BASE_TYPE;
    }

    public static PythonLikeType getTypeType() {
        if (TYPE_TYPE != null) {
            return TYPE_TYPE;
        }
        return new PythonLikeType("type", PythonLikeType.class);
    }

    public void addMethod(PythonUnaryOperator operator, PythonFunctionSignature method) {
        addMethod(operator.getDunderMethod(), method);
    }

    public void addMethod(PythonBinaryOperators operator, PythonFunctionSignature method) {
        addMethod(operator.getDunderMethod(), method);
    }

    public void addMethod(PythonTernaryOperators operator, PythonFunctionSignature method) {
        addMethod(operator.getDunderMethod(), method);
    }

    public void addMethod(String methodName, PythonFunctionSignature method) {
        PythonKnownFunctionType knownFunctionType = functionNameToKnownFunctionType.computeIfAbsent(methodName,
                key -> new PythonKnownFunctionType(methodName, new ArrayList<>()));
        knownFunctionType.getOverloadFunctionSignatureList().add(method);
    }

    public Set<String> getKnownMethods() {
        Set<String> out = new HashSet<>();
        getAssignableTypesStream().forEach(type -> out.addAll(type.functionNameToKnownFunctionType.keySet()));
        return out;
    }

    public void setConstructor(PythonLikeFunction constructor) {
        this.constructor = constructor;
    }

    public Optional<PythonKnownFunctionType> getMethodType(String methodName) {
        PythonKnownFunctionType out = new PythonKnownFunctionType(methodName, new ArrayList<>());
        getAssignableTypesStream().forEach(type -> {
            PythonKnownFunctionType knownFunctionType = type.functionNameToKnownFunctionType.get(methodName);
            if (knownFunctionType != null) {
                out.getOverloadFunctionSignatureList().addAll(knownFunctionType.getOverloadFunctionSignatureList());
            }
        });

        if (out.getOverloadFunctionSignatureList().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(out);
    }

    public Optional<FieldDescriptor> getInstanceFieldDescriptor(String fieldName) {
        return getAssignableTypesStream().map(PythonLikeType::getInstanceFieldToFieldDescriptorMap)
                .filter(map -> map.containsKey(fieldName))
                .map(map -> map.get(fieldName))
                .findAny();
    }

    public void addInstanceField(FieldDescriptor fieldDescriptor) {
        Optional<FieldDescriptor> maybeExistingField = getInstanceFieldDescriptor(fieldDescriptor.getPythonFieldName());
        if (maybeExistingField.isPresent()) {
            PythonLikeType existingFieldType = maybeExistingField.get().getFieldPythonLikeType();
            if (!fieldDescriptor.getFieldPythonLikeType().isSubclassOf(existingFieldType)) {
                throw new IllegalStateException("Field (" + fieldDescriptor.getPythonFieldName() + ") already exist with type ("
                        +
                        existingFieldType + ") which is not assignable from (" + fieldDescriptor.getFieldPythonLikeType()
                        + ").");
            }
        } else {
            instanceFieldToFieldDescriptorMap.put(fieldDescriptor.getPythonFieldName(), fieldDescriptor);
        }
    }

    private Map<String, FieldDescriptor> getInstanceFieldToFieldDescriptorMap() {
        return instanceFieldToFieldDescriptorMap;
    }

    public PythonLikeType unifyWith(PythonLikeType other) {
        Optional<PythonLikeType> maybeCommonType = other.getAssignableTypesStream().filter(otherType -> {
            if (otherType.isSubclassOf(this)) {
                return true;
            }
            return this.isSubclassOf(otherType);
        }).findAny();

        if (maybeCommonType.isPresent() && maybeCommonType.get() != BASE_TYPE) {
            PythonLikeType commonType = maybeCommonType.get();
            if (commonType.isSubclassOf(this)) {
                return this;
            } else {
                return commonType;
            }
        }

        for (PythonLikeType parent : getParentList()) {
            PythonLikeType parentUnification = parent.unifyWith(other);
            if (parentUnification != BASE_TYPE) {
                return parentUnification;
            }
        }
        return BASE_TYPE;
    }

    public boolean isSubclassOf(PythonLikeType type) {
        return isSubclassOf(type, new HashSet<>());
    }

    private Stream<PythonLikeType> getAssignableTypesStream() {
        return Stream.concat(
                Stream.of(this),
                getParentList().stream()
                        .flatMap(PythonLikeType::getAssignableTypesStream))
                .distinct();
    }

    private boolean isSubclassOf(PythonLikeType type, Set<PythonLikeType> visited) {
        if (visited.contains(this)) {
            return false;
        }

        if (this == type) {
            return true;
        }

        visited.add(this);
        for (PythonLikeType parent : PARENT_TYPES) {
            if (parent.isSubclassOf(type, visited)) {
                return true;
            }
        }
        return false;
    }

    public int getDepth() {
        if (PARENT_TYPES.size() == 0) {
            return 0;
        } else {
            return 1 + PARENT_TYPES.stream().map(PythonLikeType::getDepth).max(Comparator.naturalOrder()).get();
        }
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        return constructor.__call__(positionalArguments, namedArguments);
    }

    public PythonLikeObject __getAttributeOrNull(String attributeName) {
        PythonLikeObject out = __dir__.get(attributeName);
        if (out == null) {
            for (PythonLikeType type : PARENT_TYPES) {
                out = type.__getAttributeOrNull(attributeName);
                if (out != null) {
                    return out;
                }
            }
            return null;
        } else {
            return out;
        }
    }

    @Override
    public void __setAttribute(String attributeName, PythonLikeObject value) {
        __dir__.put(attributeName, value);
    }

    @Override
    public void __deleteAttribute(String attributeName) {
        // TODO: Descriptors: https://docs.python.org/3/howto/descriptor.html
        __dir__.remove(attributeName);
    }

    @Override
    public PythonLikeType __getType() {
        return new PythonLikeGenericType(this);
    }

    public String getTypeName() {
        return TYPE_NAME;
    }

    public String getJavaTypeInternalName() {
        return JAVA_TYPE_INTERNAL_NAME;
    }

    /**
     * Return the Java class corresponding to this type, if it exists. Throws {@link ClassNotFoundException} otherwise.
     */
    public Class<?> getJavaClass() throws ClassNotFoundException {
        return Class.forName(JAVA_TYPE_INTERNAL_NAME.replace('/', '.'), true,
                PythonBytecodeToJavaBytecodeTranslator.asmClassLoader);
    }

    public List<PythonLikeType> getParentList() {
        return PARENT_TYPES;
    }

    @Override
    public String toString() {
        return "<class " + TYPE_NAME + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !PythonLikeType.class.isAssignableFrom(o.getClass())) {
            return false;
        }
        PythonLikeType that = (PythonLikeType) o;
        return JAVA_TYPE_INTERNAL_NAME.equals(that.JAVA_TYPE_INTERNAL_NAME);
    }

    @Override
    public int hashCode() {
        return Objects.hash(JAVA_TYPE_INTERNAL_NAME);
    }
}
