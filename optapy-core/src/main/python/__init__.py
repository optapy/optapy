"""
This module wraps OptaPlanner and allow Python Objects
to be used as the domain and Python functions to be used
as the constraints.

Using any decorators in this module will automatically start
the JVM. If you want to pass custom arguments to the JVM,
use init before decorators and any optapy.types imports.
"""

from .annotations import *
from .optaplanner_api_wrappers import *
from .optaplanner_java_interop import _planning_clone
