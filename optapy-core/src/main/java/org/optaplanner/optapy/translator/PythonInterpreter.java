package org.optaplanner.optapy.translator;

import org.optaplanner.optapy.PythonLikeObject;

public interface PythonInterpreter {
    PythonInterpreter DEFAULT = new CPythonBackedPythonInterpreter();

    PythonLikeObject getGlobal(String name);
    void setGlobal(String name, PythonLikeObject value);
    void deleteGlobal(String name);
    void print(PythonLikeObject object);
}
