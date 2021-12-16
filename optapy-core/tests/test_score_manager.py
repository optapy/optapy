import optapy
import optapy.score
import optapy.config
import optapy.constraint


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

    @optapy.planning_id
    def get_id(self):
        return self.code


@optapy.constraint_provider
def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
    return [
        constraint_factory.forEach(optapy.get_class(Entity))
            .reward('Maximize Value', optapy.score.SimpleScore.ONE, lambda entity: entity.value),
    ]


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


solver_config = optapy.config.solver.SolverConfig()
solver_config.withSolutionClass(optapy.get_class(Solution)) \
    .withEntityClasses(optapy.get_class(Entity)) \
    .withConstraintProviderClass(optapy.get_class(my_constraints))


def assert_score_manager(score_manager):
    problem: Solution = Solution([Entity('A', 1), Entity('B', 1), Entity('C', 1)], [1, 2, 3], None)
    assert problem.score is None
    score = score_manager.updateScore(problem)
    assert score.getScore() == 3
    assert problem.score.getScore() == 3

    score_explanation = score_manager.explainScore(problem)
    assert score_explanation.getSolution() is problem
    assert score_explanation.getScore().getScore() == 3
    assert score_explanation.getConstraintMatchTotalMap() \
                            .get(optapy.compose_constraint_id(Solution, 'Maximize Value')) \
                            .getConstraintMatchCount() == 3


def test_solver_manager_score_manager():
    with optapy.solver_manager_create(solver_config) as solver_manager:
        assert_score_manager(optapy.score_manager_create(solver_manager))


def test_solver_factory_score_manager():
    assert_score_manager(optapy.score_manager_create(optapy.solver_factory_create(solver_config)))
