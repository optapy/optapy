import optapy
import optapy.score
import optapy.config
import optapy.constraint


@optapy.problem_fact
class Value:
    def __init__(self, number):
        self.number = number


@optapy.planning_entity
class Entity:
    def __init__(self, code, value=None):
        self.code = code
        self.value = value

    @optapy.planning_variable(Value, ['value_range'])
    def get_value(self):
        return self.value

    def set_value(self, value):
        self.value = value


@optapy.planning_solution
class Solution:
    def __init__(self, entity_list, value_list, score=None):
        self.entity_list = entity_list
        self.value_list = value_list
        self.score = score

    @optapy.planning_entity_collection_property(Entity)
    def get_entity_list(self):
        return self.entity_list

    def set_entity_list(self, entity_list):
        self.entity_list = entity_list

    @optapy.problem_fact_collection_property(Value)
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


def create_score_manage(constraint_provider):
    return optapy.score_manager_create(optapy.solver_factory_create(optapy.config.solver.SolverConfig()
                 .withSolutionClass(Solution)
                 .withEntityClasses(Entity)
                 .withConstraintProviderClass(constraint_provider)))


def test_min():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
                .groupBy(optapy.constraint.ConstraintCollectors.min(lambda entity: entity.value.number))
                .reward('Min value', optapy.score.SimpleScore.ONE, lambda min_value: min_value)
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)


def test_max():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.max(lambda entity: entity.value.number))
            .reward('Max value', optapy.score.SimpleScore.ONE, lambda max_value: max_value)
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)


def test_sum():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.sum(lambda entity: entity.value.number))
            .reward('Sum value', optapy.score.SimpleScore.ONE, lambda sum_value: sum_value)
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(3)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(4)


def test_average():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.average(lambda entity: entity.value.number))
            .reward('Average value', optapy.score.SimpleScore.ONE, lambda average_value: int(10 * average_value))
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(10)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(15)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(20)


def test_count():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
                .filter(lambda entity: entity.code[0] == 'A')
                .groupBy(optapy.constraint.ConstraintCollectors.count())
                .reward('Count value', optapy.score.SimpleScore.ONE, lambda count: count)
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a1: Entity = Entity('A1')
    entity_a2: Entity = Entity('A2')
    entity_b: Entity = Entity('B1')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a1, entity_a2, entity_b], [value_1, value_2])
    entity_a1.set_value(value_1)
    entity_a2.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)


def test_count_distinct():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.countDistinct(lambda entity: entity.value))
            .reward('Count distinct value', optapy.score.SimpleScore.ONE, lambda count: count)
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)


def test_to_list():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.toList(lambda entity: entity.value))
            .reward('list size', optapy.score.SimpleScore.ONE, lambda values: len(values))
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)


def test_to_set():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.toSet(lambda entity: entity.value))
            .reward('set size', optapy.score.SimpleScore.ONE, lambda values: len(values))
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)


def test_to_map():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.toMap(lambda entity: entity.code, lambda entity: entity.value.number))
            .filter(lambda entity_map: next(iter(entity_map['A'])) == 1)
            .reward('map at B', optapy.score.SimpleScore.ONE, lambda entity_map: next(iter(entity_map['B'])))
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(0)


def test_to_sorted_set():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.toSortedSet(lambda entity: entity.value.number))
            .reward('min', optapy.score.SimpleScore.ONE, lambda values: next(iter(values)))
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)


def test_to_sorted_map():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.toMap(lambda entity: entity.code, lambda entity: entity.value.number))
            .filter(lambda entity_map: next(iter(entity_map['B'])) == 1)
            .reward('map at A', optapy.score.SimpleScore.ONE, lambda entity_map: next(iter(entity_map['A'])))
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(1)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(0)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(0)

    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)


def test_conditionally():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.conditionally(lambda entity: entity.code[0] == 'A',
                                                                          optapy.constraint.ConstraintCollectors.count()))
            .reward('Conditionally count value', optapy.score.SimpleScore.ONE, lambda count: count)
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a1: Entity = Entity('A1')
    entity_a2: Entity = Entity('A2')
    entity_b: Entity = Entity('B1')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a1, entity_a2, entity_b], [value_1, value_2])
    entity_a1.set_value(value_1)
    entity_a2.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(2)


def test_compose():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .groupBy(optapy.constraint.ConstraintCollectors.compose(
                optapy.constraint.ConstraintCollectors.min(lambda entity: entity.value.number),
                optapy.constraint.ConstraintCollectors.max(lambda entity: entity.value.number),
                lambda a,b: (a,b)
            ))
            .reward('Max value', optapy.score.SimpleScore.ONE, lambda min_max: min_max[0] + min_max[1] * 10)
            # min is in lower digit; max in upper digit
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')
    entity_b: Entity = Entity('B')

    value_1 = Value(1)
    value_2 = Value(2)

    problem = Solution([entity_a, entity_b], [value_1, value_2])
    entity_a.set_value(value_1)
    entity_b.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(11)

    entity_a.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(21)

    entity_b.set_value(value_2)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(22)


def test_flatten_last():
    @optapy.constraint_provider
    def define_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(Entity)
            .map(lambda entity: (1, 2, 3))
            .flattenLast(lambda the_tuple: the_tuple)
            .reward('Count', optapy.score.SimpleScore.ONE)
        ]

    score_manager = create_score_manage(define_constraints)

    entity_a: Entity = Entity('A')

    value_1 = Value(1)

    problem = Solution([entity_a], [value_1])
    entity_a.set_value(value_1)

    assert score_manager.explainScore(problem).getScore() == optapy.score.SimpleScore.of(3)
