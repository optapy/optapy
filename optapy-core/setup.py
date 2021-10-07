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


class FetchDependencies(build_py):
    """
    A command class that fetch Java Dependencies and
    add them as files within a python package
    """
    def run(self):
        if not self.dry_run:
            project_root = Path(__file__).parent
            # Do a mvn clean install
            # which is configured to add dependency jars to 'target/dependency'
            command = 'mvnw'
            if platform.system() == 'Windows':
                command = 'mvnw.cmd'
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


this_directory = Path(__file__).parent
long_description = (this_directory / "README.md").read_text()
setup(
    name='optapy',
    version='8.11.0a2',
    license='Apache License Version 2.0',
    license_file='LICENSE',
    description='An AI constraint solver that optimizes planning and scheduling problems',
    long_description=long_description,
    long_description_content_type='text/markdown',
    url='https://github.com/optapy/optapy',
    project_urls={
        'OptaPy Homepage': 'https://optapy.org',
        'OptaPlanner Homepage': 'https://www.optaplanner.org/',
        'OptaPlanner Documentation': 'https://www.optaplanner.org/learn/documentation.html'
    },
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Programming Language :: Python :: 3',
        'Topic :: Scientific/Engineering :: Artificial Intelligence',
        'Topic :: Software Development :: Libraries :: Java Libraries',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent'
    ],
    packages=['optapy', 'optapy.types'],
    package_dir={'optapy': 'src/main/python'},
    python_requires='>=3.9',
    install_requires=['JPype1'],
    cmdclass={'build_py': FetchDependencies},
    package_data={
        'optapy': ['classpath.txt'],
        'optapy.jars': ['*.jar'],
    },
)
