import optapy
import optapy.score
import optapy.config
import optapy.constraint


@optapy.planning_entity
class InverseRelationEntity:
    def __init__(self, code, value=None):
        self.code = code
        self.value = value

    @optapy.planning_variable(object, ['value_range'])
    def get_value(self):
        return self.value

    def set_value(self, value):
        self.value = value


@optapy.planning_entity
class InverseRelationValue:
    def __init__(self, code, entities=None):
        self.code = code
        if entities is None:
            self.entities = []
        else:
            self.entities = entities

    @optapy.inverse_relation_shadow_variable(InverseRelationEntity, source_variable_name='value')
    def get_entities(self):
        return self.entities

    def set_entities(self, entities):
        self.entities = entities


@optapy.planning_solution
class InverseRelationSolution:
    def __init__(self, values, entities, score=None):
        self.values = values
        self.entities = entities
        self.score = score

    @optapy.planning_entity_collection_property(InverseRelationEntity)
    def get_entities(self):
        return self.entities

    @optapy.planning_entity_collection_property(InverseRelationValue)
    @optapy.value_range_provider('value_range')
    def get_values(self):
        return self.values

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score


@optapy.constraint_provider
def inverse_relation_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
    return [
        constraint_factory.for_each(InverseRelationValue)
                          .filter(lambda value: len(value.entities) > 1)
                          .penalize('Only one entity per value', optapy.score.SimpleScore.ONE)
    ]


def test_inverse_relation():
    termination = optapy.config.solver.termination.TerminationConfig()
    termination.setBestScoreLimit('0')
    solver_config = optapy.config.solver.SolverConfig() \
        .withSolutionClass(InverseRelationSolution) \
        .withEntityClasses(InverseRelationEntity, InverseRelationValue) \
        .withConstraintProviderClass(inverse_relation_constraints) \
        .withTerminationConfig(termination)
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(InverseRelationSolution(
        [
            InverseRelationValue('A'),
            InverseRelationValue('B'),
            InverseRelationValue('C')
        ],
        [
            InverseRelationEntity('1'),
            InverseRelationEntity('2'),
            InverseRelationEntity('3'),
        ]
    ))
    assert solution.score.getScore() == 0
    visited_set = set()
    for value in solution.values:
        assert len(value.entities) == 1
        assert value.entities[0] is not None
        assert value.entities[0] not in visited_set
        visited_set.add(value.entities[0])
