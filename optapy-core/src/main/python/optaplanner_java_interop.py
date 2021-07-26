import os
import tempfile
try:
    import importlib.resources as pkg_resources
except ImportError:
    # Try backported to PY<37 `importlib_resources`.
    import importlib_resources as pkg_resources

from . import jars
import jpype
import jpype.imports
from jpype.types import *
from jpype import JProxy, JImplements, JOverride, JImplementationFor
import copy

# Need to be global; directory is deleted when it out of scope
optaplanner_jars = tempfile.TemporaryDirectory()
def extract_optaplanner_jars():
    classpath_list = pkg_resources.read_text('optapy', 'classpath.txt').splitlines()
    classpath = []
    for jar in classpath_list:
        new_classpath_item = os.path.join(optaplanner_jars.name, jar)
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


def getPythonObjectFromId(item_id):
    return item_id


def __getPythonObjectAttribute(objectId, name):
    import java.lang.Object
    import org.optaplanner.core.api.score.Score
    the_object = objectId
    pythonObjectGetter = getattr(the_object, str(name))
    pythonObject = pythonObjectGetter()
    if pythonObject is None:
        return None
    elif isinstance(pythonObject, (str, int, float, complex, org.optaplanner.core.api.score.Score)):
        out = JObject(pythonObject, java.lang.Object)
        return out
    else:
        return JProxy(java.io.Serializable, inst=pythonObject, convert=True)


def getPythonArrayIdToIdArray(arrayId):
    import java.lang.Object
    import java.io.Serializable
    the_object = arrayId
    out = toList(list(map(lambda x: JProxy(java.io.Serializable, inst=x, convert=True), the_object)))
    return out


def setPythonObjectAttribute(objectId, name, value):
    from org.optaplanner.optapy import PythonObject
    the_object = objectId
    the_value = value
    if isinstance(the_value, PythonObject):
        the_value = value.get__optapy_Id()
    getattr(the_object, str(name))(the_value)


def deepClonePythonObject(the_object):
    import java.io.Serializable
    the_clone = the_object.__deepcopy__()
    my_refs.add(the_clone)
    return JProxy(java.io.Serializable, inst=the_clone, convert=True)


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
    PythonWrapperGenerator.setPythonArrayIdToIdArray(JObject(PythonFunction(getPythonArrayIdToIdArray), java.util.function.Function))
    PythonWrapperGenerator.setPythonObjectIdAndAttributeNameToValue(JObject(PythonBiFunction(__getPythonObjectAttribute), java.util.function.BiFunction))
    PythonWrapperGenerator.setPythonObjectIdAndAttributeSetter(JObject(PythonTriFunction(setPythonObjectAttribute), org.optaplanner.core.api.function.TriFunction))
    PythonPlanningSolutionCloner.setDeepClonePythonObject(JObject(PythonFunction(deepClonePythonObject), java.util.function.Function))


def ensure_init():
    if jpype.isJVMStarted():
        return
    else:
        init()

my_refs = set()
ref_map = dict()

@JImplements('java.io.Serializable', deferred=True)
class PythonRef:
    def __init__(self, ref):
        self.__dict__['ref'] = ref
        ref_map[id(ref)] = self

    def __getattr__(self, item):
        return getattr(self.__dict__['ref'], item)

    def __setattr__(self, key, value):
        setattr(self.__dict__['ref'], key, value)

@JImplementationFor('org.optaplanner.optapy.PythonObject')
class _PythonObject:
    def __jclass_init__(self):
        pass

    def __getattr__(self, name):
        from org.optaplanner.optapy import PythonWrapperGenerator
        item = PythonWrapperGenerator.getPythonObjectId(self)
        out = getattr(item, name)
        if id(out) in ref_map:
            return ref_map[id(out)]
        return PythonRef(out)

    def __setattr__(self, key, value):
        from org.optaplanner.optapy import PythonWrapperGenerator
        item = PythonWrapperGenerator.getPythonObjectId(self)
        setattr(item, key, value)

    def __copy__(self):
        from org.optaplanner.optapy import PythonWrapperGenerator
        item = PythonWrapperGenerator.getPythonObjectId(self)
        copied_item = copy.copy(item)
        return copied_item

    def __deepcopy__(self, memodict={}):
        from org.optaplanner.optapy import PythonWrapperGenerator
        import java.lang.Object
        item = PythonWrapperGenerator.getPythonObjectId(self)
        copied_item = copy.copy(item)
        for attribute, value in vars(copied_item).items():
            if not isinstance(value, java.lang.Object):
                vars(copied_item)[attribute] = copy.deepcopy(value, memodict)
        return copied_item

@JImplementationFor('org.optaplanner.core.config.solver.SolverConfig')
class SolverConfig:
    def __jclass_init__(self):
        pass

def solve(solverConfig, problem):
    from org.optaplanner.optapy import PythonSolver
    import java.io.Serializable
    return unwrap(PythonSolver.solve(solverConfig, JProxy(java.io.Serializable, inst=problem, convert=True)))

def unwrap(javaObject):
    return javaObject.get__optapy_Id()

def toMap(pythonDict):
    import java.lang.Object
    import java.util.HashMap
    out = java.util.HashMap()
    for key, value in pythonDict.items():
        if isinstance(value, list):
            out.put(JObject(key, java.lang.Object), toList(value).toArray())
        else:
            out.put(JObject(key, java.lang.Object), JObject(value, java.lang.Object))
    return out


def toList(pythonList):
    import java.lang.Object
    import java.util.ArrayList
    out = java.util.ArrayList()
    for item in pythonList:
        if isinstance(item, dict):
            out.add(toMap(item))
        else:
            out.add(JObject(item, java.lang.Object))
    return out

def getOptaPlannerAnnotations(pythonClass):
    method_list = [attribute for attribute in dir(pythonClass) if callable(getattr(pythonClass, attribute)) and attribute.startswith('__') is False]
    annotated_methods = []
    for method in method_list:
        optaplanner_annotations = [attribute for attribute in dir(getattr(pythonClass, method)) if attribute.startswith('__optaplanner')]
        if optaplanner_annotations:
            returnType = getattr(getattr(pythonClass, method), "__return", None)
            annotated_methods.append(
                toList([method, returnType,
                        toList(list(map(lambda annotation: getattr(getattr(pythonClass, method), annotation), optaplanner_annotations)))
                                       ]))
    return toList(annotated_methods)

def wrap(javaClass, pythonObject):
    from org.optaplanner.optapy import PythonWrapperGenerator
    return PythonWrapperGenerator.wrap(javaClass, id(pythonObject))

def getClass(pythonClass):
    return pythonClass.__javaClass

unique_class_id = 0
def generateProblemFactClass(pythonClass):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator
    optaplannerAnnotations = getOptaPlannerAnnotations(pythonClass)
    out = PythonWrapperGenerator.defineProblemFactClass(pythonClass.__name__ + str(unique_class_id), optaplannerAnnotations)
    unique_class_id = unique_class_id + 1
    return out

def generatePlanningEntityClass(pythonClass):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator
    optaplannerAnnotations = getOptaPlannerAnnotations(pythonClass)
    out = PythonWrapperGenerator.definePlanningEntityClass(pythonClass.__name__ + str(unique_class_id), optaplannerAnnotations)
    unique_class_id = unique_class_id + 1
    return out

def generatePlanningSolutionClass(pythonClass):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator
    optaplannerAnnotations = getOptaPlannerAnnotations(pythonClass)
    out = PythonWrapperGenerator.definePlanningSolutionClass(pythonClass.__name__ + str(unique_class_id), optaplannerAnnotations)
    unique_class_id = unique_class_id + 1
    return out

def _toConstraintArray(pythonList):
    import org.optaplanner.core.api.score.stream.Constraint as Constraint
    out = jpype.JArray(Constraint)(len(pythonList))
    for i in range(len(pythonList)):
        out[i] = pythonList[i]
    return out

def generateConstraintProviderClass(constraintProvider):
    global unique_class_id
    ensure_init()
    from org.optaplanner.optapy import PythonWrapperGenerator
    import java.util.function.Function
    out = PythonWrapperGenerator.defineConstraintProviderClass(constraintProvider.__name__ + str(unique_class_id), JObject(PythonFunction(lambda cf: _toConstraintArray(constraintProvider(cf))), java.util.function.Function))
    unique_class_id = unique_class_id + 1
    return out