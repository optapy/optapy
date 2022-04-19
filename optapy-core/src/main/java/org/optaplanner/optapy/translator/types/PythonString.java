package org.optaplanner.optapy.translator.types;

public class PythonString extends AbstractPythonLikeObject {
    public final String value;

    private final static PythonLikeType STRING_TYPE = new PythonLikeType("str");

    static {
        PythonLikeComparable.setup(STRING_TYPE.__dir__);
    }

    public PythonString(String value) {
        super(STRING_TYPE);
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof String) {
            return value.equals(o);
        } else if (o instanceof PythonString) {
            return ((PythonString) o).value.equals(value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static PythonString valueOf(String value) {
        return new PythonString(value);
    }

    public String getValue() {
        return value;
    }
}
