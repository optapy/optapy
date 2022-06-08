package org.optaplanner.python.translator.types;

import java.util.Map;

public class PythonString extends AbstractPythonLikeObject implements Comparable<PythonString> {
    public final String value;

    public final static PythonLikeType STRING_TYPE = new PythonLikeType("str", PythonString.class);

    static {
        try {
            PythonLikeComparable.setup(STRING_TYPE.__dir__);
            STRING_TYPE.__dir__.put("__len__", new JavaMethodReference(PythonString.class.getMethod("length"),
                    Map.of()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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

    public int length() {
        return value.length();
    }

    @Override
    public int compareTo(PythonString pythonString) {
        return value.compareTo(pythonString.value);
    }
}
