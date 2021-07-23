from optapy import getClass, solve
from optapy.types import SolverConfig, Duration
from domain import TimeTable, Lesson, generateProblem

try:
    # Check to determine if we are in GraalPython
    # (GraalPython and JPype use different name mangling methods
    # so the constraint files is very sightly different
    # (GraalPython uses ["from"](...), JPype uses from_(...))
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