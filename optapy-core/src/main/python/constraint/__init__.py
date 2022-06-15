from ..optaplanner_java_interop import ensure_init
import jpype.imports  # noqa
from jpype import JImplements, JOverride
import inspect

ensure_init()

from org.optaplanner.core.api.score.stream import Joiners as JavaJoiners, \
    ConstraintCollectors as JavaConstraintCollectors, Constraint  # noqa
from org.optaplanner.core.api.score.constraint import ConstraintMatch, ConstraintMatchTotal
from org.optaplanner.core.api.score import Score as _Score
from ..constraint_stream import PythonConstraintFactory as ConstraintFactory, \
    PythonUniConstraintStream as UniConstraintStream, PythonBiConstraintStream as BiConstraintStream, \
    PythonTriConstraintStream as TriConstraintStream, PythonQuadConstraintStream as QuadConstraintStream
from org.optaplanner.core.api.score.stream.uni import UniConstraintCollector
from org.optaplanner.core.api.score.stream.bi import BiJoiner, BiConstraintCollector
from org.optaplanner.core.api.score.stream.tri import TriJoiner, TriConstraintCollector
from org.optaplanner.core.api.score.stream.quad import QuadJoiner, QuadConstraintCollector
from org.optaplanner.core.api.score.stream.penta import PentaJoiner
from typing import List, Set, Dict, Callable, overload, TypeVar, Any


# Cannot import DefaultConstraintMatchTotal as it is in impl
@JImplements(ConstraintMatchTotal)
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

    @JOverride
    def getConstraintPackage(self):
        return self.constraint_package

    @JOverride
    def getConstraintName(self):
        return self.constraint_name

    @JOverride
    def getConstraintWeight(self):
        return self.constraint_weight

    @JOverride
    def getConstraintMatchSet(self):
        return self.constraint_match_set

    @JOverride
    def getScore(self):
        return self.score

    @JOverride
    def getConstraintId(self):
        return self.constraint_id

    @JOverride
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

    @JOverride
    def equals(self, other):
        if self is other:
            return True
        elif isinstance(other, DefaultConstraintMatchTotal):
            return self.constraint_id == other.constraint_id
        else:
            return False

    def __eq__(self, other):
        return self.constraint_id == other.constraint_id

    @JOverride
    def hashCode(self):
        return hash(self.constraint_id)

    def __hash__(self):
        return hash(self.constraint_id)

    @JOverride
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


class Joiners:
    #  Method parameter type variables
    A = TypeVar('A')
    B = TypeVar('B')
    C = TypeVar('C')
    D = TypeVar('D')
    E = TypeVar('E')

    #  Method return type variables
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    @staticmethod
    def _call_comparison_java_joiner(java_joiner, mapping_or_left_mapping, right_mapping):
        if mapping_or_left_mapping is None and right_mapping is None:
            raise ValueError
        elif mapping_or_left_mapping is not None and right_mapping is None:
            return java_joiner(_cast(mapping_or_left_mapping))
        elif mapping_or_left_mapping is not None and right_mapping is not None:
            return java_joiner(_cast(mapping_or_left_mapping), _cast(right_mapping))
        else:
            raise ValueError

    @overload  # noqa
    @staticmethod
    def equal() -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) -> 'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) -> 'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def equal(mapping_or_left_mapping=None, right_mapping=None):
        """Joins every A and B that share a property.

        :return:
        """
        if mapping_or_left_mapping is None and right_mapping is None:
            return JavaJoiners.equal()
        return Joiners._call_comparison_java_joiner(JavaJoiners.equal, mapping_or_left_mapping, right_mapping)

    @overload  # noqa
    @staticmethod
    def filtering(predicate: Callable[[A, B], bool]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def filtering(predicate: Callable[[A, B, C], bool]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def filtering(predicate: Callable[[A, B, C, D], bool]) -> 'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def filtering(predicate: Callable[[A, B, C, D, E], bool]) -> 'QuadJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def filtering(predicate):
        """Applies a filter to the joined tuple

        :param predicate: the filter to apply

        :return:
        """
        return JavaJoiners.filtering(_filtering_cast(predicate))

    @overload  # noqa
    @staticmethod
    def greater_than(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) -> 'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) ->\
            'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def greater_than(mapping_or_left_mapping, right_mapping=None):
        """Joins every A and B where a value of property on A is greater than the value of a property on B.

        :return:
        """
        return Joiners._call_comparison_java_joiner(JavaJoiners.greaterThan, mapping_or_left_mapping,
                                                    right_mapping)

    greaterThan = greater_than

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) ->\
            'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) ->\
            'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) ->\
            'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def greater_than_or_equal(mapping_or_left_mapping, right_mapping=None):
        """Joins every A and B where a value of property on A is greater than or equal to the value of a property on B.

        :return:
        """
        return Joiners._call_comparison_java_joiner(JavaJoiners.greaterThanOrEqual, mapping_or_left_mapping,
                                                    right_mapping)

    greaterThanOrEqual = greater_than_or_equal

    @overload  # noqa
    @staticmethod
    def less_than(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) -> 'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) ->\
            'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def less_than(mapping_or_left_mapping, right_mapping=None):
        """Joins every A and B where a value of property on A is less than the value of a property on B.

        :return:
        """
        return Joiners._call_comparison_java_joiner(JavaJoiners.lessThan, mapping_or_left_mapping, right_mapping)

    lessThan = less_than

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) ->\
            'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) ->\
            'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def less_than_or_equal(mapping_or_left_mapping, right_mapping=None):
        """Joins every A and B where a value of property on A is less than or equal to the value of a property on B.

        :return:
        """
        return Joiners._call_comparison_java_joiner(JavaJoiners.lessThanOrEqual, mapping_or_left_mapping,
                                                    right_mapping)

    lessThanOrEqual = less_than_or_equal

    @overload  # noqa
    @staticmethod
    def overlapping(start_mapping: Callable[[A], A_], end_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def overlapping(left_start_mapping: Callable[[A], A_], left_end_mapping: Callable[[A], A_],
                    right_start_mapping: Callable[[B], A_], right_end_mapping: Callable[[B], A_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def overlapping(left_start_mapping: Callable[[A, B], A_], left_end_mapping: Callable[[A, B], A_],
                    right_start_mapping: Callable[[C], A_], right_end_mapping: Callable[[C], A_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def overlapping(left_start_mapping: Callable[[A, B, C], A_], left_end_mapping: Callable[[A, B, C], A_],
                    right_start_mapping: Callable[[D], A_], right_end_mapping: Callable[[D], A_]) ->\
            'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def overlapping(left_start_mapping: Callable[[A, B, C, D], A_], left_end_mapping: Callable[[A, B, C, D], A_],
                    right_start_mapping: Callable[[E], A_], right_end_mapping: Callable[[E], A_]) ->\
            'PentaJoiner[A,B,C,D]':
        ...

    @staticmethod
    def overlapping(start_mapping_or_left_start_mapping, end_mapping_or_left_end_mapping,
                    right_start_mapping=None, right_end_mapping=None):
        """Joins every A and B that overlap for an interval which is specified by a start and end property on both A and
        B.

        :return:
        """
        if start_mapping_or_left_start_mapping is None or end_mapping_or_left_end_mapping is None:
            raise ValueError
        if right_start_mapping is None and right_end_mapping is None:
            return JavaJoiners.overlapping(_cast(start_mapping_or_left_start_mapping),
                                           _cast(end_mapping_or_left_end_mapping))
        elif right_start_mapping is not None and right_end_mapping is not None:
            return JavaJoiners.overlapping(_cast(start_mapping_or_left_start_mapping),
                                           _cast(end_mapping_or_left_end_mapping),
                                           _cast(right_start_mapping),
                                           _cast(right_end_mapping))
        else:
            raise ValueError


class ConstraintCollectors:
    #  Method parameter type variables
    A = TypeVar('A')
    B = TypeVar('B')
    C = TypeVar('C')
    D = TypeVar('D')
    E = TypeVar('E')

    #  Method return type variables
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    @overload  # noqa
    @staticmethod
    def average(group_value_mapping: Callable[[A], int]) -> 'UniConstraintCollector[A, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def average(group_value_mapping: Callable[[A, B], int]) -> 'BiConstraintCollector[A, B, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def average(group_value_mapping: Callable[[A, B, C], int]) -> 'TriConstraintCollector[A, B, C, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def average(group_value_mapping: Callable[[A, B, C, D], int]) -> 'QuadConstraintCollector[A, B, C, D, Any, int]':
        ...

    @staticmethod
    def average(group_value_mapping):
        """Returns a collector that calculates an average of an int property of the elements that are being grouped.

        :param group_value_mapping:

        :return:
        """
        return JavaConstraintCollectors.average(_int_function_cast(group_value_mapping))

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'UniConstraintCollector[A, Any, A_]',
                sub_collector_2: 'UniConstraintCollector[A, Any, B_]',
                compose_function: Callable[[A_, B_], C_]) -> 'UniConstraintCollector[A, Any, C_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'UniConstraintCollector[A, Any, A_]',
                sub_collector_2: 'UniConstraintCollector[A, Any, B_]',
                sub_collector_3: 'UniConstraintCollector[A, Any, C_]',
                compose_function: Callable[[A_, B_, C_], D_]) -> 'UniConstraintCollector[A, Any, D_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'UniConstraintCollector[A, Any, A_]',
                sub_collector_2: 'UniConstraintCollector[A, Any, B_]',
                sub_collector_3: 'UniConstraintCollector[A, Any, C_]',
                sub_collector_4: 'UniConstraintCollector[A, Any, D_]',
                compose_function: Callable[[A_, B_, C_, D_], E_]) -> 'UniConstraintCollector[A, Any, E_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'BiConstraintCollector[A, B, Any, A_]',
                sub_collector_2: 'BiConstraintCollector[A, B, Any, B_]',
                compose_function: Callable[[A_, B_], C_]) -> 'BiConstraintCollector[A, B, Any, C_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'BiConstraintCollector[A, B, Any, A_]',
                sub_collector_2: 'BiConstraintCollector[A, B, Any, B_]',
                sub_collector_3: 'BiConstraintCollector[A, B, Any, C_]',
                compose_function: Callable[[A_, B_, C_], D_]) -> 'BiConstraintCollector[A, B, Any, D_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'BiConstraintCollector[A, B, Any, A_]',
                sub_collector_2: 'BiConstraintCollector[A, B, Any, B_]',
                sub_collector_3: 'BiConstraintCollector[A, B, Any, C_]',
                sub_collector_4: 'BiConstraintCollector[A, B, Any, D_]',
                compose_function: Callable[[A_, B_, C_, D_], E_]) -> 'BiConstraintCollector[A, B, Any, E_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'TriConstraintCollector[A, B, C, Any, A_]',
                sub_collector_2: 'TriConstraintCollector[A, B, C, Any, B_]',
                compose_function: Callable[[A_, B_], C_]) -> 'TriConstraintCollector[A, B, C, Any, C_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'TriConstraintCollector[A, B, C, Any, A_]',
                sub_collector_2: 'TriConstraintCollector[A, B, C, Any, B_]',
                sub_collector_3: 'TriConstraintCollector[A, B, C, Any, C_]',
                compose_function: Callable[[A_, B_, C_], D_]) -> 'TriConstraintCollector[A, B, C, Any, D_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'TriConstraintCollector[A, B, C, Any, A_]',
                sub_collector_2: 'TriConstraintCollector[A, B, C, Any, B_]',
                sub_collector_3: 'TriConstraintCollector[A, B, C, Any, C_]',
                sub_collector_4: 'TriConstraintCollector[A, B, C, Any, D_]',
                compose_function: Callable[[A_, B_, C_, D_], E_]) -> 'TriConstraintCollector[A, B, C, Any, E_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                sub_collector_2: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                compose_function: Callable[[A_, B_], C_]) -> 'QuadConstraintCollector[A, B, C, D, Any, C_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                sub_collector_2: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                sub_collector_3: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                compose_function: Callable[[A_, B_, C_], D_]) -> 'QuadConstraintCollector[A, B, C, D, Any, D_]':
        ...

    @overload  # noqa
    @staticmethod
    def compose(sub_collector_1: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                sub_collector_2: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                sub_collector_3: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                sub_collector_4: 'QuadConstraintCollector[A, B, C, D, Any, D_]',
                compose_function: Callable[[A_, B_, C_, D_], E_]) -> 'QuadConstraintCollector[A, B, C, D, Any, E_]':
        ...

    @staticmethod
    def compose(*args):
        """Returns a constraint collector the result of which is a composition of other constraint collectors.

        :return:
        """
        if len(args) < 3:  # Need at least two collectors + 1 compose function
            raise ValueError
        collectors = args[:-1]
        compose_function = args[-1]
        compose_args = (*collectors, _cast(compose_function))
        return JavaConstraintCollectors.compose(*compose_args)

    @overload  # noqa
    @staticmethod
    def conditionally(predicate: Callable[[A], bool], delegate: 'UniConstraintCollector[A, Any, A_]') ->\
            'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def conditionally(predicate: Callable[[A, B], bool],
                      delegate: 'BiConstraintCollector[A, B, Any, A_]') -> 'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def conditionally(predicate: Callable[[A, B, C], bool],
                      delegate: 'TriConstraintCollector[A, B, C, Any, A_]') ->\
            'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def conditionally(predicate: Callable[[A, B, C, D], bool],
                      delegate: 'QuadConstraintCollector[A, B, C, D, Any, A_]') ->\
            'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @staticmethod
    def conditionally(predicate, delegate):
        """Returns a collector that delegates to the underlying collector if and only if the input tuple meets the given
        condition.

        :param predicate:

        :param delegate:

        :return:
        """
        return JavaConstraintCollectors.conditionally(_filtering_cast(predicate), delegate)

    @staticmethod
    def count() -> 'UniConstraintCollector[A, Any, int]':
        """Returns a collector that counts the number of elements that are being grouped.

        :return:
        """
        return JavaConstraintCollectors.count()

    @staticmethod
    def count_bi() -> 'BiConstraintCollector[A, B, Any, int]':
        """Returns a collector that counts the number of elements that are being grouped.

        :return:
        """
        return JavaConstraintCollectors.countBi()

    countBi = count_bi

    @staticmethod
    def count_tri() -> 'TriConstraintCollector[A, B, C, Any, int]':
        """Returns a collector that counts the number of elements that are being grouped.

        :return:
        """
        return JavaConstraintCollectors.countTri()

    countTri = count_tri

    @staticmethod
    def count_quad() -> 'QuadConstraintCollector[A, B, C, D, Any, int]':
        """Returns a collector that counts the number of elements that are being grouped.

        :return:
        """
        return JavaConstraintCollectors.countQuad()

    countQuad = count_quad

    @overload  # noqa
    @staticmethod
    def count_distinct() -> 'UniConstraintCollector[A, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def count_distinct(group_value_mapping: Callable[[A], int]) -> 'UniConstraintCollector[A, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def count_distinct(group_value_mapping: Callable[[A, B], int]) -> 'BiConstraintCollector[A, B, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def count_distinct(group_value_mapping: Callable[[A, B, C], int]) -> 'TriConstraintCollector[A, B, C, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def count_distinct(group_value_mapping: Callable[[A, B, C, D], int]) ->\
            'QuadConstraintCollector[A, B, C, D, Any, int]':
        ...

    @staticmethod
    def count_distinct(function=None):
        """Returns a collector that counts the number of unique elements that are being grouped.

        :return:
        """
        if function is None:
            return JavaConstraintCollectors.countDistinct()
        else:
            return JavaConstraintCollectors.countDistinct(_cast(function)) # noqa

    countDistinct = count_distinct

    @overload  # noqa
    @staticmethod
    def max() -> 'UniConstraintCollector[A, Any, A]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(comparator: Callable[[A, A], int]) -> 'UniConstraintCollector[A, Any, A]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A], A_], comparator: Callable[[A_, A_], int]) ->\
            'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B], A_], comparator: Callable[[A_, A_], int]) ->\
            'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B, C], A_], comparator: Callable[[A_, A_], int]) ->\
            'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B, C, D], A_]) -> 'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def max(group_value_mapping: Callable[[A, B, C, D], A_], comparator: Callable[[A_, A_], int]) ->\
            'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @staticmethod
    def max(function=None, comparator=None):
        """Returns a collector that finds a maximum value in a group of Comparable elements.

        :return:
        """
        if function is None and comparator is None:
            return JavaConstraintCollectors.max()
        elif function is not None and comparator is None:
            return JavaConstraintCollectors.max(_cast(function)) # noqa
        elif function is None and comparator is not None:
            return JavaConstraintCollectors.max(_PythonComparator(comparator)) # noqa
        else:
            return JavaConstraintCollectors.max(_cast(function), _PythonComparator(comparator)) # noqa

    @overload  # noqa
    @staticmethod
    def min() -> 'UniConstraintCollector[A, Any, A]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(comparator: Callable[[A, A], int]) -> 'UniConstraintCollector[A, Any, A]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A], A_], comparator: Callable[[A_, A_], int]) ->\
            'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B], A_], comparator: Callable[[A_, A_], int]) ->\
            'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B, C], A_], comparator: Callable[[A_, A_], int]) ->\
            'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B, C, D], A_]) -> 'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def min(group_value_mapping: Callable[[A, B, C, D], A_], comparator: Callable[[A_, A_], int]) ->\
            'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @staticmethod
    def min(function=None, comparator=None):
        """Returns a collector that finds a minimum value in a group of Comparable elements.

        :return:
        """
        if function is None and comparator is None:
            return JavaConstraintCollectors.min()
        elif function is not None and comparator is None:
            return JavaConstraintCollectors.min(_cast(function)) # noqa
        elif function is None and comparator is not None:
            return JavaConstraintCollectors.min(_PythonComparator(comparator)) # noqa
        else:
            return JavaConstraintCollectors.min(_cast(function), _PythonComparator(comparator)) # noqa

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A], int]) -> 'UniConstraintCollector[A, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B], int]) -> 'BiConstraintCollector[A, B, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B, C], int]) -> 'TriConstraintCollector[A, B, C, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B, C, D], int]) -> 'QuadConstraintCollector[A, B, C, D, Any, int]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A], A_], zero: A_, adder: Callable[[A_, A_], A_], subtractor: Callable[[A_, A_], A_]) \
            -> 'UniConstraintCollector[A, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B], A_], zero: A_, adder: Callable[[A_, A_], A_],
            subtractor: Callable[[A_, A_], A_]) \
            -> 'BiConstraintCollector[A, B, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B, C], A_], zero: A_, adder: Callable[[A_, A_], A_],
            subtractor: Callable[[A_, A_], A_]) \
            -> 'TriConstraintCollector[A, B, C, Any, A_]':
        ...

    @overload  # noqa
    @staticmethod
    def sum(function: Callable[[A, B, C, D], A_], zero: A_, adder: Callable[[A_, A_], A_],
            subtractor: Callable[[A_, A_], A_]) \
            -> 'QuadConstraintCollector[A, B, C, D, Any, A_]':
        ...

    @staticmethod
    def sum(function, zero=None, adder=None, subtractor=None):
        """Returns a collector that sums an int property of the elements that are being grouped.

        :param function:

        :return:
        """
        if zero is None and adder is None and subtractor is None:
            return JavaConstraintCollectors.sum(_int_function_cast(function))
        elif zero is not None and adder is not None and subtractor is not None:
            return JavaConstraintCollectors.sum(_cast(function), zero, # noqa
                                                _PythonBinaryOperator(adder), _PythonBinaryOperator(subtractor))
        else:
            raise ValueError

    @overload  # noqa
    @staticmethod
    def to_collection(collection_creator: Callable[[int], B_]) -> 'UniConstraintCollector[A, Any, B_]':
        ...

    @overload  # noqa
    @staticmethod
    def to_collection(group_value_mapping: Callable[[A], A_], collection_creator: Callable[[int], B_]) ->\
            'UniConstraintCollector[A, Any, B_]':
        ...

    @overload  # noqa
    @staticmethod
    def to_collection(group_value_mapping: Callable[[A, B], A_], collection_creator: Callable[[int], B_]) ->\
            'BiConstraintCollector[A, B, Any, B_]':
        ...

    @overload  # noqa
    @staticmethod
    def to_collection(group_value_mapping: Callable[[A, B, C], A_], collection_creator: Callable[[int], B_]) ->\
            'TriConstraintCollector[A, B, C, Any, B_]':
        ...

    @overload  # noqa
    @staticmethod
    def to_collection(group_value_mapping: Callable[[A, B, C, D], A_], collection_creator: Callable[[int], B_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, B_]':
        ...

    @staticmethod
    def to_collection(group_value_mapping_or_collection_creator, collection_creator=None):
        """Deprecated; use to_list, to_set or to_sorted_set instead

        :return:
        """
        if collection_creator is None:
            return JavaConstraintCollectors.toCollection(
                _PythonIntFunction(group_value_mapping_or_collection_creator))  # noqa
        else:
            return JavaConstraintCollectors.toCollection(_cast(group_value_mapping_or_collection_creator),  # noqa
                                                         _PythonIntFunction(collection_creator))

    toCollection = to_collection

    @overload  # noqa
    @staticmethod
    def to_list() -> 'UniConstraintCollector[A, Any, List[A]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_list(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, List[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_list(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, List[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_list(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, List[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_list(group_value_mapping: Callable[[A, B, C, D], A_]) ->\
            'QuadConstraintCollector[A, B, C, D, Any, List[A_]]':
        ...

    @staticmethod
    def to_list(group_value_mapping=None):
        """Creates constraint collector that returns List of the given element type.

        :return:
        """
        if group_value_mapping is None:
            return JavaConstraintCollectors.toList()
        else:
            return JavaConstraintCollectors.toList(_cast(group_value_mapping)) # noqa

    toList = to_list

    @overload  # noqa
    @staticmethod
    def to_set() -> 'UniConstraintCollector[A, Any, Set[A]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_set(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_set(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_set(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_set(group_value_mapping: Callable[[A, B, C, D], A_]) -> 'QuadConstraintCollector[A, B, C, D, Any, Set[A_]]':
        ...

    @staticmethod
    def to_set(group_value_mapping=None):
        """Creates constraint collector that returns Set of the same element type as the ConstraintStream.

        :return:
        """
        if group_value_mapping is None:
            return JavaConstraintCollectors.toSet()
        else:
            return JavaConstraintCollectors.toSet(_cast(group_value_mapping)) # noqa

    toSet = to_set

    @overload  # noqa
    @staticmethod
    def to_sorted_set() -> 'UniConstraintCollector[A, Any, Set[A]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A], A_]) -> 'UniConstraintCollector[A, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(comparator: Callable[[A, A], int]) -> 'UniConstraintCollector[A, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A], A_], comparator: Callable[[A_, A_], int]) ->\
            'UniConstraintCollector[A, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B], A_]) -> 'BiConstraintCollector[A, B, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B], A_], comparator: Callable[[A_, A_], int]) ->\
            'BiConstraintCollector[A, B, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B, C], A_]) -> 'TriConstraintCollector[A, B, C, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B, C], A_], comparator: Callable[[A_, A_], int]) ->\
            'TriConstraintCollector[A, B, C, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B, C, D], A_]) ->\
            'QuadConstraintCollector[A, B, C, D, Any, Set[A_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_set(group_value_mapping: Callable[[A, B, C, D], A_], comparator: Callable[[A_, A_], int]) ->\
            'QuadConstraintCollector[A, B, C, D, Any, Set[A_]]':
        ...

    @staticmethod
    def to_sorted_set(group_value_mapping=None, comparator=None):
        """Creates constraint collector that returns SortedSet of the same element type as the ConstraintStream.

        :return:
        """
        if group_value_mapping is None and comparator is None:
            return JavaConstraintCollectors.toSortedSet()
        elif group_value_mapping is not None and comparator is None:
            return JavaConstraintCollectors.toSortedSet(_cast(group_value_mapping))

    toSortedSet = to_sorted_set

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_]) ->\
            'UniConstraintCollector[A, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_],
               merge_function: Callable[[B_, B_], B_]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_],
               set_creator: Callable[[int], Set[B_]]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_]) ->\
            'BiConstraintCollector[A, B, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_],
               merge_function: Callable[[B_, B_], B_]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_],
               set_creator: Callable[[int], Set[B_]]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_]) ->\
            'TriConstraintCollector[A, B, C, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_],
               merge_function: Callable[[B_, B_], B_]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_],
               set_creator: Callable[[int], Set[B_]]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B, C, D], B_]) ->\
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B, C], B_],
               merge_function: Callable[[B_, B_], B_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B], B_],
               set_creator: Callable[[int], Set[B_]]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, Set[B_]]]':
        ...

    @staticmethod
    def to_map(key_mapper, value_mapper, merge_function_or_set_creator=None):
        """Creates a constraint collector that returns a Map with given keys and values consisting of a Set of mappings.

        :return:
        """
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

    toMap = to_map

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_]) ->\
            'UniConstraintCollector[A, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_],
                      merge_function: Callable[[B_, B_], B_]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A], A_], value_mapper: Callable[[A], B_],
                      set_creator: Callable[[int], Set[B_]]) -> \
            'UniConstraintCollector[A, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_]) ->\
            'BiConstraintCollector[A, B, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_],
                      merge_function: Callable[[B_, B_], B_]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B], A_], value_mapper: Callable[[A, B], B_],
                      set_creator: Callable[[int], Set[B_]]) -> \
            'BiConstraintCollector[A, B, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_]) ->\
            'TriConstraintCollector[A, B, C, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_],
                      merge_function: Callable[[B_, B_], B_]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C], A_], value_mapper: Callable[[A, B, C], B_],
                      set_creator: Callable[[int], Set[B_]]) -> \
            'TriConstraintCollector[A, B, C, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B, C, D], B_]) ->\
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, Set[B_]]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B, C], B_],
                      merge_function: Callable[[B_, B_], B_]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, B_]]':
        ...

    @overload  # noqa
    @staticmethod
    def to_sorted_map(key_mapper: Callable[[A, B, C, D], A_], value_mapper: Callable[[A, B], B_],
                      set_creator: Callable[[int], Set[B_]]) -> \
            'QuadConstraintCollector[A, B, C, D, Any, Dict[A_, Set[B_]]]':
        ...

    @staticmethod
    def to_sorted_map(key_mapper, value_mapper, merge_function_or_set_creator=None):
        """Creates a constraint collector that returns a SortedMap with given keys and values consisting of a Set of
        mappings.

        :return:
        """
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

    toSortedMap = to_sorted_map
