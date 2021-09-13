from .optaplanner_java_interop import ensure_init, _add_deep_copy_to_class, _generate_planning_entity_class,\
    _generate_problem_fact_class, _generate_planning_solution_class, _generate_constraint_provider_class
from jpype import JImplements

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


def planning_id(getter_function):
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


def planning_variable(variable_type, value_range_provider_refs, nullable=False, graph_type=None,
                      strength_comparator_class=None, strength_weight_factory_class=None):
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
    def planning_variable_function_wrapper(variable_getter_function):
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
        variable_getter_function.__return = variable_type.__javaClass
        return variable_getter_function
    return planning_variable_function_wrapper


def problem_fact_collection_property(fact_type):
    """Specifies that a property on a PlanningSolution class is a Collection of problem facts.

    A problem fact must not change during solving (except through a ProblemFactChange event). The constraints in a
    ConstraintProvider rely on problem facts for ConstraintFactory.from(Class).
    Do not annotate planning entities as problem facts: they are automatically available as facts for
    ConstraintFactory.from(Class).
    """
    def problem_fact_collection_property_function_mapper(getter_function):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator
        from org.optaplanner.core.api.domain.solution import \
            ProblemFactCollectionProperty as JavaProblemFactCollectionProperty
        getter_function.__return = PythonWrapperGenerator.getArrayClass(fact_type.__javaClass)
        getter_function.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaProblemFactCollectionProperty
        }
        return getter_function
    return problem_fact_collection_property_function_mapper


def planning_entity_collection_property(entity_type):
    """Specifies that a property on a PlanningSolution class is a Collection of planning entities.

    Every element in the planning entity collection should have the PlanningEntity annotation. Every element in the
    planning entity collection will be added to the ScoreDirector.
    """
    def planning_entity_collection_property_function_mapper(getter_function):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator
        from org.optaplanner.core.api.domain.solution import \
            PlanningEntityCollectionProperty as JavaPlanningEntityCollectionProperty
        getter_function.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaPlanningEntityCollectionProperty
        }
        getter_function.__return = PythonWrapperGenerator.getArrayClass(entity_type.__javaClass)
        return getter_function
    return planning_entity_collection_property_function_mapper


def value_range_provider(range_id):
    """Provides the planning values that can be used for a PlanningVariable.

    This is specified on a getter which returns a list or ValueRange. A list is implicitly converted to a ValueRange.
    """
    def value_range_provider_function_wrapper(getter_function):
        ensure_init()
        from org.optaplanner.core.api.domain.valuerange import ValueRangeProvider as JavaValueRangeProvider
        getter_function.__optaplannerValueRangeProvider = {
            'annotationType': JavaValueRangeProvider,
            'id': range_id
        }
        return getter_function
    return value_range_provider_function_wrapper


def planning_score(score_type,
                   bendable_hard_levels_size=None,
                   bendable_soft_levels_size=None,
                   score_definition_class=None):
    """Specifies that a property on a PlanningSolution class holds the Score of that solution.

    This property can be null if the PlanningSolution is uninitialized.
    This property is modified by the Solver, every time when the Score of this PlanningSolution has been calculated.

    :param score_type: The type of the score. Should be imported from optapy.types.
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
        getter_function.__return = score_type
        return getter_function
    return planning_score_function_wrapper


def planning_entity(entity_class):
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
    """
    ensure_init()
    out = JImplements('org.optaplanner.optapy.OpaquePythonReference')(entity_class)
    out.__javaClass = _generate_planning_entity_class(entity_class)
    _add_deep_copy_to_class(out)
    return out


def problem_fact(fact_class):
    """Specifies that a class is a problem fact.

    A problem fact must not change during solving (except through a ProblemFactChange event).
    The constraints in a ConstraintProvider rely on problem facts for ConstraintFactory.from(Class).
    Do not annotate a planning entity as a problem fact:
    they are automatically available as facts for ConstraintFactory.from(Class)
    """
    ensure_init()
    out = JImplements('org.optaplanner.optapy.OpaquePythonReference')(fact_class)
    out.__javaClass = _generate_problem_fact_class(fact_class)
    _add_deep_copy_to_class(out)
    return out


def planning_solution(planning_solution_class):
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
    out.__javaClass = _generate_planning_solution_class(planning_solution_class)
    _add_deep_copy_to_class(out)
    return out


def constraint_provider(constraint_provider_function):
    """Marks a function as a ConstraintProvider.

    The function takes a single parameter, the ConstraintFactory, and
    must return a list of Constraints.
    To create a Constraint, start with ConstraintFactory.from(get_class(PythonClass)).
    """
    ensure_init()
    constraint_provider_function.__javaClass = _generate_constraint_provider_class(constraint_provider_function)
    return constraint_provider_function
