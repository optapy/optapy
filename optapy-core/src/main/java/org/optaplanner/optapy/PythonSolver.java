package org.optaplanner.optapy;

import java.io.Serializable;
import java.util.HashMap;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;

public class PythonSolver {
    public static Object solve(SolverConfig solverConfig, Serializable problem) {
        solverConfig = new SolverConfig().inherit(solverConfig);
        solverConfig.setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        solverConfig.setClassLoader(PythonWrapperGenerator.gizmoClassLoader);
        Solver solver = SolverFactory.create(solverConfig).buildSolver();
        return solver.solve(PythonWrapperGenerator.wrap(solverConfig.getSolutionClass(), problem, new HashMap<>()));
    }
}
