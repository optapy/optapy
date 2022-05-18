package org.optaplanner.python.translator.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.CPythonBackedPythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;

public class PythonObjectWrapper implements PythonLikeObject, PythonLikeFunction, Comparable<PythonObjectWrapper> {

    private final static PythonLikeType PYTHON_REFERENCE_TYPE = new PythonLikeType("python-reference");

    private final OpaquePythonReference pythonReference;
    private final Map<String, PythonLikeObject> cachedAttributeMap;

    private final CPythonType type;

    public PythonObjectWrapper(OpaquePythonReference pythonReference) {
        this.pythonReference = pythonReference;
        cachedAttributeMap = new HashMap<>();
        type = CPythonType.lookupTypeOfPythonObject(pythonReference);
    }

    public OpaquePythonReference getWrappedObject() {
        return pythonReference;
    }

    @Override
    public PythonLikeObject __getAttributeOrNull(String attributeName) {
        return cachedAttributeMap.computeIfAbsent(attributeName,
                key -> CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference(pythonReference,
                        attributeName));
    }

    @Override
    public void __setAttribute(String attributeName, PythonLikeObject value) {
        cachedAttributeMap.put(attributeName, value);
        CPythonBackedPythonInterpreter.setAttributeOnPythonReference(pythonReference, attributeName, value);
    }

    @Override
    public void __deleteAttribute(String attributeName) {
        cachedAttributeMap.remove(attributeName);
        CPythonBackedPythonInterpreter.deleteAttributeOnPythonReference(pythonReference, attributeName);
    }

    @Override
    public PythonLikeType __getType() {
        return type;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        return CPythonBackedPythonInterpreter.callPythonReference(pythonReference, positionalArguments, namedArguments);
    }

    @Override
    public int compareTo(PythonObjectWrapper other) {
        if (equals(other)) {
            return 0;
        }

        PythonLikeFunction lessThan = (PythonLikeFunction) __getAttributeOrError("__lt__");
        PythonBoolean result = (PythonBoolean) lessThan.__call__(List.of(other), Map.of());
        if (result.getValue()) {
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
        PythonLikeFunction equals = (PythonLikeFunction) __getAttributeOrError("__eq__");
        PythonBoolean result = (PythonBoolean) equals.__call__(List.of(other), Map.of());
        return result.getValue();
    }

    @Override
    public int hashCode() {
        PythonLikeFunction hash = (PythonLikeFunction) __getAttributeOrError("__hash__");
        PythonInteger result = (PythonInteger) hash.__call__(List.of(), Map.of());
        return result.value.hashCode();
    }

    @Override
    public String toString() {
        PythonLikeFunction str = (PythonLikeFunction) __getAttributeOrError("__str__");
        PythonString result = (PythonString) str.__call__(List.of(), Map.of());
        return result.toString();
    }
}
