import optapy
import optapy.score
import optapy.config
import optapy.constraint


def test_easy_score_calculator():
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

    @optapy.planning_solution
    class Solution:
        def __init__(self, entity_list, value_range, score=None):
            self.entity_list = entity_list
            self.value_range = value_range
            self.score = score

        @optapy.planning_entity_collection_property(Entity)
        def get_entity_list(self):
            return self.entity_list

        @optapy.problem_fact_collection_property(int)
        @optapy.value_range_provider(range_id='value_range')
        def get_value_range(self):
            return self.value_range

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score

    @optapy.easy_score_calculator
    def my_score_calculator(solution: Solution):
        total_score = 0
        for entity in solution.entity_list:
            total_score += 0 if entity.value is None else entity.value
        return optapy.score.SimpleScore.of(total_score)

    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('9')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Entity)) \
        .withEasyScoreCalculatorClass(optapy.get_class(my_score_calculator)) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution([Entity('A'), Entity('B'), Entity('C')], [1, 2, 3])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 9
    assert solution.entity_list[0].value == 3
    assert solution.entity_list[1].value == 3
    assert solution.entity_list[2].value == 3
