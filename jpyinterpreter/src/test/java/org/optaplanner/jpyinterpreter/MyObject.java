package org.optaplanner.jpyinterpreter;

import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;

public class MyObject {
    public String name;
    public PythonLikeFunction attributeFunction;

    public String concatToName(String other) {
        return name + other;
    }
}
