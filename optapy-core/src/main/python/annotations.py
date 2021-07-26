from .optaplanner_java_interop import ensure_init, _generate_planning_entity_class, _generate_problem_fact_class, _generate_planning_solution_class, _generate_constraint_provider_class


def planning_id(getter_function):
    ensure_init()
    from org.optaplanner.core.api.domain.lookup import PlanningId as JavaPlanningId
    getter_function.__optaplannerPlanningId = {
        'annotationType': JavaPlanningId
    }
    return getter_function


def planning_variable(variable_type, value_range_provider_refs, nullable=False, graph_type=None,
                      strength_comparator_class=None, strength_weight_factory_class=None):
    def planning_variable_function_wrapper(variableGetterFunction):
        ensure_init()
        from org.optaplanner.core.api.domain.variable import PlanningVariable as JavaPlanningVariable
        variableGetterFunction.__optaplannerPlanningVariable = {
            'annotationType': JavaPlanningVariable,
            'valueRangeProviderRefs': value_range_provider_refs,
            'nullable': nullable,
            'graphType': graph_type,
            'strengthComparatorClass': strength_comparator_class,
            'strengthWeightFactoryClass': strength_weight_factory_class
        }
        variableGetterFunction.__return = variable_type.__javaClass
        return variableGetterFunction
    return planning_variable_function_wrapper


def problem_fact_collection_property(fact_type):
    def problem_fact_collection_property_function_mapper(getter_function):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator
        from org.optaplanner.core.api.domain.solution import ProblemFactCollectionProperty as JavaProblemFactCollectionProperty
        getter_function.__return = PythonWrapperGenerator.getArrayClass(fact_type.__javaClass)
        getter_function.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaProblemFactCollectionProperty
        }
        return getter_function
    return problem_fact_collection_property_function_mapper


def planning_entity_collection_property(entity_type):
    def planning_entity_collection_property_function_mapper(getter_function):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator
        from org.optaplanner.core.api.domain.solution import PlanningEntityCollectionProperty as JavaPlanningEntityCollectionProperty
        getter_function.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaPlanningEntityCollectionProperty
        }
        getter_function.__return = PythonWrapperGenerator.getArrayClass(entity_type.__javaClass)
        return getter_function
    return planning_entity_collection_property_function_mapper


def value_range_provider(range_id):
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
    def planning_score_function_wrapper(getterFunction):
        ensure_init()
        from org.optaplanner.core.api.domain.solution import PlanningScore as JavaPlanningScore
        getterFunction.__optaplannerPlanningScore = {
            'annotationType': JavaPlanningScore,
            'bendableHardLevelsSize': bendable_hard_levels_size,
            'bendableSoftLevelsSize': bendable_soft_levels_size,
            'scoreDefinitionClass': score_definition_class
        }
        getterFunction.__return = score_type
        return getterFunction
    return planning_score_function_wrapper


def planning_entity(entity_class):
    ensure_init()
    entity_class.__javaClass = _generate_planning_entity_class(entity_class)
    return entity_class


def problem_fact(fact_class):
    ensure_init()
    fact_class.__javaClass = _generate_problem_fact_class(fact_class)
    return fact_class


def planning_solution(planning_solution_class):
    ensure_init()
    planning_solution_class.__javaClass = _generate_planning_solution_class(planning_solution_class)
    return planning_solution_class


def constraint_provider(constraint_provider_function):
    ensure_init()
    constraint_provider_function.__javaClass = _generate_constraint_provider_class(constraint_provider_function)
    return constraint_provider_function
