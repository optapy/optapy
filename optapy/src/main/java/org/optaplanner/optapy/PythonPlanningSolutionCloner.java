package org.optaplanner.optapy;

import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;

import java.util.function.Function;

public class PythonPlanningSolutionCloner implements SolutionCloner {
    private static Function<PythonObject, String> deepClonePythonObject;

    public static void setDeepClonePythonObject(Function<PythonObject, String> cloner) {
        deepClonePythonObject = cloner;
    }

    @Override
    public Object cloneSolution(Object o) {
        String deepCloneId = deepClonePythonObject.apply((PythonObject) o);
        return PythonWrapperGenerator.wrap(o.getClass(), deepCloneId);
    }
}
