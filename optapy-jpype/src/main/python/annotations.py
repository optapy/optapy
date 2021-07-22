from .optaplanner_java_interop import generatePlanningEntityClass, generateProblemFactClass, generatePlanningSolutionClass, generateConstraintProviderClass

from org.optaplanner.optapy import PythonWrapperGenerator
from org.optaplanner.core.api.domain.lookup import PlanningId as JavaPlanningId
from org.optaplanner.core.api.domain.variable import PlanningVariable as JavaPlanningVariable
from org.optaplanner.core.api.domain.solution import ProblemFactCollectionProperty as JavaProblemFactCollectionProperty
from org.optaplanner.core.api.domain.solution import PlanningEntityCollectionProperty as JavaPlanningEntityCollectionProperty
from org.optaplanner.core.api.domain.valuerange import ValueRangeProvider as JavaValueRangeProvider
from org.optaplanner.core.api.domain.solution import PlanningScore as JavaPlanningScore

def PlanningId(getterFunction):
    getterFunction.__optaplannerPlanningId = {
        'annotationType': JavaPlanningId
    }
    return getterFunction

def PlanningVariable(type, valueRangeProviderRefs, nullable=False, graphType=None,
                     strengthComparatorClass=None, strengthWeightFactoryClass=None):
    def PlanningVariableFunctionWrapper(variableGetterFunction):
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
        getterFunction.__return = PythonWrapperGenerator.getArrayClass(type.__javaClass)
        getterFunction.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaProblemFactCollectionProperty
        }
        return getterFunction
    return ProblemFactCollectionPropertyFunctionMapper

def PlanningEntityCollectionProperty(type):
    def PlanningEntityCollectionPropertyFunctionMapper(getterFunction):
        getterFunction.__optaplannerPlanningEntityCollectionProperty = {
            'annotationType': JavaPlanningEntityCollectionProperty
        }
        getterFunction.__return = PythonWrapperGenerator.getArrayClass(type.__javaClass)
        return getterFunction
    return PlanningEntityCollectionPropertyFunctionMapper

def ValueRangeProvider(id):
    def ValueRangeProviderFunctionWrapper(getterFunction):
        getterFunction.__optaplannerValueRangeProvider = {
            'annotationType': JavaValueRangeProvider,
            'id': id
        }
        return getterFunction
    return ValueRangeProviderFunctionWrapper

def PlanningScore(type,
                  bendableHardLevelsSize=JavaPlanningScore.NO_LEVEL_SIZE,
                  bendableSoftLevelsSize=JavaPlanningScore.NO_LEVEL_SIZE,
                  scoreDefinitionClass=None):
    def PlanningScoreFunctionWrapper(getterFunction):
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