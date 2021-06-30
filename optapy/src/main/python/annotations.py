import java
from .optaplanner_java_interop import generatePlanningEntityClass, generateProblemFactClass, generatePlanningSolutionClass, generateConstraintProviderClass

PythonWrapperGenerator = java.type("org.optaplanner.optapy.PythonWrapperGenerator")

JavaPlanningId = java.type("org.optaplanner.core.api.domain.lookup.PlanningId")

class MyAnnotation:
    def __init__(self, annotationType):
        self.__annotationType = annotationType
    def annotationType(self):
        return self.__annotationType

class MyPlanningId(MyAnnotation):
    def __init__(self):
        super().__init__(JavaPlanningId)

def PlanningId(getterFunction):
    getterFunction.__optaplannerPlanningId = MyPlanningId()
    return getterFunction

JavaPlanningVariable = java.type("org.optaplanner.core.api.domain.variable.PlanningVariable")
PlanningVariableGraphType = java.type("org.optaplanner.core.api.domain.variable.PlanningVariableGraphType")

class MyPlanningVariable(MyAnnotation):
    def __init__(self, valueRangeProviderRefs, nullable, graphType, strengthComparatorClass, strengthWeightFactoryClass):
        super().__init__(JavaPlanningVariable)
        self.__valueRangeProviderRefs = valueRangeProviderRefs
        self.__nullable = nullable
        self.__graphType = graphType
        self.__strengthComparatorClass = strengthComparatorClass
        self.__strengthWeightFactoryClass = strengthWeightFactoryClass
    def valueRangeProviderRefs(self):
        return self.__valueRangeProviderRefs
    def nullable(self):
        return self.__nullable
    def graphType(self):
        return self.__graphType
    def strengthComparatorClass(self):
        return self.__strengthComparatorClass
    def strengthWeightFactoryClass(self):
        return self.__strengthWeightFactoryClass

def PlanningVariable(type, valueRangeProviderRefs, nullable=False, graphType=None,
                     strengthComparatorClass=None, strengthWeightFactoryClass=None):
    def PlanningVariableFunctionWrapper(variableGetterFunction):
        variableGetterFunction.__optaplannerPlanningVariable = MyPlanningVariable(valueRangeProviderRefs, nullable, graphType, strengthComparatorClass,
                                                                                  strengthWeightFactoryClass)
        variableGetterFunction.__return = type.__javaClass
        return variableGetterFunction
    return PlanningVariableFunctionWrapper

JavaProblemFactCollectionProperty = java.type("org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty")
class MyProblemFactCollectionProperty(MyAnnotation):
    def __init__(self):
        super().__init__(JavaProblemFactCollectionProperty)

def ProblemFactCollectionProperty(type):
    def ProblemFactCollectionPropertyFunctionMapper(getterFunction):
        getterFunction.__return = PythonWrapperGenerator.getArrayClass(type.__javaClass)
        getterFunction.__optaplannerPlanningEntityCollectionProperty = MyProblemFactCollectionProperty()
        return getterFunction
    return ProblemFactCollectionPropertyFunctionMapper

JavaPlanningEntityCollectionProperty = java.type("org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty")
class MyPlanningEntityCollectionProperty(MyAnnotation):
    def __init__(self):
        super().__init__(JavaPlanningEntityCollectionProperty)

def PlanningEntityCollectionProperty(type):
    def PlanningEntityCollectionPropertyFunctionMapper(getterFunction):
        getterFunction.__optaplannerPlanningEntityCollectionProperty = MyPlanningEntityCollectionProperty()
        getterFunction.__return = PythonWrapperGenerator.getArrayClass(type.__javaClass)
        return getterFunction
    return PlanningEntityCollectionPropertyFunctionMapper

JavaValueRangeProvider = java.type("org.optaplanner.core.api.domain.valuerange.ValueRangeProvider")
class MyValueRangeProvider(MyAnnotation):
    def __init__(self, id):
        super().__init__(JavaValueRangeProvider)
        self.__id = id

    def id(self):
        return self.__id

def ValueRangeProvider(id):
    def ValueRangeProviderFunctionWrapper(getterFunction):
        getterFunction.__optaplannerValueRangeProvider = MyValueRangeProvider(id)
        return getterFunction
    return ValueRangeProviderFunctionWrapper

JavaPlanningScore = java.type("org.optaplanner.core.api.domain.solution.PlanningScore")

class MyPlanningScore(MyAnnotation):
    def __init__(self, bendableHardLevelsSize, bendableSoftLevelsSize, scoreDefinitionClass):
        super().__init__(JavaPlanningScore)
        self.__bendableHardLevelsSize = bendableHardLevelsSize
        self.__bendableSoftLevelsSize = bendableSoftLevelsSize
        self.__scoreDefinitionClass = scoreDefinitionClass

    def bendableHardLevelsSize(self):
        return self.__bendableHardLevelsSize

    def bendableSoftLevelsSize(self):
        return self.__bendableSoftLevelsSize

    def scoreDefinitionClass(self):
        return self.__scoreDefinitionClass

def PlanningScore(type,
                  bendableHardLevelsSize=JavaPlanningScore.NO_LEVEL_SIZE,
                  bendableSoftLevelsSize=JavaPlanningScore.NO_LEVEL_SIZE,
                  scoreDefinitionClass=None):
    def PlanningScoreFunctionWrapper(getterFunction):
        getterFunction.__optaplannerPlanningScore = MyPlanningScore(bendableHardLevelsSize, bendableSoftLevelsSize, scoreDefinitionClass)
        getterFunction.__return = type
        return getterFunction
    return PlanningScoreFunctionWrapper

def PlanningEntity(planningEntityClass):
    planningEntityClass.__javaClass = generatePlanningEntityClass(planningEntityClass)
    return planningEntityClass

def ProblemFact(problemFactClass):
    problemFactClass.__javaClass = generateProblemFactClass(problemFactClass)
    return problemFactClass

def PlanningSolution(planningSolutionClass):
    planningSolutionClass.__javaClass = generatePlanningSolutionClass(planningSolutionClass)
    return planningSolutionClass

def ConstraintProvider(constraintProviderFunction):
    constraintProviderFunction.__javaClass = generateConstraintProviderClass(constraintProviderFunction)
    return constraintProviderFunction