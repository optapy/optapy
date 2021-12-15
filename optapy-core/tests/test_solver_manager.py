import optapy
import optapy.score
import optapy.config
import optapy.constraint


def test_solve():
    from threading import Lock
    from org.optaplanner.core.api.solver import SolverStatus
    lock = Lock()

    def get_lock(entity):
        lock.acquire()
        lock.release()
        return False

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
                .filter(get_lock)
                .reward('Wait for lock', optapy.score.SimpleScore.ONE),
            constraint_factory.forEach(optapy.get_class(Entity))
                .reward('Maximize Value', optapy.score.SimpleScore.ONE, lambda entity: entity.value),
            constraint_factory.forEachUniquePair(optapy.get_class(Entity),
                                                 optapy.constraint.Joiners.equal(lambda entity: entity.value))
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
    termination_config.setBestScoreLimit('6')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Entity)) \
        .withConstraintProviderClass(optapy.get_class(my_constraints)) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution([Entity('A'), Entity('B'), Entity('C')], [1, 2, 3], optapy.score.SimpleScore.ONE)

    def assert_solver_run(solver_manager, solver_job):
        assert solver_manager.getSolverStatus(1) != SolverStatus.NOT_SOLVING
        lock.release()
        solution = solver_job.getFinalBestSolution()
        assert solution.get_score().getScore() == 6
        value_list = [entity.value for entity in solution.entity_list]
        assert 1 in value_list
        assert 2 in value_list
        assert 3 in value_list
        assert solver_manager.getSolverStatus(1) == SolverStatus.NOT_SOLVING
        solver_run_dicts = solver_manager._optapy_debug_get_solver_runs_dicts()
        assert len(solver_run_dicts['solver_run_id_to_refs']) == 0
        assert len(solver_run_dicts['ref_id_to_solver_run_id']) == 0

    with optapy.solver_manager_create(solver_config) as solver_manager:
        lock.acquire()
        solver_job = solver_manager.solve(1, problem)
        assert_solver_run(solver_manager, solver_job)


        def get_problem(problem_id):
            assert problem_id == 1
            return problem

        lock.acquire()
        solver_job = solver_manager.solve(1, get_problem)
        assert_solver_run(solver_manager, solver_job)

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
        assert_solver_run(solver_manager, solver_job)
        assert len(solution_list) == 1

        solution_list = []
        lock.acquire()
        solver_job = solver_manager.solveAndListen(1, get_problem, on_best_solution_changed, on_best_solution_changed)
        assert_solver_run(solver_manager, solver_job)
        assert len(solution_list) == 2
