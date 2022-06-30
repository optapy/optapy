package org.optaplanner.python.translator.types;

import java.util.HashMap;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;

public abstract class CPythonBackedPythonLikeObject extends AbstractPythonLikeObject {
    public static final PythonLikeType CPYTHON_BACKED_OBJECT_TYPE =
            new PythonLikeType("object", CPythonBackedPythonLikeObject.class);

    public OpaquePythonReference $cpythonReference;

    public Map<Number, PythonLikeObject> $instanceMap;

    public CPythonBackedPythonLikeObject(PythonLikeType __type__) {
        this(__type__, (OpaquePythonReference) null);
    }

    public CPythonBackedPythonLikeObject(PythonLikeType __type__, Map<String, PythonLikeObject> __dir__) {
        this(__type__, __dir__, null);
    }

    public CPythonBackedPythonLikeObject(PythonLikeType __type__,
            OpaquePythonReference reference) {
        super(__type__);
        this.$cpythonReference = reference;
        $instanceMap = new HashMap<>();
    }

    public CPythonBackedPythonLikeObject(PythonLikeType __type__,
            Map<String, PythonLikeObject> __dir__,
            OpaquePythonReference reference) {
        super(__type__, __dir__);
        this.$cpythonReference = reference;
        $instanceMap = new HashMap<>();
    }

    public OpaquePythonReference $getCPythonReference() {
        return $cpythonReference;
    }

    public void $setCPythonReference(OpaquePythonReference pythonReference) {
        this.$cpythonReference = pythonReference;
    }

    public Map<Number, PythonLikeObject> $getInstanceMap() {
        return $instanceMap;
    }

    public void $setInstanceMap(Map<Number, PythonLikeObject> $instanceMap) {
        this.$instanceMap = $instanceMap;
    }

    public void $readFieldsFromCPythonReference() {
    }

    public void $writeFieldsToCPythonReference() {
    }
}
