package org.optaplanner.optapy;

import java.util.HashMap;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.optaplanner.core.config.solver.SolverConfig;

@SuppressWarnings("unused")
public class PythonSolver {
    public static SolverConfig updateSolverConfig(SolverConfig solverConfig) {
        // Create a copy of the SolverConfig
        solverConfig = new SolverConfig().inherit(solverConfig);

        // Use the Gizmo Class Loader so OptaPlanner can find our generated classes
        solverConfig.setClassLoader(PythonWrapperGenerator.gizmoClassLoader);

        return solverConfig;
    }

    public static Object wrapProblem(Class<?> solutionClass, OpaquePythonReference problem) {
        try {
            return PythonWrapperGenerator.wrap(solutionClass, problem, new HashMap<>());
        } catch (Throwable t) {
            throw new OptaPyException("A problem occurred when wrapping the python problem (" +
                    PythonWrapperGenerator.getPythonObjectString(problem) +
                    "). Maybe an annotation was passed an incorrect type " +
                    "(for example, @problem_fact_collection_property(str) " +
                    " on a function that return a list of int).", t);
        }
    }

    public static Object solve(SolverConfig solverConfig, OpaquePythonReference problem, SolverEventListener<Object> listener) {
        // Create a copy of the SolverConfig
        solverConfig = updateSolverConfig(solverConfig);

        Solver<Object> solver = SolverFactory.create(solverConfig).buildSolver();
        Object wrappedProblem;
        try {
            // Wrap the problem into a PythonObject
            // TODO: Maybe use a weak reference map?
            wrappedProblem = PythonWrapperGenerator.wrap(solverConfig.getSolutionClass(), problem, new HashMap<>());
        } catch (Throwable t) {
            throw new OptaPyException("A problem occurred when wrapping the python problem (" +
                    PythonWrapperGenerator.getPythonObjectString(problem) +
                    "). Maybe an annotation was passed an incorrect type " +
                    "(for example, @problem_fact_collection_property(str) " +
                    " on a function that return a list of int).", t);
        }
        if (listener != null) {
            solver.addEventListener(listener);
        }
        return solver.solve(wrappedProblem);
    }
}
