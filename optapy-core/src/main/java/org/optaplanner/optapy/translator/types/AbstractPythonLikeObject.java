package org.optaplanner.optapy.translator.types;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.optaplanner.optapy.PythonLikeObject;

public abstract class AbstractPythonLikeObject implements PythonLikeObject {
    private final PythonLikeType __type__;
    private final Map<String, PythonLikeObject> __dir__;

    public AbstractPythonLikeObject(PythonLikeType __type__) {
        this(__type__, new HashMap<>());
    }

    public AbstractPythonLikeObject(PythonLikeType __type__, Map<String, PythonLikeObject> __dir__) {
        this.__type__ = __type__;
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

    @Override
    public PythonLikeType __type__() {
        return __type__;
    }
}
