package org.optaplanner.python.translator;

import java.util.Map;

import org.optaplanner.python.translator.types.errors.PythonTraceback;

public interface PythonInterpreter {
    PythonInterpreter DEFAULT = new CPythonBackedPythonInterpreter();

    PythonLikeObject getGlobal(Map<String, PythonLikeObject> globalsMap, String name);

    void setGlobal(Map<String, PythonLikeObject> globalsMap, String name, PythonLikeObject value);

    void deleteGlobal(Map<String, PythonLikeObject> globalsMap, String name);

    void print(PythonLikeObject object);

    PythonTraceback getTraceback();
}
