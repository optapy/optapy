package org.optaplanner.optapy;

import java.util.Collections;
import java.util.IdentityHashMap;

import org.optaplanner.jpyinterpreter.CPythonBackedPythonInterpreter;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;

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
            PythonObject out = (PythonObject) PythonWrapperGenerator.wrap(solutionClass, problem,
                    new MirrorWithExtrasMap<>(CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap),
                    onlyUseJavaSettersForThisInstance ? PythonWrapperGenerator.NONE_PYTHON_SETTER
                            : PythonWrapperGenerator.pythonObjectIdAndAttributeSetter);
            out.visitIds(out.get__optapy_reference_map());

            // Mirror the reference map (not pass a reference to it)
            // so Score + list variables can be safely garbage collected in Python
            // (if it was not mirrored, reading the python object would add entries for them to the map,
            //  which is used when cloning. If score/list variable was garbage collected by Python, another
            //  Python Object can have the same id, leading to the old value in the map being returned,
            //  causing an exception (or worse, a subtle bug))
            out.readFromPythonObject(Collections.newSetFromMap(new IdentityHashMap<>()),
                    new MirrorWithExtrasMap<>(out.get__optapy_reference_map()));
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
