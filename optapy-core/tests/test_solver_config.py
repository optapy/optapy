import pathlib
import pytest
import re
import optapy
import optapy.score
import optapy.config
import optapy.constraint

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


def test_load_from_solver_config_file():
    solver_config = optapy.solver_config_create_from_xml_file(pathlib.Path('tests', 'solverConfig-simple.xml'))
    assert solver_config.getSolutionClass() == optapy.get_class(Solution)
    entity_class_list = solver_config.getEntityClassList()
    assert entity_class_list.size() == 1
    assert entity_class_list.get(0) == optapy.get_class(Entity)
    assert solver_config.getScoreDirectorFactoryConfig().getConstraintProviderClass() == \
           optapy.get_class(my_constraints)
    assert solver_config.getTerminationConfig().getBestScoreLimit() == '0hard/0soft'


def test_reload_from_solver_config_file():
    @optapy.planning_solution
    class RedefinedSolution:
        ...

    RedefinedSolution1 = RedefinedSolution
    solver_config_1 = optapy.solver_config_create_from_xml_file(pathlib.Path('tests', 'solverConfig-redefined.xml'))

    @optapy.planning_solution
    class RedefinedSolution:
        ...

    RedefinedSolution2 = RedefinedSolution
    solver_config_2 = optapy.solver_config_create_from_xml_file(pathlib.Path('tests', 'solverConfig-redefined.xml'))

    assert solver_config_1.getSolutionClass() == optapy.get_class(RedefinedSolution1)
    assert solver_config_2.getSolutionClass() == optapy.get_class(RedefinedSolution2)


def test_cannot_find_solver_config_file():
    from java.lang import Thread
    current_thread = Thread.currentThread()
    thread_class_loader = current_thread.getContextClassLoader()
    with pytest.raises(FileNotFoundError, match=re.escape("Unable to find SolverConfig file (does-not-exist.xml).")):
        optapy.solver_config_create_from_xml_file(pathlib.Path('does-not-exist.xml'))
    assert current_thread.getContextClassLoader() == thread_class_loader
