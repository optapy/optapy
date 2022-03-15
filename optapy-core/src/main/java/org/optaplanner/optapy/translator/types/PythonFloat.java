package org.optaplanner.optapy.translator.types;

import java.util.HashMap;
import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;

public class PythonFloat extends AbstractPythonLikeObject implements PythonNumber {
    final static Map<String, PythonLikeObject> DEFAULT_DICT;
    final double value;

    static {
        DEFAULT_DICT = new HashMap<>();
        PythonLikeComparable.setup(DEFAULT_DICT);
        PythonNumericOperations.setup(DEFAULT_DICT);
    }

    public PythonFloat(double value) {
        super(new CopyOnWriteMap<>(DEFAULT_DICT));
        this.value = value;
    }

    @Override
    public Number getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue() == value;
        } else if (o instanceof PythonInteger) {
            return ((PythonInteger) o).value == value;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    public static PythonFloat valueOf(float value) {
        return new PythonFloat(value);
    }

    public static PythonFloat valueOf(double value) {
        return new PythonFloat(value);
    }
}
