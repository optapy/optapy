package org.optaplanner.python.translator;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonModule;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.PythonTraceback;

public interface PythonInterpreter {
    PythonInterpreter DEFAULT = new CPythonBackedPythonInterpreter();

    PythonLikeObject getGlobal(Map<String, PythonLikeObject> globalsMap, String name);

    void setGlobal(Map<String, PythonLikeObject> globalsMap, String name, PythonLikeObject value);

    void deleteGlobal(Map<String, PythonLikeObject> globalsMap, String name);

    PythonModule importModule(PythonInteger level, List<PythonString> fromList, Map<String, PythonLikeObject> globalsMap,
            Map<String, PythonLikeObject> localsMap, String moduleName);

    void print(PythonLikeObject object);

    PythonTraceback getTraceback();
}
