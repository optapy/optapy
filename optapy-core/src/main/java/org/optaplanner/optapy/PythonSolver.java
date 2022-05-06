package org.optaplanner.optapy;

import java.util.HashMap;

@SuppressWarnings("unused")
public class PythonSolver {

    /**
     * If true, only use Java setters and only go back to CPython for cloning.
     * If false, invoking a Java setter also invoke the CPython setter.
     */
    public static boolean onlyUseJavaSetters = false;

    public static Object wrapProblem(Class<?> solutionClass, OpaquePythonReference problem) {
        try {
            final boolean onlyUseJavaSettersForThisInstance = onlyUseJavaSetters;
            onlyUseJavaSetters = false;
            return PythonWrapperGenerator.wrap(solutionClass, problem, new HashMap<>(), onlyUseJavaSettersForThisInstance?
                    PythonWrapperGenerator.NONE_PYTHON_SETTER : PythonWrapperGenerator.pythonObjectIdAndAttributeSetter);
        } catch (Throwable t) {
            throw new OptaPyException("A problem occurred when wrapping the python problem (" +
                    PythonWrapperGenerator.getPythonObjectString(problem) +
                    "). Maybe an annotation was passed an incorrect type " +
                    "(for example, @problem_fact_collection_property(str) " +
                    " on a function that return a list of int).", t);
        }
    }
}
