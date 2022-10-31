import optapy
import optapy.types
import optapy.score
import optapy.config
import optapy.constraint
import dataclasses


def test_solve_partial():
    class Code:
        def __init__(self, value):
            self.value = value

    @optapy.problem_fact
    class Value:
        def __init__(self, code):
            self.code = Code(code)

    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = Code(code)
            self.value = value

        @optapy.planning_variable(Value, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value


    def is_value_one(constraint_factory: optapy.constraint.ConstraintFactory):
        return (constraint_factory.for_each(Entity)
                .filter(lambda e: e.value.code.value == 'v1')
                .reward('Value 1', optapy.score.SimpleScore.ONE)
                )

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
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
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('3')
    solver_config.withSolutionClass(Solution) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)

    e1 = Entity('e1')
    e2 = Entity('e2')
    e3 = Entity('e3')

    v1 = Value('v1')
    v2 = Value('v2')
    v3 = Value('v3')

    e1.value = v1
    e2.value = v2
    e3.value = v3

    problem = Solution([e1, e2, e3], [v1, v2, v3])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)

    assert solution.score.getScore() == 3
    assert solution.entities[0].value == v1
    assert solution.entities[1].value == v1
    assert solution.entities[2].value == v1


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
            constraint_factory.for_each(Entity)
                              .join(Value,
                                    optapy.constraint.Joiners.equal(lambda entity: entity.value,
                                                                    lambda value: value.code))
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
    solver_config.withSolutionClass(Solution) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution(Entity('A'), Value('1'), ['1', '2', '3'])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 1
    assert solution.entity.value == '1'


def test_constraint_stream_in_join():
    @optapy.problem_fact
    class Value:
        def __init__(self, code):
            self.code = code

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

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
                .filter(lambda e: e.code == 'A')
                .join(constraint_factory.for_each(Entity).filter(lambda e: e.code == 'B'))
                .join(constraint_factory.for_each(Entity).filter(lambda e: e.code == 'C'))
                .join(constraint_factory.for_each(Entity).filter(lambda e: e.code == 'D'))
                .group_by(optapy.constraint.ConstraintCollectors.sum(lambda a, b, c, d: a.value.code + b.value.code +
                                                                                        c.value.code + d.value.code))
                .reward('First Four Entities', optapy.score.SimpleScore.ONE, lambda the_sum: the_sum),
        ]

    @optapy.planning_solution
    class Solution:
        def __init__(self, entity_list, value_list, score=None):
            self.entity_list = entity_list
            self.value_list = value_list
            self.score = score

        @optapy.planning_entity_collection_property(Entity)
        def get_entity_list(self):
            return self.entity_list

        @optapy.problem_fact_collection_property(Value)
        @optapy.value_range_provider(range_id='value_range')
        def get_value(self):
            return self.value_list

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score

    solver_config = optapy.config.solver.SolverConfig()
    solver_config.withSolutionClass(Solution) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints)

    entity_1, entity_2, entity_3, entity_4, entity_5 = Entity('A'), Entity('B'), Entity('C'), Entity('D'), Entity('E')
    value_1, value_2, value_3 = Value(1), Value(2), Value(3)
    problem = Solution([entity_1, entity_2, entity_3, entity_4, entity_5], [value_1, value_2, value_3])
    score_manager = optapy.score_manager_create(optapy.solver_factory_create(solver_config))

    entity_1.set_value(value_1)
    entity_2.set_value(value_1)
    entity_3.set_value(value_1)
    entity_4.set_value(value_1)
    entity_5.set_value(value_1)

    assert score_manager.updateScore(problem) == optapy.score.SimpleScore.of(4)

    entity_5.set_value(value_2)

    assert score_manager.updateScore(problem) == optapy.score.SimpleScore.of(4)

    entity_1.set_value(value_2)
    assert score_manager.updateScore(problem) == optapy.score.SimpleScore.of(5)

    entity_2.set_value(value_2)
    assert score_manager.updateScore(problem) == optapy.score.SimpleScore.of(6)

    entity_3.set_value(value_2)
    assert score_manager.updateScore(problem) == optapy.score.SimpleScore.of(7)

    entity_4.set_value(value_2)
    assert score_manager.updateScore(problem) == optapy.score.SimpleScore.of(8)

    entity_1.set_value(value_3)
    assert score_manager.updateScore(problem) == optapy.score.SimpleScore.of(9)


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
            constraint_factory.for_each(Entity)
                .join(Value,
                      optapy.constraint.Joiners.equal(lambda entity: entity.value,
                                                      lambda value: value.code))
                .group_by(lambda entity, value: (0, value), optapy.constraint.ConstraintCollectors.count_bi())
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
    solver_config.withSolutionClass(Solution) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
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
    import ctypes
    pointer1 = ctypes.c_void_p(1)
    pointer2 = ctypes.c_void_p(2)
    pointer3 = ctypes.c_void_p(3)

    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            self.value = value

        @optapy.planning_variable(ctypes.c_void_p, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
                .filter(lambda entity: entity.value == pointer1)
                .reward('Same as value', optapy.score.SimpleScore.ONE),
            constraint_factory.for_each(Entity)
                .group_by(lambda entity: entity.value.value, optapy.constraint.ConstraintCollectors.count())
                .reward('Entity have same value', optapy.score.SimpleScore.ONE, lambda value, count: count * count),
            constraint_factory.for_each(Entity)
                .group_by(lambda entity: (entity.code, entity.value.value))
                .join(Entity,
                      optapy.constraint.Joiners.equal(lambda pair: pair[0], lambda entity: entity.code),
                      optapy.constraint.Joiners.equal(lambda pair: pair[1], lambda entity: entity.value.value))
                .reward('Entity for pair', optapy.score.SimpleScore.ONE),
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

        @optapy.value_range_provider(range_id='value_range', value_range_type=ctypes.c_void_p)
        def get_value_range(self):
            return self.value_range

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score

    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('3')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution(Entity('A'), [pointer1, pointer2, pointer3])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 3
    assert solution.entity.value is pointer1


def test_custom_planning_id():
    from uuid import uuid4
    id_1 = uuid4()
    id_2 = uuid4()
    id_3 = uuid4()

    @optapy.problem_fact
    class Value:
        def __init__(self, code):
            self.code = code

    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            self.value = value

        @optapy.planning_id
        def get_code(self):
            return self.code

        @optapy.planning_variable(Value, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.for_each_unique_pair(Entity,
                                                    optapy.constraint.Joiners.equal(lambda entity: entity.value))
                .penalize('Same value', optapy.score.SimpleScore.ONE),
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
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('0')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)

    entity_1 = Entity(id_1)
    entity_2 = Entity(id_2)
    entity_3 = Entity(id_3)

    value_1 = Value('A')
    value_2 = Value('B')
    value_3 = Value('C')
    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ], [
        value_1,
        value_2,
        value_3
    ])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 0

    encountered = set()
    for entity in solution.entities:
        assert entity.value not in encountered
        encountered.add(entity.value)


def test_custom_comparator():
    @optapy.problem_fact
    class Value:
        def __init__(self, code):
            self.code = code

        def __lt__(self, other):
            return self.code < other.code

    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            self.value = value

        @optapy.planning_id
        def get_code(self):
            return self.code

        @optapy.planning_variable(Value, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            # use less_than_or_equal and greater_than_or_equal since they require Comparable instances
            .if_exists_other(Entity, optapy.constraint.Joiners.less_than_or_equal(lambda entity: entity.value),
                             optapy.constraint.Joiners.greater_than_or_equal(lambda entity: entity.value))
            .penalize('Same value', optapy.score.SimpleScore.ONE),
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
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('0')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)

    entity_1 = Entity(0)
    entity_2 = Entity(1)
    entity_3 = Entity(2)

    value_1 = Value('A')
    value_2 = Value('B')
    value_3 = Value('C')
    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ], [
        value_1,
        value_2,
        value_3
    ])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 0

    encountered = set()
    for entity in solution.entities:
        assert entity.value not in encountered
        encountered.add(entity.value)


def test_custom_equals():
    class Code:
        def __init__(self, code):
            self.code = code

        def __eq__(self, other):
            return self.code == other.code

        def __hash__(self):
            return hash(self.code)

    @optapy.problem_fact
    class Value:
        code: Code

        def __init__(self, code):
            self.code = Code(code)

    @optapy.planning_entity
    class Entity:
        def __init__(self, code, value=None):
            self.code = code
            self.value = value

        @optapy.planning_id
        def get_code(self):
            return self.code

        @optapy.planning_variable(Value, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.for_each_unique_pair(Entity,
                                                    optapy.constraint.Joiners.equal(lambda entity: entity.value.code))
            .penalize('Same value', optapy.score.SimpleScore.ONE)
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
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('-1')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)

    value_1a = Value('A')
    value_1b = Value('A')
    value_2a = Value('B')

    entity_1 = Entity(0, value_1a)
    entity_2 = Entity(1, value_1b)
    entity_3 = Entity(2, value_2a)
    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ], [
        value_1a,
        value_1b,
        value_2a,
    ])
    score_manager = optapy.score_manager_create(optapy.solver_factory_create(solver_config))
    score = score_manager.updateScore(problem)
    assert score.getScore() == -1


def test_entity_value_range_provider():
    @optapy.problem_fact
    class Value:
        def __init__(self, code):
            self.code = code

    @optapy.planning_entity
    class Entity:
        def __init__(self, code, possible_values, value=None):
            self.code = code
            self.possible_values = possible_values
            self.value = value

        @optapy.planning_id
        def get_code(self):
            return self.code

        @optapy.planning_variable(Value, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value

        @optapy.value_range_provider(range_id='value_range', value_range_type=Value)
        def get_possible_values(self):
            return self.possible_values

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.for_each_unique_pair(Entity,
                                                    optapy.constraint.Joiners.equal(lambda entity: entity.value))
            .reward('Same value', optapy.score.SimpleScore.ONE),
        ]

    @optapy.planning_solution
    class Solution:
        def __init__(self, entities, score=None):
            self.entities = entities
            self.score = score

        @optapy.planning_entity_collection_property(Entity)
        def get_entities(self):
            return self.entities

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score

    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('0')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)

    value_1 = Value('A')
    value_2 = Value('B')
    value_3 = Value('C')

    entity_1 = Entity('1', [value_1])
    entity_2 = Entity('2', [value_2])
    entity_3 = Entity('3', [value_3])


    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 0

    encountered = set()
    for entity in solution.entities:
        assert entity.value not in encountered
        encountered.add(entity.value)


def test_int_value_range_provider():
    @optapy.planning_entity
    class Entity:
        def __init__(self, code, actual_value, value=None):
            self.code = code
            self.actual_value = actual_value
            self.value = value

        @optapy.planning_id
        def get_code(self):
            return self.code

        @optapy.planning_variable(int, value_range_provider_refs=['value_range'])
        def get_value(self):
            return self.value

        def set_value(self, value):
            self.value = value

        @optapy.value_range_provider(range_id='value_range', value_range_type=optapy.types.CountableValueRange)
        def get_possible_values(self):
            return optapy.types.ValueRangeFactory.createIntValueRange(self.actual_value, self.actual_value + 1)

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.for_each_unique_pair(Entity,
                                                    optapy.constraint.Joiners.equal(lambda entity: entity.value))
            .reward('Same value', optapy.score.SimpleScore.ONE),
        ]

    @optapy.planning_solution
    class Solution:
        def __init__(self, entities, score=None):
            self.entities = entities
            self.score = score

        @optapy.planning_entity_collection_property(Entity)
        def get_entities(self):
            return self.entities

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score

    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('0')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)

    entity_1 = Entity('1', 1)
    entity_2 = Entity('2', 2)
    entity_3 = Entity('3', 3)


    problem: Solution = Solution([
        entity_1,
        entity_2,
        entity_3
    ])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 0

    encountered = set()
    for entity in solution.entities:
        assert entity.value not in encountered
        encountered.add(entity.value)


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
            constraint_factory.for_each(Entity)
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
    solver_config.withSolutionClass(Solution) \
        .withEntityClasses(Entity) \
        .withConstraintProviderClass(my_constraints) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution(Entity('A'), [1, 2, 3])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 0
    assert solution.entity.value == [1, 2, 3]
