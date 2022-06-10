from ..optaplanner_java_interop import ensure_init
from ..jpype_type_conversions import _convert_to_java_compatible_object
import jpype.imports  # noqa
from jpype import JImplements as _JImplements, JOverride as _JOverride, JObject as _JObject
import inspect
from typing import TYPE_CHECKING

ensure_init()

Joiners = None
from org.optaplanner.core.api.score.stream import Joiners as JavaJoiners, \
    ConstraintCollectors as JavaConstraintCollectors, Constraint, ConstraintFactory  # noqa
from org.optaplanner.core.api.score.constraint import ConstraintMatch, ConstraintMatchTotal
from org.optaplanner.core.api.score import Score as _Score

if TYPE_CHECKING:
    Joiners = JavaJoiners
    ConstraintCollectors = JavaConstraintCollectors


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

# Below is needed so unknown Python objects can properly be proxied
from jpype import JImplements, JOverride # noqa
from ..constraint_stream import function_cast as _cast, predicate_cast as _filtering_cast, \
    to_int_function_cast as _int_function_cast


@JImplements('java.util.Comparator', deferred=True)
class _PythonComparator:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def compare(self, a, b):
        return self.delegate(a, b)


@JImplements('java.util.function.BinaryOperator', deferred=True)
class _PythonBinaryOperator:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, a, b):
        return self.delegate(a, b)


@JImplements('java.util.function.IntFunction', deferred=True)
class _PythonIntFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, a):
        return self.delegate(a)


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


class _ConstraintCollectorsWrapper:
    @staticmethod
    def average(group_value_mapping):
        return JavaConstraintCollectors.average(_int_function_cast(group_value_mapping))

    @staticmethod
    def compose(*args):
        if len(args) < 3:  # Need at least two collectors + 1 compose function
            raise ValueError
        collectors = args[:-1]
        compose_function = args[-1]
        compose_args = (*collectors, _cast(compose_function))
        return JavaConstraintCollectors.compose(*compose_args)

    @staticmethod
    def conditionally(predicate, delegate):
        return JavaConstraintCollectors.conditionally(_filtering_cast(predicate), delegate)

    @staticmethod
    def count():
        return JavaConstraintCollectors.count()

    @staticmethod
    def countBi():
        return JavaConstraintCollectors.countBi()

    @staticmethod
    def countTri():
        return JavaConstraintCollectors.countTri()

    @staticmethod
    def countQuad():
        return JavaConstraintCollectors.countQuad()

    @staticmethod
    def countDistinct(function=None):
        if function is None:
            return JavaConstraintCollectors.countDistinct()
        else:
            return JavaConstraintCollectors.countDistinct(_cast(function)) # noqa

    @staticmethod
    def max(function=None, comparator=None):
        if function is None and comparator is None:
            return JavaConstraintCollectors.max()
        elif function is not None and comparator is None:
            return JavaConstraintCollectors.max(_cast(function)) # noqa
        elif function is None and comparator is not None:
            return JavaConstraintCollectors.max(_PythonComparator(comparator)) # noqa
        else:
            return JavaConstraintCollectors.max(_cast(function), _PythonComparator(comparator)) # noqa

    @staticmethod
    def min(function=None, comparator=None):
        if function is None and comparator is None:
            return JavaConstraintCollectors.min()
        elif function is not None and comparator is None:
            return JavaConstraintCollectors.min(_cast(function)) # noqa
        elif function is None and comparator is not None:
            return JavaConstraintCollectors.min(_PythonComparator(comparator)) # noqa
        else:
            return JavaConstraintCollectors.min(_cast(function), _PythonComparator(comparator)) # noqa

    @staticmethod
    def sum(function, zero=None, adder=None, subtractor=None):
        if zero is None and adder is None and subtractor is None:
            return JavaConstraintCollectors.sum(_int_function_cast(function))
        elif zero is not None and adder is not None and subtractor is not None:
            return JavaConstraintCollectors.sum(_cast(function), zero, # noqa
                                                _PythonBinaryOperator(adder), _PythonBinaryOperator(subtractor))
        else:
            raise ValueError

    @staticmethod
    def toList(group_value_mapping=None):
        if group_value_mapping is None:
            return JavaConstraintCollectors.toList()
        else:
            return JavaConstraintCollectors.toList(_cast(group_value_mapping)) # noqa

    @staticmethod
    def toSet(group_value_mapping=None):
        if group_value_mapping is None:
            return JavaConstraintCollectors.toSet()
        else:
            return JavaConstraintCollectors.toSet(_cast(group_value_mapping)) # noqa

    @staticmethod
    def toSortedSet(group_value_mapping=None, comparator=None):
        if group_value_mapping is None and comparator is None:
            return JavaConstraintCollectors.toSortedSet()
        elif group_value_mapping is not None and comparator is None:
            return JavaConstraintCollectors.toSortedSet(_cast(group_value_mapping))

    @staticmethod
    def toMap(key_mapper, value_mapper, merge_function_or_set_creator=None):
        if merge_function_or_set_creator is None:
            return JavaConstraintCollectors.toMap(_cast(key_mapper), _cast(value_mapper))

        arg_count = len(inspect.signature(merge_function_or_set_creator).parameters)
        if arg_count == 1:  # set_creator
            return JavaConstraintCollectors.toMap(_cast(key_mapper), _cast(value_mapper), # noqa
                                                  _PythonIntFunction(merge_function_or_set_creator))
        elif arg_count == 2:  # merge_function
            return JavaConstraintCollectors.toMap(_cast(key_mapper), _cast(value_mapper), # noqa
                                                  _PythonBinaryOperator(merge_function_or_set_creator))
        else:
            raise ValueError

    @staticmethod
    def toSortedMap(key_mapper, value_mapper, merge_function_or_set_creator=None):
        if merge_function_or_set_creator is None:
            return JavaConstraintCollectors.toSortedMap(_cast(key_mapper), _cast(value_mapper))

        arg_count = len(inspect.signature(merge_function_or_set_creator).parameters)
        if arg_count == 1:  # set_creator
            return JavaConstraintCollectors.toSortedMap(_cast(key_mapper), _cast(value_mapper), # noqa
                                                        _PythonIntFunction(merge_function_or_set_creator))
        elif arg_count == 2:  # merge_function
            return JavaConstraintCollectors.toSortedMap(_cast(key_mapper), _cast(value_mapper), # noqa
                                                        _PythonBinaryOperator(merge_function_or_set_creator))
        else:
            raise ValueError


if not TYPE_CHECKING:
    Joiners = _JoinersWrapper()
    ConstraintCollectors = _ConstraintCollectorsWrapper
