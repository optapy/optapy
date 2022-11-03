from typing import Callable, Generic, List, Type, TypeVar, TYPE_CHECKING, overload, Union
from jpype import JProxy

from ..jpype_type_conversions import PythonBiFunction
from ..optaplanner_java_interop import get_class
from ..constraint_stream import PythonConstraintFactory, BytecodeTranslation

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from org.optaplanner.core.api.score.stream import Constraint, ConstraintFactory
    from org.optaplanner.core.config.solver import SolverConfig
    from org.optaplanner.core.api.score import Score


Solution_ = TypeVar('Solution_')


class ConstraintVerifier(Generic[Solution_]):
    """
    Entry point for the ConstraintVerifier API, which is used to test constraints defined by
    a @constraint_provider function.
    """
    def __init__(self, delegate):
        self.delegate = delegate
        self.bytecode_translation = BytecodeTranslation.IF_POSSIBLE

    def with_bytecode_translation(self, bytecode_translation: BytecodeTranslation) ->\
            'ConstraintVerifier[Solution_]':
        """
        All subsequent calls to verify_that(constraint_function) will translate bytecode according to the rules
        of the given BytecodeTranslation

        :param bytecode_translation: A BytecodeTranslation member.
        :return: self, for chaining
        """
        self.bytecode_translation = bytecode_translation
        return self

    @overload
    def verify_that(self) -> 'MultiConstraintVerification[Solution_]':
        """
        Creates a constraint verifier for all constraints of the ConstraintProvider.
        """
        ...

    @overload
    def verify_that(self, constraint_function: Callable[['ConstraintFactory'], 'Constraint']) -> \
            'SingleConstraintVerification[Solution_]':
        """
        Creates a constraint verifier for a given Constraint of the ConstraintProvider.
        :param constraint_function: The constraint to verify
        """
        ...

    def verify_that(self, constraint_function: Callable[['ConstraintFactory'], 'Constraint'] = None):
        """
        Creates a constraint verifier for a given Constraint of the ConstraintProvider.
        :param constraint_function: Sometimes None, the constraint to verify. If not provided, all
                                    constraints will be tested
        """
        if constraint_function is None:
            return MultiConstraintVerification(self.delegate.verifyThat())
        else:
            return SingleConstraintVerification(self.delegate.verifyThat(
                PythonBiFunction(lambda _, constraint_factory:
                                 constraint_function(PythonConstraintFactory(constraint_factory,
                                                                             self.bytecode_translation)))))


class SingleConstraintVerification(Generic[Solution_]):
    def __init__(self, delegate):
        self.delegate = delegate

    def given(self, *facts) -> 'SingleConstraintAssertion':
        """
        Set the facts for this assertion
        :param facts: Never None, at least one
        """
        from org.optaplanner.optapy import PythonSolver  # noqa
        from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter  # noqa
        from org.optaplanner.jpyinterpreter.types import CPythonBackedPythonLikeObject  # noqa
        from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference  # noqa
        reference_map = PythonSolver.getNewReferenceMap()
        wrapped_facts = []

        for fact in facts:
            fact_class = get_class(type(fact))
            wrapped_fact = PythonSolver.wrapFact(fact_class, fact, reference_map)
            wrapped_facts.append(wrapped_fact)

        referenced_facts = list(reference_map.values())
        for fact in referenced_facts:
            if isinstance(fact, CPythonBackedPythonLikeObject):
                CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(fact, JProxy(OpaquePythonReference,
                                                                                inst=getattr(fact, '$cpythonReference'),
                                                                                convert=True), reference_map)

        return SingleConstraintAssertion(self.delegate.given(wrapped_facts))


    def given_solution(self, solution: 'Solution_') -> 'SingleConstraintAssertion':
        """
        Set the solution to be used for this assertion
        :param solution: Never None
        """
        from org.optaplanner.optapy import PythonSolver  # noqa
        solution_class = get_class(type(solution))
        wrapped_solution = PythonSolver.wrapProblem(solution_class, solution)
        return SingleConstraintAssertion(self.delegate.givenSolution(wrapped_solution))


class MultiConstraintVerification(Generic[Solution_]):
    def __init__(self, delegate):
        self.delegate = delegate

    def given(self, *facts) -> 'MultiConstraintAssertion':
        """
        Set the facts for this assertion
        :param facts: Never None, at least one
        """
        from org.optaplanner.optapy import PythonSolver  # noqa
        from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter  # noqa
        from org.optaplanner.jpyinterpreter.types import CPythonBackedPythonLikeObject  # noqa
        from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference  # noqa
        reference_map = PythonSolver.getNewReferenceMap()
        wrapped_facts = []

        for fact in facts:
            fact_class = get_class(type(fact))
            wrapped_fact = PythonSolver.wrapFact(fact_class, fact, reference_map)
            wrapped_facts.append(wrapped_fact)

        referenced_facts = list(reference_map.values())
        for fact in referenced_facts:
            if isinstance(fact, CPythonBackedPythonLikeObject):
                CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(fact, JProxy(OpaquePythonReference,
                                                                                inst=getattr(fact, '$cpythonReference'),
                                                                                convert=True), reference_map)

        return MultiConstraintAssertion(self.delegate.given(wrapped_facts))

    def given_solution(self, solution: 'Solution_') -> 'MultiConstraintAssertion':
        """
        Set the solution to be used for this assertion
        :param solution: Never None
        """
        from org.optaplanner.optapy import PythonSolver  # noqa
        solution_class = get_class(type(solution))
        wrapped_solution = PythonSolver.wrapProblem(solution_class, solution)
        return MultiConstraintAssertion(self.delegate.givenSolution(wrapped_solution))


class SingleConstraintAssertion:
    def __init__(self, delegate):
        self.delegate = delegate

    @overload
    def penalizes(self) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in any number of penalties.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will succeed.
        If there are no matches, it will fail.

        :raises AssertionError: when there are no penalties
        """
        ...

    @overload
    def penalizes(self, times: int) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a given number of penalties.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        :param times: the expected number of penalties
        :raises AssertionError: when the expected penalty is not observed
        """
        ...

    @overload
    def penalizes(self, message: str) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in any number of penalties.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will succeed.
        If there are no matches, it will fail.

        :param message: sometimes None, description of the scenario being asserted

        :raises AssertionError: when there are no penalties
        """
        ...

    @overload
    def penalizes(self, times: int, message: str) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a given number of penalties.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        :param times: the expected number of penalties
        :param message: sometimes None, description of the scenario being asserted

        :raises AssertionError: when the expected penalty is not observed
        """
        ...

    def penalizes(self, times: Union[int, str] = None, message: str = None) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a given number of penalties.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        :param times: sometimes None, the expected number of penalties. If not provided, it raises an AssertionError
                      when there are no penalties
        :param message: sometimes None, description of the scenario being asserted

        :raises AssertionError: when the expected penalty is not observed if times is provided, or
                                when there are no penalties if times is not provided

        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if times is None and message is None:
                self.delegate.penalizes()
            elif times is not None and message is None:
                self.delegate.penalizes(times)
            elif times is None and message is not None:
                self.delegate.penalizes(message)
            else:
                self.delegate.penalizes(times, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    @overload
    def penalizes_by(self, match_weight_total: int) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a specific penalty.

        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of -2hard reduces the
        score by -20hard. In that case, this assertion checks for 10.

        :param match_weight_total: the expected penalty
        :raises AssertionError: when the expected penalty is not observed
        """
        ...

    @overload
    def penalizes_by(self, match_weight_total: int, message: str) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a specific penalty.

        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of -2hard reduces the
        score by -20hard. In that case, this assertion checks for 10.

        :param match_weight_total: the expected penalty
        :param message: sometimes None, description of the scenario being asserted
        :raises AssertionError: when the expected penalty is not observed
        """
        ...

    def penalizes_by(self, match_weight_total: int, message: str = None):
        """
        Asserts that the Constraint being tested, given a set of facts, results in a specific penalty.

        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of -2hard reduces the
        score by -20hard. In that case, this assertion checks for 10.

        :param match_weight_total: the expected penalty
        :param message: sometimes None, description of the scenario being asserted
        :raises AssertionError: when the expected penalty is not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.penalizesBy(match_weight_total)
            else:
                self.delegate.penalizesBy(match_weight_total, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    @overload
    def rewards(self) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in any number of rewards.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will succeed.
        If there are no matches, it will fail.

        :raises AssertionError: when there are no rewards
        """
        ...

    @overload
    def rewards(self, times: int) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a given number of rewards.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        :param times: the expected number of rewards
        :raises AssertionError: when the expected reward is not observed
        """
        ...

    @overload
    def rewards(self, message: str) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in any number of rewards.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will succeed.
        If there are no matches, it will fail.

        :param message: sometimes None, description of the scenario being asserted
        :raises AssertionError: when there are no rewards
        """
        ...

    @overload
    def rewards(self, times: int, message: str) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a given number of rewards.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        :param times: the expected number of rewards
        :param message: sometimes None, description of the scenario being asserted
        :raises AssertionError: when the expected reward is not observed
        """
        ...

    def rewards(self, times: int = None, message: str = None):
        """
        Asserts that the Constraint being tested, given a set of facts, results in a given number of rewards.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        :param times: sometimes None, the expected number of rewards. If not provided, it raises an AssertionError
                      when there are no rewards
        :param message: sometimes None, description of the scenario being asserted

        :raises AssertionError: when the expected reward is not observed if times is provided, or
                                when there are no rewards if times is not provided

        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if times is None and message is None:
                self.delegate.rewards()
            elif times is not None and message is None:
                self.delegate.rewards(times)
            elif times is None and message is not None:
                self.delegate.rewards(message)
            else:
                self.delegate.rewards(times, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    @overload
    def rewards_with(self, match_weight_total: int) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a specific reward.

        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of -2hard reduces the
        score by -20hard. In that case, this assertion checks for 10.

        :param match_weight_total: the expected reward
        :raises AssertionError: when the expected reward is not observed
        """
        ...

    @overload
    def rewards_with(self, match_weight_total: int, message: str) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a specific reward.

        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of -2hard reduces the
        score by -20hard. In that case, this assertion checks for 10.

        :param match_weight_total: the expected reward
        :param message: sometimes None, description of the scenario being asserted
        :raises AssertionError: when the expected reward is not observed
        """
        ...

    def rewards_with(self, match_weight_total: int, message: str = None):
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.rewardsWith(match_weight_total)
            else:
                self.delegate.rewardsWith(match_weight_total, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())


class MultiConstraintAssertion:
    def __init__(self, delegate):
        self.delegate = delegate

    @overload
    def scores(self, score: 'Score') -> None:
        """
        Asserts that the ConstraintProvider under test, given a set of facts, results in a specific Score.
        :param score: total score calculated for the given set of facts
        :raises AssertionError: when the expected score does not match the calculated score
        """
        ...

    @overload
    def scores(self, score: 'Score', message: str) -> None:
        """
        Asserts that the ConstraintProvider under test, given a set of facts, results in a specific Score.
        :param score: total score calculated for the given set of facts
        :param message: sometimes None, description of the scenario being asserted
        :raises AssertionError: when the expected score does not match the calculated score
        """
        ...

    def scores(self, score: 'Score', message: str = None):
        """
        Asserts that the ConstraintProvider under test, given a set of facts, results in a specific Score.
        :param score: total score calculated for the given set of facts
        :param message: sometimes None, description of the scenario being asserted
        :raises AssertionError: when the expected score does not match the calculated score
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.scores(score)
            else:
                self.delegate.scores(score, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())


def constraint_verifier_build(constraint_provider: Callable[['ConstraintFactory'], List['Constraint']],
                              planning_solution_class: Type[Solution_], *entity_classes: Type) -> \
        ConstraintVerifier[Solution_]:
    """
    Build a constraint verifier for the given @constraint_provider function.

    :param constraint_provider: The constraint provider function (decorated with @constraint_provider) to
                                create the ConstraintVerifier for
    :param planning_solution_class: The type of the planning solution (decorated with @planning_solution)
    :param entity_classes: The types of the entity classes (each decorated with @planning_entity)

    :return: A ConstraintVerifier that can be used to test constraints
    """
    from org.optaplanner.test.api.score.stream import ConstraintVerifier as JavaConstraintVerifier  # noqa
    from org.optaplanner.optapy import PythonSolver  # noqa
    constraint_provider_instance = PythonSolver.createConstraintProvider(get_class(constraint_provider))
    planning_solution_java_class = get_class(planning_solution_class)
    entity_java_classes = list(map(get_class, entity_classes))
    return ConstraintVerifier(JavaConstraintVerifier.build(constraint_provider_instance,
                                                           planning_solution_java_class,
                                                           entity_java_classes))


def constraint_verifier_create(solver_config: 'SolverConfig') -> ConstraintVerifier:
    """
    Uses a SolverConfig to build a ConstraintVerifier. Alternative to build(ConstraintProvider, Type, Type).

    :param solver_config: never None, must have a PlanningSolution class, PlanningEntity classes and a
                          ConstraintProvider configured.

    :return: A ConstraintVerifier that can be used to test constraints configured using the solver_config's
             planning solution class, planning entity classes and constraint provider function.
    """
    from org.optaplanner.test.api.score.stream import ConstraintVerifier as JavaConstraintVerifier  # noqa
    return ConstraintVerifier(JavaConstraintVerifier.create(solver_config))
