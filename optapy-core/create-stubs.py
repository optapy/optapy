import pathlib
import jpype

try:
    from stubgenj.stubgenj._stubgenj import generateJavaStubs # noqa
except ModuleNotFoundError:
    pass

jars = list(map(str, pathlib.Path('target/dependency').glob('**/*.jar')))

jpype.startJVM(classpath=jars)

import jpype.imports  # noqa
import org.optaplanner  # noqa
import java.time # noqa
import java.util # noqa

generateJavaStubs([java.time, java.util, org.optaplanner], useStubsSuffix=True)

