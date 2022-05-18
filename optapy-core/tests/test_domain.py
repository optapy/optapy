import optapy
import optapy.score
import optapy.config
import optapy.constraint
import dataclasses


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
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 1
    assert solution.entity.value == '1'


def test_tuple_group_by_key():
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
    @dataclasses.dataclass(eq=False)
    class Value:
        code: str

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(optapy.get_class(Entity))
                .join(optapy.get_class(Value),
                      [optapy.constraint.Joiners.equal(lambda entity: entity.value,
                                                       lambda value: value.code)])
                .groupBy(lambda entity, value: (0, value), optapy.constraint.ConstraintCollectors.countBi())
                .reward('Same as value', optapy.score.SimpleScore.ONE, lambda _, count: count),
        ]

    @optapy.planning_solution
    class Solution:
        def __init__(self, entity_list, value_list, value_range, score=None):
            self.entity_list = entity_list
            self.value_list = value_list
            self.value_range = value_range
            self.score = score

        @optapy.planning_entity_collection_property(Entity)
        def get_entity_list(self):
            return self.entity_list

        @optapy.problem_fact_collection_property(Value)
        def get_value_list(self):
            return self.value_list

        @optapy.problem_fact_collection_property(str)
        @optapy.value_range_provider(range_id='value_range')
        def get_value_range(self):
            return self.value_range

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score

    entity_list = [Entity('A0'), Entity('B0'), Entity('C0'),
                   Entity('A1'), Entity('B1'), Entity('C1'),
                   Entity('A2'), Entity('B2'), Entity('C2'),
                   Entity('A3'), Entity('B3'), Entity('C3'),
                   Entity('A4'), Entity('B4'), Entity('C4'),
                   Entity('A5'), Entity('B5'), Entity('C5'),
                   Entity('A6'), Entity('B6'), Entity('C6'),
                   Entity('A7'), Entity('B7'), Entity('C7'),
                   Entity('A8'), Entity('B8'), Entity('C8'),
                   Entity('A9'), Entity('B9'), Entity('C9')]

    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit(str(len(entity_list)))
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints)) \
        .withTerminationConfig(termination_config)

    problem: Solution = Solution(entity_list,
                                 [Value('1')],
                                 ['1', '2', '3'])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == len(entity_list)
    for entity in solution.entity_list:
        assert entity.value == '1'


def test_python_object():
    import datetime
    date1 = datetime.date(2021, 12, 2)
    date2 = datetime.date(2021, 12, 3)
    date3 = datetime.date(2021, 12, 4)

    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            self.value = value

        @optapy.planning_variable(datetime.date, value_range_provider_refs=['value_range'])
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
                      [optapy.constraint.Joiners.lessThanOrEqual(lambda entity: entity.value,
                                                                 lambda value: value.code)])
                .reward('Same as value', optapy.score.SimpleScore.ONE),
            constraint_factory.forEach(optapy.get_class(Entity))
                .groupBy(lambda entity: entity.value, optapy.constraint.ConstraintCollectors.count())
                .reward('Entity have same value', optapy.score.SimpleScore.ONE, lambda value, count: count * count),
            constraint_factory.forEach(optapy.get_class(Entity))
                .groupBy(lambda entity: (entity.code, entity.value))
                .join(optapy.get_class(Entity), [
                    optapy.constraint.Joiners.equal(lambda pair: pair[0], lambda entity: entity.code),
                    optapy.constraint.Joiners.equal(lambda pair: pair[1], lambda entity: entity.value)
                ])
                .reward('Entity for pair', optapy.score.SimpleScore.ONE),
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
    termination_config.setBestScoreLimit('2')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints)) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution(Entity('A'), Value(date1), [date1, date2, date3])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 3
    assert solution.entity.value == date1


def test_list_variable():
    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            if value is None:
                value = []
            self.value = value

        @optapy.planning_list_variable(int, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value


    def count_mismatches(entity):
        mismatches = 0
        for index in range(len(entity.value)):
            if entity.value[index] != index + 1:
                mismatches += 1
        return mismatches


    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(optapy.get_class(Entity))
                .filter(lambda entity: any(entity.value[index] != index + 1 for index in range(len(entity.value))))
                .penalize('Value is not the same as index', optapy.score.SimpleScore.ONE, count_mismatches),
        ]

    @optapy.planning_solution
    class Solution:
        def __init__(self, entity, value_range, score=None):
            self.entity = entity
            self.value_range = value_range
            self.score = score

        @optapy.planning_entity_property(Entity)
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
    termination_config.setBestScoreLimit('0')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints)) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution(Entity('A'), [1, 2, 3])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 0
    assert solution.entity.value == [1, 2, 3]
