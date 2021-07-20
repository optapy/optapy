from optapy import getClass, SolverConfig, solve
from constraints import defineConstraints
from domain import TimeTable, Lesson, generateProblem

import java.time.Duration as Duration

solverConfig = SolverConfig().withEntityClasses(getClass(Lesson)) \
    .withSolutionClass(getClass(TimeTable)) \
    .withConstraintProviderClass(getClass(defineConstraints)) \
    .withTerminationSpentLimit(Duration.ofSeconds(30))

solution = solve(solverConfig, generateProblem())

print(solution)