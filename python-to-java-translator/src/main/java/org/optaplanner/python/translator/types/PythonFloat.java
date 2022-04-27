package org.optaplanner.python.translator.types;

public class PythonFloat extends AbstractPythonLikeObject implements PythonNumber {
    final double value;

    private final static PythonLikeType FLOAT_TYPE = new PythonLikeType("float");

    static {
        PythonLikeComparable.setup(FLOAT_TYPE.__dir__);
        PythonNumericOperations.setup(FLOAT_TYPE.__dir__);
    }

    public PythonFloat(double value) {
        super(FLOAT_TYPE);
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
            return ((PythonInteger) o).value.equals(value);
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
