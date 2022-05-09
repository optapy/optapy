package org.optaplanner.optapy;

import java.util.function.Function;

import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.python.translator.types.OpaquePythonReference;

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
            pythonSetter = (TriFunction<OpaquePythonReference, String, Object, Object>)
                    toClone.getClass().getField(PythonWrapperGenerator.PYTHON_SETTER_FIELD_NAME).get(toClone);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (pythonSetter == PythonWrapperGenerator.NONE_PYTHON_SETTER) {
            toClone.forceUpdate();
        }

        OpaquePythonReference planningClone = deepClonePythonObject.apply(toClone);

        // Wrap the deep cloned OpaquePythonReference into a new PythonObject
        return PythonWrapperGenerator.wrap(o.getClass(), planningClone, toClone.get__optapy_reference_map(),
                                           pythonSetter);
    }
}
