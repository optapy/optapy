package org.optaplanner.optapy.translator;

import java.util.function.Function;

import org.optaplanner.optapy.translator.types.PythonLikeFunction;

public class MyObject {
    public String name;
    public PythonLikeFunction attributeFunction;

    public String concatToName(String other) {
        return name + other;
    }
}
