import pytest
from typing import Callable, Any
from copy import deepcopy


class FunctionVerifier:
    def __init__(self, python_function, java_function):
        self.python_function = python_function
        self.java_function = java_function

    def verify(self, *args, expected_result=..., expected_error=...):
        try:
            expected = self.python_function(*deepcopy(args))
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

            self.expect_error(error, *deepcopy(args))
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

        self.expect(expected, *deepcopy(args))


    def verify_property(self, *args, predicate):
        python_result = self.python_function(*deepcopy(args))
        if not predicate(python_result):
            import inspect
            raise AssertionError(f'Python function result ({python_result}) does not satisfy the property '
                                 f'({inspect.getsource(predicate).strip()}) '
                                 f'for arguments {args}.')

        java_result = self.java_function(*deepcopy(args))
        if not predicate(java_result):
            import inspect
            raise AssertionError(f'Translated bytecode result ({java_result}) does not satisfy the property '
                                 f'({inspect.getsource(predicate)}) '
                                 f'for arguments {args}.')


    def expect(self, expected, *args):
        java_result = self.java_function(*args)
        if java_result != expected:
            raise AssertionError(f'Translated bytecode did not return expected result ({expected}) '
                                 f'for arguments {args}; it returned ({java_result}) instead.')
        if type(java_result) is not type(expected):
            raise AssertionError(f'Translated bytecode did not return expected result ({expected}, {type(expected)}) '
                                 f'for arguments {args}; it returned the equal but different type ({java_result}, '
                                 f'{type(java_result)}) instead.')

    def expect_error(self, error, *args):
        with pytest.raises(type(error)) as error_info:
            self.java_function(*args)


def verifier_for(the_function: Callable[..., Any]) -> FunctionVerifier:
    import javapython
    java_function = javapython.as_java(the_function)
    return FunctionVerifier(the_function, java_function)


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
    import javapython
    import pathlib
    import sys
    javapython.init()
    javapython.set_class_output_directory(pathlib.Path('target', 'tox-generated-classes', 'python', f'{sys.version_info[0]}.{sys.version_info[1]}'))


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