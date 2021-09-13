package org.optaplanner.optapy;

import java.util.HashMap;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

@SuppressWarnings("unused")
public class PythonSolver {
    public static Object solve(SolverConfig solverConfig, OpaquePythonReference problem) {
        // Create a copy of the SolverConfig
        solverConfig = new SolverConfig().inherit(solverConfig);

        // Use the Gizmo Class Loader so OptaPlanner can find our generated classes
        solverConfig.setClassLoader(PythonWrapperGenerator.gizmoClassLoader);
        Solver<Object> solver = SolverFactory.create(solverConfig).buildSolver();

        // Wrap the problem into a PythonObject then solve it
        return solver.solve(PythonWrapperGenerator.wrap(solverConfig.getSolutionClass(), problem, new HashMap<>()));
    }
}
