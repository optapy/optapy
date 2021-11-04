import jpype
import jpype.imports
from jpype.types import *
from jpype import JProxy, JImplements, JOverride, JImplementationFor, JConversion
import importlib.metadata
from inspect import signature, Parameter
from typing import cast, List, Tuple, Type, TypeVar, Callable, Dict, Any, Union, TYPE_CHECKING
import copy

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from org.optaplanner.core.api.score.stream import Constraint, ConstraintFactory
    from org.optaplanner.core.config.solver import SolverConfig


Solution_ = TypeVar('Solution_')


def extract_optaplanner_jars() -> list[str]:
    """Extracts and return a list of OptaPy Java dependencies

    Invoking this function extracts OptaPy Dependencies from the optapy.jars module
    into a temporary directory and returns a list contains classpath entries for
    those dependencies. The temporary directory exists for the entire execution of the
    program.

    :return: None
    """
    return [str(p.locate()) for p in importlib.metadata.files('optapy') if p.name.endswith('.jar')]

# ***********************************************************
# Python Wrapper for Java Interfaces
# ***********************************************************


@JImplements('org.optaplanner.core.api.score.stream.ConstraintProvider', deferred=True)
class ConstraintProviderFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def defineConstraints(self, constraint_factory):
        return self.delegate(constraint_factory)


@JImplements('java.util.function.Function', deferred=True)
class PythonFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument):
        return self.delegate(argument)


@JImplements('java.util.function.BiFunction', deferred=True)
class PythonBiFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2):
        return self.delegate(argument1, argument2)


@JImplements('org.optaplanner.core.api.function.TriFunction', deferred=True)
class PythonTriFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2, argument3):
        return self.delegate(argument1, argument2, argument3)

# ****************************************************************************


def _get_python_object_id(item):
    """Returns a unique id for a Python Object (used in cloning)"""
    return id(item)


def _get_python_object_str(item):
    """Returns the Python Object represented as a String (used in toString)"""
    return str(item)


# No Op -- is this needed?
def _get_python_object_from_id(item_id):
    return item_id


def _get_python_object_attribute(object_id, name):
    """Gets an attribute from a Python Object"""
    import java.lang.Object
    import org.optaplanner.core.api.score.Score
    the_object = object_id
    python_object_getter = getattr(the_object, str(name))
    if not callable(python_object_getter):
        from org.optaplanner.optapy import OptaPyException # noqa
        error = (f'The attribute {name} on {object_id }is not callable (got {python_object_getter}, '
                 f'expecting a function). You might have overridden the function {name} with a value.')
        raise OptaPyException(error)
    python_object = python_object_getter()
    if python_object is None:
        return None
    elif isinstance(python_object, (str, bool, int, float, complex, org.optaplanner.core.api.score.Score)):
        out = JObject(python_object, java.lang.Object)
        return out
    else:
        return JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=python_object, convert=True)


def _get_python_array_to_id_array(the_object: List):
    """Maps a Python List to a Java List of OpaquePythonReference"""
    import org.optaplanner.optapy.OpaquePythonReference
    out = _to_java_list(list(map(lambda x: JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=x, convert=True),
                                 the_object)))
    return out


def _get_python_array_to_java_list(the_object: List):
    """Maps a Python List of primitive elements to """
    return _to_java_list(the_object)


def _set_python_object_attribute(object_id: int, name: str, value: Any) -> None:
    """Sets an attribute on an Python Object"""
    from org.optaplanner.optapy import PythonObject # noqa
    the_object = object_id
    the_value = value
    if isinstance(the_value, PythonObject):
        the_value = value.get__optapy_Id()
    getattr(the_object, str(name))(the_value)


def _deep_clone_python_object(the_object: Any):
    """Deeps clone a Python Object, and keeps a reference to it

    Java Objects are shallowed copied. A reference is kept since
    the Object is kept in Java NOT in Python, meaning it'll be
    garbage collected.

    :parameter the_object: the object to be cloned.
    :return: An OpaquePythonReference of the cloned Python Object
    """
    import org.optaplanner.optapy.OpaquePythonReference
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    # ...Python evaluate default arg once, so if we don't set the memo arg to a new dictionary,
    # the same dictionary is reused!
    item = PythonWrapperGenerator.getPythonObject(the_object)
    the_clone = copy.deepcopy(item, memo={})
    for run_id in ref_id_to_solver_run_id[id(item)]:
        solver_run_id_to_refs[run_id].add(the_clone)
    ref_id_to_solver_run_id[id(the_clone)] = ref_id_to_solver_run_id[id(item)]
    return JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=the_clone, convert=True)


def init(*args, path: List[str] = None, include_optaplanner_jars: bool = True, log_level='INFO'):
    """Start the JVM. Throws a RuntimeError if it is already started.

    :param args: JVM args.
    :param path: If not None, a list of dependencies to use as the classpath. Default to None.
    :param include_optaplanner_jars: If True, add optaplanner jars to path. Default to True.
    :param log_level: What OptaPlanner log level should be set to.
                      Must be one of 'TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'.
                      Defaults to 'INFO'
    :return: None
    """
    if jpype.isJVMStarted():
        raise RuntimeError('JVM already started. Maybe call init before optapy.type imports?')
    if path is None:
        include_optaplanner_jars = True
        path = []
    if include_optaplanner_jars:
        path = path + extract_optaplanner_jars()
    if len(args) == 0:
        args = (jpype.getDefaultJVMPath(), '-Dlogback.level.org.optaplanner={}'.format(log_level))
    else:
        args = args + ('-Dlogback.level.org.optaplanner={}'.format(log_level),)
    jpype.startJVM(*args, classpath=path)
    import java.util.function.Function
    import java.util.function.BiFunction
    import org.optaplanner.core.api.function.TriFunction
    from org.optaplanner.optapy import PythonWrapperGenerator, PythonPlanningSolutionCloner # noqa
    PythonWrapperGenerator.setPythonObjectToId(JObject(PythonFunction(_get_python_object_id),
                                                       java.util.function.Function))
    PythonWrapperGenerator.setPythonObjectToString(JObject(PythonFunction(_get_python_object_str),
                                                           java.util.function.Function))
    PythonWrapperGenerator.setPythonArrayIdToIdArray(JObject(PythonFunction(_get_python_array_to_id_array),
                                                             java.util.function.Function))
    PythonWrapperGenerator.setPythonArrayToJavaList(JObject(PythonFunction(_get_python_array_to_java_list),
                                                             java.util.function.Function))
    PythonWrapperGenerator.setPythonObjectIdAndAttributeNameToValue(
        JObject(PythonBiFunction(_get_python_object_attribute), java.util.function.BiFunction))
    PythonWrapperGenerator.setPythonObjectIdAndAttributeSetter(JObject(PythonTriFunction(_set_python_object_attribute),
                                                                       org.optaplanner.core.api.function.TriFunction))
    PythonPlanningSolutionCloner.setDeepClonePythonObject(JObject(PythonFunction(_deep_clone_python_object),
                                                                  java.util.function.Function))


def ensure_init():
    """Start the JVM if it isn't started; does nothing otherwise

    Used by OptaPy to start the JVM when needed by a method, so
    users don't need to start the JVM themselves.

    :return: None
    """
    if jpype.isJVMStarted():
        return
    else:
        init()


solver_run_id_to_refs = dict()
"""Maps solver run id to solution clones it references"""


ref_id_to_solver_run_id = dict()
"""Maps solution clone ids to the solver runs it is used in"""


@JImplementationFor('org.optaplanner.optapy.PythonObject')
class _PythonObject:
    """Maps a Java Python Object to a Python Python Object.

    Overrides __getattr__ and __setattr__ so it can be
    accessed like a normal Python Object in Python code.
    Note: JPype goes into infinite recursion when trying
    to access an attribute on the Java Object when
    used in a @JImplementationFor class with __getattr__
    overridden, which is why we pass the Java Object
    to PythonWrapperGenerator to get the corresponding
    Python Object versus accessing it directly.
    """
    def __jclass_init__(self):
        pass

    def __getattr__(self, name):
        from org.optaplanner.optapy import PythonWrapperGenerator # noqa
        item = PythonWrapperGenerator.getPythonObject(self)
        return getattr(item, name)

    def __setattr__(self, key, value):
        from org.optaplanner.optapy import PythonWrapperGenerator # noqa
        item = PythonWrapperGenerator.getPythonObject(self)
        setattr(item, key, value)


def _add_deep_copy_to_class(the_class: Type):
    """Adds a __deepcopy__ method to a class if it does not have one

    Java Objects cannot be deep copied, thus the pickle default deepcopy
    method does not work. The __deepcopy__ method calls the __new__ method
    of the class with None passed for each of __init__ parameters. It then
    calls setattr for each variable in the original object on the clone.

    :param the_class: the class to add the deep copy method to.
    :return: None
    """
    if callable(getattr(the_class, '__deepcopy__', None)):
        return
    sig = signature(the_class.__init__)
    keyword_args = dict()
    positional_args = list()
    skip_self_parameter = True
    for parameter_name, parameter in sig.parameters.items():
        if skip_self_parameter:
            skip_self_parameter = False
            continue
        if parameter.default == Parameter.empty and parameter.kind != Parameter.VAR_POSITIONAL and \
                parameter.kind != Parameter.VAR_KEYWORD:
            if parameter.kind == Parameter.POSITIONAL_ONLY or parameter.kind == Parameter.POSITIONAL_OR_KEYWORD:
                positional_args.append(None)
            else:
                keyword_args[parameter_name] = None

    def class_deep_copy(self, memo):
        import java.lang.Object
        clone = the_class.__new__(the_class, *positional_args, **keyword_args) # noqa
        memo[id(self)] = clone
        item_vars = vars(self)
        for attribute, value in item_vars.items():
            if isinstance(value, java.lang.Object):
                memo[id(value)] = value
                setattr(clone, attribute, value)
            else:
                setattr(clone, attribute, copy.deepcopy(value, memo=memo))
        return clone
    the_class.__deepcopy__ = class_deep_copy


def solve(solver_config: 'SolverConfig', problem: Solution_) -> Solution_:
    """Waits for solving to terminate and return the best solution found for the given problem using the solver_config.

    Calling multiple time starts a different solver.
    :param solver_config: The Java SolverConfig. See OptaPlanner docs for details.
    :param problem: The (potentially uninitialized) Python Planning Solution object.
    :return: The best solution found.
    """
    from org.optaplanner.optapy import PythonSolver, OptaPyException # noqa
    from jpype import JException
    import org.optaplanner.optapy.OpaquePythonReference
    solver_run_id = max(solver_run_id_to_refs.keys(), default=0) + 1
    solver_run_ref_set = set()
    solver_run_ref_set.add(problem)
    solver_run_id_to_refs[solver_run_id] = solver_run_ref_set
    if id(problem) in ref_id_to_solver_run_id:
        ref_id_to_solver_run_id[id(problem)].add(solver_run_id)
    else:
        ref_id_to_solver_run_id[id(problem)] = set()
        ref_id_to_solver_run_id[id(problem)].add(solver_run_id)
    try:
        solution = _unwrap_java_object(PythonSolver.solve(solver_config,
                                                          JProxy(org.optaplanner.optapy.OpaquePythonReference,
                                                                 inst=problem, convert=True)))
    except JException as e:
        original = e
        cause = e
        while cause is not None:
            e = cause
            cause = e.__cause__
        if isinstance(e, OptaPyException):
            raise RuntimeError(e.getMessage())
        else:
            raise original
    ref_id_to_solver_run_id[id(problem)].remove(solver_run_id)
    for ref in solver_run_ref_set:
        if len(ref_id_to_solver_run_id[id(ref)]) == 0:
            del ref_id_to_solver_run_id[id(ref)]
    del solver_run_id_to_refs[solver_run_id]
    return solution


# Jpype convert int to primitive, but not to their wrappers, so add implicit conversion to wrappers
@JConversion('java.lang.Integer', exact=int)
def _convert_to_integer(jcls, obj):
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    return PythonWrapperGenerator.wrapInt(obj)


@JConversion('java.lang.Long', exact=int)
def _convert_to_long(jcls, obj):
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    return PythonWrapperGenerator.wrapLong(obj)


@JConversion('java.lang.Short', exact=int)
def _convert_to_short(jcls, obj):
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    return PythonWrapperGenerator.wrapShort(obj)


@JConversion('java.lang.Byte', exact=int)
def _convert_to_byte(jcls, obj):
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    return PythonWrapperGenerator.wrapByte(obj)


def _unwrap_java_object(java_object):
    """Gets the Python Python Object for the given Java Python Object"""
    return java_object.get__optapy_Id()


def _to_java_map(python_dict: Dict):
    """Converts a Python dict to a Java Map"""
    import java.lang.Object
    import java.util.HashMap
    out = java.util.HashMap()
    for key, value in python_dict.items():
        if isinstance(value, list):
            out.put(JObject(key, java.lang.Object), _to_java_list(value).toArray())
        else:
            out.put(JObject(key, java.lang.Object), JObject(value, java.lang.Object))
    return out


def _to_java_list(python_list: List):
    """Converts a Python list to a Java List"""
    import java.lang.Object
    import java.util.ArrayList
    out = java.util.ArrayList()
    for item in python_list:
        if isinstance(item, dict):
            out.add(_to_java_map(item))
        else:
            out.add(JObject(item, java.lang.Object))
    return out


def _get_optaplanner_annotations(python_class: Type) -> List[Tuple[str, JClass, List[dict]]]:
    """Gets the methods with OptaPlanner annotations in the given class"""
    method_list = [attribute for attribute in dir(python_class) if callable(getattr(python_class, attribute)) and
                   attribute.startswith('__') is False]
    annotated_methods = []
    for method in method_list:
        optaplanner_annotations = [attribute for attribute in dir(getattr(python_class, method)) if
                                   attribute.startswith('__optaplanner')]
        if optaplanner_annotations:
            return_type = getattr(getattr(python_class, method), "__return", None)
            annotated_methods.append(
                _to_java_list([method, return_type,
                               _to_java_list(list(map(lambda annotation: getattr(getattr(python_class, method),
                                                                                 annotation), optaplanner_annotations)))
                               ]))
    return _to_java_list(annotated_methods)


def get_class(python_class: Union[Type, Callable]) -> JClass:
    """Return the Java Class for the given Python Class"""
    if isinstance(python_class, jpype.JClass):
        return cast(JClass, python_class)
    if hasattr(python_class, '__javaClass'):
        return python_class.__javaClass
    if python_class == int:
        from java.lang import Integer
        return cast(JClass, Integer)
    if python_class == str:
        from java.lang import String
        return cast(JClass, String)
    if python_class == bool:
        from java.lang import Boolean
        return cast(JClass, Boolean)
    raise ValueError((
                      f"python_class {python_class} is not annotated with @planning_entity, @problem_fact or "
                      f"@planning_solution, is not a Java class, and is not int or str. Maybe annotate {python_class} "
                      f"with @planning_entity, @problem_fact or @planning_solution?"
                    ))



unique_class_id = 0
"""A unique identifier; used to guarantee the generated class java name is unique"""


def _generate_problem_fact_class(python_class):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    out = PythonWrapperGenerator.defineProblemFactClass(python_class.__name__ + str(unique_class_id),
                                                        optaplanner_annotations)
    unique_class_id = unique_class_id + 1
    return out


def _generate_planning_entity_class(python_class: Type, annotation_data: Dict[str, Any]):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    out = PythonWrapperGenerator.definePlanningEntityClass(python_class.__name__ + str(unique_class_id),
                                                           optaplanner_annotations,
                                                           _to_java_map(annotation_data))
    unique_class_id = unique_class_id + 1
    return out


def _generate_planning_solution_class(python_class: Type) -> JClass:
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    out = PythonWrapperGenerator.definePlanningSolutionClass(python_class.__name__ + str(unique_class_id),
                                                             optaplanner_annotations)
    unique_class_id = unique_class_id + 1
    return out


def _to_constraint_java_array(python_list: List['Constraint']) -> JArray:
    # reimport since the one in global scope is only for type checking
    import org.optaplanner.core.api.score.stream.Constraint as ActualConstraintClass
    out = jpype.JArray(ActualConstraintClass)(len(python_list))
    for i in range(len(python_list)):
        out[i] = python_list[i]
    return out


def _generate_constraint_provider_class(constraint_provider: Callable[['ConstraintFactory'], List['Constraint']]) -> \
        JClass:
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator # noqa
    from org.optaplanner.core.api.score.stream import ConstraintProvider
    out = PythonWrapperGenerator.defineConstraintProviderClass(
        constraint_provider.__name__ + str(unique_class_id),
        JObject(ConstraintProviderFunction(lambda cf: _to_constraint_java_array(constraint_provider(cf))),
                ConstraintProvider))
    unique_class_id = unique_class_id + 1
    return out
