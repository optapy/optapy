from jpype import JProxy, JImplements, JOverride, JConversion
from jpype.types import *
from types import FunctionType


@JImplements('org.optaplanner.core.api.score.stream.ConstraintProvider', deferred=True)
class ConstraintProviderFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def defineConstraints(self, constraint_factory):
        return self.delegate(constraint_factory)


@JImplements('java.util.function.Supplier', deferred=True)
class PythonSupplier:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def get(self):
        return self.delegate()


@JImplements('java.util.function.Function', deferred=True)
class PythonFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument):
        return self.delegate(argument)


@JImplements('java.util.function.BiFunction', deferred=True)
class PythonBiFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2):
        return self.delegate(argument1, argument2)


@JImplements('org.optaplanner.core.api.function.TriFunction', deferred=True)
class PythonTriFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2, argument3):
        return self.delegate(argument1, argument2, argument3)


@JImplements('org.optaplanner.core.api.function.QuadFunction', deferred=True)
class PythonQuadFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2, argument3, argument4):
        return self.delegate(argument1, argument2, argument3, argument4)


@JImplements('org.optaplanner.core.api.function.PentaFunction', deferred=True)
class PythonPentaFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def apply(self, argument1, argument2, argument3, argument4, argument5):
        return self.delegate(argument1, argument2, argument3, argument4, argument5)


@JImplements('java.util.function.ToIntFunction', deferred=True)
class PythonToIntFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument):
        return self.delegate(argument)


@JImplements('java.util.function.ToIntBiFunction', deferred=True)
class PythonToIntBiFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument1, argument2):
        return self.delegate(argument1, argument2)


@JImplements('org.optaplanner.core.api.function.ToIntTriFunction', deferred=True)
class PythonToIntTriFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument1, argument2, argument3):
        return self.delegate(argument1, argument2, argument3)


@JImplements('org.optaplanner.core.api.function.ToIntQuadFunction', deferred=True)
class PythonToIntQuadFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument1, argument2, argument3, argument4):
        return self.delegate(argument1, argument2, argument3, argument4)


@JImplements('org.optaplanner.core.api.function.ToIntPentaFunction', deferred=True)
class PythonToIntPentaFunction:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def applyAsInt(self, argument1, argument2, argument3, argument4, argument5):
        return self.delegate(argument1, argument2, argument3, argument4, argument5)

# Function convertors
def _has_java_class(item):
    if isinstance(item, (JObject, int, str, bool)):
        return True
    if hasattr(type(item), '__optapy_java_class'):
        return True
    return False


def _proxy(value):
    from org.optaplanner.optapy import OpaquePythonReference  # noqa
    return JProxy(OpaquePythonReference, inst=value, convert=True)


def _convert_to_java_compatible_object(item):
    from org.optaplanner.optapy import PythonComparable  # noqa
    if _has_java_class(item):
        return item
    return PythonComparable(_proxy(item))


@JConversion('java.util.function.Function', exact=FunctionType)
def _convert_to_function(jcls, obj):
    return PythonFunction(lambda a: _convert_to_java_compatible_object(obj(a)))


@JConversion('java.util.function.BiFunction', exact=FunctionType)
def _convert_to_bi_function(jcls, obj):
    return PythonBiFunction(lambda a, b: _convert_to_java_compatible_object(obj(a, b)))


@JConversion('org.optaplanner.core.api.function.TriFunction', exact=FunctionType)
def _convert_to_tri_function(jcls, obj):
    return PythonTriFunction(lambda a, b, c: _convert_to_java_compatible_object(obj(a, b, c)))


@JConversion('org.optaplanner.core.api.function.QuadFunction', exact=FunctionType)
def _convert_to_quad_function(jcls, obj):
    return PythonQuadFunction(lambda a, b, c, d: _convert_to_java_compatible_object(obj(a, b, c, d)))


@JConversion('org.optaplanner.core.api.function.PentaFunction', exact=FunctionType)
def _convert_to_quad_function(jcls, obj):
    return PythonPentaFunction(lambda a, b, c, d, e: _convert_to_java_compatible_object(obj(a, b, c, d, e)))


@JConversion('java.util.function.ToIntFunction', exact=FunctionType)
def _convert_to_int_function(jcls, obj):
    return PythonToIntFunction(lambda a: JInt(obj(a)))


@JConversion('java.util.function.ToIntBiFunction', exact=FunctionType)
def _convert_to_int_bi_function(jcls, obj):
    return PythonToIntBiFunction(lambda a, b: JInt(obj(a, b)))


@JConversion('org.optaplanner.core.api.function.ToIntTriFunction', exact=FunctionType)
def _convert_to_int_tri_function(jcls, obj):
    return PythonToIntTriFunction(lambda a, b, c: JInt(obj(a, b, c)))


@JConversion('org.optaplanner.core.api.function.ToIntQuadFunction', exact=FunctionType)
def _convert_to_int_quad_function(jcls, obj):
    return PythonToIntQuadFunction(lambda a, b, c, d: JInt(obj(a, b, c, d)))


@JConversion('org.optaplanner.core.api.function.ToIntPentaFunction', exact=FunctionType)
def _convert_to_int_quad_function(jcls, obj):
    return PythonToIntPentaFunction(lambda a, b, c, d, e: JInt(obj(a, b, c, d, e)))


# Jpype convert int to primitive, but not to their wrappers, so add implicit conversion to wrappers
@JConversion('java.lang.Integer', exact=int)
def _convert_to_integer(jcls, obj):
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    return PythonWrapperGenerator.wrapInt(obj)


@JConversion('java.lang.Long', exact=int)
def _convert_to_long(jcls, obj):
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    return PythonWrapperGenerator.wrapLong(obj)


@JConversion('java.lang.Short', exact=int)
def _convert_to_short(jcls, obj):
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    return PythonWrapperGenerator.wrapShort(obj)


@JConversion('java.lang.Byte', exact=int)
def _convert_to_byte(jcls, obj):
    from org.optaplanner.optapy import PythonWrapperGenerator  # noqa
    return PythonWrapperGenerator.wrapByte(obj)