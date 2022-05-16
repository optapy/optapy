package org.optaplanner.python.translator.types;

import java.math.BigInteger;

public class PythonInteger extends AbstractPythonLikeObject implements PythonNumber {
    final BigInteger value;

    public final static PythonLikeType INT_TYPE = new PythonLikeType("int");

    static {
        PythonLikeComparable.setup(INT_TYPE.__dir__);
        PythonNumericOperations.setup(INT_TYPE.__dir__);
    }

    public PythonInteger(long value) {
        this(BigInteger.valueOf(value));
    }

    public PythonInteger(BigInteger value) {
        super(INT_TYPE);
        this.value = value;
    }

    @Override
    public Number getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Number) {
            return value.equals(BigInteger.valueOf(((Number) o).longValue()));
        } else if (o instanceof PythonInteger) {
            return ((PythonInteger) o).value.equals(value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
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

    public static PythonInteger valueOf(BigInteger value) {
        return new PythonInteger(value);
    }
}
