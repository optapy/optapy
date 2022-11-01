import optapy


def pytest_addoption(parser):
    """
    Allows adding command line options to pytest
    """
    parser.addoption('--jacoco-agent', action='store', default='')
    parser.addoption('--jacoco-output', action='store', default='target/jacoco.exec')
    parser.addoption('--output-generated-classes', action='store', default='false')


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
    import pathlib
    import sys

    jacoco_agent = session.config.getoption('--jacoco-agent')
    if jacoco_agent != '':
        jacoco_output = session.config.getoption('--jacoco-output')
        optapy.init(f'-javaagent:{jacoco_agent}=destfile={jacoco_output}')
    else:
        optapy.init()

    if session.config.getoption('--output-generated-classes') != 'false':
        optapy.set_class_output_directory(pathlib.Path('target', 'tox-generated-classes', 'python', f'{sys.version_info[0]}.{sys.version_info[1]}'))



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