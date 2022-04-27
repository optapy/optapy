import pathlib
import jpype
import jpype.imports
import importlib.metadata
from typing import List


def extract_python_translator_jars() -> list[str]:
    """Extracts and return a list of the Python Translator Java dependencies

    Invoking this function extracts Python Translator Dependencies from the javapython.jars module
    into a temporary directory and returns a list contains classpath entries for
    those dependencies. The temporary directory exists for the entire execution of the
    program.

    :return: None
    """
    return [str(p.locate()) for p in importlib.metadata.files('javapython') if p.name.endswith('.jar')]


def init(*args, path: List[str] = None, include_translator_jars: bool = True):
    """Start the JVM. Throws a RuntimeError if it is already started.

    :param args: JVM args.
    :param path: If not None, a list of dependencies to use as the classpath. Default to None.
    :param include_translator_jars: If True, add translators jars to path. Default to True.
    :return: None
    """
    if jpype.isJVMStarted():  # noqa
        raise RuntimeError('JVM already started.')
    if path is None:
        include_optaplanner_jars = True
        path = []
    if include_translator_jars:
        path = path + extract_python_translator_jars()
    jpype.startJVM(*args, classpath=path, convertStrings=True)  # noqa


def ensure_init():
    """Start the JVM if it isn't started; does nothing otherwise

    Used by OptaPy to start the JVM when needed by a method, so
    users don't need to start the JVM themselves.

    :return: None
    """
    if jpype.isJVMStarted(): # noqa
        return
    else:
        init()


def set_class_output_directory(path: pathlib.Path):
    ensure_init()

    from org.optaplanner.python.translator import PythonBytecodeToJavaBytecodeTranslator # noqa
    PythonBytecodeToJavaBytecodeTranslator.classOutputRootPath = path
