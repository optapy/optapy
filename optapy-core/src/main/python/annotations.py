import jpype

from .optaplanner_java_interop import ensure_init, _add_shallow_copy_to_class, _generate_planning_entity_class, \
    _generate_problem_fact_class, _generate_planning_solution_class, _generate_constraint_provider_class, \
    _generate_easy_score_calculator_class, _generate_incremental_score_calculator_class,\
    _generate_variable_listener_class, get_class
from jpype import JImplements, JOverride
from typing import Union, List, Callable, Type, Any, TYPE_CHECKING, TypeVar

if TYPE_CHECKING:
    from org.optaplanner.core.api.solver.change import ProblemChange as _ProblemChange
    from org.optaplanner.core.api.score.stream import Constraint as _Constraint, ConstraintFactory as _ConstraintFactory
    from org.optaplanner.core.api.score import Score as _Score
    from org.optaplanner.core.api.score.calculator import IncrementalScoreCalculator as _IncrementalScoreCalculator
    from org.optaplanner.core.api.domain.valuerange import ValueRange as _ValueRange
    from org.optaplanner.core.api.domain.variable import PlanningVariableGraphType as _PlanningVariableGraphType, \
        VariableListener as _VariableListener, PlanningVariableReference as _PlanningVariableReference

"""
All OptaPlanner Python annotations work like this:

1. Ensure OptaPy is init using ensure_init
2. Import the corresponding Java annotation
3. Set __optaplanner<annotation_name> (ex: __optaplannerPlanningId) on the given function/class
   to a dict containing the following:
       - 'annotationType' -> the imported Java annotation
       - annotation parameter -> parameter value or None if unset
4. Return the modified function/class

For classes, JImplements('org.optaplanner.optapy.OpaquePythonReference')(the_class)
is called and used (which allows __getattr__ to work w/o casting to a Java Proxy).
"""

Solution_ = TypeVar('Solution_')


def planning_id(getter_function: Callable[[], Union[int, str]]) -> Callable[[], Union[int, str]]:
    """Specifies that a bean property is the id to match when locating an externalObject (often from another Thread).

    Used during Move rebasing and in a ProblemFactChange.
    It is specified on a getter of a java bean property of a PlanningEntity class,
    planning value class or any problem fact class.

    The return type can be any Comparable type which overrides Object.equals(Object) and Object.hashCode(),
    and is usually number or str. It must never return a null instance.
    """
    ensure_init()
    from org.optaplanner.core.api.domain.lookup import PlanningId as JavaPlanningId
    getter_function.__optaplannerPlanningId = {
        'annotationType': JavaPlanningId
    }
    return getter_function


def planning_pin(getter_function: Callable[[], bool]) -> Callable[[], bool]:
    """Specifies that a boolean property (or field) of a @planning_entity determines if the planning entity is pinned.
       A pinned planning entity is never changed during planning.
       For example, it allows the user to pin a shift to a specific employee before solving
       and the solver will not undo that, regardless of the constraints.

       The boolean is false if the planning entity is movable and true if the planning entity is pinned.

       It applies to all the planning variables of that planning entity.
       To make individual variables pinned, see https://issues.redhat.com/browse/PLANNER-124

       This is syntactic sugar for @planning_entity(pinning_filter=is_pinned_function),
       which is a more flexible and verbose way to pin a planning entity.

       :type getter_function: Callable[[], bool]
    """
    ensure_init()
    from org.optaplanner.core.api.domain.entity import PlanningPin as JavaPlanningPin
    getter_function.__optaplannerPlanningId = {
        'annotationType': JavaPlanningPin
    }
    getter_function.__optapy_return = get_class(bool)
    return getter_function


def planning_variable(variable_type: Type, value_range_provider_refs: List[str], nullable: bool = False,
                      graph_type: '_PlanningVariableGraphType' = None, strength_comparator_class=None,
                      strength_weight_factory_class=None) -> Callable[[Callable[[], Any]], Callable[[], Any]]:
    """Specifies that a bean property can be changed and should be optimized by the optimization algorithms.

    It is specified on a getter of a java bean property (or directly on a field) of
    a PlanningEntity class. A PlanningVariable MUST be annotated on the getter.
    The getter MUST be named get<X> (ex: getRoom) and has
    a corresponding setter set<X> (ex: setRoom).

    :param variable_type: The type of values this variable can take.
    :param value_range_provider_refs: The value range providers refs that this
                                      planning variable takes values from.
    :param nullable: If this planning variable can take None as a value. Default False.
    :param graph_type: In some use cases, such as Vehicle Routing, planning entities form a specific graph type, as
                       specified by org.optaplanner.core.api.domain.variable.PlanningVariableGraphType; default None.
    :param strength_comparator_class: Allows a collection of planning values for this variable to be sorted by strength.
    :param strength_weight_factory_class: The SelectionSorterWeightFactory alternative for strength_comparator_class.
    """

    def planning_variable_function_wrapper(variable_getter_function: Callable[[], Any]):
        ensure_init()
        from org.optaplanner.core.api.domain.variable import PlanningVariable as JavaPlanningVariable
        variable_getter_function.__optaplannerPlanningVariable = {
            'annotationType': JavaPlanningVariable,
            'valueRangeProviderRefs': value_range_provider_refs,
            'nullable': nullable,
            'graphType': graph_type,
            'strengthComparatorClass': strength_comparator_class,
            'strengthWeightFactoryClass': strength_weight_factory_class
        }
        variable_getter_function.__optapy_return = get_class(variable_type)
        return variable_getter_function

    return planning_variable_function_wrapper


def planning_list_variable(variable_type: Type, value_range_provider_refs: List[str]) -> \
        Callable[[Callable[[], Any]], Callable[[], Any]]:
    """Specifies that a bean property of a list type should be optimized by the optimization
    algorithms. Unlike @planning_variable, the @planning_list_variable tells solver to change elements
    inside the list variable instead of changing the list reference.

    It is specified on a getter of a java bean property (or directly on a field) of a @planning_entity class.
    The getter MUST be named get<X> (ex: get_room_list) and has
    a corresponding setter set<X> (ex: set_room_list).

    The type of the @planning_list_variable annotated bean property must be a List.
    Furthermore, the current implementation works under the assumption that the list variables of all entity instances
    are "disjoint lists":

    - List means that the order of elements inside a list planning variable is significant.
    - Disjoint means that any given pair of entities have no common elements in their list variables.

    In other words, each element from the list variable's value range appears in exactly one entity's list variable.

    Therefore, we refer to such a planning variable as a list variable.

    This makes sense for common use cases, for example the Vehicle Routing Problem or Task Assigning. In both cases
    the order in which customers are visited and tasks are being worked on matters. Also, each customer
    must be visited once and each task must be completed by exactly one employee.

    :param variable_type: The type of values in the list
    :param value_range_provider_refs: The value range providers refs that this
                                      planning list variable takes values from.
    """

    def planning_list_variable_function_wrapper(variable_getter_function: Callable[[], Any]):
        ensure_init()
        from java.util import List as JavaList
        from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
        from org.optaplanner.core.api.domain.variable import PlanningListVariable as JavaPlanningListVariable
        variable_getter_function.__optaplannerPlanningListVariable = {
            'annotationType': JavaPlanningListVariable,
            'valueRangeProviderRefs': value_range_provider_refs,
        }
        variable_getter_function.__optapy_is_planning_clone = True
        variable_getter_function.__optapy_return = JavaList
        variable_getter_function.__optapy_signature = PythonWrapperGenerator.getCollectionSignature(
            JavaList, get_class(variable_type))
        return variable_getter_function

    return planning_list_variable_function_wrapper


def planning_variable_reference(variable_name: str, entity_class: Type = None) -> '_PlanningVariableReference':
    """
    Creates a reference to a planning variable (both genuine variables and shadow variables).

    :param variable_name: The name of the variable
    :param entity_class: The class the variable is on. If not specified, the current class will be use

    :return: A PlanningVariableReference to that variable. Used in custom_shadow_variable decorators.
    :rtype _PlanningVariableReference
    """
    ensure_init()
    from typing import cast
    from org.optaplanner.core.api.domain.variable import PlanningVariableReference as JavaPlanningVariableReference
    return cast(JavaPlanningVariableReference, {
        'annotationType': JavaPlanningVariableReference,
        'variableName': variable_name,
        'entityClass': entity_class
    })


def custom_shadow_variable(shadow_variable_type: Type, *,
                           variable_listener_class: Type['_VariableListener'] = None,
                           variable_listener_ref: '_PlanningVariableReference' = None,
                           sources: List['_PlanningVariableReference'] = None) -> \
        Callable[[], Any]:
    """Specifies that a property is a custom shadow of 1 or more planning_variable's.

    It is specified on a getter of a java bean property (or a field) of a planning_entity class.
    """
    ensure_init()
    def custom_shadow_variable_function_mapper(custom_variable_getter_function: Callable[[], Any]):
        ensure_init()
        from org.optaplanner.core.api.domain.variable import CustomShadowVariable as JavaCustomShadowVariable

        custom_variable_getter_function.__optaplannerCustomShadowVariable = {
            'annotationType': JavaCustomShadowVariable,
            'sources': sources,
            'variableListenerClass': get_class(variable_listener_class),
            'variableListenerRef': variable_listener_ref,
        }
        custom_variable_getter_function.__optapy_return = get_class(shadow_variable_type)
        return custom_variable_getter_function

    return custom_shadow_variable_function_mapper


def index_shadow_variable(anchor_type: Type, source_variable_name: str) -> Callable[[Callable[[], Any]],
                                                                                     Callable[[], Any]]:
    """Specifies that a bean property (or a field) is an index of this planning value in another entity's
    a shadow variable.

    It is specified on a getter of a java bean property (or a field) of a @planning_entity class.

    :param source_variable_name: The source variable must be a list variable.

           When the Solver changes a genuine variable, it adjusts the shadow variable accordingly.
           In practice, the Solver ignores shadow variables (except for consistency housekeeping).
    """

    def index_shadow_variable_function_mapper(index_getter_function: Callable[[], Any]):
        ensure_init()
        from org.optaplanner.core.api.domain.variable import IndexShadowVariable as JavaIndexShadowVariable
        planning_variable_name = source_variable_name
        index_getter_function.__optaplannerIndexShadowVariable = {
            'annotationType': JavaIndexShadowVariable,
            'sourceVariableName': planning_variable_name,
        }
        index_getter_function.__optapy_return = get_class(anchor_type)
        return index_getter_function

    return index_shadow_variable_function_mapper


def anchor_shadow_variable(anchor_type: Type, source_variable_name: str) -> Callable[[Callable[[], Any]],
                                                                                     Callable[[], Any]]:
    """
    Specifies that a bean property (or a field) is the anchor of a chained @planning_variable, which implies it's
    a shadow variable.

    It is specified on a getter of a java bean property (or a field) of a @planning_entity class.

    :param anchor_type: The type of the anchor class.
    :param source_variable_name: The source planning variable is a chained planning variable that leads to the anchor.
           Both the genuine variable and the shadow variable should be consistent:
           if A chains to B, then A must have the same anchor as B (unless B is the anchor).

           When the Solver changes a genuine variable, it adjusts the shadow variable accordingly.
           In practice, the Solver ignores shadow variables (except for consistency housekeeping).
    """

    def anchor_shadow_variable_function_mapper(anchor_getter_function: Callable[[], Any]):
        ensure_init()
        from org.optaplanner.core.api.domain.variable import AnchorShadowVariable as JavaAnchorShadowVariable
        planning_variable_name = source_variable_name
        anchor_getter_function.__optaplannerAnchorShadowVariable = {
            'annotationType': JavaAnchorShadowVariable,
            'sourceVariableName': planning_variable_name,
        }
        anchor_getter_function.__optapy_return = get_class(anchor_type)
        return anchor_getter_function

    return anchor_shadow_variable_function_mapper


def inverse_relation_shadow_variable(source_type: Type, source_variable_name: str,
                                     is_singleton: bool = False) -> Callable[
    [Callable[[], Any]],
    Callable[[], Any]]:
    """
    Specifies that a bean property (or a field) is the inverse of a @planning_variable, which implies it's a shadow
    variable.

    It is specified on a getter of a java bean property (or a field) of a @planning_entity class.

    :param source_type: The planning entity that contains the planning variable that reference this entity.

    :param source_variable_name: In a bidirectional relationship, the shadow side (= the follower side) uses this
           property (and nothing else) to declare for which @planning_variable (= the leader side) it is a shadow.

           Both sides of a bidirectional relationship should be consistent: if A points to B, then B must point to A.

           When the Solver changes a genuine variable, it adjusts the shadow variable accordingly.
           In practice, the Solver ignores shadow variables (except for consistency housekeeping).

    :param is_singleton: True if and only if the shadow variable has a 1-to-{0,1} relationship
                         (i.e. if at most one planning variable can take this value). Defaults to False.
    """

    def inverse_relation_shadow_variable_function_mapper(inverse_relation_getter_function):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator, SelfType  # noqa
        from org.optaplanner.core.api.domain.variable import InverseRelationShadowVariable as \
            JavaInverseRelationShadowVariable
        from java.util import Collection
        the_source_type = source_type
        if the_source_type is None:
            the_source_type = SelfType
        planning_variable_name = source_variable_name
        inverse_relation_getter_function.__optaplannerInverseRelationVariable = {
            'annotationType': JavaInverseRelationShadowVariable,
            'sourceVariableName': planning_variable_name,
        }
        if is_singleton:
            inverse_relation_getter_function.__optapy_return = the_source_type
        else:
            inverse_relation_getter_function.__optapy_return = Collection
            inverse_relation_getter_function.__optapy_signature = PythonWrapperGenerator.getCollectionSignature(
                Collection, get_class(the_source_type))
        return inverse_relation_getter_function

    return inverse_relation_shadow_variable_function_mapper  # noqa


def __verify_is_problem_fact(type, problem_fact_type):
    if type == str or type == int or type == bool:
        # These built-in python types have direct java equivalents
        # and thus can be used in Lists without an illegal item on the stack
        return
    if not hasattr(type, '__optapy_java_class'):
        raise ValueError(f'{type} is not a @{problem_fact_type}. Maybe decorate {type} with '
                         f'@{problem_fact_type}?')


def problem_fact_property(fact_type: Type) -> Callable[[Callable[[], List]],
                                                       Callable[[], List]]:
    """Specifies that a property on a @planning_solution class is a problem fact.

    A problem fact must not change during solving (except through a ProblemFactChange event). The constraints in a
    ConstraintProvider rely on problem facts for ConstraintFactory.from(Class).

    Do not annotate planning entities as problem facts: they are automatically available as facts for
    ConstraintFactory.from(Class).
    """

    def problem_fact_property_function_mapper(getter_function: Callable[[], Any]):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
        from org.optaplanner.core.api.domain.solution import \
            ProblemFactProperty as JavaProblemFactProperty
        __verify_is_problem_fact(fact_type, 'problem_fact')
        getter_function.__optapy_return = get_class(fact_type)
        getter_function.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaProblemFactProperty
        }
        return getter_function

    return problem_fact_property_function_mapper


def problem_fact_collection_property(fact_type: Type) -> Callable[[Callable[[], List]],
                                                                  Callable[[], List]]:
    """Specifies that a property on a @planning_solution class is a Collection of problem facts.

    A problem fact must not change during solving (except through a ProblemFactChange event). The constraints in a
    ConstraintProvider rely on problem facts for ConstraintFactory.from(Class).
    Do not annotate planning entities as problem facts: they are automatically available as facts for
    ConstraintFactory.from(Class).
    """

    def problem_fact_collection_property_function_mapper(getter_function: Callable[[], List]):
        ensure_init()
        from java.util import List as JavaList
        from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
        from org.optaplanner.core.api.domain.solution import \
            ProblemFactCollectionProperty as JavaProblemFactCollectionProperty
        __verify_is_problem_fact(fact_type, 'problem_fact')
        getter_function.__optapy_return = JavaList
        getter_function.__optapy_signature = PythonWrapperGenerator.getCollectionSignature(
            JavaList, get_class(fact_type))
        getter_function.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaProblemFactCollectionProperty
        }
        return getter_function

    return problem_fact_collection_property_function_mapper


def planning_entity_property(entity_type: Type) -> Callable[[Callable[[], List]],
                                                            Callable[[], List]]:
    """Specifies that a property on a PlanningSolution class is a Collection of planning entities.

    Every element in the planning entity collection should have the @planning_entity annotation. Every element in the
    planning entity collection will be added to the ScoreDirector.
    """

    def planning_entity_property_function_mapper(getter_function: Callable[[], List]):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
        from org.optaplanner.core.api.domain.solution import \
            PlanningEntityProperty as JavaPlanningEntityProperty
        __verify_is_problem_fact(entity_type, 'planning_entity')
        getter_function.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaPlanningEntityProperty
        }
        getter_function.__optapy_return = get_class(entity_type)
        return getter_function

    return planning_entity_property_function_mapper


def planning_entity_collection_property(entity_type: Type) -> Callable[[Callable[[], List]],
                                                                       Callable[[], List]]:
    """Specifies that a property on a PlanningSolution class is a Collection of planning entities.

    Every element in the planning entity collection should have the @planning_entity annotation. Every element in the
    planning entity collection will be added to the ScoreDirector.
    """

    def planning_entity_collection_property_function_mapper(getter_function: Callable[[], List]):
        ensure_init()
        from java.util import List as JavaList
        from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
        from org.optaplanner.core.api.domain.solution import \
            PlanningEntityCollectionProperty as JavaPlanningEntityCollectionProperty
        __verify_is_problem_fact(entity_type, 'planning_entity')
        getter_function.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaPlanningEntityCollectionProperty
        }
        getter_function.__optapy_return = JavaList
        getter_function.__optapy_signature = PythonWrapperGenerator.getCollectionSignature(
            JavaList, get_class(entity_type))
        return getter_function

    return planning_entity_collection_property_function_mapper


def value_range_provider(range_id: str, value_range_type: type = list) -> Callable[
    [Callable[[], Union[List, '_ValueRange']]], Callable[[], Union[List, '_ValueRange']]]:
    """Provides the planning values that can be used for a PlanningVariable.

    This is specified on a getter which returns a list or ValueRange. A list is implicitly converted to a ValueRange.

    :param range_id: The id of the value range. Referenced by @planning_variable's value_range_provider_refs
                     parameter. Required.

    :param value_range_type: The type of the value range. Should either be
                             list or a Java class that implements ValueRangeProvider.
    """

    def value_range_provider_function_wrapper(getter_function: Callable[[], Union[List, '_ValueRange']]):
        ensure_init()
        from org.optaplanner.core.api.domain.valuerange import ValueRangeProvider as JavaValueRangeProvider
        from org.optaplanner.optapy import PythonWrapperGenerator, OpaquePythonReference  # noqa

        getter_function.__optaplannerValueRangeProvider = {
            'annotationType': JavaValueRangeProvider,
            'id': range_id
        }
        if not hasattr(getter_function, '__optapy_return'):
            if value_range_type == list:
                getter_function.__optapy_return = PythonWrapperGenerator.getArrayClass(OpaquePythonReference)
            else:
                getter_function.__optapy_return = get_class(value_range_type)
        return getter_function

    return value_range_provider_function_wrapper


def planning_score(score_type: Type['_Score'],
                   bendable_hard_levels_size: int = None,
                   bendable_soft_levels_size: int = None,
                   score_definition_class: Type = None):
    """Specifies that a property on a @planning_solution class holds the Score of that solution.

    This property can be null if the @planning_solution is uninitialized.
    This property is modified by the Solver, every time when the Score of this PlanningSolution has been calculated.

    :param score_type: The type of the score. Should be imported from optapy.types.
    :type score_type: Type[Score]
    :param bendable_hard_levels_size: Required for bendable scores.
                                      For example with 3 hard levels, hard level 0 always outweighs hard level 1 which
                                      always outweighs hard level 2, which outweighs all the soft levels.
    :param bendable_soft_levels_size: Required for bendable scores. For example with 3 soft levels,
                                      soft level 0 always outweighs soft level 1 which always outweighs soft level 2.
    :param score_definition_class: Overrides the default determined ScoreDefinition to implement a custom one.
                                   In most cases, this should not be used.
    """

    def planning_score_function_wrapper(getter_function):
        ensure_init()
        from org.optaplanner.core.api.domain.solution import PlanningScore as JavaPlanningScore
        getter_function.__optaplannerPlanningScore = {
            'annotationType': JavaPlanningScore,
            'bendableHardLevelsSize': bendable_hard_levels_size,
            'bendableSoftLevelsSize': bendable_soft_levels_size,
            'scoreDefinitionClass': score_definition_class
        }
        getter_function.__optapy_return = get_class(score_type)
        return getter_function

    return planning_score_function_wrapper


@JImplements('org.optaplanner.core.api.domain.entity.PinningFilter', deferred=True)
class _PythonPinningFilter:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def accept(self, solution, entity):
        return self.delegate(solution, entity)


def planning_entity(entity_class: Type = None, /, *, pinning_filter: Callable = None) -> Union[Type,
                                                                                               Callable[[Type], Type]]:
    """Specifies that the class is a planning entity. Each planning entity must have at least
    1 PlanningVariable property.

    The class MUST allow passing None to all of __init__ arguments, so it can be cloned.
    (ex: this is allowed:

    def __init__(self, a_list):
        self.a_list = a_list

    this is NOT allowed:

    def __init__(self, a_list):
        self.a_list = a_list
        self.list_length = len(a_list)
    )

    Optional Parameters: @:param pinning_filter: A function that takes the @planning_solution class and an entity,
    and return true if the entity cannot be changed, false otherwise
    """
    ensure_init()
    from org.optaplanner.core.api.domain.entity import PlanningEntity as JavaPlanningEntity
    annotation_data = {
        'annotationType': JavaPlanningEntity,
        'pinningFilter': _PythonPinningFilter(pinning_filter) if pinning_filter is not None else None,
        'difficultyComparatorClass': None,
        'difficultyWeightFactoryClass': None,
    }

    def planning_entity_wrapper(entity_class_argument):
        out = JImplements('org.optaplanner.optapy.OpaquePythonReference')(entity_class_argument)
        out.__optapy_java_class = _generate_planning_entity_class(entity_class_argument, annotation_data)
        out.__optapy_is_planning_clone = True
        _add_shallow_copy_to_class(out)
        return out

    if entity_class:  # Called as @planning_entity
        return planning_entity_wrapper(entity_class)
    else:  # Called as @planning_entity(pinning_filter=some_function)
        return planning_entity_wrapper


def problem_fact(fact_class: Type) -> Type:
    """Specifies that a class is a problem fact.

    A problem fact must not change during solving (except through a ProblemFactChange event).
    The constraints in a ConstraintProvider rely on problem facts for ConstraintFactory.from(Class).
    Do not annotate a planning entity as a problem fact:
    they are automatically available as facts for ConstraintFactory.from(Class)
    """
    ensure_init()
    out = JImplements('org.optaplanner.optapy.OpaquePythonReference')(fact_class)
    out.__optapy_java_class = _generate_problem_fact_class(fact_class)
    return out


def planning_solution(planning_solution_class: Type) -> Type:
    """Specifies that the class is a planning solution (represents a problem and a possible solution of that problem).

    A possible solution does not need to be optimal or even feasible.
    A solution's planning variables might not be initialized (especially when delivered as a problem).

    A solution is mutable. For scalability reasons (to facilitate incremental score calculation),
    the same solution instance (called the working solution per move thread) is continuously modified.
    It's cloned to recall the best solution.

    Each planning solution must have exactly 1 PlanningScore property.
    Each planning solution must have at least 1 PlanningEntityCollectionProperty property.

    The class MUST allow passing None to all of __init__ arguments, so it can be cloned.
    (ex: this is allowed:

    def __init__(self, a_list):
        self.a_list = a_list

    this is NOT allowed:

    def __init__(self, a_list):
        self.a_list = a_list
        self.list_length = len(a_list)
    )
    """
    ensure_init()
    out = JImplements('org.optaplanner.optapy.OpaquePythonReference')(planning_solution_class)
    out.__optapy_java_class = _generate_planning_solution_class(planning_solution_class)
    out.__optapy_is_planning_solution = True
    out.__optapy_is_planning_clone = True
    _add_shallow_copy_to_class(out)
    return out


def deep_planning_clone(planning_clone_object: Union[Type, Callable]):
    """
    Marks a problem fact class as being required to be deep planning cloned.
    Not needed for a @planning_solution or @planning_entity because those are automatically deep cloned.

    It can also mark a property (getter for a field) as being required to be deep planning cloned.
    This is especially useful for list (or dictionary) properties.
    Not needed for a list or map that contain only planning entities or planning solution as values,
    because they are automatically deep cloned.
    Note: If a list or map contains both planning entities and problem facts, this decorator is needed.

    :param planning_clone_object: The class or property that should be deep planning cloned.
    :return: planning_clone_object marked as being required for deep planning clone.
    """
    planning_clone_object.__optapy_is_planning_clone = True
    if isinstance(planning_clone_object, type):
        _add_shallow_copy_to_class(planning_clone_object)
    return planning_clone_object


def constraint_provider(constraint_provider_function: Callable[['_ConstraintFactory'], List['_Constraint']]) -> \
        Callable[['_ConstraintFactory'], List['_Constraint']]:
    """Marks a function as a ConstraintProvider.

    The function takes a single parameter, the ConstraintFactory, and
    must return a list of Constraints.
    To create a Constraint, start with ConstraintFactory.from(get_class(PythonClass)).

    :type constraint_provider_function: Callable[[ConstraintFactory], List[Constraint]]
    :rtype: Callable[[ConstraintFactory], List[Constraint]]
    """
    ensure_init()
    def wrapped_constraint_provider_function(constraint_factory: '_ConstraintFactory'):
        from .constraint_stream import PythonConstraintFactory
        return constraint_provider_function(PythonConstraintFactory(constraint_factory))

    constraint_provider_function.__optapy_java_class = _generate_constraint_provider_class(constraint_provider_function,
                                                                                           wrapped_constraint_provider_function)
    return constraint_provider_function


def easy_score_calculator(easy_score_calculator_function: Callable[[Solution_], '_Score']) -> \
        Callable[[Solution_], '_Score']:
    """Used for easy python Score calculation. This is non-incremental calculation, which is slow.

    The function takes a single parameter, the Solution, and
    must return a Score compatible with the Solution Score Type.
    An implementation must be stateless.

    :type easy_score_calculator_function: Callable[[Solution_], '_Score']
    :rtype: Callable[[Solution_], '_Score']
    """
    ensure_init()
    easy_score_calculator_function.__optapy_java_class = \
        _generate_easy_score_calculator_class(easy_score_calculator_function)
    return easy_score_calculator_function


def incremental_score_calculator(incremental_score_calculator: Type['_IncrementalScoreCalculator']) -> \
        Type['_IncrementalScoreCalculator']:
    """Used for incremental python Score calculation. This is much faster than EasyScoreCalculator
    but requires much more code to implement too.

    Any implementation is naturally stateful.

    The following methods must exist:

    def resetWorkingSolution(self, workingSolution: Solution_);

    def beforeEntityAdded(self, entity: any);

    def afterEntityAdded(self, entity: any);

    def beforeVariableChanged(self, entity: any, variableName: str);

    def afterVariableChanged(self, entity: any, variableName: str);

    def beforeEntityRemoved(self, entity: any);

    def afterEntityRemoved(self, entity: any);

    def calculateScore(self) -> Score;

    If you also want to support Constraint Matches, the following methods need to be added:

    def getConstraintMatchTotals(self)

    def getIndictmentMap(self)

    def resetWorkingSolution(self, workingSolution: Solution_, constraintMatchEnabled=False);
    (A default value must be specified in resetWorkingSolution for constraintMatchEnabled)

    :type incremental_score_calculator: '_IncrementalScoreCalculator'
    :rtype: Type
    """
    ensure_init()
    from org.optaplanner.core.api.score.calculator import IncrementalScoreCalculator, \
        ConstraintMatchAwareIncrementalScoreCalculator
    constraint_match_aware = callable(getattr(incremental_score_calculator, 'getConstraintMatchTotals', None)) and \
        callable(getattr(incremental_score_calculator, 'getIndictmentMap', None))
    methods = ['resetWorkingSolution',
               'beforeEntityAdded',
               'afterEntityAdded',
               'beforeVariableChanged',
               'afterVariableChanged',
               'beforeEntityRemoved',
               'afterEntityRemoved',
               'calculateScore']
    base_interface = IncrementalScoreCalculator
    if constraint_match_aware:
        methods.extend(['getIndictmentMap', 'getConstraintMatchTotals'])
        base_interface = ConstraintMatchAwareIncrementalScoreCalculator

    missing_method_list = []
    for method in methods:
        if not callable(getattr(incremental_score_calculator, method, None)):
            missing_method_list.append(method)
    if len(missing_method_list) != 0:
        raise ValueError(f'The following required methods are missing from @incremental_score_calculator class '
                         f'{incremental_score_calculator}: {missing_method_list}')
    for method in methods:
        method_on_class = getattr(incremental_score_calculator, method, None)
        setattr(incremental_score_calculator, method, JOverride()(method_on_class))

    out = jpype.JImplements(base_interface)(incremental_score_calculator)
    out.__optapy_java_class = _generate_incremental_score_calculator_class(out, constraint_match_aware)
    return out


def variable_listener(variable_listener_class: Type['_VariableListener'] = None, /, *,
                      require_unique_entity_events: bool = False) -> Type['_VariableListener']:
    """Changes shadow variables when a genuine planning variable changes.
    Important: it must only change the shadow variable(s) for which it's configured!
    It should never change a genuine variable or a problem fact.
    It can change its shadow variable(s) on multiple entity instances
    (for example: an arrival_time change affects all trailing entities too).

    It is recommended that implementations be kept stateless.
    If state must be implemented, implementations may need to override the default methods
    resetWorkingSolution(score_director: ScoreDirector) and close().

    The following methods must exist:

    def beforeEntityAdded(score_director: ScoreDirector[Solution_], entity: Entity_);

    def afterEntityAdded(score_director: ScoreDirector[Solution_], entity: Entity_);

    def beforeEntityRemoved(score_director: ScoreDirector[Solution_], entity: Entity_);

    def afterEntityRemoved(score_director: ScoreDirector[Solution_], entity: Entity_);

    def beforeVariableChanged(score_director: ScoreDirector[Solution_], entity: Entity_);

    def afterVariableChanged(score_director: ScoreDirector[Solution_], entity: Entity_);

    If the implementation is stateful, then the following methods should also be defined:

    def resetWorkingSolution(score_director: ScoreDirector)

    def close()

    :param require_unique_entity_events: Set to True to guarantee that each of the before/after methods will only be
                                         called once per entity instance per operation type (add, change or remove).
                                         When set to True, this has a slight performance loss.
                                         When set to False, it's often easier to make the listener implementation
                                         correct and fast.
                                         Defaults to False

    :type variable_listener_class: '_VariableListener'
    :type require_unique_entity_events: bool
    :rtype: Type
    """
    ensure_init()

    def variable_listener_wrapper(the_variable_listener_class):
        from org.optaplanner.core.api.domain.variable import VariableListener
        methods = ['beforeEntityAdded',
                   'afterEntityAdded',
                   'beforeVariableChanged',
                   'afterVariableChanged',
                   'beforeEntityRemoved',
                   'afterEntityRemoved']

        base_interface = VariableListener

        missing_method_list = []
        for method in methods:
            if not callable(getattr(the_variable_listener_class, method, None)):
                missing_method_list.append(method)
        if len(missing_method_list) != 0:
            raise ValueError(f'The following required methods are missing from @variable_listener class '
                             f'{the_variable_listener_class}: {missing_method_list}')
        for method in methods:
            method_on_class = getattr(the_variable_listener_class, method, None)
            setattr(the_variable_listener_class, method, JOverride()(method_on_class))

        method_on_class = getattr(the_variable_listener_class, 'requiresUniqueEntityEvents', None)
        if method_on_class is None:
            def class_requires_unique_entity_events(self):
                return require_unique_entity_events

            setattr(the_variable_listener_class, 'requiresUniqueEntityEvents',
                    JOverride()(class_requires_unique_entity_events))
        else:
            setattr(the_variable_listener_class, 'requiresUniqueEntityEvents',
                    JOverride()(method_on_class))


        method_on_class = getattr(the_variable_listener_class, 'close', None)
        if method_on_class is None:
            def close(self):
                pass

            setattr(the_variable_listener_class, 'close',
                    JOverride()(close))
        else:
            setattr(the_variable_listener_class, 'close',
                    JOverride()(method_on_class))

        method_on_class = getattr(the_variable_listener_class, 'resetWorkingSolution', None)
        if method_on_class is None:
            def reset_working_solution(self, score_director):
                pass

            setattr(the_variable_listener_class, 'resetWorkingSolution',
                    JOverride()(reset_working_solution))
        else:
            setattr(the_variable_listener_class, 'resetWorkingSolution',
                    JOverride()(method_on_class))

        out = jpype.JImplements(base_interface)(the_variable_listener_class)
        out.__optapy_java_class = _generate_variable_listener_class(out)
        return out

    if variable_listener_class:  # Called as @variable_listener
        return variable_listener_wrapper(variable_listener_class)
    else:  # Called as @variable_listener(require_unique_entity_events=True)
        return variable_listener_wrapper


def problem_change(problem_change_class: Type['_ProblemChange']) -> \
        Type['_ProblemChange']:
    """A ProblemChange represents a change in 1 or more planning entities or problem facts of a PlanningSolution.
    Problem facts used by a Solver must not be changed while it is solving,
    but by scheduling this command to the Solver, you can change them when the time is right.

    Note that the Solver clones a PlanningSolution at will. Any change must be done on the problem facts and planning
    entities referenced by the PlanningSolution of the ProblemChangeDirector.

    The following methods must exist:

    def doChange(self, workingSolution: Solution_, problemChangeDirector: ProblemChangeDirector)

    :type problem_change_class: '_ProblemChange'
    :rtype: Type
    """
    ensure_init()
    from org.optaplanner.core.api.solver.change import ProblemChange
    if not callable(getattr(problem_change_class, 'doChange', None)):
        raise ValueError(f'@problem_change annotated class ({problem_change_class}) does not have required method '
                         f'doChange(self, solution, problem_change_director).')

    class_doChange = getattr(problem_change_class, 'doChange', None)
    def wrapper_doChange(self, solution, problem_change_director):
        run_id = id(problem_change_director)
        problem_change_director._set_instance_map(run_id, solution.get__optapy_reference_map())
        class_doChange(self, solution, problem_change_director)
        problem_change_director._unset_instance_map(run_id)

    setattr(problem_change_class, 'doChange', JOverride()(wrapper_doChange))
    out = jpype.JImplements(ProblemChange)(problem_change_class)
    return out
