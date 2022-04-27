import dis

from jpype import JInt, JFloat, JBoolean

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
        PythonLikeList, PythonLikeTuple, PythonLikeSet, PythonLikeDict, PythonNone
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
    else:
        # TODO: Get compiled class corresponding to function / class bytecode
        raise NotImplementedError('Unable to convert object of type (' + type(value) + ')')


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
