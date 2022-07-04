import pathlib
import threading

from jpype.types import *
from jpype import JImplements, JImplementationFor, JOverride
from typing import TypeVar, Generic, Callable, Union, TYPE_CHECKING
from types import FunctionType
from uuid import uuid1 as _uuid1
from .optaplanner_java_interop import _setup_solver_run, _cleanup_solver_run, _unwrap_java_object, \
    solver_run_id_to_refs as _solver_run_id_to_refs, get_class, \
    class_identifier_to_java_class_map as _class_identifier_to_java_class_map

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from org.optaplanner.core.config.solver import SolverConfig as _SolverConfig
    from org.optaplanner.core.api.solver import SolverFactory as _SolverFactory, SolverManager as _SolverManager,\
        SolverJob as _SolverJob, SolverStatus as _SolverStatus
    from org.optaplanner.core.api.score import ScoreManager as _ScoreManager

Solution_ = TypeVar('Solution_')
ProblemId_ = TypeVar('ProblemId_')


def await_best_solution_from_solver_job(solver_job: '_SolverJob', problem_id, exception_handler):
    try:
        solver_job.getFinalBestSolution()
    except Exception as e:
        exception_handler(problem_id, e)
        raise e


def create_python_thread_for_solver_job(solver_job: '_SolverJob', problem_id, exception_handler):
    threading.Thread(target=await_best_solution_from_solver_job, args=(solver_job, problem_id, exception_handler)).start()


@JImplements('org.optaplanner.core.api.solver.SolverManager', deferred=True)
class _PythonSolverManager(Generic[Solution_, ProblemId_]):
    def __init__(self, solver_config: '_SolverConfig'):
        from org.optaplanner.optapy import PythonSolver  # noqa
        from org.optaplanner.core.api.solver import SolverManager
        self.delegate = SolverManager.create(solver_config)
        self.problem_id_to_solver_run_ref_list = dict()

    def _optapy_debug_get_solver_runs_dicts(self):
        """
        Internal method used for testing; do not use
        """
        return {
            'solver_run_id_to_refs': _solver_run_id_to_refs
        }

    def _get_problem_getter_and_cleanup(self, problem_id, the_problem):
        from org.optaplanner.optapy import PythonSolver # noqa

        problem_function = the_problem
        if not callable(problem_function):
            def the_problem_function(the_problem_id):
                return the_problem
            problem_function = the_problem_function

        def problem_getter(the_problem_id):
            from copy import copy
            problem = copy(problem_function(the_problem_id))
            solver_run_id = (id(self), the_problem_id)
            problem._optapy_solver_run_id = solver_run_id
            self.problem_id_to_solver_run_ref_list[the_problem_id] = [problem, problem]
            _setup_solver_run(solver_run_id, self.problem_id_to_solver_run_ref_list[the_problem_id])
            wrapped_problem = PythonSolver.wrapProblem(get_class(type(problem)), problem)
            return wrapped_problem

        def cleanup():
            solver_run_id = (id(self), problem_id)
            _cleanup_solver_run(solver_run_id)
            del self.problem_id_to_solver_run_ref_list[problem_id]

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
    def addProblemChange(self, problem_id, problem_change):
        self.delegate.addProblemChange(problem_id, problem_change)

    @JOverride
    def solve(self, problem_id: ProblemId_, problem: Union[Solution_, Callable[[ProblemId_], Solution_]],
              final_best_solution_consumer: Callable[[Solution_], None] = None,
              exception_handler: Callable[[ProblemId_, JException], None] = None) -> \
            '_SolverJob[Solution_, ProblemId_]':
        problem_getter, cleanup = self._get_problem_getter_and_cleanup(problem_id, problem)
        wrapped_final_best_solution_consumer, wrapped_exception_handler = \
            self._wrap_final_best_solution_and_exception_handler(cleanup, final_best_solution_consumer,
                                                                 exception_handler)

        solver_job = self.delegate.solve(problem_id, problem_getter, wrapped_final_best_solution_consumer,
                                         wrapped_exception_handler)
        create_python_thread_for_solver_job(solver_job, problem_id,
                                            exception_handler if exception_handler is not None else lambda _1, _2: None)
        return solver_job

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

        solver_job = self.delegate.solveAndListen(problem_id, problem_getter, wrapped_best_solution_consumer,
                                                  wrapped_final_best_solution_consumer,
                                                  wrapped_exception_handler)

        create_python_thread_for_solver_job(solver_job, problem_id,
                                            exception_handler if exception_handler is not None else lambda _1, _2: None)
        return solver_job

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

        # No solution cloning happens in ScoreManager
        # so we don't need to clone the problem and set run id.
        solver_run_id = (id(self), id(problem), _uuid1())
        solver_run_ref_list = [problem, problem]
        wrapped_problem = PythonSolver.wrapProblem(get_class(type(problem)), problem)
        _setup_solver_run(solver_run_id, solver_run_ref_list)
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
            _cleanup_solver_run(solver_run_id)

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


def _wrap_object(object_to_wrap, instance_map):
    from org.optaplanner.optapy import PythonSolver, PythonWrapperGenerator  # noqa
    maybe_object = instance_map.get(id(object_to_wrap))
    if maybe_object is not None:
        return maybe_object
    if isinstance(object_to_wrap, int):
        return PythonWrapperGenerator.wrapInt(object_to_wrap)
    elif isinstance(object_to_wrap, bool):
        return PythonWrapperGenerator.wrapBoolean(object_to_wrap)
    object_class = get_class(type(object_to_wrap))
    return PythonWrapperGenerator.wrap(object_class, object_to_wrap, instance_map)


_problem_change_director_to_instance_dict = dict()


@JImplementationFor('org.optaplanner.core.api.solver.change.ProblemChangeDirector')
class _PythonProblemChangeDirector:
    def _set_instance_map(self, run_id, instance_map):
        global _problem_change_director_to_instance_dict
        _problem_change_director_to_instance_dict[run_id] = instance_map

    def _unset_instance_map(self, run_id):
        global _problem_change_director_to_instance_dict
        del _problem_change_director_to_instance_dict[run_id]

    @JOverride(sticky=True, rename='_java_addEntity')
    def addEntity(self, entity, entityConsumer):
        global _problem_change_director_to_instance_dict
        instance_map = _problem_change_director_to_instance_dict[id(self)]
        self._java_addEntity(_wrap_object(entity, instance_map), entityConsumer)

    @JOverride(sticky=True, rename='_java_addProblemFact')
    def addProblemFact(self, problemFact, problemFactConsumer):
        global _problem_change_director_to_instance_dict
        instance_map = _problem_change_director_to_instance_dict[id(self)]
        self._java_addProblemFact(_wrap_object(problemFact, instance_map), problemFactConsumer)

    @JOverride(sticky=True, rename='_java_changeProblemProperty')
    def changeProblemProperty(self, problemFactOrEntity, problemFactOrEntityConsumer):
        global _problem_change_director_to_instance_dict
        instance_map = _problem_change_director_to_instance_dict[id(self)]
        self._java_changeProblemProperty(_wrap_object(problemFactOrEntity, instance_map), problemFactOrEntityConsumer)

    @JOverride(sticky=True, rename='_java_changeVariable')
    def changeVariable(self, entity, variableName, entityConsumer):
        global _problem_change_director_to_instance_dict
        instance_map = _problem_change_director_to_instance_dict[id(self)]
        self._java_changeVariable(_wrap_object(entity, instance_map), variableName, entityConsumer)

    @JOverride(sticky=True, rename='_java_lookUpWorkingObject')
    def lookUpWorkingObject(self, externalObject):
        global _problem_change_director_to_instance_dict
        instance_map = _problem_change_director_to_instance_dict[id(self)]
        return self._java_lookUpWorkingObject(_wrap_object(externalObject, instance_map))

    @JOverride(sticky=True, rename='_java_lookUpWorkingObjectOrFail')
    def lookUpWorkingObjectOrFail(self, externalObject):
        global _problem_change_director_to_instance_dict
        instance_map = _problem_change_director_to_instance_dict[id(self)]
        return self._java_lookUpWorkingObjectOrFail(_wrap_object(externalObject, instance_map))

    @JOverride(sticky=True, rename='_java_removeEntity')
    def removeEntity(self, entity, entityConsumer):
        global _problem_change_director_to_instance_dict
        instance_map = _problem_change_director_to_instance_dict[id(self)]
        self._java_removeEntity(_wrap_object(entity, instance_map), entityConsumer)

    @JOverride(sticky=True, rename='_java_removeProblemFact')
    def removeProblemFact(self, problemFact, problemFactConsumer):
        global _problem_change_director_to_instance_dict
        instance_map = _problem_change_director_to_instance_dict[id(self)]
        self._java_removeProblemFact(_wrap_object(problemFact, instance_map), problemFactConsumer)


def solver_config_create_from_xml_file(solver_config_path: pathlib.Path) -> '_SolverConfig':
    """Loads a SolverConfig from the given file.

    :param solver_config_path: The path to the file
    :return: A new SolverConfig generated from the file at path
    """
    from java.lang import Thread, IllegalArgumentException
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    from org.optaplanner.core.config.solver import SolverConfig
    class_loader = PythonWrapperGenerator.getClassLoaderForAliasMap(_class_identifier_to_java_class_map)
    current_thread = Thread.currentThread()
    thread_class_loader = current_thread.getContextClassLoader()
    try:
        current_thread.setContextClassLoader(class_loader)
        solver_config = SolverConfig.createFromXmlFile(solver_config_path)
    except IllegalArgumentException as e:
        raise FileNotFoundError(f'Unable to find SolverConfig file ({solver_config_path}).') from e
    finally:
        current_thread.setContextClassLoader(thread_class_loader)
    return solver_config


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
        }

    @JOverride(sticky=True, rename='_java_solve')
    def solve(self, problem):
        from org.optaplanner.optapy import PythonSolver  # noqa
        from jpype import JException
        from copy import copy

        if problem is None:
            raise ValueError(f'A problem was not passed to solve (parameter problem was ({problem})). Maybe '
                             f'pass an instance of a class annotated with @planning_solution to solve?')

        if not hasattr(type(problem), '__optapy_is_planning_solution'):
            raise ValueError(f'The problem ({problem}) is not an instance of a @planning_solution class. Maybe '
                             f'decorate the problem class ({type(problem)}) with @planning_solution?')

        problem = copy(problem)
        solver_run_id = (id(self), id(problem), _uuid1())
        solver_run_ref_list = [problem, problem]
        object_class = get_class(type(problem))
        problem._optapy_solver_run_id = solver_run_id

        wrapped_problem = PythonSolver.wrapProblem(object_class, problem)
        _setup_solver_run(solver_run_id, solver_run_ref_list)
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
            _cleanup_solver_run(solver_run_id)
