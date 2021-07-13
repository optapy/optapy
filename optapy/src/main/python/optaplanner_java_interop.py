import os
import sys
import tempfile
try:
    import importlib.resources as pkg_resources
except ImportError:
    # Try backported to PY<37 `importlib_resources`.
    import importlib_resources as pkg_resources

from . import jars
import java

optaplanner_jars = tempfile.TemporaryDirectory()
classpath_list = text = pkg_resources.read_text('optapy', 'classpath.txt').splitlines()
for classpath in classpath_list:
    new_classpath_item = os.path.join(optaplanner_jars.name, classpath)
    jar_file = pkg_resources.read_binary(jars, classpath)
    with open(new_classpath_item, 'wb') as temp_file:
        temp_file.write(jar_file)
    java.add_to_classpath(new_classpath_item)

PythonWrapperGenerator = java.type("org.optaplanner.optapy.PythonWrapperGenerator")

# Exported types
SolverConfig = java.type("org.optaplanner.core.config.solver.SolverConfig")
PythonSolver = java.type("org.optaplanner.optapy.PythonSolver")

def getOptaPlannerAnnotations(pythonClass):
    method_list = [attribute for attribute in dir(pythonClass) if callable(getattr(pythonClass, attribute)) and attribute.startswith('__') is False]
    annotated_methods = []
    for method in method_list:
        optaplanner_annotations = [attribute for attribute in dir(getattr(pythonClass, method)) if attribute.startswith('__optaplanner')]
        if optaplanner_annotations:
            returnType = getattr(pythonClass, method).__annotations__.get("return")
            if returnType is None:
                returnType = getattr(getattr(pythonClass, method), "__return", None)
            if not isinstance(returnType, type(PythonWrapperGenerator)):
                returnType = None
            annotated_methods = annotated_methods + [((method, returnType, list(map(lambda annotation: getattr(getattr(pythonClass, method), annotation), optaplanner_annotations))))]
    return annotated_methods

def wrap(javaClass, pythonObject):
    return PythonWrapperGenerator.wrap(javaClass, pythonObject)

def getClass(pythonClass):
    return pythonClass.__javaClass

def generateProblemFactClass(pythonClass):
    optaplannerAnnotations = getOptaPlannerAnnotations(pythonClass)
    out = PythonWrapperGenerator.defineProblemFactClass(pythonClass.__name__, optaplannerAnnotations)
    return out

def generatePlanningEntityClass(pythonClass):
    optaplannerAnnotations = getOptaPlannerAnnotations(pythonClass)
    out = PythonWrapperGenerator.definePlanningEntityClass(pythonClass.__name__, optaplannerAnnotations)
    return out

def generatePlanningSolutionClass(pythonClass):
    optaplannerAnnotations = getOptaPlannerAnnotations(pythonClass)
    out = PythonWrapperGenerator.definePlanningSolutionClass(pythonClass.__name__, optaplannerAnnotations)
    return out

def generateConstraintProviderClass(constraintProvider):
    out = PythonWrapperGenerator.defineConstraintProviderClass(constraintProvider.__name__, constraintProvider)
    return out