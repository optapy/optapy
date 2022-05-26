import optapy
import optapy.constraint
import optapy.score
import optapy.config
from optapy.types import ScoreDirector


def test_custom_shadow_variable():
    @optapy.variable_listener
    class MyVariableListener:
        def afterVariableChanged(self, score_director: ScoreDirector, entity: 'MyPlanningEntity'):
            score_director.beforeVariableChanged(entity, 'value_squared')
            if entity.value is None:
                entity.value_squared = None
            else:
                entity.value_squared = entity.value ** 2
            score_director.afterVariableChanged(entity, 'value_squared')

        def beforeVariableChanged(self, score_director: ScoreDirector, entity: 'MyPlanningEntity'):
            pass

        def beforeEntityAdded(self, score_director: ScoreDirector, entity: 'MyPlanningEntity'):
            pass

        def afterEntityAdded(self, score_director: ScoreDirector, entity: 'MyPlanningEntity'):
            pass

        def beforeEntityRemoved(self, score_director: ScoreDirector, entity: 'MyPlanningEntity'):
            pass

        def afterEntityRemoved(self, score_director: ScoreDirector, entity: 'MyPlanningEntity'):
            pass

    @optapy.planning_entity
    class MyPlanningEntity:
        value: int
        value_squared: int

        def __init__(self):
            self.value = None
            self.value_squared = None

        @optapy.planning_variable(int, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, new_value):
            self.value = new_value

        @optapy.custom_shadow_variable(int, variable_listener_class=MyVariableListener,
                                       sources=[optapy.planning_variable_reference('value')])
        def get_value_squared(self):
            return self.value_squared

        def set_value_squared(self, new_value_squared):
            self.value_squared = new_value_squared

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(MyPlanningEntity)
                .filter(lambda entity: entity.value * 2 == entity.value_squared)
                .reward('Double value is value squared', optapy.score.SimpleScore.ONE)
        ]

    @optapy.planning_solution
    class MySolution:
        entity_list: list[MyPlanningEntity]
        value_list: list[int]
        score: optapy.score.SimpleScore

        def __init__(self, entity_list, value_list, score=None):
            self.entity_list = entity_list
            self.value_list = value_list
            self.score = score

        @optapy.planning_entity_collection_property(MyPlanningEntity)
        def get_entity_list(self):
            return self.entity_list

        def set_entity_list(self, entity_list):
            self.entity_list = entity_list

        @optapy.problem_fact_collection_property(int)
        @optapy.value_range_provider('value_range')
        def get_value_list(self):
            return self.value_list

        def set_value_list(self, value_list):
            self.value_list = value_list

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self):
            return self.score

        def set_score(self, score):
            self.score = score

    solver_config = optapy.config.solver.SolverConfig() \
        .withSolutionClass(MySolution) \
        .withEntityClasses(MyPlanningEntity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(optapy.config.solver.termination.TerminationConfig()
                               .withBestScoreLimit('1'))

    solver_factory = optapy.solver_factory_create(solver_config)
    solver = solver_factory.buildSolver()
    problem = MySolution([MyPlanningEntity()], [1, 2, 3])
    solution: MySolution = solver.solve(problem)
    assert solution.score.getScore() == 1
    assert solution.entity_list[0].value == 2
    assert solution.entity_list[0].value_squared == 4
