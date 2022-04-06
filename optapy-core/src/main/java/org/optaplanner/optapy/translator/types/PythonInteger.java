package org.optaplanner.optapy.translator.types;

import java.util.HashMap;
import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;

public class PythonInteger extends AbstractPythonLikeObject implements PythonNumber {
    final long value;

    private final static PythonLikeType INT_TYPE = new PythonLikeType("int");

    static {
        PythonLikeComparable.setup(INT_TYPE.__dir__);
        PythonNumericOperations.setup(INT_TYPE.__dir__);
    }

    public PythonInteger(long value) {
        super(INT_TYPE);
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
