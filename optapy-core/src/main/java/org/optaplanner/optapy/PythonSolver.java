package org.optaplanner.optapy;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;

import org.optaplanner.python.translator.types.OpaquePythonReference;

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
            PythonObject out = (PythonObject) PythonWrapperGenerator.wrap(solutionClass, problem, new HashMap<>(),
                    onlyUseJavaSettersForThisInstance ? PythonWrapperGenerator.NONE_PYTHON_SETTER
                            : PythonWrapperGenerator.pythonObjectIdAndAttributeSetter);
            out.visitIds(out.get__optapy_reference_map());
            out.readFromPythonObject(Collections.newSetFromMap(new IdentityHashMap<>()), out.get__optapy_reference_map());
            return out;
        } catch (Throwable t) {
            throw new OptaPyException("A problem occurred when wrapping the python problem (" +
                    PythonWrapperGenerator.getPythonObjectString(problem) +
                    "). Maybe an annotation was passed an incorrect type " +
                    "(for example, @problem_fact_collection_property(str) " +
                    " on a function that return a list of int).", t);
        }
    }
}
