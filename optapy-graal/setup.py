from distutils.core import setup
from distutils.command.build_py import build_py
import glob
import os
import subprocess
from shutil import copyfile

class FetchDependencies(build_py):
    def run(self):
        if not self.dry_run:
            project_root = os.environ.get("PWD")
            subprocess.run([os.path.join(project_root, 'mvnw'), 'clean', 'install'], cwd=project_root, check=True)
            classpath_jars=[]
            classpath_jars.extend(glob.glob(os.path.join(project_root, 'target', '*.jar')))
            classpath_jars.extend(glob.glob(os.path.join(project_root, 'target', 'dependency', '*.jar')))
            filenames = list(map(os.path.basename, classpath_jars))
            classpath_list_text = "\n".join(filenames)

            self.mkpath(os.path.join(self.build_lib, 'optapy', 'jars'))

            for file in classpath_jars:
                copyfile(file, os.path.join(self.build_lib, 'optapy', 'jars', os.path.basename(file)))

            fp = open(os.path.join(self.build_lib, 'optapy', 'classpath.txt'), 'w')
            fp.write(classpath_list_text)
            fp.close()

            fp = open(os.path.join(self.build_lib, 'optapy', 'jars', '__init__.py'), 'w')
            fp.close()
        build_py.run(self)

setup(
    name = 'optapy',
    version = '0.0.0',
    author = 'Christopher Chianelli',
    license = 'Apache 2.0',
    description = 'OptaPlanner Annotations and Wrappers for GraalVM',
    packages=['optapy'],
    package_dir={'optapy': 'src/main/python'},
    python_requires='>=3.6',
    install_requires=['pygal==2.4.0'],
    cmdclass={'build_py': FetchDependencies},
    package_data={
        'optapy': ['classpath.txt'],
        'optapy.jars': ['*.jar'],
    },
)