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


def copy_constants(constants_iterable):
    from java.util import ArrayList
    if constants_iterable is None:
        return None
    iterable_copy = ArrayList()
    for item in constants_iterable:
        if item is None:
            iterable_copy.add(None)
        elif isinstance(item, (int, float, str)):
            iterable_copy.add(item)
        else:
            # TODO: Get compiled class corresponding to function / class bytecode
            raise NotImplementedError()
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
