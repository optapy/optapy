package org.optaplanner.optapy;

import org.graalvm.polyglot.Value;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

public class PythonSolver {
    public static Object solve(SolverConfig solverConfig, Value problem) {
        solverConfig = new SolverConfig().inherit(solverConfig);
        solverConfig.setClassLoader(PythonWrapperGenerator.gizmoClassLoader);
        solverConfig.getScoreDirectorFactoryConfig().withConstraintStreamImplType(ConstraintStreamImplType.BAVET);
        Solver solver = SolverFactory.create(solverConfig).buildSolver();
        return PythonWrapperGenerator.unwrap(solverConfig.getSolutionClass(),
                solver.solve(PythonWrapperGenerator.wrap(solverConfig.getSolutionClass(), problem)));
    }
}
