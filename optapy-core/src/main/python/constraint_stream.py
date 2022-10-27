import dataclasses
import sys

import jpyinterpreter

from .optaplanner_java_interop import get_class
from .jpype_type_conversions import _convert_to_java_compatible_object
from .jpype_type_conversions import PythonFunction, PythonBiFunction, PythonTriFunction, PythonQuadFunction, \
    PythonPentaFunction, PythonToIntFunction, PythonToIntBiFunction, PythonToIntTriFunction, PythonToIntQuadFunction, \
    PythonPredicate, PythonBiPredicate, PythonTriPredicate, PythonQuadPredicate, PythonPentaPredicate
from jpyinterpreter import translate_python_bytecode_to_java_bytecode, check_current_python_version_supported
from enum import Enum
import jpype.imports  # noqa
from jpype import JImplements, JOverride, JObject, JClass, JInt
import inspect
import logging
from typing import TYPE_CHECKING, Type, Callable, Optional, overload, TypeVar, Generic, Any, Union, List, Sequence

if TYPE_CHECKING:
    from org.optaplanner.core.api.score.stream import Constraint, ConstraintFactory
    from org.optaplanner.core.api.score.stream.uni import UniConstraintCollector, UniConstraintStream
    from org.optaplanner.core.api.score.stream.bi import BiJoiner, BiConstraintCollector, BiConstraintStream
    from org.optaplanner.core.api.score.stream.tri import TriJoiner, TriConstraintCollector, TriConstraintStream
    from org.optaplanner.core.api.score.stream.quad import QuadJoiner, QuadConstraintCollector, QuadConstraintStream
    from org.optaplanner.core.api.score.stream.penta import PentaJoiner
    from org.optaplanner.core.api.score import Score


class BytecodeTranslation(Enum):
    """
    Specifies how bytecode translation should occur
    """
    FORCE = 'FORCE'
    """
    Forces bytecode translation, raising an error if it is impossible
    """
    IF_POSSIBLE = 'IF_POSSIBLE'
    """
    Use bytecode translation if possible, resorting to original Python implementation if impossible (default)
    """
    NONE = 'NONE'
    """
    Always use original Python implementation; bytecode translation will not occur
    """


function_bytecode_translation: BytecodeTranslation = BytecodeTranslation.IF_POSSIBLE
all_translated_successfully = True
logger = logging.getLogger('optapy')

def _check_if_bytecode_translation_possible():
    try:
        check_current_python_version_supported()
    except NotImplementedError:
        if all_translated_successfully:
            logger.warning('The bytecode translator does not support the current python version %s. This will '
                           'severely degrade performance.', sys.version, exc_info=True)
        raise


@JImplements('java.util.function.Predicate', deferred=True)
class PythonPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument):
        return self.delegate(argument)


@JImplements('java.util.function.BiPredicate', deferred=True)
class PythonBiPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2):
        return self.delegate(argument1, argument2)


@JImplements('org.optaplanner.core.api.function.TriPredicate', deferred=True)
class PythonTriPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3):
        return self.delegate(argument1, argument2, argument3)


@JImplements('org.optaplanner.core.api.function.QuadPredicate', deferred=True)
class PythonQuadPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3, argument4):
        return self.delegate(argument1, argument2, argument3, argument4)


@JImplements('org.optaplanner.core.api.function.PentaPredicate', deferred=True)
class PythonPentaPredicate:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def test(self, argument1, argument2, argument3, argument4, argument5):
        return self.delegate(argument1, argument2, argument3, argument4, argument5)


def _check_if_type_args_are_python_object_wrappers(type_args):
    global function_bytecode_translation, all_translated_successfully
    from org.optaplanner.jpyinterpreter.types.wrappers import PythonObjectWrapper

    for cls in type_args:
        if PythonObjectWrapper.class_.isAssignableFrom(cls):
            all_translated_successfully = False
            return True

    return False


def function_cast(function, *type_args):
    global function_bytecode_translation, all_translated_successfully
    arg_count = len(inspect.signature(function).parameters)
    if len(type_args) != arg_count:
        raise ValueError(f'Invalid function: expected {len(type_args)} arguments but got {arg_count}')

    if _check_if_type_args_are_python_object_wrappers(type_args):
        if function_bytecode_translation is BytecodeTranslation.FORCE:
            raise ValueError('Cannot force bytecode translation since some types could not be translated')

        return default_function_cast(function, arg_count)

    if function_bytecode_translation is not BytecodeTranslation.NONE:
        from java.util.function import Function, BiFunction
        from org.optaplanner.core.api.function import TriFunction, QuadFunction, PentaFunction
        from org.optaplanner.jpyinterpreter import PythonLikeObject

        try:
            _check_if_bytecode_translation_possible()
            if arg_count == 1:
                return translate_python_bytecode_to_java_bytecode(function, Function, *type_args, PythonLikeObject)
            elif arg_count == 2:
                return translate_python_bytecode_to_java_bytecode(function, BiFunction, *type_args, PythonLikeObject)
            elif arg_count == 3:
                return translate_python_bytecode_to_java_bytecode(function, TriFunction, *type_args, PythonLikeObject)
            elif arg_count == 4:
                return translate_python_bytecode_to_java_bytecode(function, QuadFunction, *type_args, PythonLikeObject)
            elif arg_count == 5:
                return translate_python_bytecode_to_java_bytecode(function, PentaFunction, *type_args, PythonLikeObject)
        except:  # noqa
            if function_bytecode_translation is BytecodeTranslation.FORCE:
                raise

            all_translated_successfully = False
            return default_function_cast(function, arg_count)

    return default_function_cast(function, arg_count)


def default_function_cast(function, arg_count):
    if arg_count == 1:
        return PythonFunction(lambda a: _convert_to_java_compatible_object(function(a)))
    elif arg_count == 2:
        return PythonBiFunction(lambda a, b: _convert_to_java_compatible_object(function(a, b)))
    elif arg_count == 3:
        return PythonTriFunction(lambda a, b, c: _convert_to_java_compatible_object(function(a, b, c)))
    elif arg_count == 4:
        return PythonQuadFunction(lambda a, b, c, d: _convert_to_java_compatible_object(function(a, b, c, d)))
    elif arg_count == 5:
        return PythonPentaFunction(lambda a, b, c, d, e: _convert_to_java_compatible_object(function(a, b, c, d, e)))
    else:
        raise ValueError


def predicate_cast(predicate, *type_args):
    global function_bytecode_translation, all_translated_successfully
    arg_count = len(inspect.signature(predicate).parameters)
    if len(type_args) != arg_count:
        raise ValueError(f'Invalid function: expected {len(type_args)} arguments but got {arg_count}')

    if _check_if_type_args_are_python_object_wrappers(type_args):
        if function_bytecode_translation is BytecodeTranslation.FORCE:
            raise ValueError('Cannot force bytecode translation since some types could not be translated')

        return default_predicate_cast(predicate, arg_count)

    if function_bytecode_translation is not BytecodeTranslation.NONE:
        from java.util.function import Predicate, BiPredicate
        from org.optaplanner.core.api.function import TriPredicate, QuadPredicate, PentaPredicate
        try:
            _check_if_bytecode_translation_possible()
            if arg_count == 1:
                return translate_python_bytecode_to_java_bytecode(predicate, Predicate, *type_args)
            elif arg_count == 2:
                return translate_python_bytecode_to_java_bytecode(predicate, BiPredicate, *type_args)
            elif arg_count == 3:
                return translate_python_bytecode_to_java_bytecode(predicate, TriPredicate, *type_args)
            elif arg_count == 4:
                return translate_python_bytecode_to_java_bytecode(predicate, QuadPredicate, *type_args)
            elif arg_count == 5:
                return translate_python_bytecode_to_java_bytecode(predicate, PentaPredicate, *type_args)
        except:  # noqa
            if function_bytecode_translation is BytecodeTranslation.FORCE:
                raise

            all_translated_successfully = False
            return default_predicate_cast(predicate, arg_count)

    return default_predicate_cast(predicate, arg_count)


def default_predicate_cast(predicate, arg_count):
    if arg_count == 1:
        return PythonPredicate(predicate)
    elif arg_count == 2:
        return PythonBiPredicate(predicate)
    elif arg_count == 3:
        return PythonTriPredicate(predicate)
    elif arg_count == 4:
        return PythonQuadPredicate(predicate)
    elif arg_count == 5:
        return PythonPentaPredicate(predicate)
    else:
        raise ValueError


def to_int_function_cast(function, *type_args):
    global function_bytecode_translation, all_translated_successfully
    arg_count = len(inspect.signature(function).parameters)
    if len(type_args) != arg_count:
        raise ValueError(f'Invalid function: expected {len(type_args)} arguments but got {arg_count}')

    if _check_if_type_args_are_python_object_wrappers(type_args):
        if function_bytecode_translation is BytecodeTranslation.FORCE:
            raise ValueError('Cannot force bytecode translation since some types could not be translated')

        return default_to_int_function_cast(function, arg_count)

    if function_bytecode_translation is not BytecodeTranslation.NONE:
        from java.util.function import ToIntFunction, ToIntBiFunction
        from org.optaplanner.core.api.function import ToIntTriFunction, ToIntQuadFunction

        try:
            _check_if_bytecode_translation_possible()
            if arg_count == 1:
                return translate_python_bytecode_to_java_bytecode(function, ToIntFunction, *type_args)
            elif arg_count == 2:
                return translate_python_bytecode_to_java_bytecode(function, ToIntBiFunction, *type_args)
            elif arg_count == 3:
                return translate_python_bytecode_to_java_bytecode(function, ToIntTriFunction, *type_args)
            elif arg_count == 4:
                return translate_python_bytecode_to_java_bytecode(function, ToIntQuadFunction, *type_args)
        except:  # noqa
            if function_bytecode_translation is BytecodeTranslation.FORCE:
                raise

            all_translated_successfully = False
            return default_to_int_function_cast(function, arg_count)

    return default_to_int_function_cast(function, arg_count)


def default_to_int_function_cast(function, arg_count):
    if arg_count == 1:
        return PythonToIntFunction(lambda a: _convert_to_java_compatible_object(function(a)))
    elif arg_count == 2:
        return PythonToIntBiFunction(lambda a, b: _convert_to_java_compatible_object(function(a, b)))
    elif arg_count == 3:
        return PythonToIntTriFunction(lambda a, b, c: _convert_to_java_compatible_object(function(a, b, c)))
    elif arg_count == 4:
        return PythonToIntQuadFunction(lambda a, b, c, d: _convert_to_java_compatible_object(function(a, b, c, d)))
    else:
        raise ValueError


@dataclasses.dataclass
class SamePropertyUniJoiner:
    joiner_creator: Callable
    join_function: Callable


@dataclasses.dataclass
class PropertyJoiner:
    joiner_creator: Callable
    left_join_function: Callable
    right_join_function: Callable


@dataclasses.dataclass
class SameOverlappingPropertyUniJoiner:
    joiner_creator: Callable
    start_function: Callable
    end_function: Callable


@dataclasses.dataclass
class OverlappingPropertyJoiner:
    joiner_creator: Callable
    left_start_function: Callable
    left_end_function: Callable
    right_start_function: Callable
    right_end_function: Callable


@dataclasses.dataclass
class FilteringJoiner:
    joiner_creator: Callable
    filter_function: Callable


def extract_joiners(joiner_tuple, *stream_types):
    from org.optaplanner.core.api.score.stream.bi import BiJoiner
    from org.optaplanner.core.api.score.stream.tri import TriJoiner
    from org.optaplanner.core.api.score.stream.quad import QuadJoiner
    from org.optaplanner.core.api.score.stream.penta import PentaJoiner

    if len(joiner_tuple) == 1 and (isinstance(joiner_tuple[0], list) or isinstance(joiner_tuple[0], tuple)):
        joiner_tuple = joiner_tuple[0] # Joiners was passed as a list of Joiners instead of varargs
    array_size = len(joiner_tuple)
    output_array = None
    array_type = None
    if len(stream_types) == 2:
        array_type = BiJoiner
        output_array = BiJoiner[array_size]
    elif len(stream_types) == 3:
        array_type = TriJoiner
        output_array = TriJoiner[array_size]
    elif len(stream_types) == 4:
        array_type = QuadJoiner
        output_array = QuadJoiner[array_size]
    elif len(stream_types) == 5:
        array_type = PentaJoiner
        output_array = PentaJoiner[array_size]
    else:
        raise ValueError

    for i in range(array_size):
        joiner_info = joiner_tuple[i]
        created_joiner = None
        if isinstance(joiner_info, SamePropertyUniJoiner):
            property_function = function_cast(joiner_info.join_function, stream_types[0])
            created_joiner = joiner_info.joiner_creator(property_function)
        elif isinstance(joiner_info, PropertyJoiner):
            left_property_function = function_cast(joiner_info.left_join_function, *stream_types[:-1])
            right_property_function = function_cast(joiner_info.right_join_function, stream_types[-1])
            created_joiner = joiner_info.joiner_creator(left_property_function, right_property_function)
        elif isinstance(joiner_info, SameOverlappingPropertyUniJoiner):
            start_function = function_cast(joiner_info.start_function, stream_types[0])
            end_function = function_cast(joiner_info.end_function, stream_types[0])
            created_joiner = joiner_info.joiner_creator(start_function, end_function)
        elif isinstance(joiner_info, OverlappingPropertyJoiner):
            left_start_function = function_cast(joiner_info.left_start_function, *stream_types[:-1])
            left_end_function = function_cast(joiner_info.left_end_function, *stream_types[:-1])
            right_start_function = function_cast(joiner_info.right_start_function, stream_types[-1])
            right_end_function = function_cast(joiner_info.right_end_function, stream_types[-1])
            created_joiner = joiner_info.joiner_creator(left_start_function, left_end_function,
                                                        right_start_function, right_end_function)
        elif isinstance(joiner_info, FilteringJoiner):
            filter_function = predicate_cast(joiner_info.filter_function, *stream_types)
            created_joiner = joiner_info.joiner_creator(filter_function)
        else:
            raise ValueError(f'Invalid Joiner: {joiner_info}. Create Joiners via optapy.constraint.Joiners.')

        output_array[i] = array_type @ created_joiner

    return output_array


#  Class type variables
A = TypeVar('A')
B = TypeVar('B')
C = TypeVar('C')
D = TypeVar('D')


@dataclasses.dataclass
class ConstraintInfo:
    constraint_package: str
    constraint_name: str
    score: Optional['Score']
    impact_function: Optional[Callable]


def extract_constraint_info(default_package: str, args: tuple) -> ConstraintInfo:
    from org.optaplanner.core.api.score import Score
    constraint_package = default_package
    constraint_name = None
    if isinstance(args[1], str):
        constraint_package = args[0]
        constraint_name = args[1]
        args = args[2:]
    else:
        constraint_name = args[0]
        args = args[1:]

    if len(args) == 0:
        return ConstraintInfo(constraint_package, constraint_name, None, None)

    if isinstance(args[0], Score):
        score = args[0]
        args = args[1:]
    else:
        score = None

    if len(args) == 0:
        return ConstraintInfo(constraint_package, constraint_name, score, None)
    else:
        return ConstraintInfo(constraint_package, constraint_name, score, args[0])


@dataclasses.dataclass
class NoArgsConstraintCollector:
    collector_creator: Callable


@dataclasses.dataclass
class GroupMappingSingleArgConstraintCollector:
    collector_creator: Callable
    group_mapping: Callable


@dataclasses.dataclass
class KeyValueMappingConstraintCollector:
    collector_creator: Callable
    key_mapping: Callable
    value_mapping: Callable


@dataclasses.dataclass
class GroupIntMappingSingleArgConstraintCollector:
    collector_creator: Callable
    group_mapping: Callable


@dataclasses.dataclass
class ComposeConstraintCollector:
    collector_creator: Callable
    subcollectors: Sequence[Any]
    compose_function: Callable


@dataclasses.dataclass
class ConditionalConstraintCollector:
    collector_creator: Callable
    predicate: Callable
    delegate: Any


def extract_collector(collector_info, *type_arguments):
    if isinstance(collector_info, NoArgsConstraintCollector):
        return collector_info.collector_creator()
    elif isinstance(collector_info, GroupMappingSingleArgConstraintCollector):
        return collector_info.collector_creator(function_cast(collector_info.group_mapping, *type_arguments))
    elif isinstance(collector_info, KeyValueMappingConstraintCollector):
        return collector_info.collector_creator(function_cast(collector_info.key_mapping, *type_arguments),
                                                function_cast(collector_info.value_mapping, *type_arguments))
    elif isinstance(collector_info, GroupIntMappingSingleArgConstraintCollector):
        return collector_info.collector_creator(to_int_function_cast(collector_info.group_mapping, *type_arguments))
    elif isinstance(collector_info, ComposeConstraintCollector):
        subcollectors = tuple(map(lambda subcollector_info: extract_collector(subcollector_info, *type_arguments),
                                  collector_info.subcollectors))
        compose_parameters = (JClass('java.lang.Object'), ) * len(subcollectors)
        compose_function = function_cast(collector_info.compose_function, *compose_parameters)
        return collector_info.collector_creator(*subcollectors, compose_function)
    elif isinstance(collector_info, ConditionalConstraintCollector):
        delegate_collector = extract_collector(collector_info.delegate, *type_arguments)
        predicate = predicate_cast(collector_info.predicate, *type_arguments)
        return collector_info.collector_creator(predicate, delegate_collector)
    else:
        raise ValueError(f'Invalid Collector: {collector_info}. '
                         f'Create Collectors via optapy.constraint.ConstraintCollectors.')


def perform_group_by(delegate, package, group_by_args, *type_arguments):
    actual_group_by_args = []
    for i in range(len(group_by_args)):
        if callable(group_by_args[i]):
            actual_group_by_args.append(function_cast(group_by_args[i], *type_arguments))
        else:
            collector_info = group_by_args[i]
            created_collector = extract_collector(collector_info, *type_arguments)
            actual_group_by_args.append(created_collector)

    if len(group_by_args) == 1:
        return PythonUniConstraintStream(delegate.groupBy(*actual_group_by_args), package, JClass('java.lang.Object'))
    elif len(group_by_args) == 2:
        return PythonBiConstraintStream(delegate.groupBy(*actual_group_by_args), package, JClass('java.lang.Object'),
                                        JClass('java.lang.Object'))
    elif len(group_by_args) == 3:
        return PythonTriConstraintStream(delegate.groupBy(*actual_group_by_args), package, JClass('java.lang.Object'),
                                         JClass('java.lang.Object'), JClass('java.lang.Object'))
    elif len(group_by_args) == 4:
        return PythonQuadConstraintStream(delegate.groupBy(*actual_group_by_args), package, JClass('java.lang.Object'),
                                          JClass('java.lang.Object'), JClass('java.lang.Object'),
                                          JClass('java.lang.Object'))
    else:
        raise ValueError


class PythonConstraintFactory:
    delegate: 'ConstraintFactory'
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    function_bytecode_translation: BytecodeTranslation

    def __init__(self, delegate: 'ConstraintFactory', function_bytecode_translation: BytecodeTranslation):
        self.delegate = delegate
        self.function_bytecode_translation = function_bytecode_translation

    def get_default_constraint_package(self) -> str:
        """This is ConstraintConfiguration.constraintPackage() if available, otherwise the module of the @constraint_provider function

        :return:
        """
        return self.delegate.getDefaultConstraintPackage()

    getDefaultConstraintPackage = get_default_constraint_package

    def for_each(self, source_class: Type[A_]) -> 'PythonUniConstraintStream[A_]':
        """Start a ConstraintStream of all instances of the source_class that are known as problem facts or planning entities.

        :param source_class:

        :return:
        """
        global function_bytecode_translation
        source_class = get_class(source_class)
        function_bytecode_translation = self.function_bytecode_translation
        return PythonUniConstraintStream(self.delegate.forEach(source_class), self.getDefaultConstraintPackage(),
                                             source_class)

    forEach = for_each

    def for_each_including_null_vars(self, source_class: Type[A_]) -> 'PythonUniConstraintStream[A_]':
        """Start a ConstraintStream of all instances of the source_class that are known as problem facts or planning
        entities, without filtering of entities with null planning variables.

        :param source_class:

        :return:
        """
        global function_bytecode_translation
        source_class = get_class(source_class)
        function_bytecode_translation = self.function_bytecode_translation
        return PythonUniConstraintStream(self.delegate.forEachIncludingNullVars(source_class),
                                             self.getDefaultConstraintPackage(), source_class)

    forEachIncludingNullVars = for_each_including_null_vars

    def for_each_unique_pair(self, source_class: Type[A_], *joiners: 'BiJoiner[A_, A_]') ->\
            'PythonBiConstraintStream[A_, A_]':
        """Create a new BiConstraintStream for every unique combination of A and another A with a higher @planning_id
        that satisfies all specified joiners.

        :param source_class:

        :param joiners:

        :return:
        """
        global function_bytecode_translation
        source_class = get_class(source_class)
        function_bytecode_translation = self.function_bytecode_translation
        return PythonBiConstraintStream(self.delegate.forEachUniquePair(source_class,
                                                                            extract_joiners(joiners, source_class, source_class)),
                                            self.getDefaultConstraintPackage(), source_class, source_class)

    forEachUniquePair = for_each_unique_pair

    def from_(self, source_class: Type[A_]) -> 'PythonUniConstraintStream[A_]':
        """Deprecated, for removal: use for_each instead

        :param source_class:

        :return:
        """
        global function_bytecode_translation
        source_class = get_class(source_class)
        function_bytecode_translation = self.function_bytecode_translation
        return PythonUniConstraintStream(self.delegate.from_(source_class), self.getDefaultConstraintPackage(),
                                             source_class)

    def from_unfiltered(self, source_class: Type[A_]) -> 'PythonUniConstraintStream[A_]':
        """Deprecated, for removal: use for_each_including_null_vars instead

        :param source_class:

        :return:
        """
        global function_bytecode_translation
        source_class = get_class(source_class)
        function_bytecode_translation = self.function_bytecode_translation
        return PythonUniConstraintStream(self.delegate.fromUnfiltered(source_class), self.getDefaultConstraintPackage(),
                                             source_class)

    fromUnfiltered = from_unfiltered

    def from_unique_pair(self, source_class: Type[A_], *joiners: 'BiJoiner[A_, A_]') ->\
            'PythonBiConstraintStream[A_, A_]':
        """Deprecated, for removal: use for_each_unique_pair instead

        :param source_class:

        :return:
        """
        global function_bytecode_translation
        source_class = get_class(source_class)
        function_bytecode_translation = self.function_bytecode_translation
        return PythonBiConstraintStream(self.delegate.fromUniquePair(source_class,
                                                                     extract_joiners(joiners, source_class, source_class)),
                                        self.getDefaultConstraintPackage(), source_class, source_class)

    fromUniquePair = from_unique_pair


class PythonUniConstraintStream(Generic[A]):
    delegate: 'UniConstraintStream[A]'
    package: str
    a_type: Type[A]
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: 'UniConstraintStream[A]', package: str, a_type: Type[A]):
        self.delegate = delegate
        self.package = package
        self.a_type = a_type

    def get_constraint_factory(self):
        return PythonConstraintFactory(self.delegate.getConstraintFactory())

    getConstraintFactory = get_constraint_factory

    def filter(self, predicate: Callable[[A], bool]) -> 'PythonUniConstraintStream[A]':
        """Exhaustively test each fact against the predicate and match if the predicate returns True.

        :param predicate:

        :return:
        """
        translated_predicate = predicate_cast(predicate, self.a_type)
        return PythonUniConstraintStream(self.delegate.filter(translated_predicate), self.package, self.a_type)

    def join(self, unistream_or_type: Union['PythonUniConstraintStream[B_]', Type[B_]], *joiners: 'BiJoiner[A, B_]') ->\
            'PythonBiConstraintStream[A,B_]':
        """Create a new BiConstraintStream for every combination of A and B that satisfy all specified joiners.

        :param unistream_or_type:

        :param joiners:

        :return:
        """
        b_type = None
        if isinstance(unistream_or_type, PythonUniConstraintStream):
            b_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            b_type = get_class(unistream_or_type)
            unistream_or_type = b_type

        join_result = self.delegate.join(unistream_or_type, extract_joiners(joiners, self.a_type, b_type))
        return PythonBiConstraintStream(join_result, self.package, self.a_type, b_type)

    def if_exists(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> 'PythonUniConstraintStream[A]':
        """Create a new UniConstraintStream for every A where B exists that satisfy all specified joiners.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonUniConstraintStream(self.delegate.ifExists(item_type,
                                                                extract_joiners(joiners, self.a_type, item_type)),
                                         self.package, self.a_type)

    ifExists = if_exists

    def if_exists_including_null_vars(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') ->\
            'PythonUniConstraintStream[A]':
        """Create a new UniConstraintStream for every A where B exists that satisfy all specified joiners.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonUniConstraintStream(self.delegate.ifExistsIncludingNullVars(item_type,
                                                                                 extract_joiners(joiners, self.a_type,
                                                                                                 item_type)),
                                         self.package,
                                         self.a_type)

    ifExistsIncludingNullVars = if_exists_including_null_vars

    def if_exists_other(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> 'PythonUniConstraintStream[A]':
        """Create a new UniConstraintStream for every A, if another A exists that does not equal the first, and for which all specified joiners are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonUniConstraintStream(self.delegate.ifExistsOther(item_type, extract_joiners(joiners, self.a_type,
                                                                                                item_type)),
                                         self.package, self.a_type)

    ifExistsOther = if_exists_other

    def if_exists_other_including_null_vars(self, item_type: Type, *joiners: 'BiJoiner') -> \
            'PythonUniConstraintStream':
        item_type = get_class(item_type)
        return PythonUniConstraintStream(self.delegate.ifExistsOtherIncludingNullVars(item_type,
                                                                                      extract_joiners(joiners,
                                                                                                      self.a_type,
                                                                                                      item_type)),
                                         self.package,
                                         self.a_type)

    ifExistsOtherIncludingNullVars = if_exists_other_including_null_vars

    def if_not_exists(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> 'PythonUniConstraintStream[A]':
        """Create a new UniConstraintStream for every A where there does not exist a B where all specified joiners are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonUniConstraintStream(self.delegate.ifNotExists(item_type, extract_joiners(joiners, self.a_type,
                                                                                              item_type)),
                                         self.package, self.a_type)

    ifNotExists = if_not_exists

    def if_not_exists_including_null_vars(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> \
            'PythonUniConstraintStream[A]':
        """Create a new UniConstraintStream for every A where there does not exist a B where all specified joiners are
        satisfied.

       :param item_type:

       :param joiners:

       :return:
       """
        item_type = get_class(item_type)
        return PythonUniConstraintStream(self.delegate.ifNotExistsIncludingNullVars(item_type,
                                                                                    extract_joiners(joiners,
                                                                                                    self.a_type,
                                                                                                    item_type)),
                                         self.package,
                                         self.a_type)

    ifNotExistsIncludingNullVars = if_not_exists_including_null_vars

    def if_not_exists_other(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') ->\
            'PythonUniConstraintStream[A]':
        """Create a new UniConstraintStream for every A where there does not exist a different A where all specified
        joiners are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonUniConstraintStream(self.delegate.ifNotExistsOther(item_type, extract_joiners(joiners, self.a_type,
                                                                                                   item_type)),
                                         self.package,
                                         self.a_type)


    ifNotExistsOther = if_not_exists_other

    def if_not_exists_other_including_null_vars(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> \
            'PythonUniConstraintStream[A]':
        """Create a new UniConstraintStream for every A where there does not exist a different A where all specified
        joiners are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonUniConstraintStream(self.delegate.ifNotExistsOtherIncludingNullVars(item_type,
                                                                                         extract_joiners(joiners,
                                                                                                         self.a_type,
                                                                                                         item_type)),
                                         self.package, self.a_type)

    ifNotExistsOtherIncludingNullVars = if_not_exists_other_including_null_vars

    @overload
    def group_by(self, key_mapping: Callable[[A], A_]) -> 'PythonUniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, collector: 'UniConstraintCollector[A, Any, A_]') -> 'PythonUniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_]) -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A], A_], collector: 'UniConstraintCollector[A, Any, B_]') -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_collector: 'UniConstraintCollector[A, Any, A_]',
                 second_collector: 'UniConstraintCollector[A, Any, B_]') -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 third_key_mapping: Callable[[A], C_]) -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 collector: 'UniConstraintCollector[A, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A], A_], first_collector: 'UniConstraintCollector[A, Any, B_]',
                 second_collector: 'UniConstraintCollector[A, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_collector: 'UniConstraintCollector[A, Any, A_]',
                 second_collector: 'UniConstraintCollector[A, Any, B_]',
                 third_collector: 'UniConstraintCollector[A, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 third_key_mapping: Callable[[A], C_], fourth_key_mapping: Callable[[A], D_]) -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 third_key_mapping: Callable[[A], C_], collector: 'UniConstraintCollector[A, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 first_collector: 'UniConstraintCollector[A, Any, C_]',
                 second_collector: 'UniConstraintCollector[A, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A], A_], first_collector: 'UniConstraintCollector[A, Any, B_]',
                 second_collector: 'UniConstraintCollector[A, Any, C_]',
                 third_collector: 'UniConstraintCollector[A, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_collector: 'UniConstraintCollector[A, Any, A_]',
                 second_collector: 'UniConstraintCollector[A, Any, B_]',
                 third_collector: 'UniConstraintCollector[A, Any, C_]',
                 fourth_collector: 'UniConstraintCollector[A, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    def group_by(self, *args):
        """Collect items into groups using the group_key_function(s) and optionally aggregate the group's items into a
        result.

        The syntax of group_by is zero to four group_key functions, followed by zero to four collectors. At most
        four arguments can be passed to group_by.

        If no group_key function is passed to group_by, all items in the stream are aggregated into a single result
        by the passed constraint collectors.

        Examples:

            - # count the items in this stream; returns Uni[int]

              group_by(ConstraintCollectors.count())

            - # count the number of shifts each employee has; returns Bi[Employee]

              group_by(lambda shift: shift.employee, ConstraintCollectors.count())

            - # count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

              group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

            - # count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

              group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

            - # get the dates of the first and last shift of each employee; returns Tri[Employee, datetime.date,
              datetime.date]

              group_by(lambda shift: shift.employee,
              ConstraintCollectors.min(lambda shift: shift.date)
              ConstraintCollectors.max(lambda shift: shift.date))

        The type of stream returned depends on the number of arguments passed:

        - 1 -> UniConstraintStream

        - 2 -> BiConstraintStream

        - 3 -> TriConstraintStream

        - 4 -> QuadConstraintStream

        :param args:

        :return:
        """
        return perform_group_by(self.delegate, self.package, args, self.a_type)

    groupBy = group_by


    def map(self, mapping_function: Callable[[A], A_]) -> 'PythonUniConstraintStream[A_]':
        """Transforms the stream in such a way that tuples are remapped using the given function.

        :param mapping_function:

        :return:
        """
        translated_function = function_cast(mapping_function, self.a_type)
        return PythonUniConstraintStream(self.delegate.map(translated_function), self.package,
                                         JClass('java.lang.Object'))

    def flatten_last(self, flattening_function: Callable[[A], A_]) -> 'PythonUniConstraintStream[A_]':
        """Takes each tuple and applies a mapping on it, which turns the tuple into an Iterable.

        :param flattening_function:

        :return:
        """
        translated_function = function_cast(flattening_function, self.a_type)
        return PythonUniConstraintStream(self.delegate.flattenLast(translated_function), self.package,
                                         JClass('java.lang.Object'))

    flattenLast = flatten_last

    def distinct(self) -> 'PythonUniConstraintStream[A]':
        """Transforms the stream in such a way that all the tuples going through it are distinct.

        :return:
        """
        return PythonUniConstraintStream(self.delegate.distinct(), self.package, self.a_type)

    @overload
    def penalize(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A], int]) -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
                 match_weigher: Callable[[A], int]) -> 'Constraint':
        ...

    def penalize(self, *args) -> 'Constraint':
        """Negatively impact the Score: subtract the constraint_weight for each match (multiplied by an optional
        match_weigher function).

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use penalize_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - penalize(constraint_name: str, constraint_weight: Score)

            - penalize(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - penalize(constraint_name: str, constraint_weight: Score, match_weigher: A -> int)

            - penalize(constraint_package: str, constraint_name: str, constraint_weight: Score, match_weigher: A -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.penalize(constraint_info.constraint_package, constraint_info.constraint_name,
                                          constraint_info.score)
        else:
            return self.delegate.penalize(constraint_info.constraint_package, constraint_info.constraint_name,
                                          constraint_info.score,
                                          to_int_function_cast(constraint_info.impact_function, self.a_type))

    def penalize_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeLong = penalize_long

    def penalize_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeBigDecimal = penalize_big_decimal

    def penalize_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurable = penalize_configurable

    def penalize_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurableLong = penalize_configurable_long

    def penalize_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurableBigDecimal = penalize_configurable_big_decimal

    @overload
    def reward(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A], int]) -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
               match_weigher: Callable[[A], int]) -> 'Constraint':
        ...

    def reward(self, *args) -> 'Constraint':
        """Positively impact the Score: add the constraint_weight for each match (multiplied by an optional
        match_weigher function).

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use reward_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - reward(constraint_name: str, constraint_weight: Score)

            - reward(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - reward(constraint_name: str, constraint_weight: Score, match_weigher: A -> int)

            - reward(constraint_package: str, constraint_name: str, constraint_weight: Score, match_weigher: A -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.reward(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score)
        else:
            return self.delegate.reward(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score,
                                        to_int_function_cast(constraint_info.impact_function, self.a_type))

    def reward_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardLong = reward_long

    def reward_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardBigDecimal = reward_big_decimal

    def reward_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurable = reward_configurable

    def reward_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurableLong = reward_configurable_long

    def reward_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurableBigDecimal = reward_configurable_big_decimal

    @overload
    def impact(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A], int]) -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
               match_weigher: Callable[[A], int]) -> 'Constraint':
        ...

    def impact(self, *args) -> 'Constraint':
        """Positively or negatively impact the Score: add the constraint_weight for each match
        (multiplied by an optional match_weigher function).

        Use penalize(...) or reward(...) instead, unless this constraint can both have positive and negative weights.

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use impact_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - impact(constraint_name: str, constraint_weight: Score)

            - impact(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - impact(constraint_name: str, constraint_weight: Score, match_weigher: A -> int)

            - impact(constraint_package: str, constraint_name: str, constraint_weight: Score, match_weigher: A -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.impact(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score)
        else:
            return self.delegate.impact(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score,
                                        to_int_function_cast(constraint_info.impact_function, self.a_type))

    def impact_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactLong = impact_long

    def impact_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactBigDecimal = impact_big_decimal

    def impact_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurable = impact_configurable

    def impact_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurableLong = impact_configurable_long

    def impact_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurableBigDecimal = impact_configurable_big_decimal


class PythonBiConstraintStream(Generic[A, B]):
    delegate: 'BiConstraintStream[A,B]'
    package: str
    a_type: Type[A]
    b_type: Type[B]
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: 'BiConstraintStream[A,B]', package: str, a_type: Type[A], b_type: Type[B]):
        self.delegate = delegate
        self.package = package
        self.a_type = a_type
        self.b_type = b_type

    def get_constraint_factory(self):
        return PythonConstraintFactory(self.delegate.getConstraintFactory())

    getConstraintFactory = get_constraint_factory

    def filter(self, predicate: Callable[[A,B], bool]) -> 'PythonBiConstraintStream[A,B]':
        """Exhaustively test each fact against the predicate and match if the predicate returns True.

        :param predicate:

        :return:
        """
        translated_predicate = predicate_cast(predicate, self.a_type, self.b_type)
        return PythonBiConstraintStream(self.delegate.filter(translated_predicate), self.package, self.a_type,
                                        self.b_type)

    def join(self, unistream_or_type: Union[PythonUniConstraintStream[C_], Type[C_]],
             *joiners: 'TriJoiner[A,B,C_]') -> 'PythonTriConstraintStream[A,B,C_]':
        """Create a new TriConstraintStream for every combination of A, B and C that satisfy all specified joiners.

        :param unistream_or_type:

        :param joiners:

        :return:
        """
        c_type = None
        if isinstance(unistream_or_type, PythonUniConstraintStream):
            c_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            c_type = get_class(unistream_or_type)
            unistream_or_type = c_type

        join_result = self.delegate.join(unistream_or_type, extract_joiners(joiners, self.a_type, self.b_type, c_type))
        return PythonTriConstraintStream(join_result, self.package, self.a_type, self.b_type, c_type)

    def if_exists(self, item_type: Type[C_], *joiners: 'TriJoiner[A, B, C_]') -> 'PythonBiConstraintStream[A,B]':
        """Create a new BiConstraintStream for every A, B where C exists that satisfy all specified joiners.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonBiConstraintStream(self.delegate.ifExists(item_type,
                                                               extract_joiners(joiners, self.a_type,
                                                                               self.b_type, item_type)),
                                        self.package, self.a_type, self.b_type)

    ifExists = if_exists

    def if_exists_including_null_vars(self, item_type: Type[C_], *joiners: 'TriJoiner[A, B, C_]') ->\
            'PythonBiConstraintStream[A,B]':
        """Create a new BiConstraintStream for every A, B where C exists that satisfy all specified joiners.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonBiConstraintStream(self.delegate.ifExistsIncludingNullVars(item_type, extract_joiners(joiners,
                                                                                                           self.a_type,
                                                                                                           self.b_type,
                                                                                                           item_type)),
                                        self.package,
                                        self.a_type, self.b_type)

    ifExistsIncludingNullVars = if_exists_including_null_vars

    def if_not_exists(self, item_type: Type[C_], *joiners: 'TriJoiner[A, B, C_]') ->\
            'PythonBiConstraintStream[A,B]':
        """Create a new BiConstraintStream for every A, B, where there does not exist a C where all specified joiners
        are satisfied.

       :param item_type:

       :param joiners:

       :return:
       """
        item_type = get_class(item_type)
        return PythonBiConstraintStream(self.delegate.ifNotExists(item_type, extract_joiners(joiners, self.a_type,
                                                                                             self.b_type, item_type)),
                                        self.package, self.a_type, self.b_type)

    ifNotExists = if_not_exists

    def if_not_exists_including_null_vars(self, item_type: Type[C_], *joiners: 'TriJoiner[A, B, C_]') -> \
            'PythonBiConstraintStream[A,B]':
        """Create a new BiConstraintStream for every A, B, where there does not exist a C where all specified joiners
        are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonBiConstraintStream(self.delegate.ifNotExistsIncludingNullVars(item_type,
                                                                                   extract_joiners(joiners,
                                                                                                   self.a_type,
                                                                                                   self.b_type,
                                                                                                   item_type)),
                                        self.package, self.a_type, self.b_type)

    ifNotExistsIncludingNullVars = if_not_exists_including_null_vars

    @overload
    def group_by(self, key_mapping: Callable[[A, B], A_]) -> 'PythonUniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, collector: 'BiConstraintCollector[A, B, Any, A_]') -> 'PythonUniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_]) -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B], A_], collector: 'BiConstraintCollector[A, B, Any, B_]') -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_collector: 'BiConstraintCollector[A, B, Any, A_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, B_]') -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 third_key_mapping: Callable[[A, B], C_]) -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 collector: 'BiConstraintCollector[A, B, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B], A_], first_collector: 'BiConstraintCollector[A, B, Any, B_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_collector: 'BiConstraintCollector[A, B, Any, A_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, B_]',
                 third_collector: 'BiConstraintCollector[A, B, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 third_key_mapping: Callable[[A, B], C_], fourth_key_mapping: Callable[[A, B], D_]) -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 third_key_mapping: Callable[[A, B], C_], collector: 'BiConstraintCollector[A, B, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 first_collector: 'BiConstraintCollector[A, B, Any, C_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B], A_], first_collector: 'BiConstraintCollector[A, B, Any, B_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, C_]',
                 third_collector: 'BiConstraintCollector[A, B, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_collector: 'BiConstraintCollector[A, B, Any, A_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, B_]',
                 third_collector: 'BiConstraintCollector[A, B, Any, C_]',
                 fourth_collector: 'BiConstraintCollector[A, B, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    def group_by(self, *args):
        """Collect items into groups using the group_key_function(s) and optionally aggregate the group's items into a
        result.

        The syntax of group_by is zero to four group_key functions, followed by zero to four collectors. At most
        four arguments can be passed to group_by.

        If no group_key function is passed to group_by, all items in the stream are aggregated into a single result
        by the passed constraint collectors.

        Examples:

            - # count the items in this stream; returns Uni[int]

              group_by(ConstraintCollectors.count_bi())

            - # count the number of shifts each employee has; returns Bi[Employee]

              group_by(lambda shift, _: shift.employee, ConstraintCollectors.count_bi())

            - # count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

              group_by(lambda shift, _: shift.employee, lambda shift, _: shift.date, ConstraintCollectors.count_bi())

            - # count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

              group_by(lambda shift, _: shift.employee, lambda shift, _: shift.date, ConstraintCollectors.count_bi())

            - # get the dates of the first and last shift of each employee; returns Tri[Employee, datetime.date,
              datetime.date]

              group_by(lambda shift, _: shift.employee,
              ConstraintCollectors.min(lambda shift, _: shift.date)
              ConstraintCollectors.max(lambda shift, _: shift.date))

        The type of stream returned depends on the number of arguments passed:

        - 1 -> UniConstraintStream

        - 2 -> BiConstraintStream

        - 3 -> TriConstraintStream

        - 4 -> QuadConstraintStream

        :param args:

        :return:
        """
        return perform_group_by(self.delegate, self.package, args, self.a_type, self.b_type)

    groupBy = group_by

    def map(self, mapping_function: Callable[[A,B],A_]) -> 'PythonUniConstraintStream[A_]':
        """Transforms the stream in such a way that tuples are remapped using the given function.

        :param mapping_function:

        :return:
        """
        translated_function = function_cast(mapping_function, self.a_type, self.b_type)
        return PythonUniConstraintStream(self.delegate.map(translated_function), self.package, JClass('java.lang.Object'))

    def flatten_last(self, flattening_function: Callable[[B], B_]) -> 'PythonBiConstraintStream[A,B_]':
        """Takes each tuple and applies a mapping on it, which turns the tuple into an Iterable.

        :param flattening_function:

        :return:
        """
        translated_function = function_cast(flattening_function, self.b_type)
        return PythonBiConstraintStream(self.delegate.flattenLast(translated_function), self.package,
                                        self.a_type, JClass('java.lang.Object'))

    flattenLast = flatten_last

    def distinct(self) -> 'PythonBiConstraintStream[A,B]':
        """Transforms the stream in such a way that all the tuples going through it are distinct.

        :return:
        """
        return PythonBiConstraintStream(self.delegate.distinct(), self.package, self.a_type, self.b_type)

    @overload
    def penalize(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B], int]) ->\
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
                 match_weigher: Callable[[A, B], int]) -> 'Constraint':
        ...

    def penalize(self, *args) -> 'Constraint':
        """Negatively impact the Score: subtract the constraint_weight for each match (multiplied by an optional
        match_weigher function).

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use penalize_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - penalize(constraint_name: str, constraint_weight: Score)

            - penalize(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - penalize(constraint_name: str, constraint_weight: Score, match_weigher: (A, B) -> int)

            - penalize(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.penalize(constraint_info.constraint_package, constraint_info.constraint_name,
                                          constraint_info.score)
        else:
            return self.delegate.penalize(constraint_info.constraint_package, constraint_info.constraint_name,
                                          constraint_info.score,
                                          to_int_function_cast(constraint_info.impact_function, self.a_type,
                                                               self.b_type))

    def penalize_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeLong = penalize_long

    def penalize_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeBigDecimal = penalize_big_decimal

    def penalize_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurable = penalize_configurable

    def penalize_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurableLong = penalize_configurable_long

    def penalize_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurableBigDecimal = penalize_configurable_big_decimal

    @overload
    def reward(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B], int]) -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
                 match_weigher: Callable[[A, B], int]) -> 'Constraint':
        ...

    def reward(self, *args) -> 'Constraint':
        """Positively impact the Score: add the constraint_weight for each match (multiplied by an optional
        match_weigher function).

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use reward_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - reward(constraint_name: str, constraint_weight: Score)

            - reward(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - reward(constraint_name: str, constraint_weight: Score, match_weigher: (A, B) -> int)

            - reward(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.reward(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score)
        else:
            return self.delegate.reward(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score,
                                        to_int_function_cast(constraint_info.impact_function, self.a_type, self.b_type))

    def reward_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardLong = reward_long

    def reward_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardBigDecimal = reward_big_decimal

    def reward_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurable = reward_configurable

    def reward_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurableLong = reward_configurable_long

    def reward_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurableBigDecimal = reward_configurable_big_decimal

    @overload
    def impact(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B], int]) -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
               match_weigher: Callable[[A, B], int]) -> 'Constraint':
        ...

    def impact(self, *args) -> 'Constraint':
        """Positively or negatively impact the Score: add the constraint_weight for each match
        (multiplied by an optional match_weigher function).

        Use penalize(...) or reward(...) instead, unless this constraint can both have positive and negative weights.

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use impact_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - impact(constraint_name: str, constraint_weight: Score)

            - impact(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - impact(constraint_name: str, constraint_weight: Score, match_weigher: (A, B) -> int)

            - impact(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.impact(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score)
        else:
            return self.delegate.impact(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score,
                                        to_int_function_cast(constraint_info.impact_function, self.a_type, self.b_type))

    def impact_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactLong = impact_long

    def impact_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactBigDecimal = impact_big_decimal

    def impact_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurable = impact_configurable

    def impact_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurableLong = impact_configurable_long

    def impact_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurableBigDecimal = impact_configurable_big_decimal


class PythonTriConstraintStream(Generic[A, B, C]):
    delegate: 'TriConstraintStream[A,B,C]'
    package: str
    a_type: Type[A]
    b_type: Type[B]
    c_type: Type[C]
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: 'TriConstraintStream[A,B,C]', package: str, a_type: Type[A], b_type: Type[B],
                 c_type: Type[C]):
        self.delegate = delegate
        self.package = package
        self.a_type = a_type
        self.b_type = b_type
        self.c_type = c_type

    def get_constraint_factory(self):
        return PythonConstraintFactory(self.delegate.getConstraintFactory())

    getConstraintFactory = get_constraint_factory

    def filter(self, predicate: Callable[[A, B, C], bool]) -> 'PythonTriConstraintStream[A,B,C]':
        """Exhaustively test each fact against the predicate and match if the predicate returns True.

        :param predicate:

        :return:
        """
        translated_predicate = predicate_cast(predicate, self.a_type, self.b_type, self.c_type)
        return PythonTriConstraintStream(self.delegate.filter(translated_predicate), self.package, self.a_type,
                                         self.b_type, self.c_type)

    def join(self, unistream_or_type: Union[PythonUniConstraintStream[D_], Type[D_]],
             *joiners: 'QuadJoiner[A, B, C, D_]') -> 'PythonQuadConstraintStream[A,B,C,D_]':
        """Create a new QuadConstraintStream for every combination of A, B and C that satisfy all specified joiners.

        :param unistream_or_type:

        :param joiners:

        :return:
        """
        d_type = None
        if isinstance(unistream_or_type, PythonUniConstraintStream):
            d_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            d_type = get_class(unistream_or_type)
            unistream_or_type = d_type

        join_result = self.delegate.join(unistream_or_type, extract_joiners(joiners, self.a_type, self.b_type,
                                                                            self.c_type, d_type))
        return PythonQuadConstraintStream(join_result, self.package, self.a_type, self.b_type, self.c_type, d_type)

    def if_exists(self, item_type: Type[D_], *joiners: 'QuadJoiner[A, B, C, D_]') ->\
            'PythonTriConstraintStream[A,B,C]':
        """Create a new TriConstraintStream for every A, B, C where D exists that satisfy all specified joiners.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonTriConstraintStream(self.delegate.ifExists(item_type, extract_joiners(joiners, self.a_type,
                                                                                           self.b_type, self.c_type,
                                                                                           item_type)), self.package,
                                         self.a_type, self.b_type, self.c_type)

    ifExists = if_exists

    def if_exists_including_null_vars(self, item_type: Type[D_], *joiners: 'QuadJoiner[A, B, C, D_]') ->\
            'PythonTriConstraintStream[A,B,C]':
        """Create a new TriConstraintStream for every A, B where D exists that satisfy all specified joiners.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonTriConstraintStream(self.delegate.ifExistsIncludingNullVars(item_type, extract_joiners(joiners,
                                                                                                            self.a_type,
                                                                                                            self.b_type,
                                                                                                            self.c_type,
                                                                                                            item_type)),
                                         self.package, self.a_type, self.b_type, self.c_type)

    ifExistsIncludingNullVars = if_exists_including_null_vars

    def if_not_exists(self, item_type: Type[D_], *joiners: 'QuadJoiner[A, B, C, D_]') ->\
            'PythonTriConstraintStream[A,B,C]':
        """Create a new TriConstraintStream for every A, B, C where there does not exist a D where all specified joiners
        are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonTriConstraintStream(self.delegate.ifNotExists(item_type, extract_joiners(joiners,
                                                                                              self.a_type,
                                                                                              self.b_type,
                                                                                              self.c_type,
                                                                                              item_type)),
                                         self.package, self.a_type, self.b_type, self.c_type)

    ifNotExists = if_not_exists

    def if_not_exists_including_null_vars(self, item_type: Type[D_], *joiners: 'QuadJoiner[A, B, C, D_]') -> \
            'PythonTriConstraintStream[A,B,C]':
        """Create a new TriConstraintStream for every A, B, C where there does not exist a D where all specified joiners
        are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonTriConstraintStream(self.delegate.ifNotExistsIncludingNullVars(item_type,
                                                                                    extract_joiners(joiners,
                                                                                                    self.a_type,
                                                                                                    self.b_type,
                                                                                                    self.c_type,
                                                                                                    item_type)),
                                         self.package, self.a_type, self.b_type, self.c_type)

    ifNotExistsIncludingNullVars = if_not_exists_including_null_vars

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C], A_]) -> 'PythonUniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, collector: 'TriConstraintCollector[A, B, C, Any, A_]') -> 'PythonUniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_]) -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C], A_], collector: 'TriConstraintCollector[A, B, C, Any, B_]') -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_collector: 'TriConstraintCollector[A, B, C, Any, A_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, B_]') -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 third_key_mapping: Callable[[A, B, C], C_]) -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 collector: 'TriConstraintCollector[A, B, C, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C], A_], first_collector: 'TriConstraintCollector[A, B, C, Any, B_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_collector: 'TriConstraintCollector[A, B, C, Any, A_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, B_]',
                 third_collector: 'TriConstraintCollector[A, B, C, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 third_key_mapping: Callable[[A, B, C], C_], fourth_key_mapping: Callable[[A, B, C], D_]) -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 third_key_mapping: Callable[[A, B, C], C_], collector: 'TriConstraintCollector[A, B, C, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 first_collector: 'TriConstraintCollector[A, B, C, Any, C_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C], A_], first_collector: 'TriConstraintCollector[A, B, C, Any, B_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, C_]',
                 third_collector: 'TriConstraintCollector[A, B, C, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_collector: 'TriConstraintCollector[A, B, C, Any, A_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, B_]',
                 third_collector: 'TriConstraintCollector[A, B, C, Any, C_]',
                 fourth_collector: 'TriConstraintCollector[A, B, C, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    def group_by(self, *args):
        """Collect items into groups using the group_key_function(s) and optionally aggregate the group's items into a
        result.

        The syntax of group_by is zero to four group_key functions, followed by zero to four collectors. At most
        four arguments can be passed to group_by.

        If no group_key function is passed to group_by, all items in the stream are aggregated into a single result
        by the passed constraint collectors.

        Examples:

            - # count the items in this stream; returns Uni[int]

              group_by(ConstraintCollectors.count_tri())

            - # count the number of shifts each employee has; returns Bi[Employee]

              group_by(lambda shift, _, _: shift.employee, ConstraintCollectors.count_tri())

            - # count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

              group_by(lambda shift, _, _: shift.employee, lambda shift, _, _: shift.date,
              ConstraintCollectors.count_tri())

            - # count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

              group_by(lambda shift, _, _: shift.employee, lambda shift, _, _: shift.date,
              ConstraintCollectors.count_tri())

            - # get the dates of the first and last shift of each employee; returns Tri[Employee, datetime.date,
              datetime.date]

              group_by(lambda shift, _, _: shift.employee,
              ConstraintCollectors.min(lambda shift, _, _: shift.date)
              ConstraintCollectors.max(lambda shift, _, _: shift.date))

        The type of stream returned depends on the number of arguments passed:

        - 1 -> UniConstraintStream

        - 2 -> BiConstraintStream

        - 3 -> TriConstraintStream

        - 4 -> QuadConstraintStream

        :param args:

        :return:
        """
        return perform_group_by(self.delegate, self.package, args, self.a_type, self.b_type, self.c_type)

    groupBy = group_by

    def map(self, mapping_function: Callable[[A,B,C], A_]) -> 'PythonUniConstraintStream[A_]':
        """Transforms the stream in such a way that tuples are remapped using the given function.

        :param mapping_function:

        :return:
        """
        translated_function = function_cast(mapping_function, self.a_type, self.b_type, self.c_type)
        return PythonUniConstraintStream(self.delegate.map(translated_function), self.package,
                                         JClass('java.lang.Object'))

    def flatten_last(self, flattening_function: Callable[[C], C_]) -> 'PythonTriConstraintStream[A,B,C_]':
        """Takes each tuple and applies a mapping on it, which turns the tuple into an Iterable.

        :param flattening_function:

        :return:
        """
        translated_function = function_cast(flattening_function, self.c_type)
        return PythonTriConstraintStream(self.delegate.flattenLast(translated_function), self.package,
                                         self.a_type, self.b_type, JClass('java.lang.Object'))

    flattenLast = flatten_last

    def distinct(self) -> 'PythonTriConstraintStream[A, B, C]':
        """Transforms the stream in such a way that all the tuples going through it are distinct.

        :return:
        """
        return PythonTriConstraintStream(self.delegate.distinct(), self.package, self.a_type,
                                         self.b_type, self.c_type)

    @overload
    def penalize(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B, C], int]) -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
                 match_weigher: Callable[[A, B, C], int]) -> 'Constraint':
        ...

    def penalize(self, *args) -> 'Constraint':
        """Negatively impact the Score: subtract the constraint_weight for each match (multiplied by an optional
        match_weigher function).

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use penalize_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - penalize(constraint_name: str, constraint_weight: Score)

            - penalize(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - penalize(constraint_name: str, constraint_weight: Score, match_weigher: (A, B, C) -> int)

            - penalize(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B, C) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.penalize(constraint_info.constraint_package, constraint_info.constraint_name,
                                          constraint_info.score)
        else:
            return self.delegate.penalize(constraint_info.constraint_package, constraint_info.constraint_name,
                                          constraint_info.score,
                                          to_int_function_cast(constraint_info.impact_function, self.a_type,
                                                               self.b_type,
                                                               self.c_type))

    def penalize_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeLong = penalize_long

    def penalize_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeBigDecimal = penalize_big_decimal

    def penalize_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurable = penalize_configurable

    def penalize_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurableLong = penalize_configurable_long

    def penalize_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurableBigDecimal = penalize_configurable_big_decimal

    @overload
    def reward(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B, C], int]) -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
               match_weigher: Callable[[A, B, C], int]) -> 'Constraint':
        ...

    def reward(self, *args) -> 'Constraint':
        """Positively impact the Score: add the constraint_weight for each match (multiplied by an optional
        match_weigher function).

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use reward_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - reward(constraint_name: str, constraint_weight: Score)

            - reward(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - reward(constraint_name: str, constraint_weight: Score, match_weigher: (A, B, C) -> int)

            - reward(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B, C) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.reward(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score)
        else:
            return self.delegate.reward(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score,
                                        to_int_function_cast(constraint_info.impact_function, self.a_type, self.b_type,
                                                             self.c_type))

    def reward_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardLong = reward_long

    def reward_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardBigDecimal = reward_big_decimal

    def reward_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurable = reward_configurable

    def reward_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurableLong = reward_configurable_long

    def reward_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurableBigDecimal = reward_configurable_big_decimal

    @overload
    def impact(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B, C], int]) -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
               match_weigher: Callable[[A, B, C], int]) -> 'Constraint':
        ...

    def impact(self, *args) -> 'Constraint':
        """Positively or negatively impact the Score: add the constraint_weight for each match
        (multiplied by an optional match_weigher function).

        Use penalize(...) or reward(...) instead, unless this constraint can both have positive and negative weights.

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use impact_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - impact(constraint_name: str, constraint_weight: Score)

            - impact(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - impact(constraint_name: str, constraint_weight: Score, match_weigher: (A, B, C) -> int)

            - impact(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B, C) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.impact(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score)
        else:
            return self.delegate.impact(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score,
                                        to_int_function_cast(constraint_info.impact_function, self.a_type, self.b_type,
                                                             self.c_type))

    def impact_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactLong = impact_long

    def impact_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactBigDecimal = impact_big_decimal

    def impact_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurable = impact_configurable

    def impact_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurableLong = impact_configurable_long

    def impact_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurableBigDecimal = impact_configurable_big_decimal


class PythonQuadConstraintStream(Generic[A, B, C, D]):
    delegate: 'QuadConstraintStream[A,B,C,D]'
    package: str
    a_type: Type[A]
    b_type: Type[B]
    c_type: Type[C]
    d_type: Type[D]
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: 'QuadConstraintStream[A,B,C,D]', package: str, a_type: Type[A], b_type: Type[B],
                 c_type: Type[C], d_type: Type[D]):
        self.delegate = delegate
        self.package = package
        self.a_type = a_type
        self.b_type = b_type
        self.c_type = c_type
        self.d_type = d_type

    def get_constraint_factory(self):
        return PythonConstraintFactory(self.delegate.getConstraintFactory())

    getConstraintFactory = get_constraint_factory

    def filter(self, predicate: Callable[[A,B,C,D], bool]) -> 'PythonQuadConstraintStream[A,B,C,D]':
        """Exhaustively test each fact against the predicate and match if the predicate returns True.

        :param predicate:

        :return:
        """
        translated_predicate = predicate_cast(predicate, self.a_type, self.b_type, self.c_type, self.d_type)
        return PythonQuadConstraintStream(self.delegate.filter(translated_predicate), self.package, self.a_type,
                                          self.b_type, self.c_type, self.d_type)

    def if_exists(self, item_type: Type[E_], *joiners: 'PentaJoiner[A, B, C, D, E_]') ->\
            'PythonQuadConstraintStream[A,B,C,D]':
        """Create a new QuadConstraintStream for every A, B, C, D where E exists that satisfy all specified joiners.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonQuadConstraintStream(self.delegate.ifExists(item_type, extract_joiners(joiners,
                                                                                            self.a_type,
                                                                                            self.b_type,
                                                                                            self.c_type,
                                                                                            self.d_type,
                                                                                            item_type)),
                                          self.package, self.a_type, self.b_type, self.c_type, self.d_type)

    ifExists = if_exists

    def if_exists_including_null_vars(self, item_type: Type[E_], *joiners: 'PentaJoiner[A, B, C, D, E_]') ->\
            'PythonQuadConstraintStream[A,B,C,D]':
        """Create a new QuadConstraintStream for every A, B, C, D where E exists that satisfy all specified joiners.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonQuadConstraintStream(self.delegate.ifExistsIncludingNullVars(item_type,
                                                                                  extract_joiners(joiners,
                                                                                                  self.a_type,
                                                                                                  self.b_type,
                                                                                                  self.c_type,
                                                                                                  self.d_type,
                                                                                                  item_type)),
                                          self.package, self.a_type, self.b_type, self.c_type, self.d_type)

    ifExistsIncludingNullVars = if_exists_including_null_vars

    def if_not_exists(self, item_type: Type[E_], *joiners: 'PentaJoiner[A, B, C, D, E_]') ->\
            'PythonQuadConstraintStream[A,B,C,D]':
        """Create a new QuadConstraintStream for every A, B, C, D where there does not exist an E where all specified
        joiners are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonQuadConstraintStream(self.delegate.ifNotExists(item_type, extract_joiners(joiners,
                                                                                               self.a_type,
                                                                                               self.b_type,
                                                                                               self.c_type,
                                                                                               self.d_type,
                                                                                               item_type)),
                                          self.package, self.a_type, self.b_type, self.c_type, self.d_type)

    ifNotExists = if_not_exists

    def if_not_exists_including_null_vars(self, item_type: Type[E_], *joiners: 'PentaJoiner[A, B, C, D, E_]') -> \
            'PythonQuadConstraintStream[A,B,C,D]':
        """Create a new QuadConstraintStream for every A, B, C, D where there does not exist an E where all specified
        joiners are satisfied.

        :param item_type:

        :param joiners:

        :return:
        """
        item_type = get_class(item_type)
        return PythonQuadConstraintStream(self.delegate.ifNotExistsIncludingNullVars(item_type,
                                                                                     extract_joiners(joiners,
                                                                                                     self.a_type,
                                                                                                     self.b_type,
                                                                                                     self.c_type,
                                                                                                     self.d_type,
                                                                                                     item_type)),
                                          self.package, self.a_type, self.b_type, self.c_type, self.d_type)

    ifNotExistsIncludingNullVars = if_not_exists_including_null_vars

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C, D], A_]) -> 'PythonUniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, collector: 'QuadConstraintCollector[A, B, C, D, Any, A_]') -> 'PythonUniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_]) -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C, D], A_], collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]') -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_collector: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]') -> 'PythonBiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 third_key_mapping: Callable[[A, B, C, D], C_]) -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C, D], A_], first_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_collector: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                 third_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]') -> 'PythonTriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 third_key_mapping: Callable[[A, B, C, D], C_], fourth_key_mapping: Callable[[A, B, C, D], D_]) -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 third_key_mapping: Callable[[A, B, C, D], C_], collector: 'QuadConstraintCollector[A, B, C, D, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 first_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C, D], A_], first_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                 third_collector: 'QuadConstraintCollector[A, B, C, D, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_collector: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                 third_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                 fourth_collector: 'QuadConstraintCollector[A, B, C, D, Any, D_]') -> 'PythonQuadConstraintStream[A_, B_, C_, D_]':
        ...

    def group_by(self, *args):
        """Collect items into groups using the group_key_function(s) and optionally aggregate the group's items into a
        result.

        The syntax of group_by is zero to four group_key functions, followed by zero to four collectors. At most
        four arguments can be passed to group_by.

        If no group_key function is passed to group_by, all items in the stream are aggregated into a single result
        by the passed constraint collectors.

        Examples:

            - # count the items in this stream; returns Uni[int]

              group_by(ConstraintCollectors.count_quad())

            - # count the number of shifts each employee has; returns Bi[Employee]

              group_by(lambda shift, _, _, _: shift.employee, ConstraintCollectors.count_quad())

            - # count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

              group_by(lambda shift, _, _, _: shift.employee, lambda shift, _, _, _: shift.date,
              ConstraintCollectors.count_quad())

            - # count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

              group_by(lambda shift, _, _, _: shift.employee, lambda shift, _, _, _: shift.date,
              ConstraintCollectors.count_quad())

            - # get the dates of the first and last shift of each employee; returns Tri[Employee, datetime.date,
              datetime.date]

              group_by(lambda shift, _, _, _: shift.employee,
              ConstraintCollectors.min(lambda shift, _, _, _: shift.date)
              ConstraintCollectors.max(lambda shift, _, _, _: shift.date))

        The type of stream returned depends on the number of arguments passed:

        - 1 -> UniConstraintStream

        - 2 -> BiConstraintStream

        - 3 -> TriConstraintStream

        - 4 -> QuadConstraintStream

        :param args:

        :return:
        """
        return perform_group_by(self.delegate, self.package, args, self.a_type, self.b_type, self.c_type, self.d_type)

    groupBy = group_by

    def map(self, mapping_function: Callable[[A,B,C,D], A_]) -> 'PythonUniConstraintStream[A_]':
        """Transforms the stream in such a way that tuples are remapped using the given function.
        :param mapping_function:

        :return:
        """
        translated_function = function_cast(mapping_function, self.a_type, self.b_type, self.c_type, self.d_type)
        return PythonUniConstraintStream(self.delegate.map(translated_function), self.package,
                                         JClass('java.lang.Object'))

    def flatten_last(self, flattening_function) -> 'PythonQuadConstraintStream[A,B,C,D]':
        """Takes each tuple and applies a mapping on it, which turns the tuple into an Iterable.

        :param flattening_function:

        :return:
        """
        translated_function = function_cast(flattening_function, self.d_type)
        return PythonQuadConstraintStream(self.delegate.flattenLast(translated_function), self.package,
                                          self.a_type, self.b_type, self.c_type, JClass('java.lang.Object'))

    flattenLast = flatten_last

    def distinct(self) -> 'PythonQuadConstraintStream[A,B,C,D]':
        """Transforms the stream in such a way that all the tuples going through it are distinct.

        :return:
        """
        return PythonQuadConstraintStream(self.delegate.distinct(), self.package, self.a_type,
                                          self.b_type, self.c_type, self.d_type)

    @overload
    def penalize(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B, C, D], int]) -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def penalize(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
                 match_weigher: Callable[[A, B, C, D], int]) -> 'Constraint':
        ...

    def penalize(self, *args) -> 'Constraint':
        """Negatively impact the Score: subtract the constraint_weight for each match (multiplied by an optional
        match_weigher function).

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use penalize_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - penalize(constraint_name: str, constraint_weight: Score)

            - penalize(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - penalize(constraint_name: str, constraint_weight: Score, match_weigher: (A, B, C, D) -> int)

            - penalize(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B, C, D) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.penalize(constraint_info.constraint_package, constraint_info.constraint_name,
                                          constraint_info.score)
        else:
            return self.delegate.penalize(constraint_info.constraint_package, constraint_info.constraint_name,
                                          constraint_info.score,
                                          to_int_function_cast(constraint_info.impact_function, self.a_type,
                                                               self.b_type, self.c_type, self.d_type))

    def penalize_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeLong = penalize_long

    def penalize_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeBigDecimal = penalize_big_decimal

    def penalize_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurable = penalize_configurable

    def penalize_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurableLong = penalize_configurable_long

    def penalize_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    penalizeConfigurableBigDecimal = penalize_configurable_big_decimal

    @overload
    def reward(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B, C, D], int]) -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def reward(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
               match_weigher: Callable[[A, B, C, D], int]) -> 'Constraint':
        ...

    def reward(self, *args) -> 'Constraint':
        """Positively impact the Score: add the constraint_weight for each match (multiplied by an optional
        match_weigher function).

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use reward_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - reward(constraint_name: str, constraint_weight: Score)

            - reward(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - reward(constraint_name: str, constraint_weight: Score, match_weigher: (A, B, C, D) -> int)

            - reward(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B, C, D) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.reward(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score)
        else:
            return self.delegate.reward(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score,
                                        to_int_function_cast(constraint_info.impact_function, self.a_type, self.b_type,
                                                             self.c_type, self.d_type))

    def reward_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardLong = reward_long

    def reward_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardBigDecimal = reward_big_decimal

    def reward_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurable = reward_configurable

    def reward_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurableLong = reward_configurable_long

    def reward_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    rewardConfigurableBigDecimal = reward_configurable_big_decimal

    @overload
    def impact(self, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_name: str, constraint_weight: 'Score', match_weigher: Callable[[A, B, C], int]) -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score') -> \
            'Constraint':
        ...

    @overload
    def impact(self, constraint_package: str, constraint_name: str, constraint_weight: 'Score',
               match_weigher: Callable[[A, B, C, D], int]) -> 'Constraint':
        ...

    def impact(self, *args) -> 'Constraint':
        """Positively or negatively impact the Score: add the constraint_weight for each match
        (multiplied by an optional match_weigher function).

        Use penalize(...) or reward(...) instead, unless this constraint can both have positive and negative weights.

        To avoid hard-coding the constraint_weight, to allow end-users to tweak it,
        use impact_configurable and a ConstraintConfiguration instead.

        There are four overloads available for this method:

            - impact(constraint_name: str, constraint_weight: Score)

            - impact(constraint_package: str, constraint_name: str, constraint_weight: Score)

            - impact(constraint_name: str, constraint_weight: Score, match_weigher: (A, B, C, D) -> int)

            - impact(constraint_package: str, constraint_name: str, constraint_weight: Score,
              match_weigher: (A, B, C, D) -> int)

        The Constraint.getConstraintPackage() defaults to the package of the PlanningSolution class.

        :return:
        """
        constraint_info = extract_constraint_info(self.package, args)
        if constraint_info.impact_function is None:
            return self.delegate.impact(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score)
        else:
            return self.delegate.impact(constraint_info.constraint_package, constraint_info.constraint_name,
                                        constraint_info.score,
                                        to_int_function_cast(constraint_info.impact_function, self.a_type, self.b_type,
                                                             self.c_type, self.d_type))

    def impact_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactLong = impact_long

    def impact_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactBigDecimal = impact_big_decimal

    def impact_configurable(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurable = impact_configurable

    def impact_configurable_long(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurableLong = impact_configurable_long

    def impact_configurable_big_decimal(self, *args) -> 'Constraint':
        raise NotImplementedError

    impactConfigurableBigDecimal = impact_configurable_big_decimal
