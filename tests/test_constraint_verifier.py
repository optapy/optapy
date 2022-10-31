import pytest

import optapy
import optapy.types
import optapy.score
import optapy.config
import optapy.constraint
import optapy.test


def verifier_suite(verifier: optapy.test.PythonConstraintVerifier, same_value, is_value_one,
                   solution, e1, e2, e3, v1, v2, v3):
    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .penalizes(0)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .rewards()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .penalizes(1)

    e1.value = v1
    e2.value = v1
    e3.value = v1

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .penalizes(1)

    verifier.verify_that(same_value) \
        .given(e1, e2) \
        .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .rewards(1)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .penalizes(0)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2) \
            .penalizes(2)

    verifier.verify_that(same_value) \
        .given(e1, e2, e3) \
        .penalizes(3)

    verifier.verify_that(same_value) \
        .given(e1, e2, e3) \
        .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .rewards(3)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .penalizes(2)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given(e1, e2, e3) \
            .penalizes(4)

    verifier.verify_that(same_value) \
        .given_solution(solution) \
        .penalizes(3)

    verifier.verify_that(same_value) \
        .given_solution(solution) \
        .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given_solution(solution) \
            .rewards(3)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given_solution(solution) \
            .penalizes(2)

    with pytest.raises(AssertionError):
        verifier.verify_that(same_value) \
            .given_solution(solution) \
            .penalizes(4)

    verifier.verify_that(is_value_one) \
        .given(e1, e2, e3) \
        .rewards(3)

    verifier.verify_that(is_value_one) \
        .given(e1, e2, e3) \
        .rewards()

    with pytest.raises(AssertionError):
        verifier.verify_that(is_value_one) \
            .given(e1, e2, e3) \
            .penalizes()

    with pytest.raises(AssertionError):
        verifier.verify_that(is_value_one) \
            .given(e1, e2, e3) \
            .penalizes(3)

    with pytest.raises(AssertionError):
        verifier.verify_that(is_value_one) \
            .given(e1, e2, e3) \
            .rewards(2)

    with pytest.raises(AssertionError):
        verifier.verify_that(is_value_one) \
            .given(e1, e2, e3) \
            .rewards(4)


def test_constraint_verifier_create():
    @optapy.problem_fact
    class Value:
        def __init__(self, code):
            self.code = code

        def __str__(self):
            return f'Value(code={self.code})'


    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            self.value = value

        @optapy.planning_variable(Value, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value


    def same_value(constraint_factory: optapy.constraint.ConstraintFactory):
        return (constraint_factory.for_each(Entity)
                    .join(Entity, optapy.constraint.Joiners.less_than(lambda e: e.code),
                                  optapy.constraint.Joiners.equal(lambda e: e.value))
                    .penalize('Same value', optapy.score.SimpleScore.ONE)
                )

    def is_value_one(constraint_factory: optapy.constraint.ConstraintFactory):
        return (constraint_factory.for_each(Entity)
                    .filter(lambda e: e.value.code == 'v1')
                    .reward('Value 1', optapy.score.SimpleScore.ONE)
                )

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            same_value(constraint_factory),
            is_value_one(constraint_factory)
        ]

    @optapy.planning_solution
    class Solution:
        def __init__(self, entities, values, score=None):
            self.entities = entities
            self.values = values
            self.score = score

        @optapy.planning_entity_collection_property(Entity)
        def get_entities(self):
            return self.entities

        @optapy.problem_fact_collection_property(Value)
        @optapy.value_range_provider(range_id='value_range')
        def get_values(self):
            return self.values

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score

    solver_config = optapy.config.solver.SolverConfig()
    solver_config.withSolutionClass(Solution) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints)

    verifier = optapy.test.constraint_verifier_create(solver_config)

    e1 = Entity('e1')
    e2 = Entity('e2')
    e3 = Entity('e3')

    v1 = Value('v1')
    v2 = Value('v2')
    v3 = Value('v3')

    solution = Solution([e1, e2, e3], [v1, v2, v3])

    verifier_suite(verifier, same_value, is_value_one,
                   solution, e1, e2, e3, v1, v2, v3)

    verifier = optapy.test.constraint_verifier_build(my_constraints, Solution, Entity)

    e1 = Entity('e1')
    e2 = Entity('e2')
    e3 = Entity('e3')

    v1 = Value('v1')
    v2 = Value('v2')
    v3 = Value('v3')

    solution = Solution([e1, e2, e3], [v1, v2, v3])

    verifier_suite(verifier, same_value, is_value_one,
                   solution, e1, e2, e3, v1, v2, v3)
