from ..optaplanner_java_interop import ensure_init
from ..jpype_type_conversions import _convert_to_java_compatible_object
import jpype.imports  # noqa
from jpype import JImplements as _JImplements, JOverride as _JOverride, JObject as _JObject
import inspect
from typing import TYPE_CHECKING

ensure_init()

Joiners = None
from org.optaplanner.core.api.score.stream import Joiners as JavaJoiners, ConstraintCollectors, Constraint, ConstraintFactory  # noqa
from org.optaplanner.core.api.score.constraint import ConstraintMatch, ConstraintMatchTotal
from org.optaplanner.core.api.score import Score as _Score

if TYPE_CHECKING:
    Joiners = JavaJoiners


# Cannot import DefaultConstraintMatchTotal as it is in impl
@_JImplements(ConstraintMatchTotal)
class DefaultConstraintMatchTotal:
    """
    A default implementation of ConstraintMatchTotal that can be used in a constraint match aware
    @incremental_score_calculator.
    """
    def __init__(self, constraint_package: str, constraint_name: str, constraint_weight: _Score = None):
        from java.util import LinkedHashSet
        self.constraint_package = constraint_package
        self.constraint_name = constraint_name
        self.constraint_id = ConstraintMatchTotal.composeConstraintId(constraint_package, constraint_name)
        self.constraint_weight = constraint_weight
        self.constraint_match_set = LinkedHashSet()
        if constraint_weight is not None:
            self.score = constraint_weight.zero()
        else:
            self.score = None

    @_JOverride
    def getConstraintPackage(self):
        return self.constraint_package

    @_JOverride
    def getConstraintName(self):
        return self.constraint_name

    @_JOverride
    def getConstraintWeight(self):
        return self.constraint_weight

    @_JOverride
    def getConstraintMatchSet(self):
        return self.constraint_match_set

    @_JOverride
    def getScore(self):
        return self.score

    @_JOverride
    def getConstraintId(self):
        return self.constraint_id

    @_JOverride
    def compareTo(self, other: 'DefaultConstraintMatchTotal'):
        if self.constraint_id == other.constraint_id:
            return 0
        elif self.constraint_id < other.constraint_id:
            return -1
        else:
            return 1

    def __lt__(self, other):
        return self.constraint_id < other.constraint_id

    def __gt__(self, other):
        return self.constraint_id > other.constraint_id

    @_JOverride
    def equals(self, other):
        if self is other:
            return True
        elif isinstance(other, DefaultConstraintMatchTotal):
            return self.constraint_id == other.constraint_id
        else:
            return False

    def __eq__(self, other):
        return self.constraint_id == other.constraint_id

    @_JOverride
    def hashCode(self):
        return hash(self.constraint_id)

    def __hash__(self):
        return hash(self.constraint_id)

    @_JOverride
    def toString(self):
        return f'{self.constraint_id}={self.score}'


    def addConstraintMatch(self, justification_list: list, score: _Score) -> ConstraintMatch:
        from java.util import Arrays
        self.score = self.score.add(score) if self.score is not None else score
        wrapped_justification_list = Arrays.asList(justification_list)
        constraint_match = ConstraintMatch(self.constraint_package, self.constraint_name, wrapped_justification_list,
                                           score)
        self.constraint_match_set.add(constraint_match)
        return constraint_match

    def removeConstraintMatch(self, constraint_match: ConstraintMatch):
        self.score = self.score.subtract(constraint_match.getScore())
        removed = self.constraint_match_set.remove(constraint_match)
        if not removed:
            raise ValueError(f'The ConstraintMatchTotal ({self}) could not remove the ConstraintMatch'
                             f'({constraint_match}) from its constraint_match_set ({self.constraint_match_set}).')

# Workaround for https://github.com/jpype-project/jpype/issues/1016
# TODO: Remove EVERYTHING below when https://github.com/jpype-project/jpype/issues/1016 is resolved
#       and a new version of JPype is released
from jpype import JImplements, JOverride # noqa
from ..jpype_type_conversions import PythonFunction as _PythonFunction, PythonBiFunction as _PythonBiFunction, \
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


def _cast(function):
    arg_count = len(inspect.signature(function).parameters)
    if arg_count == 1:
        return _PythonFunction(lambda a: _convert_to_java_compatible_object(function(a)))
    elif arg_count == 2:
        return _PythonBiFunction(lambda a, b: _convert_to_java_compatible_object(function(a, b)))
    elif arg_count == 3:
        return _PythonTriFunction(lambda a, b, c: _convert_to_java_compatible_object(function(a, b, c)))
    elif arg_count == 4:
        return _PythonQuadFunction(lambda a, b, c, d: _convert_to_java_compatible_object(function(a, b, c, d)))
    elif arg_count == 5:
        return _PythonPentaFunction(lambda a, b, c, d, e: _convert_to_java_compatible_object(function(a, b, c, d, e)))


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
