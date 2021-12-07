from ..optaplanner_java_interop import ensure_init
import jpype.imports # noqa

ensure_init()

# The JVM must be started before importing Java Types, so these
# imports cannot be at the top of the file.
from ..config import *
from ..score import *
from ..constraint import *

from org.optaplanner.core.api.domain.variable import PlanningVariableGraphType

from org.optaplanner.optapy import OpaquePythonReference as _OpaquePythonReference

PythonReference = _OpaquePythonReference
"""
Use this type for third-party objects (such as datetime.date) that do not change
throughout planning. Do note these objects are copied as-is and are not cloned.
If they are modified during planning, score corruption will occur.
"""

SolverConfig = solver.SolverConfig
TerminationConfig = solver.termination.TerminationConfig

from java.time import Duration  # noqa
