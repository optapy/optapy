from ..optaplanner_java_interop import ensure_init, has_java_class
from jpype import JProxy
import jpype.imports  # noqa
import inspect
from typing import TYPE_CHECKING

ensure_init()

Joiners = None
from org.optaplanner.optapy import PythonComparable as _PythonComparable
from org.optaplanner.core.api.score.stream import Joiners as JavaJoiners, ConstraintCollectors, Constraint, ConstraintFactory  # noqa
if TYPE_CHECKING:
    Joiners = JavaJoiners

# Workaround for https://github.com/jpype-project/jpype/issues/1016
# TODO: Remove EVERYTHING below when https://github.com/jpype-project/jpype/issues/1016 is resolved
#       and a new version of JPype is released
from jpype import JImplements, JOverride # noqa
from ..optaplanner_java_interop import PythonFunction as _PythonFunction, PythonBiFunction as _PythonBiFunction, \
    PythonTriFunction as _PythonTriFunction, PythonQuadFunction as _PythonQuadFunction,\
    PythonPentaFunction as _PythonPentaFunction # noqa


# Also need Predicates wrappers for filtering
@JImplements('java.util.function.Predicate', deferred=True)
class _PythonPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument):
        return self.delegate(argument)


@JImplements('java.util.function.BiPredicate', deferred=True)
class _PythonBiPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2):
        return self.delegate(argument1, argument2)


@JImplements('org.optaplanner.core.api.function.TriPredicate', deferred=True)
class _PythonTriPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3):
        return self.delegate(argument1, argument2, argument3)


@JImplements('org.optaplanner.core.api.function.QuadPredicate', deferred=True)
class _PythonQuadPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3, argument4):
        return self.delegate(argument1, argument2, argument3, argument4)


@JImplements('org.optaplanner.core.api.function.PentaPredicate', deferred=True)
class _PythonPentaPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3, argument4, argument5):
        return self.delegate(argument1, argument2, argument3, argument4, argument5)


def _proxy(value):
    from org.optaplanner.optapy import OpaquePythonReference
    return JProxy(OpaquePythonReference, inst=value, convert=True)


# Cannot use weakref.WeakKeyDict for this
_comparable_cache = dict()


def _compute_comparable_if_absent(item):
    if has_java_class(item):
        return item
    if item in _comparable_cache:
        return _comparable_cache[item]
    else:
        comparable = _PythonComparable(_proxy(item))
        _comparable_cache[item] = comparable
        return comparable


def _cast(function):
    arg_count = len(inspect.signature(function).parameters)
    if arg_count == 1:
        return _PythonFunction(lambda a: _compute_comparable_if_absent(function(a)))
    elif arg_count == 2:
        return _PythonBiFunction(lambda a, b: _compute_comparable_if_absent(function(a, b)))
    elif arg_count == 3:
        return _PythonTriFunction(lambda a, b, c: _compute_comparable_if_absent(function(a, b, c)))
    elif arg_count == 4:
        return _PythonQuadFunction(lambda a, b, c, d: _compute_comparable_if_absent(function(a, b, c, d)))
    elif arg_count == 5:
        return _PythonPentaFunction(lambda a, b, c, d, e: _compute_comparable_if_absent(function(a, b, c, d, e)))


def _filtering_cast(predicate):
    arg_count = len(inspect.signature(predicate).parameters)
    if arg_count == 1:
        return _PythonPredicate(predicate)
    elif arg_count == 2:
        return _PythonBiPredicate(predicate)
    elif arg_count == 3:
        return _PythonTriPredicate(predicate)
    elif arg_count == 4:
        return _PythonQuadPredicate(predicate)
    elif arg_count == 5:
        return _PythonPentaPredicate(predicate)


class _JoinersWrapper:
    def __getattr__(self, item):
        is_filtering = item == 'filtering'
        java_method = getattr(JavaJoiners, item)

        def function_wrapper(*args):
            cast_function = _cast
            if is_filtering:
                cast_function = _filtering_cast

            cast_args = tuple(map(lambda arg: cast_function(arg), args))
            return java_method(*cast_args)

        return function_wrapper


if not TYPE_CHECKING:
    Joiners = _JoinersWrapper()
