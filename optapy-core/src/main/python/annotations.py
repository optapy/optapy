from .optaplanner_java_interop import ensure_init, generatePlanningEntityClass, generateProblemFactClass, generatePlanningSolutionClass, generateConstraintProviderClass

def PlanningId(getterFunction):
    ensure_init()
    from org.optaplanner.core.api.domain.lookup import PlanningId as JavaPlanningId
    getterFunction.__optaplannerPlanningId = {
        'annotationType': JavaPlanningId
    }
    return getterFunction

def PlanningVariable(type, valueRangeProviderRefs, nullable=False, graphType=None,
                     strengthComparatorClass=None, strengthWeightFactoryClass=None):
    def PlanningVariableFunctionWrapper(variableGetterFunction):
        ensure_init()
        from org.optaplanner.core.api.domain.variable import PlanningVariable as JavaPlanningVariable
        variableGetterFunction.__optaplannerPlanningVariable = {
            'annotationType': JavaPlanningVariable,
            'valueRangeProviderRefs': valueRangeProviderRefs,
            'nullable': nullable,
            'graphType': graphType,
            'strengthComparatorClass': strengthComparatorClass,
            'strengthWeightFactoryClass': strengthWeightFactoryClass
        }
        variableGetterFunction.__return = type.__javaClass
        return variableGetterFunction
    return PlanningVariableFunctionWrapper

def ProblemFactCollectionProperty(type):
    def ProblemFactCollectionPropertyFunctionMapper(getterFunction):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator
        from org.optaplanner.core.api.domain.solution import ProblemFactCollectionProperty as JavaProblemFactCollectionProperty
        getterFunction.__return = PythonWrapperGenerator.getArrayClass(type.__javaClass)
        getterFunction.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaProblemFactCollectionProperty
        }
        return getterFunction
    return ProblemFactCollectionPropertyFunctionMapper

def PlanningEntityCollectionProperty(type):
    def PlanningEntityCollectionPropertyFunctionMapper(getterFunction):
        ensure_init()
        from org.optaplanner.optapy import PythonWrapperGenerator
        from org.optaplanner.core.api.domain.solution import PlanningEntityCollectionProperty as JavaPlanningEntityCollectionProperty
        getterFunction.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaPlanningEntityCollectionProperty
        }
        getterFunction.__return = PythonWrapperGenerator.getArrayClass(type.__javaClass)
        return getterFunction
    return PlanningEntityCollectionPropertyFunctionMapper

def ValueRangeProvider(id):
    def ValueRangeProviderFunctionWrapper(getterFunction):
        ensure_init()
        from org.optaplanner.core.api.domain.valuerange import ValueRangeProvider as JavaValueRangeProvider
        getterFunction.__optaplannerValueRangeProvider = {
            'annotationType': JavaValueRangeProvider,
            'id': id
        }
        return getterFunction
    return ValueRangeProviderFunctionWrapper

def PlanningScore(type,
                  bendableHardLevelsSize=None,
                  bendableSoftLevelsSize=None,
                  scoreDefinitionClass=None):
    def PlanningScoreFunctionWrapper(getterFunction):
        ensure_init()
        from org.optaplanner.core.api.domain.solution import PlanningScore as JavaPlanningScore
        getterFunction.__optaplannerPlanningScore = {
            'annotationType': JavaPlanningScore,
            'bendableHardLevelsSize': bendableHardLevelsSize,
            'bendableSoftLevelsSize': bendableSoftLevelsSize,
            'scoreDefinitionClass': scoreDefinitionClass
        }
        getterFunction.__return = type
        return getterFunction
    return PlanningScoreFunctionWrapper

def PlanningEntity(planningEntityClass):
    ensure_init()
    planningEntityClass.__javaClass = generatePlanningEntityClass(planningEntityClass)
    return planningEntityClass

def ProblemFact(problemFactClass):
    ensure_init()
    problemFactClass.__javaClass = generateProblemFactClass(problemFactClass)
    return problemFactClass

def PlanningSolution(planningSolutionClass):
    ensure_init()
    planningSolutionClass.__javaClass = generatePlanningSolutionClass(planningSolutionClass)
    return planningSolutionClass

def ConstraintProvider(constraintProviderFunction):
    ensure_init()
    constraintProviderFunction.__javaClass = generateConstraintProviderClass(constraintProviderFunction)
    return constraintProviderFunction