import inspect

import jpype
import jpype.imports
from jpype.types import *
from jpype import JProxy, JImplementationFor, JOverride, JImplements
import importlib.metadata
from inspect import signature, Parameter
from typing import cast, List, Tuple, Type, TypeVar, Callable, Dict, Any, Union, TYPE_CHECKING
import copy
from .jpype_type_conversions import PythonSupplier, PythonFunction, PythonBiFunction, PythonTriFunction, \
    ConstraintProviderFunction

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from org.optaplanner.core.api.score.stream import Constraint as _Constraint, ConstraintFactory as _ConstraintFactory
    from org.optaplanner.core.api.score.calculator import IncrementalScoreCalculator as _IncrementalScoreCalculator

Solution_ = TypeVar('Solution_')
ProblemId_ = TypeVar('ProblemId_')
Score_ = TypeVar('Score_')


def extract_optaplanner_jars() -> list[str]:
    """Extracts and return a list of OptaPy Java dependencies

    Invoking this function extracts OptaPy Dependencies from the optapy.jars module
    into a temporary directory and returns a list contains classpath entries for
    those dependencies. The temporary directory exists for the entire execution of the
    program.

    :return: None
    """
    return [str(p.locate()) for p in importlib.metadata.files('optapy') if p.name.endswith('.jar')]


# ****************************************************************************
# PythonWrapperGenerator Python helper functions
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
    import java.util.Collection
    import org.optaplanner.core.api.score.Score
    the_object = object_id
    python_object_getter = getattr(the_object, str(name))
    if not callable(python_object_getter):
        from org.optaplanner.optapy import OptaPyException  # noqa
        error = (f'The attribute {name} on {object_id}is not callable (got {python_object_getter}, '
                 f'expecting a function). You might have overridden the function {name} with a value.')
        raise OptaPyException(error)
    try:
        python_object = python_object_getter()
        if python_object is None:
            return None
        elif isinstance(python_object, (str, bool, int, float, complex, java.util.Collection,
                                        org.optaplanner.core.api.score.Score)):
            out = JObject(python_object, java.lang.Object)
            return out
        else:
            return JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=python_object, convert=True)
    except Exception as e:
        from org.optaplanner.optapy import OptaPyException  # noqa
        error = f'An exception occur when calling {str(name)} on {str(the_object)}: {str(e)}. Check the code.'
        raise OptaPyException(error)


def _get_python_array_to_id_array(the_object: List):
    """Maps a Python List to a Java List of OpaquePythonReference"""
    import org.optaplanner.optapy.OpaquePythonReference
    out = _to_java_list(list(map(lambda x: JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=x, convert=True),
                                 the_object)))
    return out


def _get_python_array_to_java_list(the_object: List):
    """Maps a Python List of primitive elements to """
    return _to_java_list(the_object)


def _get_python_object_java_class(the_object):
    if the_object is not None:
        return get_class(the_object)
    return None


def _set_python_object_attribute(object_id: int, name: str, value: Any) -> None:
    """Sets an attribute on an Python Object"""
    from org.optaplanner.optapy import PythonObject  # noqa
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
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    item = PythonWrapperGenerator.getPythonObject(the_object)
    the_clone = _planning_clone(item, dict())
    for run_id in ref_id_to_solver_run_id[id(item)]:
        solver_run_id_to_refs[run_id].add(the_clone)
    ref_id_to_solver_run_id[id(the_clone)] = ref_id_to_solver_run_id[id(item)]
    return JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=the_clone, convert=True)


def _is_deep_planning_clone(object):
    """
    Return True iff object should be deep planning cloned, False otherwise.
    :param object: The object to check if it should be deep planning cloned.
    :return: True iff object should be deep planning cloned, False otherwise.
    """
    return hasattr(type(object), '__optapy_is_planning_clone')


def _planning_clone(item, memo):
    """
    Replaces attributes that reference planning entities or @deep_planning_cloned attributes
    with their planning clone.
    :param item: The item to be cloned
    :param memo: Map from id to already existing planning clone
    :return: A planning clone of the item
    """
    if item is None:
        return None
    item_id = id(item)
    if item_id in memo:
        return memo[item_id]
    elif isinstance(item, list):
        out = list()
        memo[item_id] = out
        for element in item:
            if _is_deep_planning_clone(element):
                planning_clone = _planning_clone(element, memo)
                out.append(planning_clone)
            else:
                out.append(element)
        return out
    elif isinstance(item, dict):
        out = dict()
        memo[item_id] = out
        for key, value in item.items():
            new_key = key
            new_value = value
            if _is_deep_planning_clone(key):
                new_key = _planning_clone(key, memo)
            if _is_deep_planning_clone(value):
                new_value = _planning_clone(value, memo)
            out[new_key] = new_value
        return out
    planning_clone = copy.copy(item)
    memo[item_id] = planning_clone
    planning_clone_attribute_names = vars(planning_clone)
    for planning_clone_attribute_name in planning_clone_attribute_names:
        planning_clone_attribute = getattr(planning_clone, planning_clone_attribute_name)
        if planning_clone_attribute is None:
            continue
        elif id(planning_clone_attribute) in memo:
            setattr(planning_clone, planning_clone_attribute_name, memo[id(planning_clone_attribute)])
        elif _is_deep_planning_clone(planning_clone_attribute):
            setattr(planning_clone, planning_clone_attribute_name, _planning_clone(planning_clone_attribute, memo))
        elif (isinstance(planning_clone_attribute, list) and len(planning_clone_attribute) > 0 and
              _is_deep_planning_clone(planning_clone_attribute[0])):
            setattr(planning_clone, planning_clone_attribute_name, _planning_clone(planning_clone_attribute, memo))
        elif isinstance(planning_clone_attribute, dict) and len(planning_clone_attribute) > 0:
            (key, value) = next(iter(planning_clone_attribute.items()))
            if _is_deep_planning_clone(key) or _is_deep_planning_clone(value):
                setattr(planning_clone, planning_clone_attribute_name, _planning_clone(planning_clone_attribute, memo))
    # Need to go to type to look at methods
    planning_clone_type = type(planning_clone)
    for planning_clone_attribute_name in dir(planning_clone_type):
        planning_clone_attribute = getattr(planning_clone_type, planning_clone_attribute_name)
        if inspect.isfunction(planning_clone_attribute) and \
                hasattr(planning_clone_attribute, '__optapy_is_planning_clone'):
            setter = f'set{planning_clone_attribute_name[3:]}'
            try:
                attribute_value = getattr(planning_clone, planning_clone_attribute_name)()
            except Exception as e:
                from org.optaplanner.optapy import OptaPyException  # noqa
                error = (f'An exception occur when getting the @deep_planning_clone property'
                         f'{planning_clone_attribute_name} on object {str(item)}: {str(e)}')
                raise OptaPyException(error)
            attribute_value_clone = _planning_clone(attribute_value, memo)
            try:
                getattr(planning_clone, setter)(attribute_value_clone)
            except AttributeError as e:
                from org.optaplanner.optapy import OptaPyException  # noqa
                error = (f'There is no corresponding setter {setter} for deep cloned property '
                         f'{planning_clone_attribute_name} on object {str(item)}. Maybe add a setter? '
                         f'Original exception: {str(e)}')
                raise OptaPyException(error)
    return planning_clone


# ****************************************************************************
# PythonList Python helper functions
# ****************************************************************************
"""
    private static TriFunction<OpaquePythonReference, Integer, Integer, OpaquePythonReference> slicePythonList;
"""


def _clear_python_list(the_list: List):
    the_list.clear()


def _python_list_length(the_list: List):
    return len(the_list)


def _get_item_at_index_in_python_list(the_list: List, index: int):
    return the_list[index]


def _set_item_at_index_in_python_list(the_list: List, index: int, item: any):
    the_list[index] = item


def _add_item_to_python_list(the_list: List, item: any):
    the_list.append(item)
    return True


def _add_item_at_index_in_python_list(the_list: List, index: int, item: any):
    the_list.insert(index, item)
    return True


def _remove_item_from_python_list(the_list: List, item: any):
    try:
        the_list.remove(item)
        return True
    except ValueError:
        return False


def _remove_item_at_index_from_python_list(the_list: List, index: int):
    the_list.pop(index)
    return True


def _does_python_list_contain_item(the_list: List, item: any):
    return item in the_list


def _slice_python_list(the_list: List, start: int, end: int):
    return the_list[start:end]


def _compare_python_objects(a, b):
    from jpype import JInt
    if a < b:
        return JInt(-1)
    elif a > b:
        return JInt(1)
    else:
        return JInt(0)


def _equals_python_objects(a, b):
    return a == b


def _hash_python_object(obj):
    from jpype import JInt
    return JInt(hash(obj))


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
    jpype.startJVM(*args, classpath=path, convertStrings=True)
    import java.util.function.Function
    import java.util.function.BiFunction
    import org.optaplanner.core.api.function.TriFunction
    from org.optaplanner.optapy import PythonWrapperGenerator, PythonPlanningSolutionCloner, PythonList, \
        PythonComparable  # noqa
    PythonWrapperGenerator.setPythonObjectToId(JObject(PythonFunction(_get_python_object_id),
                                                       java.util.function.Function))
    PythonWrapperGenerator.setPythonObjectToString(JObject(PythonFunction(_get_python_object_str),
                                                           java.util.function.Function))
    PythonWrapperGenerator.setPythonGetJavaClass(JObject(PythonFunction(_get_python_object_java_class),
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

    PythonList.setClearPythonList(JObject(PythonFunction(_clear_python_list), java.util.function.Function))
    PythonList.setGetPythonListLength(JObject(PythonFunction(_python_list_length), java.util.function.Function))
    PythonList.setGetItemAtIndexInPythonList(JObject(PythonBiFunction(_get_item_at_index_in_python_list),
                                                     java.util.function.BiFunction))
    PythonList.setSetItemAtIndexInPythonList(JObject(PythonTriFunction(_set_item_at_index_in_python_list),
                                                     org.optaplanner.core.api.function.TriFunction))
    PythonList.setAddItemToPythonList(JObject(PythonBiFunction(_add_item_to_python_list),
                                              java.util.function.BiFunction))
    PythonList.setAddItemAtIndexInPythonList(JObject(PythonTriFunction(_add_item_at_index_in_python_list),
                                                     org.optaplanner.core.api.function.TriFunction))
    PythonList.setRemoveItemFromPythonList(JObject(PythonBiFunction(_remove_item_from_python_list),
                                                   java.util.function.BiFunction))
    PythonList.setRemoveItemAtIndexFromPythonList(JObject(PythonBiFunction(_remove_item_at_index_from_python_list),
                                                          java.util.function.BiFunction))
    PythonList.setDoesPythonListContainItem(JObject(PythonBiFunction(_does_python_list_contain_item),
                                                    java.util.function.BiFunction))
    PythonList.setSlicePythonList(JObject(PythonTriFunction(_slice_python_list),
                                          org.optaplanner.core.api.function.TriFunction))

    PythonComparable.setPythonObjectCompareTo(JObject(PythonBiFunction(_compare_python_objects),
                                                      java.util.function.BiFunction))
    PythonComparable.setPythonObjectEquals(JObject(PythonBiFunction(_equals_python_objects),
                                                   java.util.function.BiFunction))
    PythonComparable.setPythonObjectHash(JObject(PythonFunction(_hash_python_object),
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
        from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
        item = PythonWrapperGenerator.getPythonObject(self)
        return getattr(item, name)

    def __setattr__(self, key, value):
        from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
        item = PythonWrapperGenerator.getPythonObject(self)
        setattr(item, key, value)


def _add_shallow_copy_to_class(the_class: Type):
    """Adds a __copy__ method to a class, overriding it if it has one

    Java Objects cannot be pickled, thus the pickle default copy
    method does not work. The __copy__ method calls the __new__ method
    of the class with None passed for each of __init__ parameters. It then
    calls setattr for each variable in the original object on the clone.

    :param the_class: the class to add the deep copy method to.
    :return: None
    """
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

    def class_shallow_copy(self):
        clone = the_class.__new__(the_class, *positional_args, **keyword_args)  # noqa
        item_vars = vars(self)
        for attribute, value in item_vars.items():
            setattr(clone, attribute, value)
        return clone

    the_class.__copy__ = class_shallow_copy


def _setup_solver_run(solver_run_id, problem, solver_run_ref_set):
    solver_run_ref_set.add(problem)
    solver_run_id_to_refs[solver_run_id] = solver_run_ref_set
    if id(problem) in ref_id_to_solver_run_id:
        ref_id_to_solver_run_id[id(problem)].add(solver_run_id)
    else:
        ref_id_to_solver_run_id[id(problem)] = set()
        ref_id_to_solver_run_id[id(problem)].add(solver_run_id)


def _cleanup_solver_run(solver_run_id, problem, solver_run_ref_set):
    ref_id_to_solver_run_id[id(problem)].remove(solver_run_id)
    for ref in solver_run_ref_set:
        if len(ref_id_to_solver_run_id[id(ref)]) == 0:
            del ref_id_to_solver_run_id[id(ref)]
    del solver_run_id_to_refs[solver_run_id]


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


def _get_optaplanner_annotations(python_class: Type) -> List[Tuple[str, JClass, str, List[dict]]]:
    """Gets the methods with OptaPlanner annotations in the given class"""
    method_list = [attribute for attribute in dir(python_class) if callable(getattr(python_class, attribute)) and
                   attribute.startswith('__') is False]
    annotated_methods = []
    for method in method_list:
        optaplanner_annotations = [attribute for attribute in dir(getattr(python_class, method)) if
                                   attribute.startswith('__optaplanner')]
        if optaplanner_annotations:
            return_type = getattr(getattr(python_class, method), "__optapy_return", None)
            method_signature = getattr(getattr(python_class, method), "__optapy_signature", None)
            annotated_methods.append(
                _to_java_list([method, return_type, method_signature,
                               _to_java_list(list(map(lambda annotation: getattr(getattr(python_class, method),
                                                                                 annotation), optaplanner_annotations)))
                               ]))
    return _to_java_list(annotated_methods)


def get_class(python_class: Union[Type, Callable]) -> JClass:
    """Return the Java Class for the given Python Class"""
    from java.lang import Object
    if isinstance(python_class, jpype.JClass):
        return cast(JClass, python_class)
    if hasattr(python_class, '__optapy_java_class'):
        return python_class.__optapy_java_class
    if python_class == int:
        from java.lang import Integer
        return cast(JClass, Integer)
    if python_class == str:
        from java.lang import String
        return cast(JClass, String)
    if python_class == bool:
        from java.lang import Boolean
        return cast(JClass, Boolean)
    return cast(JClass, Object)


unique_class_id = 0
"""A unique identifier; used to guarantee the generated class java name is unique"""


def _does_class_define_eq_or_hashcode(python_class):
    return '__eq__' in python_class.__dict__ or '__hash__' in python_class.__dict__


def _generate_problem_fact_class(python_class):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    parent_class = None
    has_eq_and_hashcode = _does_class_define_eq_or_hashcode(python_class)
    if len(python_class.__bases__) == 1 and hasattr(python_class.__bases__[0], '__optapy_java_class'):
        parent_class = get_class(python_class.__bases__[0])

    out = PythonWrapperGenerator.defineProblemFactClass(python_class.__name__ + str(unique_class_id),
                                                        parent_class,
                                                        has_eq_and_hashcode,
                                                        optaplanner_annotations)
    unique_class_id = unique_class_id + 1
    return out


def _generate_planning_entity_class(python_class: Type, annotation_data: Dict[str, Any]):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    parent_class = None
    has_eq_and_hashcode = _does_class_define_eq_or_hashcode(python_class)
    if len(python_class.__bases__) == 1 and hasattr(python_class.__bases__[0], '__optapy_java_class'):
        parent_class = get_class(python_class.__bases__[0])
    out = PythonWrapperGenerator.definePlanningEntityClass(python_class.__name__ + str(unique_class_id),
                                                           parent_class,
                                                           has_eq_and_hashcode,
                                                           optaplanner_annotations,
                                                           _to_java_map(annotation_data))
    unique_class_id = unique_class_id + 1
    return out


def _generate_planning_solution_class(python_class: Type) -> JClass:
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    has_eq_and_hashcode = _does_class_define_eq_or_hashcode(python_class)
    out = PythonWrapperGenerator.definePlanningSolutionClass(python_class.__name__ + str(unique_class_id),
                                                             has_eq_and_hashcode,
                                                             optaplanner_annotations)
    unique_class_id = unique_class_id + 1
    return out


def _to_constraint_java_array(python_list: List['_Constraint']) -> JArray:
    # reimport since the one in global scope is only for type checking
    import org.optaplanner.core.api.score.stream.Constraint as ActualConstraintClass
    out = jpype.JArray(ActualConstraintClass)(len(python_list))
    for i in range(len(python_list)):
        out[i] = python_list[i]
    return out


def _generate_constraint_provider_class(constraint_provider: Callable[['_ConstraintFactory'], List['_Constraint']]) -> \
        JClass:
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    from org.optaplanner.core.api.score.stream import ConstraintProvider
    out = PythonWrapperGenerator.defineConstraintProviderClass(
        constraint_provider.__name__ + str(unique_class_id),
        JObject(ConstraintProviderFunction(lambda cf: _to_constraint_java_array(constraint_provider(cf))),
                ConstraintProvider))
    unique_class_id = unique_class_id + 1
    return out


def _generate_easy_score_calculator_class(easy_score_calculator: Callable[[Solution_], Score_]) -> JClass:
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    from org.optaplanner.core.api.score.calculator import EasyScoreCalculator

    @JImplements(EasyScoreCalculator)
    class EasyScoreCalculatorClass:
        def __init__(self, easy_score_calculator_impl):
            self.easy_score_calculator_impl = easy_score_calculator_impl

        @JOverride
        def calculateScore(self, solution):
            return self.easy_score_calculator_impl(solution)

    out = PythonWrapperGenerator.defineEasyScoreCalculatorClass(
        easy_score_calculator.__name__ + str(unique_class_id),
        EasyScoreCalculatorClass(easy_score_calculator))
    unique_class_id = unique_class_id + 1
    return out


def _generate_incremental_score_calculator_class(incremental_score_calculator: Type['_IncrementalScoreCalculator'],
                                                 constraint_match_aware: bool) -> \
        JClass:
    global unique_class_id
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    from org.optaplanner.core.api.score.calculator import IncrementalScoreCalculator
    from java.util.function import Supplier
    ensure_init()

    out = PythonWrapperGenerator.defineIncrementalScoreCalculatorClass(
        incremental_score_calculator.__name__ + str(unique_class_id),
        JObject(PythonSupplier(lambda: incremental_score_calculator()),
                Supplier), constraint_match_aware)
    unique_class_id = unique_class_id + 1
    return out
