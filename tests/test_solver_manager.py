import optapy
import optapy.types
import optapy.score
import optapy.config
import optapy.constraint


def test_solve():
    from threading import Lock
    import time
    from org.optaplanner.core.api.solver import SolverStatus
    lock = Lock()

    def get_lock(entity):
        lock.acquire()
        lock.release()
        return False

    @optapy.problem_fact
    class Value:
        def __init__(self, value):
            self.value = value

        @optapy.planning_id
        def get_id(self):
            return self.value

        def __str__(self):
            return f'Value({self.value})'

        def __repr__(self):
            return str(self)

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

        @optapy.planning_id
        def get_id(self):
            return self.code

        def __str__(self):
            return f'Entity(code={self.code}, value={self.value})'

        def __repr__(self):
            return str(self)

    @optapy.constraint_provider
    def my_constraints(constraint_factory: optapy.constraint.ConstraintFactory):
        return [
            constraint_factory.forEach(optapy.get_class(Entity))
                .filter(get_lock)
                .reward('Wait for lock', optapy.score.SimpleScore.ONE),
            constraint_factory.forEach(optapy.get_class(Entity))
                .reward('Maximize Value', optapy.score.SimpleScore.ONE, lambda entity: entity.value.value),
            constraint_factory.forEachUniquePair(optapy.get_class(Entity),
                                                 optapy.constraint.Joiners.equal(lambda entity: entity.value.value))
                .penalize('Same Value', optapy.score.SimpleScore.of(12)),
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

        @optapy.deep_planning_clone
        @optapy.problem_fact_collection_property(Value)
        @optapy.value_range_provider(range_id='value_range')
        def get_value_range(self):
            return self.value_range

        def set_value_range(self, value_range):
            self.value_range = value_range

        @optapy.planning_score(optapy.score.SimpleScore)
        def get_score(self) -> optapy.score.SimpleScore:
            return self.score

        def set_score(self, score):
            self.score = score

        def __str__(self):
            return f'Solution(entity_list={self.entity_list[0]}, value_list={self.value_range[0]}, score={self.score})'

    @optapy.problem_change
    class UseOnlyEntityAndValueProblemChange:
        def __init__(self, entity, value):
            self.entity = entity
            self.value = value

        def doChange(self, solution: Solution, problem_change_director: optapy.types.ProblemChangeDirector):
            problem_facts_to_remove = solution.value_range.copy()
            entities_to_remove = solution.entity_list.copy()
            for problem_fact in problem_facts_to_remove:
                problem_change_director.removeProblemFact(problem_fact,
                                                          lambda value: solution.value_range.remove(problem_fact))
            for removed_entity in entities_to_remove:
                problem_change_director.removeEntity(removed_entity,
                                                     lambda entity: solution.entity_list.remove(removed_entity))
            problem_change_director.addEntity(self.entity, lambda entity: solution.entity_list.append(entity))
            problem_change_director.addProblemFact(self.value, lambda value: solution.value_range.append(value))

    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('6')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints)) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution([Entity('A'), Entity('B'), Entity('C')], [Value(1), Value(2), Value(3)],
                                 optapy.score.SimpleScore.ONE)

    def assert_solver_run(solver_manager, solver_job):
        assert solver_manager.getSolverStatus(1) != SolverStatus.NOT_SOLVING
        lock.release()
        solution = solver_job.getFinalBestSolution()
        assert solution.get_score().getScore() == 6
        value_list = [entity.value.value for entity in solution.entity_list]
        assert 1 in value_list
        assert 2 in value_list
        assert 3 in value_list
        assert solver_manager.getSolverStatus(1) == SolverStatus.NOT_SOLVING
        time.sleep(0.1)  # Sleep so cleanup is guaranteed to be executed
        solver_run_dicts = solver_manager._optapy_debug_get_solver_runs_dicts()
        assert len(solver_run_dicts['solver_run_id_to_refs']) == 0
        assert len(solver_run_dicts['ref_id_to_solver_run_id']) == 0

    def assert_problem_change_solver_run(solver_manager, solver_job):
        assert solver_manager.getSolverStatus(1) != SolverStatus.NOT_SOLVING
        solver_manager.addProblemChange(1, UseOnlyEntityAndValueProblemChange(Entity('D'), Value(6)))
        lock.release()
        solution = solver_job.getFinalBestSolution()
        assert solution.get_score().getScore() == 6
        assert len(solution.entity_list) == 1
        assert len(solution.value_range) == 1
        assert solution.entity_list[0].code == 'D'
        assert solution.entity_list[0].value.value == 6
        assert solution.value_range[0].value == 6
        assert solver_manager.getSolverStatus(1) == SolverStatus.NOT_SOLVING
        time.sleep(0.1)  # Sleep so cleanup is guaranteed to be executed
        solver_run_dicts = solver_manager._optapy_debug_get_solver_runs_dicts()
        assert len(solver_run_dicts['solver_run_id_to_refs']) == 0
        assert len(solver_run_dicts['ref_id_to_solver_run_id']) == 0

    with optapy.solver_manager_create(solver_config) as solver_manager:
        lock.acquire()
        solver_job = solver_manager.solve(1, problem)
        assert_solver_run(solver_manager, solver_job)

        lock.acquire()
        solver_job = solver_manager.solve(1, problem)
        assert_problem_change_solver_run(solver_manager, solver_job)

        def get_problem(problem_id):
            assert problem_id == 1
            return problem

        lock.acquire()
        solver_job = solver_manager.solve(1, get_problem)
        assert_solver_run(solver_manager, solver_job)

        lock.acquire()
        solver_job = solver_manager.solve(1, get_problem)
        assert_problem_change_solver_run(solver_manager, solver_job)

        solution_list = []

        def on_best_solution_changed(solution):
            solution_list.append(solution)

        lock.acquire()
        solver_job = solver_manager.solve(1, get_problem, on_best_solution_changed)
        assert_solver_run(solver_manager, solver_job)
        assert len(solution_list) == 1

        solution_list = []
        lock.acquire()
        solver_job = solver_manager.solveAndListen(1, get_problem, on_best_solution_changed)
        assert_problem_change_solver_run(solver_manager, solver_job)
        assert len(solution_list) == 2

        solution_list = []
        lock.acquire()
        solver_job = solver_manager.solveAndListen(1, get_problem, on_best_solution_changed, on_best_solution_changed)
        assert_solver_run(solver_manager, solver_job)
        assert len(solution_list) == 2

        solution_list = []
        lock.acquire()
        solver_job = solver_manager.solveAndListen(1, get_problem, on_best_solution_changed, on_best_solution_changed)
        assert_problem_change_solver_run(solver_manager, solver_job)
        assert len(solution_list) == 3
    time.sleep(1)  # ensure the thread factory close
