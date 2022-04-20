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
    public PythonLikeObject __getAttributeOrNull(String attributeName) {
        return __dir__.get(attributeName);
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
        return __type__;
    }
}
