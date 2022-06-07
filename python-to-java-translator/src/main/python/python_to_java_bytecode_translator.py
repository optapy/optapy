import dis

import inspect
from jpype import JInt, JLong, JFloat, JBoolean, JProxy

global_dict_to_instance = dict()
type_to_compiled_java_class = dict()


def init_type_to_compiled_java_class():
    import org.optaplanner.python.translator.types as java_types
    import org.optaplanner.python.translator.types.errors as java_errors_types

    if len(type_to_compiled_java_class) > 0:
        return

    type_to_compiled_java_class[int] = java_types.PythonInteger.INT_TYPE
    type_to_compiled_java_class[float] = java_types.PythonFloat.FLOAT_TYPE
    type_to_compiled_java_class[bool] = java_types.PythonBoolean.BOOLEAN_TYPE

    type_to_compiled_java_class[type(None)] = java_types.PythonNone.NONE_TYPE
    type_to_compiled_java_class[str] = java_types.PythonString.STRING_TYPE
    type_to_compiled_java_class[object] = java_types.PythonLikeType.getBaseType()

    type_to_compiled_java_class[list] = java_types.PythonLikeList.LIST_TYPE
    type_to_compiled_java_class[tuple] = java_types.PythonLikeTuple.TUPLE_TYPE
    type_to_compiled_java_class[set] = java_types.PythonLikeSet.SET_TYPE
    type_to_compiled_java_class[dict] = java_types.PythonLikeDict.DICT_TYPE

    type_to_compiled_java_class[BaseException] = java_errors_types.PythonBaseException.BASE_EXCEPTION_TYPE
    type_to_compiled_java_class[Exception] = java_errors_types.PythonException.EXCEPTION_TYPE
    type_to_compiled_java_class[StopIteration] = java_errors_types.StopIteration.STOP_ITERATION_TYPE
    type_to_compiled_java_class[AttributeError] = java_errors_types.AttributeError.ATTRIBUTE_ERROR_TYPE




def copy_iterable(iterable):
    from java.util import ArrayList
    if iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in iterable:
        iterable_copy.add(item)
    return iterable_copy


def convert_to_java_python_like_object(value):
    from org.optaplanner.python.translator import PythonLikeObject
    from org.optaplanner.python.translator.types import PythonInteger, PythonFloat, PythonBoolean, PythonString, \
        PythonLikeList, PythonLikeTuple, PythonLikeSet, PythonLikeDict, PythonNone, PythonObjectWrapper, CPythonType, \
        OpaquePythonReference, OptaPyObjectReference

    global type_to_compiled_java_class

    if isinstance(value, PythonLikeObject):
        return value
    elif value is None:
        return PythonNone.INSTANCE
    elif isinstance(value, bool):
        return PythonBoolean.valueOf(JBoolean(value))
    elif isinstance(value, int):
        return PythonInteger.valueOf(JInt(value))
    elif isinstance(value, float):
        return PythonFloat.valueOf(JFloat(value))
    elif isinstance(value, str):
        return PythonString.valueOf(value)
    elif isinstance(value, tuple):
        out = PythonLikeTuple()
        for item in value:
            out.add(convert_to_java_python_like_object(item))
        return out
    elif isinstance(value, list):
        out = PythonLikeList()
        for item in value:
            out.add(convert_to_java_python_like_object(item))
        return out
    elif isinstance(value, set):
        out = PythonLikeSet()
        for item in value:
            out.add(convert_to_java_python_like_object(item))
        return out
    elif isinstance(value, dict):
        out = PythonLikeDict()
        for map_key, map_value in value.items():
            out.put(convert_to_java_python_like_object(map_key),
                    convert_to_java_python_like_object(map_value))
        return out
    elif hasattr(value, '__optapy_java_class'):
        return OptaPyObjectReference(JLong(id(value)))
    elif isinstance(value, type):
        if value in type_to_compiled_java_class:
            return type_to_compiled_java_class[value]
        else:
            return CPythonType.getType(JProxy(OpaquePythonReference, inst=value, convert=True))
    else:
        return PythonObjectWrapper(JProxy(OpaquePythonReference, inst=value, convert=True))
    # TODO: Get compiled class corresponding to function / class bytecode


def unwrap_python_like_object(python_like_object):
    from org.optaplanner.python.translator import PythonLikeObject
    from java.util import List, Map, Set
    from org.optaplanner.python.translator.types import PythonInteger, PythonFloat, PythonBoolean, PythonString, \
        PythonLikeList, PythonLikeTuple, PythonLikeSet, PythonLikeDict, PythonNone, PythonObjectWrapper, \
        JavaObjectWrapper

    if isinstance(python_like_object, (PythonObjectWrapper, JavaObjectWrapper)):
        out = python_like_object.getWrappedObject()
        return out
    elif python_like_object is PythonNone.INSTANCE:
        return None
    elif isinstance(python_like_object, (PythonBoolean, PythonFloat, PythonString)):
        return python_like_object.getValue()
    elif isinstance(python_like_object, PythonInteger):
        return python_like_object.getValue().longValue()
    elif isinstance(python_like_object, (PythonLikeTuple, tuple)):
        out = []
        for item in python_like_object:
            out.append(unwrap_python_like_object(item))
        return tuple(out)
    elif isinstance(python_like_object, List):
        out = []
        for item in python_like_object:
            out.append(unwrap_python_like_object(item))
        return out
    elif isinstance(python_like_object, Set):
        out = set()
        for item in python_like_object:
            out.add(unwrap_python_like_object(item))
        return out
    elif isinstance(python_like_object, Map):
        out = dict()
        for entry in python_like_object.entrySet():
            out[unwrap_python_like_object(entry.getKey())] = unwrap_python_like_object(entry.getValue())
        return out
    elif hasattr(python_like_object, 'get__optapy_Id'):
        return python_like_object.get__optapy_Id()
    elif not isinstance(python_like_object, PythonLikeObject):
        return python_like_object
    else:
        raise NotImplementedError(f'Unable to convert object of type {type(value)}')


def get_java_type_for_python_type(the_type):
    from org.optaplanner.python.translator.types import PythonLikeType
    global type_to_compiled_java_class

    if isinstance(the_type, type):
        if the_type in type_to_compiled_java_class:
            return type_to_compiled_java_class[the_type]
        else:
            try:
                return translate_python_class_to_java_class(the_type)
            except:
                return type_to_compiled_java_class[the_type]
    if isinstance(the_type, str):
        try:
            maybe_type = globals()[the_type]
            if isinstance(maybe_type, type):
                return get_java_type_for_python_type(maybe_type)
            return PythonLikeType.getBaseType()
        except:
            return PythonLikeType.getBaseType()
    raise ValueError(f'{the_type} is not a type or a str')


def copy_type_annotations(annotations_dict):
    from java.util import HashMap
    from org.optaplanner.python.translator.types import OpaquePythonReference, CPythonType # noqa

    global type_to_compiled_java_class

    out = HashMap()
    for name, value in annotations_dict.items():
        if not isinstance(name, str):
            continue
        if isinstance(value, (type, str)):
            out.put(name, get_java_type_for_python_type(value))
    return out


def copy_constants(constants_iterable):
    from java.util import ArrayList
    if constants_iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in constants_iterable:
        iterable_copy.add(convert_to_java_python_like_object(item))
    return iterable_copy


def copy_closure(closure):
    from org.optaplanner.python.translator.types import PythonLikeTuple, PythonCell
    out = PythonLikeTuple()
    if closure is None:
        return out
    else:
        for cell in closure:
            java_cell = PythonCell()
            java_cell.cellValue = convert_to_java_python_like_object(cell.cell_contents)
            out.add(java_cell)
        return out


def copy_globals(globals_dict):
    global global_dict_to_instance
    from java.util import HashMap

    globals_dict_key = id(globals_dict)
    if globals_dict_key in global_dict_to_instance:
        out = global_dict_to_instance[globals_dict_key]
    else:
        out = HashMap()

    global_dict_to_instance[globals_dict_key] = out
    for key, value in globals_dict.items():
        out.computeIfAbsent(key, lambda _: convert_to_java_python_like_object(value))

    return out


def get_function_bytecode_object(python_function):
    from java.util import ArrayList
    from org.optaplanner.python.translator import PythonBytecodeInstruction, PythonCompiledFunction # noqa

    init_type_to_compiled_java_class()

    python_compiled_function = PythonCompiledFunction()
    instruction_list = ArrayList()
    for instruction in dis.get_instructions(python_function):
        java_instruction = PythonBytecodeInstruction()
        java_instruction.opcode = PythonBytecodeInstruction.OpcodeIdentifier.valueOf(instruction.opname)
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

    python_compiled_function.instructionList = instruction_list
    python_compiled_function.co_names = copy_iterable(python_function.__code__.co_names)
    python_compiled_function.co_varnames = copy_iterable(python_function.__code__.co_varnames)
    python_compiled_function.co_cellvars = copy_iterable(python_function.__code__.co_cellvars)
    python_compiled_function.co_freevars = copy_iterable(python_function.__code__.co_freevars)
    python_compiled_function.co_constants = copy_constants(python_function.__code__.co_consts)
    python_compiled_function.co_argcount = python_function.__code__.co_argcount
    python_compiled_function.co_kwonlyargcount = python_function.__code__.co_kwonlyargcount
    python_compiled_function.closure = copy_closure(python_function.__closure__)
    python_compiled_function.globalsMap = copy_globals(python_function.__globals__)
    python_compiled_function.typeAnnotations = copy_type_annotations(python_function.__annotations__)
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


def translate_python_class_to_java_class(python_class):
    from java.util import ArrayList, HashMap
    from org.optaplanner.python.translator import PythonCompiledClass, PythonClassTranslator # noqa
    from org.optaplanner.python.translator.types import OpaquePythonReference, CPythonType # noqa

    global type_to_compiled_java_class

    init_type_to_compiled_java_class()

    if python_class in type_to_compiled_java_class:
        return type_to_compiled_java_class[python_class]

    type_to_compiled_java_class[python_class] = None
    methods = inspect.getmembers(python_class, predicate=inspect.isfunction)
    methods = [method for method in methods if method[0] in python_class.__dict__]
    static_attributes = inspect.getmembers(python_class, predicate=lambda member: not inspect.isfunction(member))
    static_attributes = [attribute for attribute in static_attributes if attribute[0] in python_class.__dict__]
    static_methods = [method for method in methods if isinstance(method[1], __STATIC_METHOD_TYPE)]
    class_methods = [method for method in methods if isinstance(method[1], __CLASS_METHOD_TYPE)]
    instance_methods = [method for method in methods if method not in static_methods and method not in class_methods]

    superclass_list = ArrayList()
    for superclass in python_class.__bases__:
        if superclass in type_to_compiled_java_class:
            superclass_list.add(type_to_compiled_java_class[superclass])
        else:
            try:
                superclass_list.add(translate_python_class_to_java_class(superclass))
            except:
                superclass_java_type = CPythonType.getType(JProxy(OpaquePythonReference, inst=superclass, convert=True))
                type_to_compiled_java_class[superclass] = superclass_java_type
                superclass_list.add(superclass_java_type)

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
    for attribute in static_attributes:
        static_attributes_map.put(attribute[0], convert_to_java_python_like_object(attribute[1]))

    python_compiled_class = PythonCompiledClass()
    python_compiled_class.className = python_class.__name__
    python_compiled_class.superclassList = superclass_list
    python_compiled_class.instanceFunctionNameToPythonBytecode = instance_method_map
    python_compiled_class.staticFunctionNameToPythonBytecode = static_method_map
    python_compiled_class.classFunctionNameToPythonBytecode = class_method_map
    python_compiled_class.staticAttributeNameToObject = static_attributes_map

    out = PythonClassTranslator.translatePythonClass(python_compiled_class)
    type_to_compiled_java_class[python_class] = out
    return out
