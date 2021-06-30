import os
from shutil import rmtree

os.remove(os.path.join('src', 'main', 'python', 'classpath.txt'))
rmtree(os.path.join('src', 'main', 'python', 'jars'))