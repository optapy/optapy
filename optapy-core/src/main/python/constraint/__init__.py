from ..optaplanner_java_interop import ensure_init
from ..constraint_translator import function_cast, predicate_cast
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

_convert_joiners_to_java = False

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


class _JoinersWrapper:
    def __getattr__(self, item):
        from ..constraint_translator import PropertyJoiner, SamePropertyUniJoiner,\
            OverlappingPropertyJoiner, SameOverlappingPropertyUniJoiner, FilteringJoiner
        is_filtering = item == 'filtering'
        is_overlapping = item == 'overlapping'
        java_method = getattr(JavaJoiners, item)

        def function_wrapper(*args):
            if is_filtering:
                return FilteringJoiner(java_method, args[0])
            elif is_overlapping:
                if len(args) == 2:
                    return SameOverlappingPropertyUniJoiner(java_method, args[0], args[1])
                else:
                    return OverlappingPropertyJoiner(java_method, args[0], args[1], args[2], args[3])
            else:
                if len(args) == 0:
                    return SamePropertyUniJoiner(java_method, lambda a: a)
                if len(args) == 1:
                    return SamePropertyUniJoiner(java_method, args[0])
                else:
                    return PropertyJoiner(java_method, args[0], args[1])

        return function_wrapper


if not TYPE_CHECKING:
    Joiners = _JoinersWrapper()
