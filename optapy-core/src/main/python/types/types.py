from ..optaplanner_java_interop import ensure_init
import jpype.imports

ensure_init()

# The JVM must be started before importing Java Types, so these
# imports cannot be at the top of the file.
"""The Solver Configuration. Controls termination, optimization algorithms, etc.

To read it from XML, use SolverConfig.createFromXmlResource(String). 
"""
from org.optaplanner.core.config.solver import SolverConfig  # nopep8

"""Creates an BiJoiner, TriJoiner, ... instance for use in ConstraintStream.join(Class, BiJoiner), ..."""
from org.optaplanner.core.api.score.stream import Joiners  # nopep8

"""This Score is based on 2 levels of int constraints: hard and soft.

Hard constraints have priority over soft constraints. Hard constraints determine feasibility. This class is immutable.
"""
from org.optaplanner.core.api.score.buildin.hardsoft import HardSoftScore  # nopep8

"""A time-based amount of time, such as '34.5 seconds'. Used in specifying solve length in SolverConfig"""
from java.time import Duration  # nopep8
