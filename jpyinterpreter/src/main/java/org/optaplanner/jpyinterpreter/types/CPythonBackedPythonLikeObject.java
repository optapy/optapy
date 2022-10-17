package org.optaplanner.jpyinterpreter.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;

public class CPythonBackedPythonLikeObject extends AbstractPythonLikeObject implements PythonLikeFunction {
    public static final PythonLikeType CPYTHON_BACKED_OBJECT_TYPE =
            new PythonLikeType("object", CPythonBackedPythonLikeObject.class);

    public OpaquePythonReference $cpythonReference;

    public PythonInteger $cpythonId;

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

    public PythonInteger $getCPythonId() {
        return $cpythonId;
    }

    public void $setCPythonId(PythonInteger $cpythonId) {
        this.$cpythonId = $cpythonId;
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

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        return PythonNone.INSTANCE;
    }
}
