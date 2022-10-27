import inspect
import re
from optapy.constraint import UniConstraintStream, BiConstraintStream, TriConstraintStream, QuadConstraintStream, \
    Joiners, ConstraintCollectors, ConstraintFactory
from org.optaplanner.core.api.score.stream import Joiners as JavaJoiners,\
    ConstraintCollectors as JavaConstraintCollectors, ConstraintFactory as JavaConstraintFactory
from org.optaplanner.core.api.score.stream.uni import UniConstraintStream as JavaUniConstraintStream
from org.optaplanner.core.api.score.stream.bi import BiConstraintStream as JavaBiConstraintStream
from org.optaplanner.core.api.score.stream.tri import TriConstraintStream as JavaTriConstraintStream
from org.optaplanner.core.api.score.stream.quad import QuadConstraintStream as JavaQuadConstraintStream

ignored_python_functions = {
    '_call_comparison_java_joiner',
    '__init__',
    'from_',  # ignored since the camelcase version is from, which is a keyword in Python
}

ignored_java_functions = {
    'equals',
    'getClass',
    'hashCode',
    'notify',
    'notifyAll',
    'toString',
    'wait',
    'countLongBi',  # Python has no concept of Long (everything a BigInteger)
    'countLongQuad',
    'countLongTri',
    '_handler',  # JPype handler field should be ignored
}

def test_camel_case_for_all_snake_case_methods():
    for class_type in (UniConstraintStream, BiConstraintStream, TriConstraintStream, QuadConstraintStream, Joiners,
                       ConstraintCollectors, ConstraintFactory):
        missing = []
        incorrect = []
        for function in inspect.getmembers(class_type, inspect.isfunction):
            # split underscore using split
            function_name = function[0]
            if function_name in ignored_python_functions:
                continue
            function_name_parts = function_name.split('_')

            # joining result
            camel_case_name = function_name_parts[0] + ''.join(ele.title() for ele in function_name_parts[1:])
            if not hasattr(class_type, camel_case_name):
                missing.append(camel_case_name)
            if getattr(class_type, camel_case_name) is not function[1]:
                incorrect.append(function)

        assert len(missing) == 0
        assert len(incorrect) == 0


def test_snake_case_for_all_camel_case_methods():
    for class_type in (UniConstraintStream, BiConstraintStream, TriConstraintStream, QuadConstraintStream, Joiners,
                       ConstraintCollectors, ConstraintFactory):
        missing = []
        incorrect = []
        for function in inspect.getmembers(class_type, inspect.isfunction):
            # split underscore using split
            function_name = function[0]
            if function_name in ignored_python_functions:
                continue
            snake_case_name = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', function_name)
            # change h_t_t_p -> http
            snake_case_name = re.sub('([a-z0-9])([A-Z])', r'\1_\2', snake_case_name).lower()

            if not hasattr(class_type, snake_case_name):
                missing.append(snake_case_name)
            if getattr(class_type, snake_case_name) is not function[1]:
                incorrect.append(function)

        assert len(missing) == 0
        assert len(incorrect) == 0


def test_has_all_methods():

    for python_type, java_type in ((UniConstraintStream, JavaUniConstraintStream),
                                   (BiConstraintStream, JavaBiConstraintStream),
                                   (TriConstraintStream, JavaTriConstraintStream),
                                   (QuadConstraintStream, JavaQuadConstraintStream),
                                   (Joiners, JavaJoiners),
                                   (ConstraintCollectors, JavaConstraintCollectors),
                                   (ConstraintFactory, JavaConstraintFactory)):
        missing = []
        for function_name, function_impl in inspect.getmembers(java_type, inspect.isfunction):
            if function_name in ignored_java_functions:
                continue
            if python_type is ConstraintCollectors and function_name.endswith(('Long', 'BigInteger', 'Duration',
                                                                               'BigDecimal', 'Period')):
                continue  # Python only has a single integer type (= BigInteger) and does not support Java Durations
                          # or Period
            if not hasattr(python_type, function_name):
                missing.append(function_name)

        assert len(missing) == 0
