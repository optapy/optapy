from jpype.types import *
from jpype import JImplements, JImplementationFor, JOverride
from typing import TypeVar, Generic, Callable, Union, TYPE_CHECKING
from uuid import uuid1 as _uuid1
from .optaplanner_java_interop import _setup_solver_run, _cleanup_solver_run, _unwrap_java_object, \
    solver_run_id_to_refs as _solver_run_id_to_refs, ref_id_to_solver_run_id as _ref_id_to_solver_run_id, get_class

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from org.optaplanner.core.config.solver import SolverConfig as _SolverConfig
    from org.optaplanner.core.api.solver import SolverFactory as _SolverFactory, SolverManager as _SolverManager,\
        SolverJob as _SolverJob, SolverStatus as _SolverStatus
    from org.optaplanner.core.api.score import ScoreManager as _ScoreManager

Solution_ = TypeVar('Solution_')
ProblemId_ = TypeVar('ProblemId_')


@JImplements('org.optaplanner.core.api.solver.SolverManager', deferred=True)
class _PythonSolverManager(Generic[Solution_, ProblemId_]):
    def __init__(self, solver_config: '_SolverConfig'):
        from org.optaplanner.optapy import PythonSolver  # noqa
        from org.optaplanner.core.api.solver import SolverManager
        self.delegate = SolverManager.create(solver_config)
        self.problem_id_to_solver_run_ref_set = dict()

    def _optapy_debug_get_solver_runs_dicts(self):
        """
        Internal method used for testing; do not use
        """
        return {
            'solver_run_id_to_refs': _solver_run_id_to_refs,
            'ref_id_to_solver_run_id': _ref_id_to_solver_run_id,
        }

    def _get_problem_getter_and_cleanup(self, problem_id, the_problem):
        from org.optaplanner.optapy import PythonSolver # noqa

        problem_list = []
        problem_function = the_problem
        if not callable(problem_function):
            def the_problem_function(the_problem_id):
                return the_problem
            problem_function = the_problem_function

        def problem_getter(the_problem_id):
            problem = problem_function(the_problem_id)
            problem_list.append(problem)
            solver_run_id = (id(self), the_problem_id)
            self.problem_id_to_solver_run_ref_set[the_problem_id] = set()
            _setup_solver_run(solver_run_id, problem, self.problem_id_to_solver_run_ref_set[the_problem_id])
            wrapped_problem = PythonSolver.wrapProblem(get_class(type(problem)), problem)
            return wrapped_problem

        def cleanup():
            if len(problem_list) == 0:
                return
            problem = problem_list[0]
            solver_run_ref_set = self.problem_id_to_solver_run_ref_set[problem_id]
            solver_run_id = (id(self), problem_id)
            _cleanup_solver_run(solver_run_id, problem, solver_run_ref_set)
            del self.problem_id_to_solver_run_ref_set[problem_id]

        return problem_getter, cleanup

    def _wrap_final_best_solution_and_exception_handler(self, cleanup, final_best_solution_consumer, exception_handler):
        def wrapped_final_best_solution_consumer(best_solution):
            if final_best_solution_consumer is not None:
                final_best_solution_consumer(_unwrap_java_object(best_solution))
            cleanup()

        def wrapped_exception_handler(problem_id, exception):
            if exception_handler is not None:
                exception_handler(problem_id, exception)
            cleanup()
        return wrapped_final_best_solution_consumer, wrapped_exception_handler

    @JOverride
    def solve(self, problem_id: ProblemId_, problem: Union[Solution_, Callable[[ProblemId_], Solution_]],
              final_best_solution_consumer: Callable[[Solution_], None] = None,
              exception_handler: Callable[[ProblemId_, JException], None] = None) -> \
            '_SolverJob[Solution_, ProblemId_]':
        problem_getter, cleanup = self._get_problem_getter_and_cleanup(problem_id, problem)
        wrapped_final_best_solution_consumer, wrapped_exception_handler = \
            self._wrap_final_best_solution_and_exception_handler(cleanup, final_best_solution_consumer,
                                                                 exception_handler)

        return self.delegate.solve(problem_id, problem_getter, wrapped_final_best_solution_consumer,
                                   wrapped_exception_handler)

    @JOverride
    def solveAndListen(self, problem_id: ProblemId_, problem: Union[Solution_, Callable[[ProblemId_], Solution_]],
                       best_solution_consumer: Callable[[Solution_], None],
                       final_best_solution_consumer: Callable[[Solution_], None] = None,
                       exception_handler: Callable[[ProblemId_, JException], None] = None) -> \
            '_SolverJob[Solution_, ProblemId_]':
        problem_getter, cleanup = self._get_problem_getter_and_cleanup(problem_id, problem)
        wrapped_final_best_solution_consumer, wrapped_exception_handler = \
            self._wrap_final_best_solution_and_exception_handler(cleanup, final_best_solution_consumer,
                                                                 exception_handler)

        def wrapped_best_solution_consumer(best_solution):
            best_solution_consumer(_unwrap_java_object(best_solution))

        return self.delegate.solveAndListen(problem_id, problem_getter, wrapped_best_solution_consumer,
                                            wrapped_final_best_solution_consumer,
                                            wrapped_exception_handler)

    @JOverride
    def getSolverStatus(self, problem_id: ProblemId_) -> '_SolverStatus':
        return self.delegate.getSolverStatus(problem_id)

    @JOverride
    def terminateEarly(self, problem_id: ProblemId_):
        return self.delegate.terminateEarly(problem_id)

    @JOverride
    def close(self):
        return self.delegate.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


@JImplementationFor('org.optaplanner.core.api.score.ScoreExplanation')
class _PythonScoreExplanation:
    @JOverride(sticky=True, rename='_java_getSolution')
    def getSolution(self):
        return _unwrap_java_object(self._java_getSolution())


@JImplementationFor('org.optaplanner.core.api.score.ScoreManager')
class _PythonScoreManager:
    def _wrap_call(self, function, problem):
        from org.optaplanner.optapy import PythonSolver  # noqa
        solver_run_id = (id(self), id(problem), _uuid1())
        solver_run_ref_set = set()
        wrapped_problem = PythonSolver.wrapProblem(get_class(type(problem)), problem)
        _setup_solver_run(solver_run_id, problem, solver_run_ref_set)
        try:
            return function(wrapped_problem)
        except JException as e:
            error_message = f'An error occurred when getting the score. This can occur when functions take the ' \
                            f'wrong number of parameters (ex: a setter that does not take exactly one parameter) or ' \
                            f'by a function returning an incompatible return type (ex: returning a str in a filter, ' \
                            f'which expects a bool). This can also occur when an exception is raised when evaluating ' \
                            f'constraints/getters/setters.'
            raise RuntimeError(error_message) from e
        finally:
            _cleanup_solver_run(solver_run_id, problem, solver_run_ref_set)

    @JOverride(sticky=True, rename='_java_updateScore')
    def updateScore(self, solution):
        score = self._wrap_call(lambda wrapped_solution: self._java_updateScore(wrapped_solution), solution)
        for attr in dir(solution):
            if hasattr(getattr(solution, attr), '__optaplannerPlanningScore'):
                setter = f'set{attr[3:]}'
                getattr(solution, setter)(score)
                break
        return score

    @JOverride(sticky=True, rename='_java_getSummary')
    def getSummary(self, solution):
        return self._wrap_call(lambda wrapped_solution: self._java_getSummary(wrapped_solution), solution)

    @JOverride(sticky=True, rename='_java_explainScore')
    def explainScore(self, solution):
        return self._wrap_call(lambda wrapped_solution: self._java_explainScore(wrapped_solution), solution)


def solver_manager_create(solver_config: '_SolverConfig') -> '_SolverManager':
    """Creates a new SolverManager, which can be used to solve problems asynchronously (ex: Web requests).

    :param solver_config: The solver configuration used in the SolverManager
    :return: A SolverManager that can be used to solve problems asynchronously.
    :rtype: SolverManager
    """
    return _PythonSolverManager(solver_config)


def score_manager_create(solver_builder: Union['_SolverFactory', '_SolverManager']) -> '_ScoreManager':
    """Creates a new SolverManager, which can be used to solve problems asynchronously (ex: Web requests).

    :param solver_builder: A SolverFactory or SolverManager which will be used to create the ScoreManager
    :return: A ScoreManager that can be used to update, explain, or get the score of a solution.
    :rtype: ScoreManager
    """
    from org.optaplanner.core.api.score import ScoreManager
    if isinstance(solver_builder, _PythonSolverManager):
        #  ScoreManager.create(SolverManager) uses an implementation specific method and expects a DefaultSolverManager
        return ScoreManager.create(solver_builder.delegate)
    return ScoreManager.create(solver_builder)


def solver_factory_create(solver_config: '_SolverConfig') -> '_SolverFactory':
    """Creates a new SolverFactory, which can be used to create Solvers.

    :param solver_config: The solver configuration used in the SolverFactory
    :return: A SolverFactory that can be used to create Solvers.
    :rtype: SolverFactory
    """
    from org.optaplanner.core.api.solver import SolverFactory
    return SolverFactory.create(solver_config)


def compose_constraint_id(solution_type_or_package: Union[type, str], constraint_name: str) -> str:
    """Returns the constraint id with the given constraint package and the given name

    :param solution_type_or_package: The constraint package, or a class decorated with @planning_solution
        (for when the constraint is in the default package)
    :param constraint_name: The name of the constraint
    :return: The constraint id with the given name in the default package.
    :rtype: str
    """
    package = solution_type_or_package
    if not isinstance(solution_type_or_package, str):
        package = get_class(solution_type_or_package).getPackage().getName()
    return f'{package}/{constraint_name}'


@JImplementationFor('org.optaplanner.core.api.solver.Solver')
class _PythonSolver:
    def __jclass_init__(self):
        pass

    @staticmethod
    def _optapy_debug_get_solver_runs_dicts():
        """
        Internal method used for testing; do not use
        """
        return {
            'solver_run_id_to_refs': _solver_run_id_to_refs,
            'ref_id_to_solver_run_id': _ref_id_to_solver_run_id,
        }

    @JOverride(sticky=True, rename='_java_solve')
    def solve(self, problem):
        from org.optaplanner.optapy import PythonSolver  # noqa
        from jpype import JException

        if problem is None:
            raise ValueError(f'A problem was not passed to solve (parameter problem was ({problem})). Maybe '
                             f'pass an instance of a class annotated with @planning_solution to solve?')

        if not hasattr(type(problem), '__optapy_is_planning_solution'):
            raise ValueError(f'The problem ({problem}) is not an instance of a @planning_solution class. Maybe '
                             f'decorate the problem class ({type(problem)}) with @planning_solution?')

        solver_run_id = (id(self), id(problem), _uuid1())
        solver_run_ref_set = set()
        wrapped_problem = PythonSolver.wrapProblem(get_class(type(problem)), problem)
        _setup_solver_run(solver_run_id, problem, solver_run_ref_set)
        try:
            return _unwrap_java_object(self._java_solve(wrapped_problem))
        except JException as e:
            error_message = f'An error occurred during solving. This can occur when functions take the wrong number '\
                            f'of parameters (ex: a setter that does not take exactly one parameter) or by ' \
                            f'a function returning an incompatible return type (ex: returning a str in a filter, ' \
                            f'which expects a bool). This can also occur when an exception is raised when evaluating ' \
                            f'constraints/getters/setters.'
            raise RuntimeError(error_message) from e
        finally:
            _cleanup_solver_run(solver_run_id, problem, solver_run_ref_set)

