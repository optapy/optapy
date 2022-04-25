package org.optaplanner.optapy.translator;

import java.util.HashMap;
import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;

public class CPythonBackedPythonInterpreter implements PythonInterpreter {
    Map<String, PythonLikeObject> globalsMap;

    public CPythonBackedPythonInterpreter() {
        globalsMap = new HashMap<>();
    }
    @Override
    public PythonLikeObject getGlobal(String name) {
        return globalsMap.get(name);
    }

    @Override
    public void setGlobal(String name, PythonLikeObject value) {
        globalsMap.put(name, value);
    }

    @Override
    public void deleteGlobal(String name) {
        globalsMap.remove(name);
    }
}
