try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup
import os
from pathlib import Path


def find_stub_files(stub_root: str):
    """
    This function is taken from the awesome sqlalchey-stubs:
    https://github.com/dropbox/sqlalchemy-stubs/blob/master/setup.py#L32
    It's licensed under Apache 2.0:
    https://github.com/dropbox/sqlalchemy-stubs/blob/master/LICENSE
    """
    result = []
    for root, dirs, files in os.walk(stub_root):
        for file in files:
            if file.endswith(".pyi"):
                if os.path.sep in root:
                    sub_root = root.split(os.path.sep, 1)[-1]
                    file = os.path.join(sub_root, file)
                result.append(file)
    return result


this_directory = Path(__file__).parent

setup(
    name='optapy-optaplanner-stubs',
    version='8.11.0a2',
    license='Apache License Version 2.0',
    license_file='LICENSE',
    description='Stubs for the org.optaplanner package used in OptaPy',
    url='https://github.com/optapy/optapy',
    project_urls={
        'OptaPy Homepage': 'https://optapy.org',
        'OptaPlanner Homepage': 'https://www.optaplanner.org/',
        'OptaPlanner Documentation': 'https://www.optaplanner.org/learn/documentation.html'
    },
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Programming Language :: Python :: 3',
        'Topic :: Software Development :: Libraries :: Java Libraries',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent'
    ],
    packages=['org-stubs'],
    package_dir={
        'org-stubs': 'org-stubs',
    },
    python_requires='>=3.9',
    install_requires=[],
    package_data={
        'org-stubs': find_stub_files('org-stubs'),
    },
)
