package org.optaplanner.optapy.translator.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.PythonBinaryOperators;
import org.optaplanner.optapy.translator.PythonTernaryOperators;
import org.optaplanner.optapy.translator.PythonUnaryOperator;
import org.optaplanner.optapy.translator.builtins.ObjectBuiltinOperations;

public class PythonLikeType implements PythonLikeObject {
    public final static PythonLikeType BASE_TYPE = new PythonLikeType("type", Collections.emptyList());
    public final Map<String, PythonLikeObject> __dir__;

    private final String TYPE_NAME;
    private final List<PythonLikeType> PARENT_TYPES;

    static {
        try {
            BASE_TYPE.__dir__.put(PythonBinaryOperators.GET_ATTRIBUTE.getDunderMethod(),
                                  new JavaMethodReference(ObjectBuiltinOperations.class.getMethod("getAttribute", PythonLikeObject.class, String.class),
                                                          Map.of("self", 0, "name", 1)));
            BASE_TYPE.__dir__.put(PythonTernaryOperators.SET_ATTRIBUTE.getDunderMethod(),
                                  new JavaMethodReference(ObjectBuiltinOperations.class.getMethod("setAttribute", PythonLikeObject.class, String.class,
                                                                                                  PythonLikeObject.class),
                                                          Map.of("self", 0, "name", 1, "value", 2)));
            BASE_TYPE.__dir__.put(PythonBinaryOperators.DELETE_ATTRIBUTE.getDunderMethod(),
                                  new JavaMethodReference(ObjectBuiltinOperations.class.getMethod("deleteAttribute", PythonLikeObject.class, String.class),
                                                          Map.of("self", 0, "name", 1)));
            BASE_TYPE.__dir__.put(PythonBinaryOperators.FORMAT.getDunderMethod(),
                                  new JavaMethodReference(ObjectBuiltinOperations.class.getMethod("formatPythonObject", PythonLikeObject.class, PythonLikeObject.class),
                                                          Map.of("self", 0, "format", 1)));
            BASE_TYPE.__dir__.put(PythonUnaryOperator.AS_STRING.getDunderMethod(),
                                  new JavaMethodReference(Object.class.getMethod("toString"), Map.of()));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    public PythonLikeType(String typeName) {
        this(typeName, List.of(BASE_TYPE));
    }

    public PythonLikeType(String typeName, List<PythonLikeType> parents) {
        TYPE_NAME = typeName;
        PARENT_TYPES = parents;
        __dir__ = new HashMap<>();
    }

    public PythonLikeType(String typeName, Consumer<PythonLikeType> initializer) {
        this(typeName, List.of(BASE_TYPE));
        initializer.accept(this);
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
}
