from optapy import getClass, SolverConfig, PythonSolver
from constraints import defineConstraints
from domain import TimeTable, Lesson, generateProblem
import java

Duration = java.type("java.time.Duration")

solverConfig = SolverConfig().withEntityClasses(getClass(Lesson)) \
    .withSolutionClass(getClass(TimeTable)) \
    .withConstraintProviderClass(getClass(defineConstraints)) \
    .withTerminationSpentLimit(Duration.ofSeconds(30))

solution = PythonSolver.solve(solverConfig, generateProblem())

print(solution)