from ..optaplanner_java_interop import ensure_init
import jpype.imports # noqa

ensure_init()

# The JVM must be started before importing Java Types, so these
# imports cannot be at the top of the file.
from ..config import *
from ..score import *
from ..constraint import *

SolverConfig = solver.SolverConfig
TerminationConfig = solver.termination.TerminationConfig

from java.time import Duration  # noqa
