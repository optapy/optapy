import pathlib
import jpype
import stubgenj

jars = list(map(str, pathlib.Path('target/dependency').glob('**/*.jar')))

jpype.startJVM(classpath=jars)

import jpype.imports  # noqa
import org.optaplanner  # noqa
import java.time # noqa
import java.util # noqa

stubgenj.generateJavaStubs([java.time, java.util, org.optaplanner], useStubsSuffix=True)

