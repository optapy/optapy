import atexit
import jpype.imports
import pytest


@pytest.fixture(scope='session', autouse=True)
def setup_jvm(request):
    # Will be executed before the first test
    yield None
    # Will be executed after the last test
    from java.lang import Runtime
    exit_status = 0 if request.session.testsfailed == 0 else 1
    atexit.register(lambda: Runtime.getRuntime().halt(exit_status))


def test_setup(setup_jvm):
    # required so teardown is executed
    pass