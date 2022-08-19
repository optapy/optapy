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

    /**
     * Writes output without a trailing newline to standard output
     *
     * @param output the text to write
     */
    void write(String output);

    /**
     * Reads a line from standard input
     *
     * @return A line read from standard input
     */
    String readLine();

    PythonTraceback getTraceback();
}
