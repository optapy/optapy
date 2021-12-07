import pathlib
import jpype
import stubgenj

jars = list(map(str, pathlib.Path('target/dependency').glob('**/*.jar')))

jpype.startJVM(classpath=jars, convertStrings=True)

import jpype.imports  # noqa
import org.optaplanner.core.api  # noqa
import org.optaplanner.core.config  # noqa
import java.lang # noqa
import java.time # noqa
import java.util # noqa

stubgenj.generateJavaStubs([java.lang, java.time, java.util, org.optaplanner.core.api, org.optaplanner.core.config],
                           useStubsSuffix=True)

