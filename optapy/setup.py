from distutils.core import setup

setup(
    name = 'optapy',
    version = '0.0.0',
    author = 'Christopher Chianelli',
    license = 'Apache 2.0',
    description = 'OptaPlanner Annotations and Wrappers for GraalVM',
    packages=['optapy', 'optapy.jars'],
    package_dir={'optapy': 'src/main/python'},
    python_requires='>=3.6',
    install_requires=['pygal==2.4.0'],
    package_data={
        'optapy': ['classpath.txt'],
        'optapy.jars': ['*.jar'],
    },
)