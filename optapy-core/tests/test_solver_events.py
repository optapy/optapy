import optapy
import optapy.score
import optapy.config
import optapy.constraint


def test_solver_events():
    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            self.value = value

        @optapy.planning_variable(int, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(optapy.get_class(Entity))
                .reward('Maximize value', optapy.score.SimpleScore.ONE, lambda entity: entity.value),
        ]

    @optapy.planning_solution
    class Solution:
        def __init__(self, entity, value_range, score=None):
            self.entity = entity
            self.value_range = value_range
            self.score = score

        @optapy.planning_entity_collection_property(Entity)
        def get_entity(self):
            return self.entity

        @optapy.problem_fact_collection_property(int)
        @optapy.value_range_provider(range_id='value_range')
        def get_value_range(self):
            return self.value_range

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score


    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('6')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints)) \
        .withTerminationConfig(termination_config)

    problem: Solution = Solution([Entity('A'), Entity('B')], [1, 2, 3])
    score_list = []
    solution_list = []

    def on_best_solution_changed(event):
        solution_list.append(event.getNewBestSolution())
        score_list.append(event.getNewBestScore())

    solution = optapy.solve(solver_config, problem, on_best_solution_changed)


    assert solution.get_score().getScore() == 6
    assert solution.entity[0].value == 3
    assert solution.entity[1].value == 3
    assert len(score_list) == len(solution_list)
    assert len(solution_list) == 1
    assert score_list[0].getScore() == 6
    assert solution_list[0].get_score().getScore() == 6
    assert solution_list[0].entity[0].value == 3
    assert solution_list[0].entity[1].value == 3

