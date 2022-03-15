package org.optaplanner.optapy.translator.types;

import java.util.HashMap;
import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;

public class PythonInteger extends AbstractPythonLikeObject implements PythonNumber {
    final static Map<String, PythonLikeObject> DEFAULT_DICT;
    final long value;

    static {
        DEFAULT_DICT = new HashMap<>();
        PythonLikeComparable.setup(DEFAULT_DICT);
        PythonNumericOperations.setup(DEFAULT_DICT);
    }

    public PythonInteger(long value) {
        super(new CopyOnWriteMap<>(DEFAULT_DICT));
        this.value = value;
    }

    @Override
    public Number getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue() == value;
        } else if (o instanceof PythonInteger) {
            return ((PythonInteger) o).value == value;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    public static PythonInteger valueOf(byte value) {
        return new PythonInteger(value);
    }

    public static PythonInteger valueOf(short value) {
        return new PythonInteger(value);
    }

    public static PythonInteger valueOf(int value) {
        return new PythonInteger(value);
    }

    public static PythonInteger valueOf(long value) {
        return new PythonInteger(value);
    }
}
