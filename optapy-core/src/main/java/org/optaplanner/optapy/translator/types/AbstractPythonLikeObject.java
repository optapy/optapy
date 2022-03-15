package org.optaplanner.optapy.translator.types;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.optaplanner.optapy.PythonLikeObject;

public abstract class AbstractPythonLikeObject implements PythonLikeObject {
    private final Map<String, PythonLikeObject> __dir__;

    public AbstractPythonLikeObject() {
        this(new HashMap<>());
    }

    public AbstractPythonLikeObject(Map<String, PythonLikeObject> __dir__) {
        this.__dir__ = __dir__;
    }

    @Override
    public PythonLikeObject __getattribute__(String attributeName) {
        PythonLikeObject out = __dir__.get(attributeName);
        if (out == null) {
            throw new NoSuchElementException();
        } else {
            return out;
        }
    }

    @Override
    public void __setattribute__(String attributeName, PythonLikeObject value) {
        __dir__.put(attributeName, value);
    }
}
