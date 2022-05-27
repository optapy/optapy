package org.optaplanner.python.translator.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.CPythonBackedPythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;

public class CPythonType extends PythonLikeType {

    private static final Map<Number, CPythonType> cpythonTypeMap = new HashMap<>();

    private final OpaquePythonReference pythonReference;
    private final Map<String, PythonLikeObject> cachedAttributeMap;

    private static String getTypeName(OpaquePythonReference pythonReference) {
        return ((PythonString) CPythonBackedPythonInterpreter
                .lookupAttributeOnPythonReference(pythonReference, "__name__"))
                        .getValue();
    }

    public static CPythonType lookupTypeOfPythonObject(OpaquePythonReference reference) {
        OpaquePythonReference type = CPythonBackedPythonInterpreter.getPythonReferenceType(reference);
        return cpythonTypeMap.computeIfAbsent(CPythonBackedPythonInterpreter.getPythonReferenceId(type),
                key -> new CPythonType(type));
    }

    public static CPythonType getType(OpaquePythonReference typeReference) {
        return cpythonTypeMap.computeIfAbsent(CPythonBackedPythonInterpreter.getPythonReferenceId(typeReference),
                key -> new CPythonType(typeReference));
    }

    private CPythonType(OpaquePythonReference pythonReference) {
        super(getTypeName(pythonReference), PythonObjectWrapper.class);
        this.pythonReference = pythonReference;
        this.cachedAttributeMap = new HashMap<>();
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
        return TYPE_TYPE;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        return CPythonBackedPythonInterpreter.callPythonReference(pythonReference, positionalArguments, namedArguments);
    }
}
