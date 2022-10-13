import pytest
from typing import Callable, Any
from copy import deepcopy


def get_argument_cloner(clone_arguments):
    return (lambda x: deepcopy(x)) if clone_arguments else (lambda x: x)


class FunctionVerifier:
    def __init__(self, python_function, java_function, untyped_java_function):
        self.python_function = python_function
        self.java_function = java_function
        self.untyped_java_function = untyped_java_function

    def verify(self, *args, expected_result=..., expected_error=..., clone_arguments=True):
        cloner = get_argument_cloner(clone_arguments)
        try:
            expected = self.python_function(*cloner(args))
        except Exception as error:
            if expected_error is not ... and isinstance(expected_error, type) and not isinstance(error, expected_error):
                raise AssertionError(f'Python function did not raise expected error ({expected_error}) '
                                     f'for arguments {args}; it raised {type(error)}({error}) instead.')

            if expected_error is not ... and not isinstance(expected_error, type) and error != expected_error:
                raise AssertionError(f'Python function did not raise expected error ({expected_error}) '
                                     f'for arguments {args}; it raised  {type(error)}({error}) instead.')

            if expected_result is not ...:
                raise AssertionError(f'Python function did not return expected result ({expected_result}) '
                                     f'for arguments {args}; it raised  {type(error)}({error}) instead.')

            self.expect_error(error, *args, clone_arguments=clone_arguments)
            return

        if expected_result is not ... and expected_result != expected:
            raise AssertionError(f'Python function did not return expected result ({expected_result}) '
                                 f'for arguments {args}; it returned ({expected}) instead.')

        if expected_error is not ...:
            raise AssertionError(f'Python function did not raise expected error ({expected_error}) '
                                 f'for arguments {args}; it returned ({expected}) instead.')

        if expected_result is not ... and type(expected_result) is not type(expected):
            raise AssertionError(f'Python function did not return expected result ({expected_result}, '
                                 f'{type(expected_result)}) for arguments {args}; it returned the equal but different '
                                 f'type ({expected}, {type(expected)}) instead.')

        self.expect(expected, *args, clone_arguments=clone_arguments)


    def verify_property(self, *args, predicate, clone_arguments=True):
        cloner = get_argument_cloner(clone_arguments)
        python_result = self.python_function(*cloner(args))
        if not predicate(python_result):
            import inspect
            raise AssertionError(f'Python function result ({python_result}) does not satisfy the property '
                                 f'({inspect.getsource(predicate).strip()}) '
                                 f'for arguments {args}.')

        java_result = self.java_function(*cloner(args))
        untyped_java_result = self.untyped_java_function(*cloner(args))
        if not predicate(java_result) or not predicate(untyped_java_result):
            import inspect
            if not predicate(java_result) and not predicate(untyped_java_result):
                raise AssertionError(f'Typed and untyped translated bytecode result ({java_result}) does not satisfy '
                                     f'the property ({inspect.getsource(predicate)}) '
                                     f'for arguments {args}.')
            elif not predicate(untyped_java_result):
                raise AssertionError(f'Untyped translated bytecode result ({java_result}) does not satisfy the '
                                     f'property ({inspect.getsource(predicate)}) '
                                     f'for arguments {args}.')
            else:
                raise AssertionError(f'Typed translated bytecode result ({java_result}) does not satisfy the '
                                     f'property ({inspect.getsource(predicate)}) '
                                     f'for arguments {args}.')

    def expect(self, expected, *args, clone_arguments=True):
        cloner = get_argument_cloner(clone_arguments)
        java_result = self.java_function(*cloner(args))
        untyped_java_result = self.untyped_java_function(*cloner(args))
        if java_result != expected or untyped_java_result != expected:
            if java_result != expected and untyped_java_result != expected:
                raise AssertionError(f'Typed and untyped translated bytecode did not return expected result '
                                     f'({expected}) for arguments {args}; it returned ({java_result}) (typed) '
                                     f'and ({untyped_java_result}) (untyped) instead.')
            elif untyped_java_result != expected:
                raise AssertionError(f'Untyped translated bytecode did not return expected result '
                                     f'({expected}) for arguments {args}; it returned ({untyped_java_result}) '
                                     f'(untyped) instead.')
            else:
                raise AssertionError(f'Typed translated bytecode did not return expected result '
                                     f'({expected}) for arguments {args}; it returned ({java_result}) '
                                     f'(typed) instead.')
        if type(java_result) is not type(expected) or type(untyped_java_result) is not type(expected):
            if type(java_result) is not type(expected) and type(untyped_java_result) is not type(expected):
                raise AssertionError(f'Typed and untyped translated bytecode did not return expected result '
                                     f'({expected}, {type(expected)}) for arguments {args}; it returned the equal but '
                                     f'different type ({java_result}, {type(java_result)}) (typed) and '
                                     f'({untyped_java_result}, {type(untyped_java_result)}) (untyped) instead.')
            elif type(untyped_java_result) is not type(expected):
                raise AssertionError(f'Untyped translated bytecode did not return expected result '
                                     f'({expected}, {type(expected)}) for arguments {args}; it returned the equal but '
                                     f'different type '
                                     f'({untyped_java_result}, {type(untyped_java_result)}) (untyped) instead.')
            else:
                raise AssertionError(f'Typed translated bytecode did not return expected result '
                                     f'({expected}, {type(expected)}) for arguments {args}; it returned the equal but '
                                     f'different type ({java_result}, {type(java_result)}) (typed) '
                                     f'instead.')

    def expect_error(self, error, *args, clone_arguments=True):
        cloner = get_argument_cloner(clone_arguments)
        with pytest.raises(type(error)) as error_info:
            self.java_function(*cloner(args))

        with pytest.raises(type(error)) as error_info:
            self.untyped_java_function(*cloner(args))


def verifier_for(the_function: Callable[..., Any]) -> FunctionVerifier:
    import jpyinterpreter

    java_function = jpyinterpreter.as_typed_java(the_function)
    untyped_java_function = jpyinterpreter.as_untyped_java(the_function)
    return FunctionVerifier(the_function, java_function, untyped_java_function)


def pytest_configure(config):
    """
    Allows plugins and conftest files to perform initial configuration.
    This hook is called for every plugin and initial conftest
    file after command line options have been parsed.
    """
    pass


def pytest_sessionstart(session):
    """
    Called after the Session object has been created and
    before performing collection and entering the run test loop.
    """
    import jpyinterpreter
    import pathlib
    import sys
    class_output_path = pathlib.Path('target', 'tox-generated-classes', 'python',
                                     f'{sys.version_info[0]}.{sys.version_info[1]}')
    jpyinterpreter.init(class_output_path=class_output_path)


def pytest_sessionfinish(session, exitstatus):
    """
    Called after whole test run finished, right before
    returning the exit status to the system.
    """
    pass


def pytest_unconfigure(config):
    """
    called before test process is exited.
    """
    pass