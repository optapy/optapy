package org.optaplanner.python.translator.types.wrappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.CPythonBackedPythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.CPythonBackedPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

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
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        return CPythonBackedPythonInterpreter.callPythonReference($cpythonReference, positionalArguments, namedArguments);
    }

    @Override
    public int compareTo(PythonObjectWrapper other) {
        if (equals(other)) {
            return 0;
        }

        PythonLikeFunction lessThan = (PythonLikeFunction) __getType().__getAttributeOrError("__lt__");
        PythonBoolean result = (PythonBoolean) lessThan.__call__(List.of(this, other), Map.of());
        if (result.getBooleanValue()) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PythonObjectWrapper)) {
            return false;
        }
        PythonObjectWrapper other = (PythonObjectWrapper) o;
        Object maybeEquals = __getType().__getAttributeOrNull("__eq__");
        if (!(maybeEquals instanceof PythonLikeFunction)) {
            return super.equals(o);
        }
        PythonLikeFunction equals = (PythonLikeFunction) maybeEquals;
        PythonLikeObject result = equals.__call__(List.of(this, other), Map.of());
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
        PythonInteger result = (PythonInteger) hash.__call__(List.of(this), Map.of());
        return result.value.hashCode();
    }

    @Override
    public String toString() {
        Object maybeStr = __getType().__getAttributeOrNull("__str__");
        if (!(maybeStr instanceof PythonLikeFunction)) {
            return super.toString();
        }
        PythonLikeFunction str = (PythonLikeFunction) maybeStr;
        PythonString result = (PythonString) str.__call__(List.of(this), Map.of());
        return result.toString();
    }
}
