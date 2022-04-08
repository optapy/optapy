package org.optaplanner.optapy.translator.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.optaplanner.optapy.PythonLikeObject;

public class PythonLikeType implements PythonLikeObject {
    public final static PythonLikeType BASE_TYPE = new PythonLikeType("type", Collections.emptyList());
    public final Map<String, PythonLikeObject> __dir__;

    private final String TYPE_NAME;
    private final List<PythonLikeType> PARENT_TYPES;

    public PythonLikeType(String typeName) {
        this(typeName, List.of(BASE_TYPE));
    }

    public PythonLikeType(String typeName, List<PythonLikeType> parents) {
        TYPE_NAME = typeName;
        PARENT_TYPES = parents;
        __dir__ = new HashMap<>();
    }

    public PythonLikeObject getAttributeOrReturnNull(String attributeName) {
        PythonLikeObject out = __dir__.get(attributeName);
        if (out == null) {
            for (PythonLikeType type : PARENT_TYPES) {
                out = type.getAttributeOrReturnNull(attributeName);
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
    public PythonLikeObject __getattribute__(String attributeName) {
        PythonLikeObject out = getAttributeOrReturnNull(attributeName);
        if (out != null) {
            return out;
        } else {
            throw new NoSuchElementException("type '" + TYPE_NAME + "' does not have attribute '" + attributeName + "'.");
        }
    }

    @Override
    public void __setattribute__(String attributeName, PythonLikeObject value) {
        __dir__.put(attributeName, value);
    }

    @Override
    public PythonLikeType __type__() {
        return BASE_TYPE;
    }

    public String getTypeName() {
        return TYPE_NAME;
    }

    public List<PythonLikeType> getParentList() {
        return PARENT_TYPES;
    }
}