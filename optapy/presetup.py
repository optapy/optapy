import glob
import os
from shutil import copyfile

classpath_jars=[]
classpath_jars.extend(glob.glob(os.path.join('target', '*.jar')))
classpath_jars.extend(glob.glob(os.path.join('target', 'dependency', '*.jar')))
filenames = list(map(os.path.basename, classpath_jars))
classpath_list_text = "\n".join(filenames)

if not os.path.exists(os.path.join('src', 'main', 'python', 'jars')):
    os.mkdir(os.path.join('src', 'main', 'python', 'jars'))

for file in classpath_jars:
    copyfile(file, os.path.join('src', 'main', 'python', 'jars', os.path.basename(file)))

fp = open(os.path.join('src', 'main', 'python', 'classpath.txt'), 'w')
fp.write(classpath_list_text)
fp.close()

fp = open(os.path.join('src', 'main', 'python', 'jars', '__init__.py'), 'w')
fp.close()