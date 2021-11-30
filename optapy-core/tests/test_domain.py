import optapy
import optapy.score
import optapy.config
import optapy.constraint

def test_single_property():
    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            self.value = value

        @optapy.planning_variable(str, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value

    @optapy.problem_fact
    class Value:
        def __init__(self, code):
            self.code = code

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(optapy.get_class(Entity))
                              .join(optapy.get_class(Value),
                                    [optapy.constraint.Joiners.equal(lambda entity: entity.value,
                                                                     lambda value: value.code)])
                              .reward('Same as value', optapy.score.SimpleScore.ONE),
        ]

    @optapy.planning_solution
    class Solution:
        def __init__(self, entity, value, value_range, score=None):
            self.entity = entity
            self.value = value
            self.value_range = value_range
            self.score = score

        @optapy.planning_entity_property(Entity)
        def get_entity(self):
            return self.entity

        @optapy.problem_fact_property(Value)
        def get_value(self):
            return self.value

        @optapy.problem_fact_collection_property(str)
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
    termination_config.setBestScoreLimit('1')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints)) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution(Entity('A'), Value('1'), ['1', '2', '3'])
    solution = optapy.solve(solver_config, problem)
    assert solution.get_score().getScore() == 1
    assert solution.entity.value == '1'
