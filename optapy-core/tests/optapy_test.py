from unittest import TestCase

import optapy
import optapy.score
import optapy.config


def is_entity_pinned(solution, entity):
    return entity.is_pinned()


@optapy.planning_entity(pinning_filter=is_entity_pinned)
class Point:
    def __init__(self, value, is_pinned=False):
        self.value = value
        self.pinned = is_pinned

    def is_pinned(self):
        return self.pinned

    @optapy.planning_variable(int, value_range_provider_refs=['value_range'])
    def get_value(self):
        return self.value

    def set_value(self, value):
        self.value = value


@optapy.planning_solution
class Solution:
    def __init__(self, values, points):
        self.values = values
        self.points = points
        self.score = None

    @optapy.problem_fact_collection_property(int)
    @optapy.value_range_provider('value_range')
    def get_value_range(self):
        return self.values

    @optapy.planning_entity_collection_property(Point)
    def get_points(self):
        return self.points

    @optapy.planning_score(optapy.score.HardSoftScore)
    def get_score(self) -> optapy.score.HardSoftScore:
        return self.score

    def set_score(self, score):
        self.score = score


@optapy.constraint_provider
def my_constraints(constraint_factory):
    return [
        constraint_factory.from_(optapy.get_class(Point))
            .penalize("Minimize Value", optapy.score.HardSoftScore.ONE_SOFT, lambda point: point.value)
    ]


class TestOptaPy(TestCase):
    def test_pinning_filter(self):
        termination_config = optapy.config.solver.termination.TerminationConfig()
        termination_config.setUnimprovedSecondsSpentLimit(1)
        solver_config = optapy.config.solver.SolverConfig() \
            .withSolutionClass(optapy.get_class(Solution)) \
            .withEntityClasses(optapy.get_class(Point)) \
            .withConstraintProviderClass(optapy.get_class(my_constraints)) \
            .withTerminationConfig(termination_config)
        problem: Solution = Solution([0, 1, 2],
                                     [Point(0),
                                      Point(1),
                                      Point(2, is_pinned=True)])
        solution = optapy.solve(solver_config, problem)
        self.assertEqual(solution.get_score().getSoftScore(), -2)
