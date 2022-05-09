import dis

from jpype import JInt, JLong, JFloat, JBoolean, JProxy

def copy_iterable(iterable):
    from java.util import ArrayList
    if iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in iterable:
        iterable_copy.add(item)
    return iterable_copy


def convert_to_java_python_like_object(value):
    from org.optaplanner.python.translator.types import OpaquePythonReference
    from org.optaplanner.python.translator import PythonLikeObject
    from org.optaplanner.python.translator.types import PythonInteger, PythonFloat, PythonBoolean, PythonString, \
        PythonLikeList, PythonLikeTuple, PythonLikeSet, PythonLikeDict, PythonNone, PythonObjectWrapper
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
        return JLong(id(value))
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
    elif isinstance(python_like_object, (PythonBoolean, PythonInteger, PythonFloat, PythonString)):
        return python_like_object.getValue()
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
            out.put(unwrap_python_like_object(entry.getKey()),
                    unwrap_python_like_object(entry.getValue()))
        return out
    elif not isinstance(python_like_object, PythonLikeObject):
        return python_like_object
    else:
        raise NotImplementedError(f'Unable to convert object of type {type(value)}')


def copy_constants(constants_iterable):
    from java.util import ArrayList
    if constants_iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in constants_iterable:
        iterable_copy.add(convert_to_java_python_like_object(item))
    return iterable_copy


def translate_python_bytecode_to_java_bytecode(python_function, java_function_type):
    from java.util import ArrayList
    from org.optaplanner.python.translator import PythonBytecodeToJavaBytecodeTranslator # noqa
    from org.optaplanner.python.translator import PythonBytecodeInstruction, PythonCompiledFunction # noqa

    python_compiled_function = PythonCompiledFunction()
    instruction_list = ArrayList()
    for instruction in dis.get_instructions(python_function):
        java_instruction = PythonBytecodeInstruction()
        java_instruction.opcode = PythonBytecodeInstruction.OpCode.valueOf(instruction.opname)
        java_instruction.opname = instruction.opname
        java_instruction.offset = JInt(instruction.offset // 2)

        if instruction.arg is not None:
            java_instruction.arg = JInt(instruction.arg)
            java_instruction.argval = instruction.argval
            java_instruction.argrepr = instruction.argrepr
        else:
            java_instruction.arg = JInt(0)
            java_instruction.argval = None
            java_instruction.argrepr = None

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

    return PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(python_compiled_function,
                                                                          java_function_type)
