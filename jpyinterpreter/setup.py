try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup
from distutils.command.build_py import build_py
import glob
import os
import platform
import subprocess
from pathlib import Path
from shutil import copyfile
import sys

class FetchDependencies(build_py):
    """
    A command class that fetch Java Dependencies and
    add them as files within a python package
    """
    def create_stubs(self, project_root, command):
        subprocess.run([str((project_root / command).absolute()), 'dependency:copy-dependencies'],
                       cwd=project_root, check=True)
        subprocess.run([str((project_root / command).absolute()), 'dependency:copy-dependencies',
                        '-Dclassifier=javadoc'], cwd=project_root, check=True)


    def run(self):
        if not self.dry_run:
            project_root = Path(__file__).parent
            # Do a mvn clean install
            # which is configured to add dependency jars to 'target/dependency'
            command = 'mvnw'
            if platform.system() == 'Windows':
                command = 'mvnw.cmd'
            self.create_stubs(project_root, command)
            subprocess.run([str((project_root / command).absolute()), 'clean', 'install'], cwd=project_root, check=True)
            classpath_jars = []
            # Add the main artifact
            classpath_jars.extend(glob.glob(os.path.join(project_root, 'target', '*.jar')))
            # Add the main artifact's dependencies
            classpath_jars.extend(glob.glob(os.path.join(project_root, 'target', 'dependency', '*.jar')))
            # Get the basename of each file (to be stored in classpath.txt, which is used
            # when setting the classpath)
            filenames = list(map(os.path.basename, classpath_jars))
            classpath_list_text = "\n".join(filenames)

            self.mkpath(os.path.join(self.build_lib, 'jpyinterpreter', 'jars'))

            # Copy classpath jars to optapy.jars
            for file in classpath_jars:
                copyfile(file, os.path.join(self.build_lib, 'jpyinterpreter', 'jars', os.path.basename(file)))

            # Add classpath.txt to optapy
            fp = open(os.path.join(self.build_lib, 'jpyinterpreter', 'classpath.txt'), 'w')
            fp.write(classpath_list_text)
            fp.close()

            # Make optapy.jars a Python module
            fp = open(os.path.join(self.build_lib, 'jpyinterpreter', 'jars', '__init__.py'), 'w')
            fp.close()
        build_py.run(self)


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
    name='jpyinterpreter',
    version='8.31.1b0',
    license='Apache License Version 2.0',
    license_file='LICENSE',
    description='A Python bytecode to Java bytecode translator',
    classifiers=[
        'Development Status :: 4 - Beta',
        'Programming Language :: Python :: 3',
        'Topic :: Software Development :: Libraries :: Java Libraries',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent'
    ],
    packages=['jpyinterpreter', 'java-stubs', 'jpype-stubs', 'org-stubs'],
    package_dir={
        'jpyinterpreter': 'src/main/python',
        # Setup tools need a non-empty directory to use as base
        # Since these packages are generated during the build,
        # we use the src/main/resources package, which does
        # not contain any python files and is already included
        # in the build
        'java-stubs': 'src/main/resources',
        'jpype-stubs': 'src/main/resources',
        'org-stubs': 'src/main/resources',
    },
    test_suite='tests',
    python_requires='>=3.9',
    install_requires=[
        'JPype1>=1.4.1',
    ],
    cmdclass={'build_py': FetchDependencies},
    package_data={
        'optapy': ['classpath.txt'],
        'optapy.jars': ['*.jar'],
    },
)
