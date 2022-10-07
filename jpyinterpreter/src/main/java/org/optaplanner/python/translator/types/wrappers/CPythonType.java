package org.optaplanner.python.translator.types.wrappers;

import static org.optaplanner.python.translator.types.BuiltinTypes.TYPE_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.CPythonBackedPythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

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
                                return (PythonLikeFunction) (pos, named) -> {
                                    PythonLikeObject result = function.__call__(pos, named);
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
                                return (PythonLikeFunction) (pos, named) -> {
                                    PythonLikeObject result = function.__call__(pos, named);
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
                                return (PythonLikeFunction) (pos, named) -> {
                                    PythonLikeObject result = function.__call__(pos, named);
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
        return TYPE_TYPE;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        return CPythonBackedPythonInterpreter.callPythonReference(pythonReference, positionalArguments, namedArguments);
    }

    public OpaquePythonReference getPythonReference() {
        return pythonReference;
    }
}
