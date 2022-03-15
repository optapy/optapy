package org.optaplanner.optapy.translator.types;

public class PythonBoolean extends AbstractPythonLikeObject {
    public static PythonBoolean TRUE = new PythonBoolean(true);
    public static PythonBoolean FALSE = new PythonBoolean(false);

    private final boolean value;

    public PythonBoolean(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public static PythonBoolean valueOf(boolean result) {
        return (result)? TRUE : FALSE;
    }
}
