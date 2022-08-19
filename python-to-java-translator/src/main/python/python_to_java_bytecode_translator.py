import builtins
import ctypes
import dis
import inspect
import sys

from jpype import JInt, JLong, JFloat, JBoolean, JProxy, JClass, JArray

global_dict_to_instance = dict()
global_dict_to_key_set = dict()
type_to_compiled_java_class = dict()


# Taken from https://stackoverflow.com/a/60953150
def is_c_native(item):
    import importlib
    import importlib.machinery
    import inspect

    QUALIFIER = '.'
    EXTENSION_SUFFIXES = tuple(suffix.lstrip(QUALIFIER)
                               for suffix
                               in importlib.machinery.EXTENSION_SUFFIXES)
    moduleof = lambda thing: getattr(thing, '__module__', '')
    suffix = lambda filename: QUALIFIER in filename \
                              and filename.rpartition(QUALIFIER)[-1] \
                              or ''

    def isnativemodule(module):
        """ isnativemodule(thing) -> boolean predicate, True if `module`
            is a native-compiled ("extension") module.

            Q.v. this fine StackOverflow answer on this subject:
                https://stackoverflow.com/a/39304199/298171
        """
        # Step one: modules only beyond this point:
        if not inspect.ismodule(module):
            return False

        # Step two: return truly when “__loader__” is set:
        if isinstance(getattr(module, '__loader__', None),
                      importlib.machinery.ExtensionFileLoader):
            return True

        # Step three: in leu of either of those indicators,
        # check the module path’s file suffix:
        try:
            ext = suffix(inspect.getfile(module))
        except TypeError as exc:
            return 'is a built-in' in str(exc)

        return ext in EXTENSION_SUFFIXES

    module = moduleof(item)
    if module == 'builtins':
        return False
    return isnativemodule(
        importlib.import_module(
            module))


def init_type_to_compiled_java_class():
    import org.optaplanner.python.translator.types as java_types
    import org.optaplanner.python.translator.types.errors as java_errors_types
    import org.optaplanner.python.translator.types.datetime as java_datetime_types
    import datetime

    if len(type_to_compiled_java_class) > 0:
        return

    type_to_compiled_java_class[int] = java_types.PythonInteger.INT_TYPE
    type_to_compiled_java_class[float] = java_types.PythonFloat.FLOAT_TYPE
    type_to_compiled_java_class[complex] = java_types.PythonComplex.COMPLEX_TYPE
    type_to_compiled_java_class[bool] = java_types.PythonBoolean.BOOLEAN_TYPE

    type_to_compiled_java_class[type(None)] = java_types.PythonNone.NONE_TYPE
    type_to_compiled_java_class[str] = java_types.PythonString.STRING_TYPE
    type_to_compiled_java_class[object] = java_types.PythonLikeType.getBaseType()
    type_to_compiled_java_class[any] = java_types.PythonLikeType.getBaseType()

    type_to_compiled_java_class[list] = java_types.PythonLikeList.LIST_TYPE
    type_to_compiled_java_class[tuple] = java_types.PythonLikeTuple.TUPLE_TYPE
    type_to_compiled_java_class[set] = java_types.PythonLikeSet.SET_TYPE
    type_to_compiled_java_class[dict] = java_types.PythonLikeDict.DICT_TYPE

    type_to_compiled_java_class[BaseException] = java_errors_types.PythonBaseException.BASE_EXCEPTION_TYPE
    type_to_compiled_java_class[Exception] = java_errors_types.PythonException.EXCEPTION_TYPE
    type_to_compiled_java_class[StopIteration] = java_errors_types.StopIteration.STOP_ITERATION_TYPE
    type_to_compiled_java_class[AttributeError] = java_errors_types.AttributeError.ATTRIBUTE_ERROR_TYPE

    type_to_compiled_java_class[datetime.datetime] = java_datetime_types.PythonDateTime.DATE_TIME_TYPE
    type_to_compiled_java_class[datetime.date] = java_datetime_types.PythonDate.DATE_TYPE
    type_to_compiled_java_class[datetime.time] = java_datetime_types.PythonTime.TIME_TYPE
    type_to_compiled_java_class[datetime.timedelta] = java_datetime_types.PythonTimeDelta.TIME_DELTA_TYPE


def copy_iterable(iterable):
    from java.util import ArrayList
    if iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in iterable:
        iterable_copy.add(item)
    return iterable_copy


def remove_from_instance_map(instance_map, object_id):
    instance_map.remove(object_id)


def put_in_instance_map(instance_map, python_object, java_object):
    global objects_without_weakref_id_set
    instance_map.put(id(python_object), java_object)


def convert_object_to_java_python_like_object(value, instance_map=None):
    import datetime
    from java.lang import Object
    from java.util import HashMap
    from org.optaplanner.python.translator import CPythonBackedPythonInterpreter
    from org.optaplanner.python.translator.types import OpaquePythonReference, PythonLikeType, CPythonType, JavaObjectWrapper, AbstractPythonLikeObject, CPythonBackedPythonLikeObject
    from org.optaplanner.python.translator.types.datetime import PythonDate, PythonDateTime, PythonTime, PythonTimeDelta

    if instance_map is None:
        instance_map = HashMap()

    if isinstance(value, Object):
        out = JavaObjectWrapper(value)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, datetime.datetime):
        out = PythonDateTime.of(value.year, value.month, value.day, value.hour, value.minute, value.second,
                                value.microsecond, value.tzname(), value.fold)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, datetime.date):
        out = PythonDate.of(value.year, value.month, value.day)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, datetime.time):
        out = PythonTime.of(value.hour, value.minute, value.second, value.microsecond, value.tzname(), value.fold)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, datetime.timedelta):
        out = PythonTimeDelta.of(value.days, value.seconds, value.microseconds)
        put_in_instance_map(instance_map, value, out)
        return out
    elif inspect.iscode(value):
        try:
            from org.optaplanner.python.translator.types import PythonLikeFunction, PythonCode
            java_class = translate_python_code_to_java_class(value, PythonLikeFunction)
            out = PythonCode(java_class)
            put_in_instance_map(instance_map, value, out)
            return out
        except:
            return None
    elif type(value) is object:
        java_type = type_to_compiled_java_class[type(value)]
        out = CPythonBackedPythonLikeObject(java_type)
        put_in_instance_map(instance_map, value, out)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        JProxy(OpaquePythonReference, inst=value,
                                                                               convert=True),
                                                                        instance_map)
        return out
    elif type(value) in type_to_compiled_java_class:
        if type_to_compiled_java_class[type(value)] is None:
            return None
        java_type = type_to_compiled_java_class[type(value)]
        if isinstance(java_type, CPythonType):
            return None
        java_class = java_type.getJavaClass()
        out = java_class.getConstructor(PythonLikeType).newInstance(java_type)
        put_in_instance_map(instance_map, value, out)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        JProxy(OpaquePythonReference, inst=value,
                                                                               convert=True),
                                                                        instance_map)

        if isinstance(out, AbstractPythonLikeObject):
            for (key, value) in getattr(value, '__dict__', dict()).items():
                out.setAttribute(key, convert_to_java_python_like_object(value, instance_map))

        return out
    elif inspect.isbuiltin(value) or is_c_native(value):
        return None
    elif inspect.isfunction(value):
        try:
            from org.optaplanner.python.translator.types import PythonLikeFunction
            out = translate_python_bytecode_to_java_bytecode(value, PythonLikeFunction)
            put_in_instance_map(instance_map, value, out)
            return out
        except:
            return None
    else:
        try:
            java_type = translate_python_class_to_java_class(type(value))
            if isinstance(java_type, CPythonType):
                return None
            java_class = java_type.getJavaClass()
            out = java_class.getConstructor(PythonLikeType).newInstance(java_type)
            put_in_instance_map(instance_map, value, out)
            CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                            JProxy(OpaquePythonReference, inst=value,
                                                                                   convert=True),
                                                                            instance_map)

            if isinstance(out, AbstractPythonLikeObject):
                for (key, value) in getattr(value, '__dict__', dict()).items():
                    out.setAttribute(key, convert_to_java_python_like_object(value, instance_map))

            return out
        except:
            return None


def is_banned_module(module: str):
    banned_modules = {'jpype', 'importlib', 'pytest'}
    for banned_module in banned_modules:
        if module == banned_module:
            return True
        elif module == f'_{banned_module}':
            return True
        elif module.startswith(f'{banned_module}.'):
            return True
        elif module.startswith(f'_{banned_module}.'):
            return True
    return False


def convert_to_java_python_like_object(value, instance_map=None):
    from java.util import HashMap
    from types import ModuleType
    from org.optaplanner.python.translator import PythonLikeObject, CPythonBackedPythonInterpreter
    from org.optaplanner.python.translator.types import PythonInteger, PythonFloat, PythonBoolean, PythonString, \
        PythonComplex, PythonLikeList, PythonLikeTuple, PythonLikeSet, PythonLikeDict, PythonNone, PythonObjectWrapper, CPythonType, \
        OpaquePythonReference, PythonModule

    global type_to_compiled_java_class

    if instance_map is None:
        instance_map = HashMap()

    if instance_map.containsKey(JLong(id(value))):
        return instance_map.get(JLong(id(value)))
    elif isinstance(value, PythonLikeObject):
        put_in_instance_map(instance_map, value, value)
        return value
    elif value is None:
        return PythonNone.INSTANCE
    elif isinstance(value, bool):
        return PythonBoolean.valueOf(JBoolean(value))
    elif isinstance(value, int):
        out = PythonInteger.valueOf(JInt(value))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, float):
        out = PythonFloat.valueOf(JFloat(value))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, complex):
        out = PythonComplex.valueOf(convert_to_java_python_like_object(value.real, instance_map),
                                    convert_to_java_python_like_object(value.imag, instance_map))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, str):
        out = PythonString.valueOf(value)
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, tuple):
        out = PythonLikeTuple()
        put_in_instance_map(instance_map, value, out)
        for item in value:
            out.add(convert_to_java_python_like_object(item, instance_map))
        return out
    elif isinstance(value, list):
        out = PythonLikeList()
        put_in_instance_map(instance_map, value, out)
        for item in value:
            out.add(convert_to_java_python_like_object(item, instance_map))
        return out
    elif isinstance(value, set):
        out = PythonLikeSet()
        put_in_instance_map(instance_map, value, out)
        for item in value:
            out.add(convert_to_java_python_like_object(item, instance_map))
        return out
    elif isinstance(value, dict):
        out = PythonLikeDict()
        put_in_instance_map(instance_map, value, out)
        for map_key, map_value in value.items():
            out.put(convert_to_java_python_like_object(map_key, instance_map),
                    convert_to_java_python_like_object(map_value, instance_map))
        return out
    elif isinstance(value, type):
        raw_type = erase_generic_args(value)
        if raw_type in type_to_compiled_java_class:
            if type_to_compiled_java_class[raw_type] is None:
                return None
            out = type_to_compiled_java_class[raw_type]
            put_in_instance_map(instance_map, value, out)
            return out
        else:
            out = translate_python_class_to_java_class(raw_type)
            put_in_instance_map(instance_map, value, out)
            return out
    elif isinstance(value, ModuleType) and repr(value).startswith('<module \'') and not \
            is_banned_module(value.__name__):  # should not convert java modules
        out = PythonModule(instance_map)
        out.setPythonReference(JProxy(OpaquePythonReference, inst=value, convert=True))
        put_in_instance_map(instance_map, value, out)
        # Module is populated lazily
        return out
    else:
        out = convert_object_to_java_python_like_object(value, instance_map)
        if out is not None:
            return out
        proxy = JProxy(OpaquePythonReference, inst=value, convert=True)
        out = PythonObjectWrapper(proxy)
        put_in_instance_map(instance_map, value, out)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        proxy,
                                                                        instance_map)
        return out


def unwrap_python_like_object(python_like_object, default=NotImplementedError):
    from org.optaplanner.python.translator import PythonLikeObject
    from java.util import List, Map, Set, Iterator
    from org.optaplanner.python.translator.types import PythonInteger, PythonFloat, PythonComplex, PythonBoolean, \
        PythonString, PythonLikeList, PythonLikeTuple, PythonLikeSet, PythonLikeDict, PythonNone, PythonModule, \
        PythonObjectWrapper, JavaObjectWrapper, CPythonBackedPythonLikeObject, PythonLikeType, PythonLikeGenericType

    if isinstance(python_like_object, (PythonObjectWrapper, JavaObjectWrapper)):
        out = python_like_object.getWrappedObject()
        return out
    elif isinstance(python_like_object, PythonNone):
        return None
    elif isinstance(python_like_object, PythonFloat):
        return float(python_like_object.getValue())
    elif isinstance(python_like_object, PythonString):
        return python_like_object.getValue()
    elif isinstance(python_like_object, PythonBoolean):
        return python_like_object == PythonBoolean.TRUE
    elif isinstance(python_like_object, PythonInteger):
        return int(python_like_object.getValue().longValue())
    elif isinstance(python_like_object, PythonComplex):
        real = unwrap_python_like_object(python_like_object.getReal())
        imaginary = unwrap_python_like_object(python_like_object.getImaginary())
        return complex(real, imaginary)
    elif isinstance(python_like_object, (PythonLikeTuple, tuple)):
        out = []
        for item in python_like_object:
            out.append(unwrap_python_like_object(item, default))
        return tuple(out)
    elif isinstance(python_like_object, List):
        out = []
        for item in python_like_object:
            out.append(unwrap_python_like_object(item, default))
        return out
    elif isinstance(python_like_object, Set):
        out = set()
        for item in python_like_object:
            out.add(unwrap_python_like_object(item, default))
        return out
    elif isinstance(python_like_object, Map):
        out = dict()
        for entry in python_like_object.entrySet():
            out[unwrap_python_like_object(entry.getKey(), default)] = unwrap_python_like_object(entry.getValue(),
                                                                                                default)
        return out
    elif isinstance(python_like_object, Iterator):
        class JavaIterator:
            def __init__(self, iterator):
                self.iterator = iterator

            def __iter__(self):
                return self
            def __next__(self):
                if not self.iterator.hasNext():
                    raise StopIteration()
                else:
                    return unwrap_python_like_object(self.iterator.next())

        return JavaIterator(python_like_object)
    elif isinstance(python_like_object, PythonModule):
        return python_like_object.getPythonReference()
    elif isinstance(python_like_object, CPythonBackedPythonLikeObject) and getattr(python_like_object, '$cpythonReference') is not None:
        return getattr(python_like_object, '$cpythonReference')
    elif isinstance(python_like_object, Exception):
        exception_name = getattr(python_like_object, '$TYPE').getTypeName()
        exception_python_type = getattr(builtins, exception_name)
        message = python_like_object.getMessage()
        return exception_python_type(message)
    elif isinstance(python_like_object, PythonLikeType):
        if python_like_object.getClass() == PythonLikeGenericType:
            return type

        for (key, value) in type_to_compiled_java_class.items():
            if value == python_like_object:
                return key
        else:
            raise KeyError(f'Cannot find corresponding Python type for Java class {python_like_object.getClass().getName()}')
    elif hasattr(python_like_object, 'get__optapy_Id'):
        return python_like_object.get__optapy_Id()
    elif not isinstance(python_like_object, PythonLikeObject):
        return python_like_object
    else:
        if default == NotImplementedError:
            raise NotImplementedError(f'Unable to convert object of type {type(python_like_object)}')
        return default


def get_java_type_for_python_type(the_type):
    from org.optaplanner.python.translator.types import PythonLikeType
    global type_to_compiled_java_class

    if isinstance(the_type, type):
        the_type = erase_generic_args(the_type)
        if the_type in type_to_compiled_java_class:
            return type_to_compiled_java_class[the_type]
        else:
            try:
                return translate_python_class_to_java_class(the_type)
            except:
                return type_to_compiled_java_class[the_type]
    if isinstance(the_type, str):
        try:
            the_type = erase_generic_args(the_type)
            maybe_type = globals()[the_type]
            if isinstance(maybe_type, type):
                return get_java_type_for_python_type(maybe_type)
            return PythonLikeType.getBaseType()
        except:
            return PythonLikeType.getBaseType()
    # return base type, since users could use something like 1
    return PythonLikeType.getBaseType()


def copy_type_annotations(annotations_dict):
    from java.util import HashMap
    from java.lang import Class as JavaClass
    from org.optaplanner.python.translator.types import OpaquePythonReference, JavaObjectWrapper, CPythonType # noqa

    global type_to_compiled_java_class

    out = HashMap()
    if annotations_dict is None or not isinstance(annotations_dict, dict):
        return out

    for name, value in annotations_dict.items():
        if not isinstance(name, str):
            continue
        if value in type_to_compiled_java_class:
            out.put(name, type_to_compiled_java_class[value])
        elif isinstance(value, (JClass, JavaClass)):
            java_type = JavaObjectWrapper.getPythonTypeForClass(value)
            type_to_compiled_java_class[value] = java_type
            out.put(name, java_type)
        elif isinstance(value, (type, str)):
            out.put(name, get_java_type_for_python_type(value))
    return out


def copy_constants(constants_iterable):
    from java.util import ArrayList
    from org.optaplanner.python.translator import CPythonBackedPythonInterpreter
    if constants_iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in constants_iterable:
        iterable_copy.add(convert_to_java_python_like_object(item, CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap))
    return iterable_copy


def copy_closure(closure):
    from org.optaplanner.python.translator.types import PythonLikeTuple, PythonCell
    from org.optaplanner.python.translator import CPythonBackedPythonInterpreter
    out = PythonLikeTuple()
    if closure is None:
        return out
    else:
        for cell in closure:
            java_cell = PythonCell()
            java_cell.cellValue = convert_to_java_python_like_object(cell.cell_contents, CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap)
            out.add(java_cell)
        return out


def copy_globals(globals_dict, co_names):
    global global_dict_to_instance
    global global_dict_to_key_set
    from java.util import HashMap
    from org.optaplanner.python.translator import CPythonBackedPythonInterpreter

    globals_dict_key = id(globals_dict)
    if globals_dict_key in global_dict_to_instance:
        out = global_dict_to_instance[globals_dict_key]
        key_set = global_dict_to_key_set[globals_dict_key]
    else:
        out = HashMap()
        key_set = set()
        global_dict_to_instance[globals_dict_key] = out
        global_dict_to_key_set[globals_dict_key] = key_set

    instance_map = CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap
    for key, value in globals_dict.items():
        if key not in key_set and key in co_names:
            key_set.add(key)
            out.put(key, convert_to_java_python_like_object(value, instance_map))

    return out


def get_function_bytecode_object(python_function):
    from java.util import ArrayList
    from org.optaplanner.python.translator import PythonBytecodeInstruction, PythonCompiledFunction, PythonVersion, OpcodeIdentifier # noqa

    init_type_to_compiled_java_class()

    python_compiled_function = PythonCompiledFunction()
    instruction_list = ArrayList()
    for instruction in dis.get_instructions(python_function):
        java_instruction = PythonBytecodeInstruction()
        java_instruction.opcode = OpcodeIdentifier.valueOf(instruction.opname)
        java_instruction.opname = instruction.opname
        java_instruction.offset = JInt(instruction.offset // 2)

        if instruction.arg is not None:
            java_instruction.arg = JInt(instruction.arg)
        else:
            java_instruction.arg = JInt(0)

        if java_instruction.startsLine is not None:
            java_instruction.startsLine = JInt(instruction.starts_line)

        java_instruction.isJumpTarget = JBoolean(instruction.is_jump_target)

        instruction_list.add(java_instruction)

    python_compiled_function.module = python_function.__module__
    python_compiled_function.qualifiedName = python_function.__qualname__
    python_compiled_function.instructionList = instruction_list
    python_compiled_function.co_names = copy_iterable(python_function.__code__.co_names)
    python_compiled_function.co_varnames = copy_iterable(python_function.__code__.co_varnames)
    python_compiled_function.co_cellvars = copy_iterable(python_function.__code__.co_cellvars)
    python_compiled_function.co_freevars = copy_iterable(python_function.__code__.co_freevars)
    python_compiled_function.co_constants = copy_constants(python_function.__code__.co_consts)
    python_compiled_function.co_argcount = python_function.__code__.co_argcount
    python_compiled_function.co_kwonlyargcount = python_function.__code__.co_kwonlyargcount
    python_compiled_function.closure = copy_closure(python_function.__closure__)
    python_compiled_function.globalsMap = copy_globals(python_function.__globals__, python_function.__code__.co_names)
    python_compiled_function.typeAnnotations = copy_type_annotations(python_function.__annotations__)
    python_compiled_function.pythonVersion = PythonVersion(sys.hexversion)
    return python_compiled_function


def get_code_bytecode_object(python_code):
    from java.util import ArrayList, HashMap
    from org.optaplanner.python.translator import PythonBytecodeInstruction, PythonCompiledFunction, PythonVersion, OpcodeIdentifier # noqa

    init_type_to_compiled_java_class()

    python_compiled_function = PythonCompiledFunction()
    instruction_list = ArrayList()
    for instruction in dis.get_instructions(python_code):
        java_instruction = PythonBytecodeInstruction()
        java_instruction.opcode = OpcodeIdentifier.valueOf(instruction.opname)
        java_instruction.opname = instruction.opname
        java_instruction.offset = JInt(instruction.offset // 2)

        if instruction.arg is not None:
            java_instruction.arg = JInt(instruction.arg)
        else:
            java_instruction.arg = JInt(0)

        if java_instruction.startsLine is not None:
            java_instruction.startsLine = JInt(instruction.starts_line)

        java_instruction.isJumpTarget = JBoolean(instruction.is_jump_target)

        instruction_list.add(java_instruction)

    python_compiled_function.module = '__code__'
    python_compiled_function.qualifiedName = '__code__'
    python_compiled_function.instructionList = instruction_list
    python_compiled_function.co_names = copy_iterable(python_code.co_names)
    python_compiled_function.co_varnames = copy_iterable(python_code.co_varnames)
    python_compiled_function.co_cellvars = copy_iterable(python_code.co_cellvars)
    python_compiled_function.co_freevars = copy_iterable(python_code.co_freevars)
    python_compiled_function.co_constants = copy_constants(python_code.co_consts)
    python_compiled_function.co_argcount = python_code.co_argcount
    python_compiled_function.co_kwonlyargcount = python_code.co_kwonlyargcount
    python_compiled_function.closure = copy_closure(None)
    python_compiled_function.globalsMap = HashMap()
    python_compiled_function.typeAnnotations = HashMap()
    python_compiled_function.pythonVersion = PythonVersion(sys.hexversion)
    return python_compiled_function


def translate_python_bytecode_to_java_bytecode(python_function, java_function_type, *type_args):
    from org.optaplanner.python.translator import PythonBytecodeToJavaBytecodeTranslator # noqa
    python_compiled_function = get_function_bytecode_object(python_function)

    if len(type_args) == 0:
        return PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                              java_function_type)
    else:
        return PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                              java_function_type,
                                                                              copy_iterable(type_args))


def translate_python_code_to_java_class(python_function, java_function_type, *type_args):
    from org.optaplanner.python.translator import PythonBytecodeToJavaBytecodeTranslator # noqa
    python_compiled_function = get_code_bytecode_object(python_function)

    if len(type_args) == 0:
        return PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(python_compiled_function,
                                                                                     java_function_type)
    else:
        return PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(python_compiled_function,
                                                                                     java_function_type,
                                                                                     copy_iterable(type_args))


def wrap_java_function(java_function):
    def wrapped_function(*args, **kwargs):
        from org.optaplanner.python.translator.types import PythonLikeFunction
        from java.util import ArrayList, HashMap

        instance_map = HashMap()
        java_args = None
        java_kwargs = None
        if isinstance(java_function, PythonLikeFunction):
            java_args = ArrayList(len(args))
            java_kwargs = HashMap()

            for arg in args:
                java_args.add(convert_to_java_python_like_object(arg, instance_map))

            for key, value in kwargs:
                java_kwargs.put(convert_to_java_python_like_object(key, instance_map),
                                convert_to_java_python_like_object(value, instance_map))
        else:
            java_args = [convert_to_java_python_like_object(arg, instance_map) for arg in args]
            java_kwargs = {
                convert_to_java_python_like_object(key, instance_map):
                convert_to_java_python_like_object(value, instance_map)
                for key, value in kwargs
            }

        try:
            return unwrap_python_like_object(getattr(java_function, '$call')(java_args, java_kwargs))
        except Exception as e:
            raise unwrap_python_like_object(e)

    return wrapped_function


def as_java(python_function):
    from org.optaplanner.python.translator.types import PythonLikeFunction
    java_function = translate_python_bytecode_to_java_bytecode(python_function, PythonLikeFunction)
    return wrap_java_function(java_function)


class MethodTypeHelper:
    @classmethod
    def class_method_type(cls):
        pass

    @staticmethod
    def static_method_type():
        pass


__CLASS_METHOD_TYPE = type(MethodTypeHelper.__dict__['class_method_type'])
__STATIC_METHOD_TYPE = type(MethodTypeHelper.__dict__['static_method_type'])


def force_update_type(python_type, java_type):
    global type_to_compiled_java_class
    type_to_compiled_java_class[python_type] = java_type


def erase_generic_args(python_type):
    from typing import get_origin
    if isinstance(python_type, type):
        out = python_type
        if get_origin(out) is not None:
            return get_origin(out)
        return out
    elif isinstance(python_type, str):
        try:
            generics_start = python_type.index('[')
            return python_type[generics_start:-2]
        except ValueError:
            return python_type
    else:
        raise ValueError


def translate_python_class_to_java_class(python_class):
    from java.lang import Class as JavaClass
    from java.util import ArrayList, HashMap
    from org.optaplanner.python.translator import PythonCompiledClass, PythonClassTranslator, CPythonBackedPythonInterpreter # noqa
    from org.optaplanner.python.translator.types import JavaObjectWrapper, OpaquePythonReference, CPythonType # noqa

    global type_to_compiled_java_class

    init_type_to_compiled_java_class()

    raw_type = erase_generic_args(python_class)
    if raw_type in type_to_compiled_java_class:
        return type_to_compiled_java_class[raw_type]

    if hasattr(python_class, '__module__') and python_class.__module__ is not None \
            and is_banned_module(python_class.__module__):
        python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
        type_to_compiled_java_class[python_class] = python_class_java_type
        return python_class_java_type

    if isinstance(python_class, JArray):
        python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
        type_to_compiled_java_class[python_class] = python_class_java_type
        return python_class_java_type

    if isinstance(python_class, (JClass, JavaClass)):
        try:
            out = JavaObjectWrapper.getPythonTypeForClass(python_class)
            type_to_compiled_java_class[python_class] = out
            return out
        except TypeError:
            print(f'Bad type: {type(python_class)}, from {python_class}')
            python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
            type_to_compiled_java_class[python_class] = python_class_java_type
            return python_class_java_type

    type_to_compiled_java_class[python_class] = None
    methods = []
    for method_name in python_class.__dict__:
        method = inspect.getattr_static(python_class, method_name)
        if inspect.isfunction(method):
            methods.append((method_name, method))

    static_attributes = inspect.getmembers(python_class, predicate=lambda member: not inspect.isfunction(member))
    static_attributes = [attribute for attribute in static_attributes if attribute[0] in python_class.__dict__]
    static_methods = [method for method in methods if isinstance(method[1], __STATIC_METHOD_TYPE)]
    class_methods = [method for method in methods if isinstance(method[1], __CLASS_METHOD_TYPE)]
    instance_methods = [method for method in methods if method not in static_methods and method not in class_methods]

    superclass_list = ArrayList()
    for superclass in python_class.__bases__:
        superclass = erase_generic_args(superclass)
        if superclass in type_to_compiled_java_class:
            if isinstance(type_to_compiled_java_class[superclass], CPythonType):
                python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
                type_to_compiled_java_class[python_class] = python_class_java_type
                return python_class_java_type
            superclass_list.add(type_to_compiled_java_class[superclass])
        else:
            try:
                superclass_list.add(translate_python_class_to_java_class(superclass))
            except Exception:
                superclass_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=superclass, convert=True))
                type_to_compiled_java_class[superclass] = superclass_java_type
                python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
                type_to_compiled_java_class[python_class] = python_class_java_type
                return python_class_java_type

    static_method_map = HashMap()
    for method in static_methods:
        static_method_map.put(method[0], get_function_bytecode_object(method[1]))

    class_method_map = HashMap()
    for method in class_methods:
        class_method_map.put(method[0], get_function_bytecode_object(method[1]))

    instance_method_map = HashMap()
    for method in instance_methods:
        instance_method_map.put(method[0], get_function_bytecode_object(method[1]))

    static_attributes_map = HashMap()
    static_attributes_to_class_instance_map = HashMap()
    for attribute in static_attributes:
        attribute_type = type(attribute[1])
        if attribute_type == python_class:
            static_attributes_to_class_instance_map.put(attribute[0],
                                                        JProxy(OpaquePythonReference,
                                                               inst=attribute[1], convert=True))
        else:
            if attribute_type not in type_to_compiled_java_class:
                try:
                    translate_python_class_to_java_class(attribute_type)
                except:
                    superclass_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=attribute_type, convert=True))
                    type_to_compiled_java_class[attribute_type] = superclass_java_type

            static_attributes_map.put(attribute[0], convert_to_java_python_like_object(attribute[1]))


    python_compiled_class = PythonCompiledClass()
    python_compiled_class.binaryType = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class,
                                                                  convert=True))
    python_compiled_class.module = python_class.__module__
    python_compiled_class.qualifiedName = python_class.__qualname__
    python_compiled_class.className = python_class.__name__
    if hasattr(python_class, '__annotations__'):
        python_compiled_class.typeAnnotations = copy_type_annotations(python_class.__annotations__)
    else:
        python_compiled_class.typeAnnotations = copy_type_annotations(None)

    python_compiled_class.superclassList = superclass_list
    python_compiled_class.instanceFunctionNameToPythonBytecode = instance_method_map
    python_compiled_class.staticFunctionNameToPythonBytecode = static_method_map
    python_compiled_class.classFunctionNameToPythonBytecode = class_method_map
    python_compiled_class.staticAttributeNameToObject = static_attributes_map
    python_compiled_class.staticAttributeNameToClassInstance = static_attributes_to_class_instance_map

    out = PythonClassTranslator.translatePythonClass(python_compiled_class)
    type_to_compiled_java_class[python_class] = out
    PythonClassTranslator.setSelfStaticInstances(python_compiled_class, out.getJavaClass(), out,
                                                 CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap)
    return out
