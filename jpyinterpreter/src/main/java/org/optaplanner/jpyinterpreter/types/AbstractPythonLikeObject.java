package org.optaplanner.jpyinterpreter.types;

import java.util.HashMap;
import java.util.Map;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.errors.AttributeError;

public abstract class AbstractPythonLikeObject implements PythonLikeObject {

    public static final PythonLikeType OBJECT_TYPE = new PythonLikeType("object", AbstractPythonLikeObject.class);

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
        if (!__dir__.containsKey(attributeName)) {
            throw new AttributeError("'" + __getType().getTypeName() + "' object has no attribute '" + attributeName + "'");
        }
        __dir__.remove(attributeName);
    }

    @Override
    public PythonLikeType __getType() {
        return __type__;
    }

    public void setAttribute(String attributeName, PythonLikeObject value) {
        __dir__.put(attributeName, value);
    }
}
