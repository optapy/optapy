from ..optaplanner_java_interop import ensure_init
import jpype.imports # noqa

ensure_init()

# The JVM must be started before importing Java Types, so these
# imports cannot be at the top of the file.
from org.optaplanner.core.config.solver import SolverConfig  # noqa
from org.optaplanner.core.api.score.stream import Joiners, ConstraintCollectors, Constraint, ConstraintFactory  # noqa
from org.optaplanner.core.api.score.buildin.hardsoft import HardSoftScore  # noqa
from java.time import Duration  # noqa
