import pathlib
import jpype
import jpype.imports
import importlib.metadata
from typing import List


def extract_python_translator_jars() -> list[str]:
    """Extracts and return a list of the Python Translator Java dependencies

    Invoking this function extracts Python Translator Dependencies from the jpyinterpreter.jars module
    into a temporary directory and returns a list contains classpath entries for
    those dependencies. The temporary directory exists for the entire execution of the
    program.

    :return: None
    """
    return [str(p.locate()) for p in importlib.metadata.files('jpyinterpreter') if p.name.endswith('.jar')]


def init(*args, path: List[str] = None, include_translator_jars: bool = True,
         class_output_path: pathlib.Path = None):
    """Start the JVM. Throws a RuntimeError if it is already started.

    :param args: JVM args.
    :param path: If not None, a list of dependencies to use as the classpath. Default to None.
    :param include_translator_jars: If True, add translators jars to path. Default to True.
    :param class_output_path: If not None, sets the generated class output path. If None, no class
                              files are written. Can be changed by set_class_output_directory
    :return: None
    """
    if jpype.isJVMStarted():  # noqa
        raise RuntimeError('JVM already started.')
    if path is None:
        include_translator_jars = True
        path = []
    if include_translator_jars:
        path = path + extract_python_translator_jars()
    jpype.startJVM(*args, classpath=path, convertStrings=True)  # noqa

    if class_output_path is not None:
        from org.optaplanner.jpyinterpreter import InterpreterStartupOptions # noqa
        InterpreterStartupOptions.classOutputRootPath = class_output_path

    from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter
    CPythonBackedPythonInterpreter.lookupPythonReferenceIdPythonFunction = GetPythonObjectId()
    CPythonBackedPythonInterpreter.lookupPythonReferenceTypePythonFunction = GetPythonObjectType()
    CPythonBackedPythonInterpreter.lookupAttributeOnPythonReferencePythonFunction = GetAttributeOnPythonObject()
    CPythonBackedPythonInterpreter.lookupPointerForAttributeOnPythonReferencePythonFunction = \
        GetAttributePointerOnPythonObject()
    CPythonBackedPythonInterpreter.lookupPointerArrayForAttributeOnPythonReferencePythonFunction = \
        GetAttributePointerArrayOnPythonObject()
    CPythonBackedPythonInterpreter.lookupAttributeOnPythonReferenceWithMapPythonFunction = \
        GetAttributeOnPythonObjectWithMap()
    CPythonBackedPythonInterpreter.lookupDictOnPythonReferencePythonFunction = GetDictOnPythonObject()
    CPythonBackedPythonInterpreter.setAttributeOnPythonReferencePythonFunction = SetAttributeOnPythonObject()
    CPythonBackedPythonInterpreter.deleteAttributeOnPythonReferencePythonFunction = DeleteAttributeOnPythonObject()
    CPythonBackedPythonInterpreter.callPythonFunction = CallPythonFunction()
    CPythonBackedPythonInterpreter.createFunctionFromCodeFunction = CreateFunctionFromCode()
    CPythonBackedPythonInterpreter.importModuleFunction = ImportModule()


@jpype.JImplements('java.util.function.Function', deferred=True)
class GetPythonObjectId:
    @jpype.JOverride()
    def apply(self, python_object):
        return id(python_object)


@jpype.JImplements('java.util.function.Function', deferred=True)
class GetPythonObjectType:
    @jpype.JOverride()
    def apply(self, python_object):
        from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference
        return jpype.JProxy(OpaquePythonReference, inst=type(python_object), convert=True)


@jpype.JImplements('java.util.function.BiFunction', deferred=True)
class GetAttributeOnPythonObject:
    @jpype.JOverride()
    def apply(self, python_object, attribute_name):
        from .python_to_java_bytecode_translator import convert_to_java_python_like_object
        if not hasattr(python_object, attribute_name):
            return None
        out = getattr(python_object, attribute_name)
        return convert_to_java_python_like_object(out)


@jpype.JImplements('java.util.function.BiFunction', deferred=True)
class GetAttributePointerOnPythonObject:
    @jpype.JOverride()
    def apply(self, python_object, attribute_name):
        from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference
        if not hasattr(python_object, attribute_name):
            return None
        out = getattr(python_object, attribute_name)
        return jpype.JProxy(OpaquePythonReference, inst=out, convert=True)


@jpype.JImplements('java.util.function.BiFunction', deferred=True)
class GetAttributePointerArrayOnPythonObject:
    @jpype.JOverride()
    def apply(self, python_object, attribute_name):
        from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference
        if not hasattr(python_object, attribute_name):
            return None
        out = getattr(python_object, attribute_name)()
        out_array = OpaquePythonReference[len(out)]

        for i in range(len(out)):
            out_array[i] = jpype.JProxy(OpaquePythonReference, inst=out[i], convert=True)

        return out_array


@jpype.JImplements('org.optaplanner.jpyinterpreter.util.function.TriFunction', deferred=True)
class GetAttributeOnPythonObjectWithMap:
    @jpype.JOverride()
    def apply(self, python_object, attribute_name, instance_map):
        from .python_to_java_bytecode_translator import convert_to_java_python_like_object
        if not hasattr(python_object, attribute_name):
            return None
        out = getattr(python_object, attribute_name)
        try:
            return convert_to_java_python_like_object(out, instance_map)
        except Exception as e:
            import traceback
            traceback.print_exception(e)
            raise e


@jpype.JImplements('org.optaplanner.jpyinterpreter.util.function.TriConsumer', deferred=True)
class SetAttributeOnPythonObject:
    @jpype.JOverride()
    def accept(self, python_object, attribute_name, value):
        from .python_to_java_bytecode_translator import unwrap_python_like_object
        setattr(python_object, attribute_name, unwrap_python_like_object(value))


@jpype.JImplements('java.util.function.BiConsumer', deferred=True)
class DeleteAttributeOnPythonObject:
    @jpype.JOverride()
    def accept(self, python_object, attribute_name):
        delattr(python_object, attribute_name)


@jpype.JImplements('java.util.function.BiFunction', deferred=True)
class GetDictOnPythonObject:
    @jpype.JOverride()
    def apply(self, python_object, instance_map):
        from java.util import HashMap
        from .python_to_java_bytecode_translator import convert_to_java_python_like_object

        out = HashMap()
        for key in dir(python_object):
            out.put(key, convert_to_java_python_like_object(getattr(python_object, key), instance_map))

        return out


@jpype.JImplements('org.optaplanner.jpyinterpreter.util.function.TriFunction', deferred=True)
class CallPythonFunction:
    @jpype.JOverride()
    def apply(self, python_object, var_args_list, keyword_args_map):
        from .python_to_java_bytecode_translator import unwrap_python_like_object, convert_to_java_python_like_object
        actual_vargs = unwrap_python_like_object(var_args_list)
        actual_keyword_args = unwrap_python_like_object(keyword_args_map)
        if actual_keyword_args is None:
            actual_keyword_args = dict()
        try:
            out = python_object(*actual_vargs, **actual_keyword_args)
            return convert_to_java_python_like_object(out)
        except Exception as e:
            from org.optaplanner.jpyinterpreter.types.errors import CPythonException
            print(e)
            raise CPythonException(str(e))


@jpype.JImplements('org.optaplanner.jpyinterpreter.util.function.QuadFunction', deferred=True)
class CreateFunctionFromCode:
    @jpype.JOverride()
    def apply(self, code_object, function_globals, closure, name):
        from types import FunctionType
        from .python_to_java_bytecode_translator import unwrap_python_like_object, find_globals_dict_for_java_map
        from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter  # noqa
        from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference, PythonObjectWrapper  # noqa
        from java.util import HashMap
        from jpype import JProxy

        instance_map = HashMap()
        python_code = JProxy.unwrap(code_object).wrapped
        python_globals_args = find_globals_dict_for_java_map(function_globals)
        python_closure = unwrap_python_like_object(closure)
        python_name = unwrap_python_like_object(name)

        python_function = FunctionType(code=python_code,
                                       globals=python_globals_args,
                                       name=python_name,
                                       closure=python_closure)

        proxy = JProxy(OpaquePythonReference, inst=python_function, convert=True)
        out = PythonObjectWrapper(proxy)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        proxy,
                                                                        instance_map)
        return out



@jpype.JImplements('org.optaplanner.jpyinterpreter.util.function.PentaFunction', deferred=True)
class ImportModule:
    @jpype.JOverride()
    def apply(self, module_name, globals_map, locals_map, from_list, level):
        from .python_to_java_bytecode_translator import unwrap_python_like_object, convert_to_java_python_like_object
        from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter  # noqa
        python_globals = unwrap_python_like_object(globals_map, None)
        python_locals = unwrap_python_like_object(locals_map, None)
        python_from_list = unwrap_python_like_object(from_list, None)
        return convert_to_java_python_like_object(
            __import__(module_name, python_globals, python_locals, python_from_list, level),
            CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap
        )


def ensure_init():
    """Start the JVM if it isn't started; does nothing otherwise

    Used by the translator to start the JVM when needed by a method, so
    users don't need to start the JVM themselves.

    :return: None
    """
    if jpype.isJVMStarted(): # noqa
        return
    else:
        init()


def set_class_output_directory(path: pathlib.Path):
    ensure_init()

    from org.optaplanner.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    PythonBytecodeToJavaBytecodeTranslator.classOutputRootPath = path
