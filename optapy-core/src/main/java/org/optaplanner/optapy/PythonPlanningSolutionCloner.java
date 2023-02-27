package org.optaplanner.optapy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;

public class PythonPlanningSolutionCloner implements SolutionCloner<Object> {
    // A function in python that deep clones a given OpaquePythonReference
    private static Function<PythonObject, OpaquePythonReference> deepClonePythonObject;

    @SuppressWarnings("unused")
    public static void setDeepClonePythonObject(Function<PythonObject, OpaquePythonReference> cloner) {
        deepClonePythonObject = cloner;
    }

    @Override
    public Object cloneSolution(Object o) {
        // Deep clone the OpaquePythonReference
        PythonObject toClone = (PythonObject) o;
        TriFunction<OpaquePythonReference, String, Object, Object> pythonSetter;
        try {
            pythonSetter = (TriFunction<OpaquePythonReference, String, Object, Object>) toClone.getClass()
                    .getField(PythonWrapperGenerator.PYTHON_SETTER_FIELD_NAME).get(toClone);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (pythonSetter == PythonWrapperGenerator.NONE_PYTHON_SETTER) {
            toClone.forceUpdate();
        }

        OpaquePythonReference planningClone = deepClonePythonObject.apply(toClone);

        // Wrap the deep cloned OpaquePythonReference into a new PythonObject
        PythonObject out =
                (PythonObject) PythonWrapperGenerator.wrap(o.getClass(), planningClone, toClone.get__optapy_reference_map(),
                        pythonSetter);

        Map<Number, Object> oldIdMap = new HashMap<>();
        Map<Number, Object> newIdMap = new HashMap<>();
        toClone.visitIds(oldIdMap);
        out.visitIds(newIdMap);

        for (Number id : oldIdMap.keySet()) {
            if (!newIdMap.containsKey(id)) {
                toClone.get__optapy_reference_map().remove(id);
            } else {
                newIdMap.remove(id);
            }
        }

        toClone.get__optapy_reference_map().putAll(newIdMap);

        // Mirror the reference map (not pass a reference to it)
        // so Score + list variables can be safely garbage collected in Python
        // (if it was not mirrored, reading the python object would add entries for them to the map,
        //  which is used when cloning. If score/list variable was garbage collected by Python, another
        //  Python Object can have the same id, leading to the old value in the map being returned,
        //  causing an exception (or worse, a subtle bug))
        Map<Number, Object> newReferenceMap = new MirrorWithExtrasMap<>(out.get__optapy_reference_map());
        out.readFromPythonObject(Collections.newSetFromMap(new IdentityHashMap<>()),
                newReferenceMap);

        List<Object> referencedValues = new ArrayList<>(newReferenceMap.values());
        for (Object value : referencedValues) {
            if (value instanceof CPythonBackedPythonLikeObject) {
                ((CPythonBackedPythonLikeObject) value).$readFieldsFromCPythonReference();
            }
        }

        return out;
    }
}
