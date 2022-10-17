package org.optaplanner.jpyinterpreter.types.wrappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.CPythonBackedPythonInterpreter;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.errors.NotImplementedError;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;

public class PythonObjectWrapper extends CPythonBackedPythonLikeObject
        implements PythonLikeObject,
        PythonLikeFunction, Comparable<PythonObjectWrapper> {

    private final static PythonLikeType PYTHON_REFERENCE_TYPE =
            new PythonLikeType("python-reference", PythonObjectWrapper.class),
            $TYPE = PYTHON_REFERENCE_TYPE;
    private final Map<String, PythonLikeObject> cachedAttributeMap;

    public PythonObjectWrapper(OpaquePythonReference pythonReference) {
        super(CPythonType.lookupTypeOfPythonObject(pythonReference), pythonReference);
        cachedAttributeMap = new HashMap<>();
    }

    public OpaquePythonReference getWrappedObject() {
        return $cpythonReference;
    }

    @Override
    public PythonLikeObject __getAttributeOrNull(String attributeName) {
        return cachedAttributeMap.computeIfAbsent(attributeName,
                key -> CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference($cpythonReference,
                        attributeName, $instanceMap));
    }

    @Override
    public void __setAttribute(String attributeName, PythonLikeObject value) {
        cachedAttributeMap.put(attributeName, value);
        CPythonBackedPythonInterpreter.setAttributeOnPythonReference($cpythonReference, attributeName, value);
    }

    @Override
    public void __deleteAttribute(String attributeName) {
        cachedAttributeMap.remove(attributeName);
        CPythonBackedPythonInterpreter.deleteAttributeOnPythonReference($cpythonReference, attributeName);
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        return CPythonBackedPythonInterpreter.callPythonReference($cpythonReference, positionalArguments, namedArguments);
    }

    @Override
    public int compareTo(PythonObjectWrapper other) {
        if (equals(other)) {
            return 0;
        }

        PythonLikeFunction lessThan = (PythonLikeFunction) __getType().__getAttributeOrError("__lt__");
        PythonLikeObject result = lessThan.$call(List.of(this, other), Map.of(), null);

        if (result instanceof PythonBoolean) {
            if (((PythonBoolean) result).getBooleanValue()) {
                return -1;
            } else {
                return 1;
            }
        } else {
            throw new NotImplementedError("Cannot compare " + this.__getType().getTypeName() +
                    " with " + other.__getType().getTypeName());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PythonObjectWrapper)) {
            return false;
        }
        PythonObjectWrapper other = (PythonObjectWrapper) o;
        Object maybeEquals = __getType().__getAttributeOrNull("__eq__");
        if (!(maybeEquals instanceof PythonLikeFunction)) {
            return super.equals(o);
        }
        PythonLikeFunction equals = (PythonLikeFunction) maybeEquals;
        PythonLikeObject result = equals.$call(List.of(this, other), Map.of(), null);
        if (result instanceof PythonBoolean) {
            return ((PythonBoolean) result).getBooleanValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        Object maybeHash = __getType().__getAttributeOrNull("__hash__");
        if (!(maybeHash instanceof PythonLikeFunction)) {
            return super.hashCode();
        }
        PythonLikeFunction hash = (PythonLikeFunction) maybeHash;
        PythonLikeObject result = hash.$call(List.of(this), Map.of(), null);
        if (result instanceof PythonInteger) {
            return ((PythonInteger) result).value.hashCode();
        } else {
            return System.identityHashCode(this);
        }
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public String toString() {
        Object maybeStr = __getType().__getAttributeOrNull("__str__");
        if (!(maybeStr instanceof PythonLikeFunction)) {
            return super.toString();
        }
        PythonLikeFunction str = (PythonLikeFunction) maybeStr;
        PythonLikeObject result = str.$call(List.of(this), Map.of(), null);
        return result.toString();
    }
}
