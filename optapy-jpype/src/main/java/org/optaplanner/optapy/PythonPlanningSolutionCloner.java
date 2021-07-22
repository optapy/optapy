package org.optaplanner.optapy;

import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;

import java.io.Serializable;
import java.util.function.Function;

public class PythonPlanningSolutionCloner implements SolutionCloner {
    private static Function<PythonObject, Serializable> deepClonePythonObject;

    public static void setDeepClonePythonObject(Function<PythonObject, Serializable> cloner) {
        deepClonePythonObject = cloner;
    }

    @Override
    public Object cloneSolution(Object o) {
        Serializable deepCloneId = deepClonePythonObject.apply((PythonObject) o);
        return PythonWrapperGenerator.wrap(o.getClass(), deepCloneId);
    }
}
