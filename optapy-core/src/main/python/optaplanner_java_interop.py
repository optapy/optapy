import os
import tempfile
from . import jars
import jpype
import jpype.imports
from jpype.types import *
from jpype import JProxy, JImplements, JOverride, JImplementationFor
from inspect import signature, Parameter
import copy
try:
    import importlib.resources as pkg_resources
except ImportError:
    # Try backported to PY<37 `importlib_resources`.
    import importlib_resources as pkg_resources

# Need to be global; directory is deleted when it out of scope
_optaplanner_jars = tempfile.TemporaryDirectory()


def extract_optaplanner_jars():
    classpath_list = pkg_resources.read_text('optapy', 'classpath.txt').splitlines()
    classpath = []
    for jar in classpath_list:
        new_classpath_item = os.path.join(_optaplanner_jars.name, jar)
        jar_file = pkg_resources.read_binary(jars, jar)
        with open(new_classpath_item, 'wb') as temp_file:
            temp_file.write(jar_file)
        classpath.append(new_classpath_item)
    return classpath


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


def _get_python_object_id(item):
    return id(item)


def _get_python_object_str(item):
    return str(item)


def _get_python_object_from_id(item_id):
    return item_id


def _get_python_object_attribute(object_id, name):
    import java.lang.Object
    import org.optaplanner.core.api.score.Score
    the_object = object_id
    python_object_getter = getattr(the_object, str(name))
    python_object = python_object_getter()
    if python_object is None:
        return None
    elif isinstance(python_object, (str, int, float, complex, org.optaplanner.core.api.score.Score)):
        out = JObject(python_object, java.lang.Object)
        return out
    else:
        return JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=python_object, convert=True)


def _get_python_array_to_id_array(arrayId):
    import org.optaplanner.optapy.OpaquePythonReference
    the_object = arrayId
    out = _to_java_list(list(map(lambda x: JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=x, convert=True), the_object)))
    return out


def _set_python_object_attribute(object_id, name, value):
    from org.optaplanner.optapy import PythonObject
    the_object = object_id
    the_value = value
    if isinstance(the_value, PythonObject):
        the_value = value.get__optapy_Id()
    getattr(the_object, str(name))(the_value)


def _deep_clone_python_object(the_object):
    import org.optaplanner.optapy.OpaquePythonReference
    from org.optaplanner.optapy import PythonWrapperGenerator
    # ...Python evaluate default arg once, so if we don't set the memo arg to a new dictionary,
    # the same dictionary is reused!
    item = PythonWrapperGenerator.getPythonObject(the_object)
    the_clone = copy.deepcopy(item, memo={})
    for run_id in ref_id_to_solver_run_id[id(item)]:
        solver_run_id_to_refs[run_id].add(the_clone)
    ref_id_to_solver_run_id[id(the_clone)] = ref_id_to_solver_run_id[id(item)]
    return JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=the_clone, convert=True)


def init(*args, path=None, include_optaplanner_jars=True, log_level='INFO'):
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
    from org.optaplanner.optapy import PythonWrapperGenerator, PythonPlanningSolutionCloner
    PythonWrapperGenerator.setPythonObjectToId(JObject(PythonFunction(_get_python_object_id), java.util.function.Function))
    PythonWrapperGenerator.setPythonObjectToString(JObject(PythonFunction(_get_python_object_str), java.util.function.Function))
    PythonWrapperGenerator.setPythonArrayIdToIdArray(JObject(PythonFunction(_get_python_array_to_id_array), java.util.function.Function))
    PythonWrapperGenerator.setPythonObjectIdAndAttributeNameToValue(JObject(PythonBiFunction(_get_python_object_attribute), java.util.function.BiFunction))
    PythonWrapperGenerator.setPythonObjectIdAndAttributeSetter(JObject(PythonTriFunction(_set_python_object_attribute), org.optaplanner.core.api.function.TriFunction))
    PythonPlanningSolutionCloner.setDeepClonePythonObject(JObject(PythonFunction(_deep_clone_python_object), java.util.function.Function))


def ensure_init():
    if jpype.isJVMStarted():
        return
    else:
        init()


solver_run_id_to_refs = dict()
ref_id_to_solver_run_id = dict()

@JImplementationFor('org.optaplanner.optapy.PythonObject')
class _PythonObject:
    def __jclass_init__(self):
        pass

    def __getattr__(self, name):
        from org.optaplanner.optapy import PythonWrapperGenerator
        item = PythonWrapperGenerator.getPythonObject(self)
        return getattr(item, name)

    def __setattr__(self, key, value):
        from org.optaplanner.optapy import PythonWrapperGenerator
        item = PythonWrapperGenerator.getPythonObject(self)
        setattr(item, key, value)


def _add_deep_copy_to_class(the_class):
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
        clone = the_class.__new__(the_class, *positional_args, **keyword_args)
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


def solve(solverConfig, problem):
    from org.optaplanner.optapy import PythonSolver
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
    solution = _unwrap_java_object(PythonSolver.solve(solverConfig, JProxy(org.optaplanner.optapy.OpaquePythonReference, inst=problem, convert=True)))
    ref_id_to_solver_run_id[id(problem)].remove(solver_run_id)
    for ref in solver_run_ref_set:
        if len(ref_id_to_solver_run_id[id(ref)]) == 0:
            del ref_id_to_solver_run_id[id(ref)]
    del solver_run_id_to_refs[solver_run_id]
    return solution

def _unwrap_java_object(javaObject):
    return javaObject.get__optapy_Id()


def _to_java_map(pythonDict):
    import java.lang.Object
    import java.util.HashMap
    out = java.util.HashMap()
    for key, value in pythonDict.items():
        if isinstance(value, list):
            out.put(JObject(key, java.lang.Object), _to_java_list(value).toArray())
        else:
            out.put(JObject(key, java.lang.Object), JObject(value, java.lang.Object))
    return out


def _to_java_list(pythonList):
    import java.lang.Object
    import java.util.ArrayList
    out = java.util.ArrayList()
    for item in pythonList:
        if isinstance(item, dict):
            out.add(_to_java_map(item))
        else:
            out.add(JObject(item, java.lang.Object))
    return out


def _get_optaplanner_annotations(pythonClass):
    method_list = [attribute for attribute in dir(pythonClass) if callable(getattr(pythonClass, attribute)) and attribute.startswith('__') is False]
    annotated_methods = []
    for method in method_list:
        optaplanner_annotations = [attribute for attribute in dir(getattr(pythonClass, method)) if attribute.startswith('__optaplanner')]
        if optaplanner_annotations:
            returnType = getattr(getattr(pythonClass, method), "__return", None)
            annotated_methods.append(
                _to_java_list([method, returnType,
                               _to_java_list(list(map(lambda annotation: getattr(getattr(pythonClass, method), annotation), optaplanner_annotations)))
                               ]))
    return _to_java_list(annotated_methods)


def get_class(python_class):
    return python_class.__javaClass


unique_class_id = 0


def _generate_problem_fact_class(python_class):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    out = PythonWrapperGenerator.defineProblemFactClass(python_class.__name__ + str(unique_class_id), optaplanner_annotations)
    unique_class_id = unique_class_id + 1
    return out


def _generate_planning_entity_class(python_class):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    out = PythonWrapperGenerator.definePlanningEntityClass(python_class.__name__ + str(unique_class_id), optaplanner_annotations)
    unique_class_id = unique_class_id + 1
    return out


def _generate_planning_solution_class(python_class):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator
    optaplanner_annotations = _get_optaplanner_annotations(python_class)
    out = PythonWrapperGenerator.definePlanningSolutionClass(python_class.__name__ + str(unique_class_id), optaplanner_annotations)
    unique_class_id = unique_class_id + 1
    return out


def _to_constraint_java_array(python_list):
    import org.optaplanner.core.api.score.stream.Constraint as Constraint
    out = jpype.JArray(Constraint)(len(python_list))
    for i in range(len(python_list)):
        out[i] = python_list[i]
    return out


def _generate_constraint_provider_class(constraint_provider):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator
    import java.util.function.Function
    out = PythonWrapperGenerator.defineConstraintProviderClass(constraint_provider.__name__ + str(unique_class_id), JObject(PythonFunction(lambda cf: _to_constraint_java_array(constraint_provider(cf))), java.util.function.Function))
    unique_class_id = unique_class_id + 1
    return out