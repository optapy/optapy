from ..optaplanner_java_interop import ensure_init

ensure_init()
import jpype.imports

"""The Solver Configuration. Controls termination, optimization algorithms, etc.

To read it from XML, use SolverConfig.createFromXmlResource(String). 
"""
from org.optaplanner.core.config.solver import SolverConfig

"""Creates an BiJoiner, TriJoiner, ... instance for use in ConstraintStream.join(Class, BiJoiner), ..."""
from org.optaplanner.core.api.score.stream import Joiners

"""This Score is based on 2 levels of int constraints: hard and soft.

Hard constraints have priority over soft constraints. Hard constraints determine feasibility. This class is immutable.
"""
from org.optaplanner.core.api.score.buildin.hardsoft import HardSoftScore

"""A time-based amount of time, such as '34.5 seconds'. Used in specifying solve length in SolverConfig"""
from java.time import Duration