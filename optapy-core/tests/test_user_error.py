import optapy
import optapy.score
import optapy.config
import optapy.constraint
from optapy.types import Duration
import pytest
import re


@optapy.planning_entity
class Entity:
    def __init__(self, value=None):
        self.value = value

    @optapy.planning_variable(str, value_range_provider_refs=['value_range'])
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

    @optapy.problem_fact_collection_property(str)
    @optapy.value_range_provider(range_id='value_range')
    def get_value_list(self):
        return self.value_list

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score


@optapy.constraint_provider
def my_constraints(constraint_factory):
    return [
        constraint_factory.forEach(optapy.get_class(Entity))
            .penalize('Penalize each entity', optapy.score.SimpleScore.ONE, lambda entity: 'TEN')
    ]


def test_non_planning_solution_being_passed_to_solve():
    solver_config = optapy.config.solver.SolverConfig()
    solver_config.withSolutionClass(optapy.get_class(Solution)).withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints))
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    with pytest.raises(ValueError, match=re.escape(
            f'A problem was not passed to solve (parameter problem was ({None})). Maybe '
            f'pass an instance of a class annotated with @planning_solution to solve?'
    )):
        solver.solve(None)


def test_non_problem_fact_being_passed_to_problem_fact_collection():
    with pytest.raises(ValueError, match=f"<class '.*MyClass'> is not a @problem_fact. Maybe decorate "
                                         f"<class '.*MyClass'> with @problem_fact?"):
        class MyClass:
            pass

        class MySolution:
            def __init__(self, my_class_list):
                self.my_class_list = my_class_list

            @optapy.problem_fact_collection_property(MyClass)
            def get_my_class_list(self):
                return self.my_class_list


def test_none_passed_to_solve():
    solver_config = optapy.config.solver.SolverConfig()
    solver_config.withSolutionClass(optapy.get_class(Solution)).withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints))
    problem = 10
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    with pytest.raises(ValueError, match=re.escape(
            f'The problem ({problem}) is not an instance of a @planning_solution class. '
            f'Maybe decorate the problem class ({type(problem)}) with @planning_solution?'
    )):
        solver.solve(10)


def test_bad_return_type():
    solver_config = optapy.config.solver.SolverConfig()
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses([optapy.get_class(Entity)]) \
        .withConstraintProviderClass(optapy.get_class(my_constraints)) \
        .withTerminationSpentLimit(optapy.types.Duration.ofSeconds(1))

    problem = Solution([Entity()], ['1', '2', '3'])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    with pytest.raises(RuntimeError, match=r'An error occurred during solving. This can occur when.*'):
        solver.solve(problem)


def test_non_proxied_class_passed():
    class NonProxied:
        pass

    solver_config = optapy.config.solver.SolverConfig()
    with pytest.raises(ValueError, match=re.escape(
            f'Type {NonProxied} does not have a Java class proxy. Maybe annotate it with '
            f'@problem_fact, @planning_entity, or @planning_solution?'
    )):
        solver_config.withSolutionClass(NonProxied)


def test_non_proxied_function_passed():
    def not_proxied():
        pass

    solver_config = optapy.config.solver.SolverConfig()
    with pytest.raises(ValueError, match=re.escape(
            f'Function {not_proxied} does not have a Java class proxy. Maybe annotate it with '
            f'@constraint_provider?')):
        solver_config.withConstraintProviderClass(not_proxied)
