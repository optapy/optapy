import builtins
import ctypes
import dis
import inspect
import sys
import abc
from typing import Union

from jpype import JInt, JLong, JDouble, JBoolean, JProxy, JClass, JArray

MINIMUM_SUPPORTED_PYTHON_VERSION = (3, 9)
MAXIMUM_SUPPORTED_PYTHON_VERSION = (3, 11)

global_dict_to_instance = dict()
global_dict_to_key_set = dict()
type_to_compiled_java_class = dict()
function_interface_pair_to_instance = dict()
function_interface_pair_to_class = dict()


def is_python_version_supported(python_version):
    python_version_major_minor = python_version[0:2]
    return MINIMUM_SUPPORTED_PYTHON_VERSION <= python_version_major_minor <= MAXIMUM_SUPPORTED_PYTHON_VERSION


def is_current_python_version_supported():
    return is_python_version_supported(sys.version_info)


def check_current_python_version_supported():
    if not is_current_python_version_supported():
        raise NotImplementedError(f'The translator does not support the current Python version ({sys.version}). '
                                  f'The minimum version currently supported is '
                                  f'{MINIMUM_SUPPORTED_PYTHON_VERSION[0]}.{MINIMUM_SUPPORTED_PYTHON_VERSION[1]}. '
                                  f'The maximum version currently supported is '
                                  f'{MAXIMUM_SUPPORTED_PYTHON_VERSION[0]}.{MAXIMUM_SUPPORTED_PYTHON_VERSION[1]}.')


def get_translated_java_system_error_message(error):
    from org.optaplanner.jpyinterpreter.util import TracebackUtils
    top_line = f'{error.getClass().getSimpleName()}:  {error.getMessage()}'
    traceback = TracebackUtils.getTraceback(error)
    return f'{top_line}\n{traceback}'


class TranslatedJavaSystemError(SystemError):
    def __init__(self, error):
        super().__init__(get_translated_java_system_error_message(error))


# Taken from https://stackoverflow.com/a/60953150
def is_native_module(module):
    """ is_native_module(thing) -> boolean predicate, True if `module`
        is a native-compiled ("extension") module.

        Q.v. this fine StackOverflow answer on this subject:
            https://stackoverflow.com/a/39304199/298171
    """
    import importlib.machinery
    import inspect

    QUALIFIER = '.'
    EXTENSION_SUFFIXES = tuple(suffix.lstrip(QUALIFIER)
                               for suffix
                               in importlib.machinery.EXTENSION_SUFFIXES)

    suffix = lambda filename: QUALIFIER in filename \
                              and filename.rpartition(QUALIFIER)[-1] \
                              or ''
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


def is_c_native(item):
    import importlib
    module = getattr(item, '__module__', '')

    # __main__ is a built-in module, according to Python (and this be seen as c-native). We can also compile builtins,
    # so return False, so we can compile them
    if module == '__main__' or \
            module == 'builtins' \
            or module == '':  # if we cannot find module, assume it is not native
        return False

    return is_native_module(importlib.import_module(module))


def init_type_to_compiled_java_class():
    from org.optaplanner.jpyinterpreter.builtins import GlobalBuiltins
    from org.optaplanner.jpyinterpreter.types import BuiltinTypes
    import org.optaplanner.jpyinterpreter.types.datetime as java_datetime_types
    import datetime
    import builtins

    if len(type_to_compiled_java_class) > 0:
        return

    check_current_python_version_supported()

    type_to_compiled_java_class[staticmethod] = BuiltinTypes.STATIC_FUNCTION_TYPE
    type_to_compiled_java_class[classmethod] = BuiltinTypes.CLASS_FUNCTION_TYPE

    type_to_compiled_java_class[int] = BuiltinTypes.INT_TYPE
    type_to_compiled_java_class[float] = BuiltinTypes.FLOAT_TYPE
    type_to_compiled_java_class[complex] = BuiltinTypes.COMPLEX_TYPE
    type_to_compiled_java_class[bool] = BuiltinTypes.BOOLEAN_TYPE

    type_to_compiled_java_class[type(None)] = BuiltinTypes.NONE_TYPE
    type_to_compiled_java_class[str] = BuiltinTypes.STRING_TYPE
    type_to_compiled_java_class[bytes] = BuiltinTypes.BYTES_TYPE
    type_to_compiled_java_class[bytearray] = BuiltinTypes.BYTE_ARRAY_TYPE
    type_to_compiled_java_class[object] = BuiltinTypes.BASE_TYPE

    type_to_compiled_java_class[list] = BuiltinTypes.LIST_TYPE
    type_to_compiled_java_class[tuple] = BuiltinTypes.TUPLE_TYPE
    type_to_compiled_java_class[set] = BuiltinTypes.SET_TYPE
    type_to_compiled_java_class[frozenset] = BuiltinTypes.FROZEN_SET_TYPE
    type_to_compiled_java_class[dict] = BuiltinTypes.DICT_TYPE

    type_to_compiled_java_class[datetime.datetime] = java_datetime_types.PythonDateTime.DATE_TIME_TYPE
    type_to_compiled_java_class[datetime.date] = java_datetime_types.PythonDate.DATE_TYPE
    type_to_compiled_java_class[datetime.time] = java_datetime_types.PythonTime.TIME_TYPE
    type_to_compiled_java_class[datetime.timedelta] = java_datetime_types.PythonTimeDelta.TIME_DELTA_TYPE

    # Type aliases
    type_to_compiled_java_class[any] = BuiltinTypes.BASE_TYPE
    type_to_compiled_java_class[type] = BuiltinTypes.TYPE_TYPE

    for java_type in GlobalBuiltins.getBuiltinTypes():
        try:
            type_to_compiled_java_class[getattr(builtins, java_type.getTypeName())] = java_type
        except AttributeError:
            # This version of python does not have this builtin type; pass
            pass


def copy_iterable(iterable):
    from java.util import ArrayList
    if iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in iterable:
        iterable_copy.add(item)
    return iterable_copy


def copy_variable_names(iterable):
    from java.util import ArrayList
    from org.optaplanner.jpyinterpreter.util import JavaIdentifierUtils

    if iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in iterable:
        iterable_copy.add(JavaIdentifierUtils.sanitizeFieldName(item))
    return iterable_copy


def remove_from_instance_map(instance_map, object_id):
    instance_map.remove(object_id)


def put_in_instance_map(instance_map, python_object, java_object):
    global objects_without_weakref_id_set
    instance_map.put(id(python_object), java_object)


class CodeWrapper:
    def __init__(self, wrapped):
        self.wrapped = wrapped

    def __getitem__(self, item):
        if item == 'wrapped':
            return self.wrapped
        else:
            raise KeyError(f'No item: {item}')


def convert_object_to_java_python_like_object(value, instance_map=None):
    import datetime
    from java.lang import Object
    from java.util import HashMap
    from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter
    from org.optaplanner.jpyinterpreter.types import PythonLikeType, AbstractPythonLikeObject, CPythonBackedPythonLikeObject
    from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference, CPythonType, JavaObjectWrapper, PythonLikeFunctionWrapper
    from org.optaplanner.jpyinterpreter.types.datetime import PythonDate, PythonDateTime, PythonTime, PythonTimeDelta

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
            from org.optaplanner.jpyinterpreter.types import PythonLikeFunction, PythonCode
            java_class = translate_python_code_to_java_class(value, PythonLikeFunction)
            out = PythonCode(java_class)
            put_in_instance_map(instance_map, value, out)
            return out
        except:
            from org.optaplanner.jpyinterpreter.types import PythonLikeFunction, PythonCode
            java_class = translate_python_code_to_python_wrapper_class(value)
            out = PythonCode(java_class)
            put_in_instance_map(instance_map, value, out)
            return out
    elif type(value) is object:
        java_type = type_to_compiled_java_class[type(value)]
        out = CPythonBackedPythonLikeObject(java_type)
        put_in_instance_map(instance_map, value, out)
        CPythonBackedPythonInterpreter.updateJavaObjectFromPythonObject(out,
                                                                        JProxy(OpaquePythonReference, inst=value,
                                                                               convert=True),
                                                                        instance_map)
        return out
    elif not inspect.isfunction(value) and type(value) in type_to_compiled_java_class:
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
            from org.optaplanner.jpyinterpreter.types import PythonLikeFunction
            wrapped = PythonLikeFunctionWrapper()
            put_in_instance_map(instance_map, value, wrapped)
            out = translate_python_bytecode_to_java_bytecode(value, PythonLikeFunction)
            wrapped.setWrapped(out)
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
    banned_modules = {'jpype', 'importlib', 'builtins'}
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
    from java.math import BigInteger
    from types import ModuleType
    from org.optaplanner.jpyinterpreter import PythonLikeObject, CPythonBackedPythonInterpreter
    from org.optaplanner.jpyinterpreter.types import PythonString, PythonBytes, PythonByteArray, PythonNone, \
        PythonModule, PythonSlice, PythonRange, NotImplemented as JavaNotImplemented
    from org.optaplanner.jpyinterpreter.types.collections import PythonLikeList, PythonLikeTuple, PythonLikeSet, \
        PythonLikeFrozenSet, PythonLikeDict
    from org.optaplanner.jpyinterpreter.types.numeric import PythonInteger, PythonFloat, PythonBoolean, PythonComplex
    from org.optaplanner.jpyinterpreter.types.wrappers import PythonObjectWrapper, CPythonType, OpaquePythonReference

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
    elif value is NotImplemented:
        return JavaNotImplemented.INSTANCE
    elif isinstance(value, bool):
        return PythonBoolean.valueOf(JBoolean(value))
    elif isinstance(value, int):
        out = PythonInteger.valueOf(BigInteger("{0:x}".format(value), 16))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, float):
        out = PythonFloat.valueOf(JDouble(value))
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
    elif isinstance(value, bytes):
        out = PythonBytes.fromIntTuple(convert_to_java_python_like_object(tuple(value)))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, bytearray):
        out = PythonByteArray.fromIntTuple(convert_to_java_python_like_object(tuple(value)))
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
    elif isinstance(value, frozenset):
        out = PythonLikeFrozenSet()
        put_in_instance_map(instance_map, value, out)
        for item in value:
            out.delegate.add(convert_to_java_python_like_object(item, instance_map))
        return out
    elif isinstance(value, dict):
        out = PythonLikeDict()
        put_in_instance_map(instance_map, value, out)
        for map_key, map_value in value.items():
            out.put(convert_to_java_python_like_object(map_key, instance_map),
                    convert_to_java_python_like_object(map_value, instance_map))
        return out
    elif isinstance(value, slice):
        out = PythonSlice(convert_to_java_python_like_object(value.start, instance_map),
                          convert_to_java_python_like_object(value.stop, instance_map),
                          convert_to_java_python_like_object(value.step, instance_map))
        put_in_instance_map(instance_map, value, out)
        return out
    elif isinstance(value, range):
        out = PythonRange(convert_to_java_python_like_object(value.start, instance_map),
                          convert_to_java_python_like_object(value.stop, instance_map),
                          convert_to_java_python_like_object(value.step, instance_map))
        put_in_instance_map(instance_map, value, out)
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
    from org.optaplanner.jpyinterpreter import PythonLikeObject
    from java.util import List, Map, Set, Iterator
    from org.optaplanner.jpyinterpreter.types import PythonString, PythonBytes, PythonByteArray, PythonNone, \
        PythonModule, PythonSlice, PythonRange, CPythonBackedPythonLikeObject, PythonLikeType, PythonLikeGenericType, \
        NotImplemented as JavaNotImplemented, PythonCell
    from org.optaplanner.jpyinterpreter.types.collections import PythonLikeList, PythonLikeTuple, PythonLikeSet, \
        PythonLikeFrozenSet, PythonLikeDict
    from org.optaplanner.jpyinterpreter.types.numeric import PythonInteger, PythonFloat, PythonBoolean, PythonComplex
    from org.optaplanner.jpyinterpreter.types.wrappers import JavaObjectWrapper, PythonObjectWrapper, CPythonType, \
        OpaquePythonReference
    from types import CellType

    if isinstance(python_like_object, (PythonObjectWrapper, JavaObjectWrapper)):
        out = python_like_object.getWrappedObject()
        return out
    elif isinstance(python_like_object, PythonNone):
        return None
    elif isinstance(python_like_object, JavaNotImplemented):
        return NotImplemented
    elif isinstance(python_like_object, PythonFloat):
        return float(python_like_object.getValue())
    elif isinstance(python_like_object, PythonString):
        return python_like_object.getValue()
    elif isinstance(python_like_object, PythonBytes):
        return bytes(unwrap_python_like_object(python_like_object.asIntTuple()))
    elif isinstance(python_like_object, PythonByteArray):
        return bytearray(unwrap_python_like_object(python_like_object.asIntTuple()))
    elif isinstance(python_like_object, PythonBoolean):
        return python_like_object == PythonBoolean.TRUE
    elif isinstance(python_like_object, PythonInteger):
        return int(python_like_object.getValue().toString(16), 16)
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

        if isinstance(python_like_object, PythonLikeFrozenSet):
            return frozenset(out)

        return out
    elif isinstance(python_like_object, Map):
        out = dict()
        for entry in python_like_object.entrySet():
            out[unwrap_python_like_object(entry.getKey(), default)] = unwrap_python_like_object(entry.getValue(),
                                                                                                default)
        return out
    elif isinstance(python_like_object, PythonSlice):
        return slice(unwrap_python_like_object(python_like_object.start),
                     unwrap_python_like_object(python_like_object.stop),
                     unwrap_python_like_object(python_like_object.step))
    elif isinstance(python_like_object, PythonRange):
        return range(unwrap_python_like_object(python_like_object.start),
                     unwrap_python_like_object(python_like_object.stop),
                     unwrap_python_like_object(python_like_object.step))
    elif isinstance(python_like_object, Iterator):
        class JavaIterator:
            def __init__(self, iterator):
                self.iterator = iterator

            def __iter__(self):
                return self

            def __next__(self):
                try:
                    if not self.iterator.hasNext():
                        raise StopIteration()
                    else:
                        return unwrap_python_like_object(self.iterator.next())
                except StopIteration:
                    raise
                except Exception as e:
                    raise unwrap_python_like_object(e)

            def send(self, sent):
                try:
                    return unwrap_python_like_object(self.iterator.send(convert_to_java_python_like_object(sent)))
                except Exception as e:
                    raise unwrap_python_like_object(e)

            def throw(self, thrown):
                try:
                    return unwrap_python_like_object(
                        self.iterator.throwValue(convert_to_java_python_like_object(thrown)))
                except Exception as e:
                    raise unwrap_python_like_object(e)

        return JavaIterator(python_like_object)
    elif isinstance(python_like_object, PythonCell):
        out = CellType()
        out.cell_contents = python_like_object.cellValue
        return out
    elif isinstance(python_like_object, PythonModule):
        return python_like_object.getPythonReference()
    elif isinstance(python_like_object, CPythonBackedPythonLikeObject):
        maybe_python_reference = getattr(python_like_object, '$cpythonReference')
        if maybe_python_reference is not None:
            return maybe_python_reference
        # does not have an existing python reference
        maybe_cpython_type = getattr(python_like_object, "$CPYTHON_TYPE")
        if isinstance(maybe_cpython_type, CPythonType):
            out = object.__new__(maybe_cpython_type.getPythonReference())
            setattr(python_like_object, '$cpythonReference', JProxy(OpaquePythonReference, inst=out, convert=True))
            setattr(python_like_object, '$cpythonId', PythonInteger.valueOf(JLong(id(out))))
            getattr(python_like_object, '$writeFieldsToCPythonReference')()
            return out
    elif isinstance(python_like_object, Exception):
        try:
            exception_name = getattr(python_like_object, '$TYPE').getTypeName()
            exception_python_type = getattr(builtins, exception_name)
            args = unwrap_python_like_object(getattr(python_like_object, '$getArgs')())
            return exception_python_type(*args)
        except AttributeError:
            return TranslatedJavaSystemError(python_like_object)
    elif isinstance(python_like_object, PythonLikeType):
        if python_like_object.getClass() == PythonLikeGenericType:
            return type

        for (key, value) in type_to_compiled_java_class.items():
            if value == python_like_object:
                return key
        else:
            raise KeyError(f'Cannot find corresponding Python type for Java class {python_like_object.getClass().getName()}')
    elif not isinstance(python_like_object, PythonLikeObject):
        return python_like_object
    else:
        out = unwrap_python_like_builtin_module_object(python_like_object)
        if out is not None:
            return out

        if default == NotImplementedError:
            raise NotImplementedError(f'Unable to convert object of type {type(python_like_object)}')
        return default


def unwrap_python_like_builtin_module_object(python_like_object):
    from org.optaplanner.jpyinterpreter.types.datetime import PythonDate, PythonTime, PythonDateTime, PythonTimeDelta
    import datetime

    if isinstance(python_like_object, PythonDateTime):
        return datetime.datetime(unwrap_python_like_object(python_like_object.year),
                                 unwrap_python_like_object(python_like_object.month),
                                 unwrap_python_like_object(python_like_object.day),
                                 unwrap_python_like_object(python_like_object.hour),
                                 unwrap_python_like_object(python_like_object.minute),
                                 unwrap_python_like_object(python_like_object.second),
                                 unwrap_python_like_object(python_like_object.microsecond),
                                 tzinfo=None,  # TODO: Support timezones
                                 fold=unwrap_python_like_object(python_like_object.fold))

    if isinstance(python_like_object, PythonDate):
        return datetime.date(unwrap_python_like_object(python_like_object.year),
                             unwrap_python_like_object(python_like_object.month),
                             unwrap_python_like_object(python_like_object.day))

    if isinstance(python_like_object, PythonTime):
        return datetime.time(unwrap_python_like_object(python_like_object.hour),
                             unwrap_python_like_object(python_like_object.minute),
                             unwrap_python_like_object(python_like_object.second),
                             unwrap_python_like_object(python_like_object.microsecond),
                             tzinfo=None,  # TODO: Support timezones
                             fold=unwrap_python_like_object(python_like_object.fold))

    if isinstance(python_like_object, PythonTimeDelta):
        return datetime.timedelta(unwrap_python_like_object(python_like_object.days),
                                  unwrap_python_like_object(python_like_object.seconds),
                                  unwrap_python_like_object(python_like_object.microseconds))

    return None

def get_java_type_for_python_type(the_type):
    from org.optaplanner.jpyinterpreter.types import PythonLikeType
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


def get_default_args(func):
    signature = inspect.signature(func)
    return {
        k: v.default
        for k, v in signature.parameters.items()
        if v.default is not inspect.Parameter.empty
    }


def copy_type_annotations(annotations_dict, default_args, vargs_name, kwargs_name):
    from java.util import HashMap
    from java.lang import Class as JavaClass
    from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference, JavaObjectWrapper, CPythonType # noqa

    global type_to_compiled_java_class

    out = HashMap()
    if annotations_dict is None or not isinstance(annotations_dict, dict):
        return out

    for name, value in annotations_dict.items():
        if not isinstance(name, str):
            continue
        if name == vargs_name:
            out.put(name, type_to_compiled_java_class[tuple])
            continue
        if name == kwargs_name:
            out.put(name, type_to_compiled_java_class[dict])
            continue
        if name in default_args:
            value = Union[value, type(default_args[name])]
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
    from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter
    if constants_iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in constants_iterable:
        iterable_copy.add(convert_to_java_python_like_object(item, CPythonBackedPythonInterpreter.pythonObjectIdToConvertedObjectMap))
    return iterable_copy


def copy_closure(closure):
    from org.optaplanner.jpyinterpreter.types import PythonCell
    from org.optaplanner.jpyinterpreter.types.collections import PythonLikeTuple
    from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter
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
    from org.optaplanner.jpyinterpreter import CPythonBackedPythonInterpreter

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


def find_globals_dict_for_java_map(java_globals):
    for python_global_id in global_dict_to_instance:
        if global_dict_to_instance[python_global_id] == java_globals:
            return ctypes.cast(python_global_id, ctypes.py_object).value

    raise ValueError(f'Could not find python globals corresponding to {str(java_globals.toString())}')


def get_instructions(python_function):
    try:
        yield from dis.get_instructions(python_function, show_caches=True)  # Python 3.11 and above
    except TypeError:  # Python 3.10 and below
        yield from dis.get_instructions(python_function)


# From https://github.com/python/cpython/blob/main/Objects/exception_handling_notes.txt
def parse_varint(iterator):
    b = next(iterator)
    val = b & 63
    while b&64:
        val <<= 6
        b = next(iterator)
        val |= b&63
    return val


# From https://github.com/python/cpython/blob/main/Objects/exception_handling_notes.txt
def parse_exception_table(code):
    iterator = iter(code.co_exceptiontable)
    try:
        while True:
            start = parse_varint(iterator)*2
            length = parse_varint(iterator)*2
            end = start + length - 2 # Present as inclusive, not exclusive
            target = parse_varint(iterator)*2
            dl = parse_varint(iterator)
            depth = dl >> 1
            lasti = bool(dl&1)
            yield start, end, target, depth, lasti
    except StopIteration:
        return


def get_python_exception_table(python_code):
    from org.optaplanner.jpyinterpreter import PythonExceptionTable, PythonVersion
    out = PythonExceptionTable()

    if hasattr(python_code, 'co_exceptiontable'):
        python_version = PythonVersion(sys.hexversion)
        for start, end, target, depth, lasti in parse_exception_table(python_code):
            out.addEntry(python_version, start, end, target, depth, lasti)

    return out


def get_function_bytecode_object(python_function):
    from java.util import ArrayList
    from org.optaplanner.jpyinterpreter import PythonBytecodeInstruction, PythonCompiledFunction, PythonVersion, OpcodeIdentifier # noqa

    init_type_to_compiled_java_class()

    python_compiled_function = PythonCompiledFunction()
    instruction_list = ArrayList()
    for instruction in get_instructions(python_function):
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
    python_compiled_function.co_exceptiontable = get_python_exception_table(python_function.__code__)
    python_compiled_function.co_names = copy_iterable(python_function.__code__.co_names)
    python_compiled_function.co_varnames = copy_variable_names(python_function.__code__.co_varnames)
    python_compiled_function.co_cellvars = copy_variable_names(python_function.__code__.co_cellvars)
    python_compiled_function.co_freevars = copy_variable_names(python_function.__code__.co_freevars)
    python_compiled_function.co_constants = copy_constants(python_function.__code__.co_consts)
    python_compiled_function.co_argcount = python_function.__code__.co_argcount
    python_compiled_function.co_kwonlyargcount = python_function.__code__.co_kwonlyargcount
    python_compiled_function.closure = copy_closure(python_function.__closure__)
    python_compiled_function.globalsMap = copy_globals(python_function.__globals__, python_function.__code__.co_names)
    python_compiled_function.typeAnnotations = copy_type_annotations(python_function.__annotations__,
                                                                     get_default_args(python_function),
                                                                     inspect.getfullargspec(python_function).varargs,
                                                                     inspect.getfullargspec(python_function).varkw)
    python_compiled_function.defaultPositionalArguments = convert_to_java_python_like_object(
        python_function.__defaults__ if python_function.__defaults__ else tuple())
    python_compiled_function.defaultKeywordArguments = convert_to_java_python_like_object(
        python_function.__kwdefaults__ if python_function.__kwdefaults__ else dict())
    python_compiled_function.supportExtraPositionalArgs = inspect.getfullargspec(python_function).varargs is not None
    python_compiled_function.supportExtraKeywordsArgs = inspect.getfullargspec(python_function).varkw is not None
    python_compiled_function.pythonVersion = PythonVersion(sys.hexversion)
    return python_compiled_function


def get_static_function_bytecode_object(the_class, python_function):
    return get_function_bytecode_object(python_function.__get__(the_class))


def get_code_bytecode_object(python_code):
    from java.util import ArrayList, HashMap
    from org.optaplanner.jpyinterpreter import PythonBytecodeInstruction, PythonCompiledFunction, PythonVersion, OpcodeIdentifier # noqa

    init_type_to_compiled_java_class()

    python_compiled_function = PythonCompiledFunction()
    instruction_list = ArrayList()
    for instruction in get_instructions(python_code):
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
    python_compiled_function.co_exceptiontable = get_python_exception_table(python_code)
    python_compiled_function.co_names = copy_iterable(python_code.co_names)
    python_compiled_function.co_varnames = copy_variable_names(python_code.co_varnames)
    python_compiled_function.co_cellvars = copy_variable_names(python_code.co_cellvars)
    python_compiled_function.co_freevars = copy_variable_names(python_code.co_freevars)
    python_compiled_function.co_constants = copy_constants(python_code.co_consts)
    python_compiled_function.co_argcount = python_code.co_argcount
    python_compiled_function.co_kwonlyargcount = python_code.co_kwonlyargcount
    python_compiled_function.closure = copy_closure(None)
    python_compiled_function.globalsMap = HashMap()
    python_compiled_function.typeAnnotations = HashMap()
    python_compiled_function.defaultPositionalArguments = convert_to_java_python_like_object(tuple())
    python_compiled_function.defaultKeywordArguments = convert_to_java_python_like_object(dict())
    python_compiled_function.typeAnnotations = HashMap()
    python_compiled_function.supportExtraPositionalArgs = False
    python_compiled_function.supportExtraKeywordsArgs = False
    python_compiled_function.pythonVersion = PythonVersion(sys.hexversion)
    return python_compiled_function


def translate_python_bytecode_to_java_bytecode(python_function, java_function_type, *type_args):
    from org.optaplanner.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    if (python_function, java_function_type, type_args) in function_interface_pair_to_instance:
        return function_interface_pair_to_instance[(python_function, java_function_type, type_args)]

    python_compiled_function = get_function_bytecode_object(python_function)

    if len(type_args) == 0:
        out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                             java_function_type)
        function_interface_pair_to_instance[(python_function, java_function_type, type_args)] = out
        return out
    else:
        out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                             java_function_type,
                                                                             copy_iterable(type_args))
        function_interface_pair_to_instance[(python_function, java_function_type, type_args)] = out
        return out


def _force_translate_python_bytecode_to_generator_java_bytecode(python_function, java_function_type):
    from org.optaplanner.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    if (python_function, java_function_type) in function_interface_pair_to_instance:
        return function_interface_pair_to_instance[(python_function, java_function_type)]

    python_compiled_function = get_function_bytecode_object(python_function)

    out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                         java_function_type)
    function_interface_pair_to_instance[(python_function, java_function_type)] = out
    return out


def translate_python_code_to_java_class(python_function, java_function_type, *type_args):
    from org.optaplanner.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    if (python_function, java_function_type, type_args) in function_interface_pair_to_class:
        return function_interface_pair_to_class[(python_function, java_function_type, type_args)]

    python_compiled_function = get_code_bytecode_object(python_function)

    if len(type_args) == 0:
        out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(python_compiled_function,
                                                                                    java_function_type)
        function_interface_pair_to_class[(python_function, java_function_type, type_args)] = out
        return out
    else:
        out = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(python_compiled_function,
                                                                                    java_function_type,
                                                                                    copy_iterable(type_args))
        function_interface_pair_to_class[(python_function, java_function_type, type_args)] = out
        return out


def translate_python_code_to_python_wrapper_class(python_function):
    from org.optaplanner.jpyinterpreter import PythonBytecodeToJavaBytecodeTranslator # noqa
    from org.optaplanner.jpyinterpreter.types.wrappers import OpaquePythonReference # noqa
    if (python_function,) in function_interface_pair_to_class:
        return function_interface_pair_to_class[(python_function,)]

    python_compiled_function = get_code_bytecode_object(python_function)
    out = PythonBytecodeToJavaBytecodeTranslator.\
        translatePythonBytecodeToPythonWrapperClass(python_compiled_function, JProxy(OpaquePythonReference,
                                                                                     CodeWrapper(python_function),
                                                                                     convert=True))
    function_interface_pair_to_class[(python_function,)] = out
    return out


def wrap_untyped_java_function(java_function):
    def wrapped_function(*args, **kwargs):
        from java.util import ArrayList, HashMap

        instance_map = HashMap()
        java_args = ArrayList(len(args))
        java_kwargs = HashMap()

        for arg in args:
            java_args.add(convert_to_java_python_like_object(arg, instance_map))

        for key, value in kwargs:
            java_kwargs.put(convert_to_java_python_like_object(key, instance_map),
                            convert_to_java_python_like_object(value, instance_map))

        try:
            return unwrap_python_like_object(getattr(java_function, '$call')(java_args, java_kwargs, None))
        except Exception as e:
            raise unwrap_python_like_object(e)

    return wrapped_function


def wrap_typed_java_function(java_function):
    def wrapped_function(*args):
        from java.util import ArrayList, HashMap

        instance_map = HashMap()
        java_args = [convert_to_java_python_like_object(arg, instance_map) for arg in args]

        try:
            return unwrap_python_like_object(java_function.invoke(*java_args))
        except Exception as e:
            raise unwrap_python_like_object(e)

    return wrapped_function


def as_java(python_function):
    return as_typed_java(python_function)


def as_untyped_java(python_function):
    from org.optaplanner.jpyinterpreter.types import PythonLikeFunction
    java_function = translate_python_bytecode_to_java_bytecode(python_function, PythonLikeFunction)
    return wrap_untyped_java_function(java_function)


def as_typed_java(python_function):
    from org.optaplanner.jpyinterpreter import PythonClassTranslator
    function_bytecode = get_function_bytecode_object(python_function)
    function_interface_declaration = PythonClassTranslator.getInterfaceForPythonFunction(function_bytecode)
    function_interface_class = PythonClassTranslator.getInterfaceClassForDeclaration(function_interface_declaration)
    java_function = translate_python_bytecode_to_java_bytecode(python_function, function_interface_class)
    return wrap_typed_java_function(java_function)


def _force_as_java_generator(python_function):
    from org.optaplanner.jpyinterpreter.types import PythonLikeFunction
    java_function = _force_translate_python_bytecode_to_generator_java_bytecode(python_function,
                                                                                PythonLikeFunction)
    return wrap_untyped_java_function(java_function)


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
    from org.optaplanner.jpyinterpreter import PythonCompiledClass, PythonClassTranslator, CPythonBackedPythonInterpreter # noqa
    from org.optaplanner.jpyinterpreter.types import BuiltinTypes
    from org.optaplanner.jpyinterpreter.types.wrappers import JavaObjectWrapper, OpaquePythonReference, CPythonType # noqa

    global type_to_compiled_java_class

    init_type_to_compiled_java_class()

    raw_type = erase_generic_args(python_class)
    if raw_type in type_to_compiled_java_class:
        return type_to_compiled_java_class[raw_type]

    if python_class == abc.ABC or inspect.isabstract(python_class):  # TODO: Implement a class for interfaces?
        python_class_java_type = BuiltinTypes.BASE_TYPE
        type_to_compiled_java_class[python_class] = python_class_java_type
        return python_class_java_type

    if hasattr(python_class, '__module__') and python_class.__module__ is not None and \
            is_banned_module(python_class.__module__):
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

    if is_c_native(python_class):
        python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
        type_to_compiled_java_class[python_class] = python_class_java_type
        return python_class_java_type

    type_to_compiled_java_class[python_class] = None
    methods = []
    for method_name in python_class.__dict__:
        method = inspect.getattr_static(python_class, method_name)
        if inspect.isfunction(method) or \
                isinstance(method, __STATIC_METHOD_TYPE) or \
                isinstance(method, __CLASS_METHOD_TYPE):
            methods.append((method_name, method))

    static_attributes = inspect.getmembers(python_class, predicate=lambda member: not (inspect.isfunction(member)
                                                                                       or isinstance(member, __STATIC_METHOD_TYPE)
                                                                                       or isinstance(member, __CLASS_METHOD_TYPE)))
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
                if isinstance(type_to_compiled_java_class[superclass], CPythonType):
                    python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
                    type_to_compiled_java_class[python_class] = python_class_java_type
                    return python_class_java_type
            except Exception:
                superclass_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=superclass, convert=True))
                type_to_compiled_java_class[superclass] = superclass_java_type
                python_class_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=python_class, convert=True))
                type_to_compiled_java_class[python_class] = python_class_java_type
                return python_class_java_type

    static_method_map = HashMap()
    for method in static_methods:
        static_method_map.put(method[0], get_static_function_bytecode_object(python_class, method[1]))

    class_method_map = HashMap()
    for method in class_methods:
        class_method_map.put(method[0], get_static_function_bytecode_object(python_class, method[1]))

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
        python_compiled_class.typeAnnotations = copy_type_annotations(python_class.__annotations__,
                                                                      dict(),
                                                                      None,
                                                                      None)
    else:
        python_compiled_class.typeAnnotations = copy_type_annotations(None,
                                                                      dict(),
                                                                      None,
                                                                      None)

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
