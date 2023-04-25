package org.optaplanner.jpyinterpreter.types.wrappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.CPythonBackedPythonInterpreter;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;

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
        switch (attributeName) {
            case "__eq__":
                return cachedAttributeMap.computeIfAbsent(attributeName,
                        key -> {
                            PythonLikeObject equals =
                                    CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference(pythonReference,
                                            attributeName);
                            if (equals instanceof PythonLikeFunction) {
                                final PythonLikeFunction function = (PythonLikeFunction) equals;
                                return (PythonLikeFunction) (pos, named, callerInstance) -> {
                                    PythonLikeObject result = function.$call(pos, named, null);
                                    if (result instanceof PythonBoolean) {
                                        return result;
                                    } else {
                                        return PythonBoolean.valueOf(pos.get(0) == pos.get(1));
                                    }
                                };
                            } else {
                                return equals;
                            }
                        });
            case "__ne__":
                return cachedAttributeMap.computeIfAbsent(attributeName,
                        key -> {
                            PythonLikeObject notEquals =
                                    CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference(pythonReference,
                                            attributeName);
                            if (notEquals instanceof PythonLikeFunction) {
                                final PythonLikeFunction function = (PythonLikeFunction) notEquals;
                                return (PythonLikeFunction) (pos, named, callerInstance) -> {
                                    PythonLikeObject result = function.$call(pos, named, null);
                                    if (result instanceof PythonBoolean) {
                                        return result;
                                    } else {
                                        return PythonBoolean.valueOf(pos.get(0) != pos.get(1));
                                    }
                                };
                            } else {
                                return notEquals;
                            }
                        });
            case "__hash__":
                return cachedAttributeMap.computeIfAbsent(attributeName,
                        key -> {
                            PythonLikeObject hash =
                                    CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference(pythonReference,
                                            attributeName);
                            if (hash instanceof PythonLikeFunction) {
                                final PythonLikeFunction function = (PythonLikeFunction) hash;
                                return (PythonLikeFunction) (pos, named, callerInstance) -> {
                                    PythonLikeObject result = function.$call(pos, named, null);
                                    if (result instanceof PythonInteger) {
                                        return result;
                                    } else {
                                        return PythonInteger.valueOf(System.identityHashCode(pos.get(0)));
                                    }
                                };
                            } else {
                                return hash;
                            }
                        });
            default:
                return cachedAttributeMap.computeIfAbsent(attributeName,
                        key -> CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference(pythonReference,
                                attributeName));
        }
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
        return BuiltinTypes.TYPE_TYPE;
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        return CPythonBackedPythonInterpreter.callPythonReference(pythonReference, positionalArguments, namedArguments);
    }

    public OpaquePythonReference getPythonReference() {
        return pythonReference;
    }
}
