package org.optaplanner.optapy.translator;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.types.errors.PythonTraceback;

public class CPythonBackedPythonInterpreter implements PythonInterpreter {
    Map<String, PythonLikeObject> globalsMap;
    PrintStream standardOutput;

    public CPythonBackedPythonInterpreter() {
        this(System.out);
    }

    public CPythonBackedPythonInterpreter(PrintStream standardOutput) {
        this.globalsMap = new HashMap<>();
        this.standardOutput = standardOutput;
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

    @Override
    public void print(PythonLikeObject object) {
        standardOutput.println(object);
    }

    @Override
    public PythonTraceback getTraceback() {
        // TODO: Implement this with an actually traceback
        return new PythonTraceback();
    }
}
