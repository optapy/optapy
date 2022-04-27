package org.optaplanner.python.translator.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonTernaryOperators;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.ObjectBuiltinOperations;

public class PythonLikeType implements PythonLikeObject,
        PythonLikeFunction {
    private static PythonLikeType BASE_TYPE; // Initialized in {@link #getBaseType()}
    public final Map<String, PythonLikeObject> __dir__;

    private final String TYPE_NAME;
    private final List<PythonLikeType> PARENT_TYPES;

    private final PythonLikeFunction constructor;

    static {
    }

    public PythonLikeType(String typeName) {
        this(typeName, List.of(getBaseType()));
    }

    public PythonLikeType(String typeName, List<PythonLikeType> parents) {
        TYPE_NAME = typeName;
        PARENT_TYPES = parents;
        constructor = (positional, keywords) -> {
            throw new UnsupportedOperationException("Cannot create instance of type (" + TYPE_NAME + ").");
        };
        __dir__ = new HashMap<>();
    }

    public PythonLikeType(String typeName, Consumer<PythonLikeType> initializer) {
        this(typeName, List.of(getBaseType()));
        initializer.accept(this);
    }

    public boolean isInstance(PythonLikeObject object) {
        PythonLikeType objectType = object.__getType();
        return objectType.isSubclassOf(this);
    }

    /**
     *
     * @return
     */
    private static PythonLikeType getBaseType() {
        if (BASE_TYPE == null) {
            BASE_TYPE = new PythonLikeType("type", Collections.emptyList());
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
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        return BASE_TYPE;
    }

    public boolean isSubclassOf(PythonLikeType type) {
        return isSubclassOf(type, new HashSet<>());
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
        return BASE_TYPE;
    }

    public String getTypeName() {
        return TYPE_NAME;
    }

    public List<PythonLikeType> getParentList() {
        return PARENT_TYPES;
    }

    @Override
    public String toString() {
        return "<class " + TYPE_NAME + ">";
    }
}
