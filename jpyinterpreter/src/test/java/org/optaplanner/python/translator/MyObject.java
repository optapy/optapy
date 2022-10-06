package org.optaplanner.python.translator;

import org.optaplanner.python.translator.types.PythonLikeFunction;

public class MyObject {
    public String name;
    public PythonLikeFunction attributeFunction;

    public String concatToName(String other) {
        return name + other;
    }
}
