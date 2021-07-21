from optapy import getClass, SolverConfig, solve, Duration
from domain import TimeTable, Lesson, generateProblem

try:
    import java
    java.type('java.lang.String')
    from graalconstraints import defineConstraints
except:
    from constraints import defineConstraints


solverConfig = SolverConfig().withEntityClasses(getClass(Lesson)) \
    .withSolutionClass(getClass(TimeTable)) \
    .withConstraintProviderClass(getClass(defineConstraints)) \
    .withTerminationSpentLimit(Duration.ofSeconds(30))

solution = solve(solverConfig, generateProblem())

print(solution)