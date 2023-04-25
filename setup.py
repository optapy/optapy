from setuptools import setup
from setuptools.command.build_py import build_py
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
        working_directory = project_root / 'optapy-core'
        subprocess.run([str((project_root / command).absolute()), 'dependency:copy-dependencies'],
                       cwd=working_directory, check=True)
        subprocess.run([str((project_root / command).absolute()), 'dependency:copy-dependencies',
                        '-Dclassifier=javadoc'], cwd=working_directory, check=True)
        subprocess.run([sys.executable, str((project_root / 'create-stubs.py').absolute())],
                       cwd=working_directory, check=True)
        target_dir = self.build_lib
        for file_name in find_stub_files(str(working_directory / 'java-stubs')):
            os.makedirs(os.path.dirname(os.path.join(target_dir, file_name)), exist_ok=True)
            copyfile(os.path.join(str(working_directory), file_name), os.path.join(target_dir, file_name))
        for file_name in find_stub_files(str(working_directory / 'jpype-stubs')):
            os.makedirs(os.path.dirname(os.path.join(target_dir, file_name)), exist_ok=True)
            copyfile(os.path.join(str(working_directory), file_name), os.path.join(target_dir, file_name))
        for file_name in find_stub_files(str(working_directory / 'org-stubs')):
            os.makedirs(os.path.dirname(os.path.join(target_dir, file_name)), exist_ok=True)
            copyfile(os.path.join(str(working_directory), file_name), os.path.join(target_dir, file_name))

    def run(self):
        if not self.dry_run:
            project_root = Path(__file__).parent
            # Do a mvn clean install
            # which is configured to add dependency jars to 'target/dependency'
            command = 'mvnw'
            if platform.system() == 'Windows':
                command = 'mvnw.cmd'
            self.create_stubs(project_root, command)
            subprocess.run([str((project_root / command).absolute()), 'clean', 'install', '-Dasciidoctor.skip',
                            '-Dassembly.skipAssembly'],
                           cwd=project_root, check=True)
            classpath_jars = []
            # Add the main artifact
            classpath_jars.extend(glob.glob(os.path.join(project_root, 'optapy-core', 'target', '*.jar')))
            # Add the main artifact's dependencies
            classpath_jars.extend(glob.glob(os.path.join(project_root, 'optapy-core', 'target', 'dependency', '*.jar')))
            # Get the basename of each file (to be stored in classpath.txt, which is used
            # when setting the classpath)
            filenames = list(map(os.path.basename, classpath_jars))
            classpath_list_text = "\n".join(filenames)

            self.mkpath(os.path.join(self.build_lib, 'optapy', 'jars'))

            # Copy classpath jars to optapy.jars
            for file in classpath_jars:
                copyfile(file, os.path.join(self.build_lib, 'optapy', 'jars', os.path.basename(file)))

            # Add classpath.txt to optapy
            fp = open(os.path.join(self.build_lib, 'optapy', 'classpath.txt'), 'w')
            fp.write(classpath_list_text)
            fp.close()

            # Make optapy.jars a Python module
            fp = open(os.path.join(self.build_lib, 'optapy', 'jars', '__init__.py'), 'w')
            fp.close()
        build_py.run(self)


def find_stub_files(stub_root: str):
    """
    This function is taken from the awesome sqlalchey-stubs:
    https://github.com/dropbox/sqlalchemy-stubs/blob/master/setup.py#L32
    It's licensed under Apache 2.0:
    https://github.com/dropbox/sqlalchemy-stubs/blob/master/LICENSE
    """
    for root, dirs, files in os.walk(stub_root):
        for file in files:
            if file.endswith(".pyi"):
                if os.path.sep in root:
                    sub_root = root.split(os.path.sep, 1)[-1]
                    yield os.path.join(sub_root, file)


this_directory = Path(__file__).parent
long_description = (this_directory / "README.md").read_text()

setup(
    name='optapy',
    version='9.37.0b0',
    license='Apache License Version 2.0',
    license_file='LICENSE',
    description='An AI constraint solver that optimizes planning and scheduling problems',
    long_description=long_description,
    long_description_content_type='text/markdown',
    url='https://github.com/optapy/optapy',
    project_urls={
        'OptaPy Documentation': 'https://optapy.org',
        'OptaPlanner Homepage': 'https://www.optaplanner.org/',
    },
    classifiers=[
        'Development Status :: 4 - Beta',
        'Programming Language :: Python :: 3',
        'Topic :: Scientific/Engineering :: Artificial Intelligence',
        'Topic :: Software Development :: Libraries :: Java Libraries',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent'
    ],
    packages=['optapy', 'optapy.config', 'optapy.constraint', 'optapy.score', 'optapy.types', 'optapy.test',
              'jpyinterpreter',
              'java-stubs', 'jpype-stubs', 'org-stubs'],
    package_dir={
        'optapy': 'optapy-core/src/main/python',
        'jpyinterpreter': 'jpyinterpreter/src/main/python',
        # Setup tools need a non-empty directory to use as base
        # Since these packages are generated during the build,
        # we use the src/main/resources package, which does
        # not contain any python files and is already included
        # in the build
        'java-stubs': 'optapy-core/src/main/resources',
        'jpype-stubs': 'optapy-core/src/main/resources',
        'org-stubs': 'optapy-core/src/main/resources',
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
