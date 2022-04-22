import dis
from jpype import JImplements, JImplementationFor, JOverride
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from org.optaplanner.core.api.score.stream import ConstraintFactory
    from org.optaplanner.core.api.score.stream.uni import UniConstraintStream


def copy_iterable(iterable):
    from java.util import ArrayList
    if iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in iterable:
        iterable_copy.add(item)
    return iterable_copy


def convert_to_java_python_like_object(value):
    from org.optaplanner.optapy import PythonLikeObject
    from org.optaplanner.optapy.translator.types import PythonInteger, PythonFloat, PythonBoolean, PythonString, \
        PythonLikeList, PythonLikeTuple, PythonLikeSet, PythonLikeDict, PythonNone
    if isinstance(value, PythonLikeObject):
        return value
    if hasattr(type(value), '__optapy_java_class'):
        return id(value)
    elif value is None:
        return PythonNone.INSTANCE
    elif isinstance(value, bool):
        return PythonBoolean.valueOf(value)
    elif isinstance(value, int):
        return PythonInteger.valueOf(value)
    elif isinstance(value, float):
        return PythonFloat.valueOf(value)
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
    from org.optaplanner.optapy.translator import PythonBytecodeToJavaBytecodeTranslator # noqa
    from org.optaplanner.optapy.translator import PythonBytecodeInstruction, PythonCompiledFunction # noqa
    try:
        python_compiled_function = PythonCompiledFunction()
        instruction_list = ArrayList()
        for instruction in dis.get_instructions(python_function):
            java_instruction = PythonBytecodeInstruction()
            java_instruction.opcode = PythonBytecodeInstruction.OpCode.valueOf(instruction.opname)
            java_instruction.opname = instruction.opname
            java_instruction.arg = instruction.arg
            java_instruction.argval = instruction.argval
            java_instruction.argrepr = instruction.argrepr
            java_instruction.offset = instruction.offset
            java_instruction.startsLine = instruction.starts_line
            java_instruction.isJumpTarget = instruction.is_jump_target
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
    except Exception as e:
        print(e)
        return python_function


@JImplements('org.optaplanner.core.api.score.stream.uni.UniConstraintStream', deferred=True)
class PythonUniConstraintStream:
    def __init__(self, delegate: 'UniConstraintStream', tuple_type):
        self.delegate = delegate
        self.tuple_type = tuple_type

    def distinct(self):
        return PythonUniConstraintStream(self.delegate.distinct(), self.tuple_type)

    def filter(self, predicate):
        from java.util.function import Predicate
        translated_predicate = translate_python_bytecode_to_java_bytecode(predicate, Predicate)
        return PythonUniConstraintStream(self.delegate.filter(translated_predicate), self.tuple_type)


@JImplementationFor('org.optaplanner.core.api.score.stream.ConstraintFactory')
class PythonConstraintFactory:
    def __getattr__(self, item):
        pass
