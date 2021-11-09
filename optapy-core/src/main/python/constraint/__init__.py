from ..optaplanner_java_interop import ensure_init
import jpype.imports  # noqa
import inspect

ensure_init()

from org.optaplanner.core.api.score.stream import Joiners, ConstraintCollectors, Constraint, ConstraintFactory  # noqa
from ..optaplanner_java_interop import PythonFunction as _PythonFunction, PythonBiFunction as _PythonBiFunction, \
    PythonTriFunction as _PythonTriFunction, PythonQuadFunction as _PythonQuadFunction,\
    PythonPentaFunction as _PythonPentaFunction

def cast(function):
    arg_count = len(inspect.signature(function).parameters)
    print(arg_count)
    if arg_count == 1:
        return _PythonFunction(function)
    elif arg_count == 2:
        return _PythonBiFunction(function)
    elif arg_count == 3:
        return _PythonTriFunction(function)
    elif arg_count == 4:
        return _PythonQuadFunction(function)
    elif arg_count == 5:
        return _PythonPentaFunction(function)


