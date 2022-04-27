package org.optaplanner.python.translator.types;

import java.util.Map;
import java.util.Objects;

public class PythonBoolean extends AbstractPythonLikeObject {
    public final static PythonBoolean TRUE;
    public final static PythonBoolean FALSE;

    private final static PythonLikeType BOOLEAN_TYPE = new PythonLikeType("bool");

    static {
        PythonNumericOperations.setup(BOOLEAN_TYPE.__dir__);
        BOOLEAN_TYPE.__dir__.put("__bool__", new UnaryLambdaReference(self -> self, Map.of()));
        BOOLEAN_TYPE.__dir__.remove("__format__"); // Bool uses object format, not number format
        TRUE = new PythonBoolean(true);
        FALSE = new PythonBoolean(false);
    }

    private final boolean value;

    private PythonBoolean(boolean value) {
        super(BOOLEAN_TYPE);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public PythonBoolean not() {
        if (this == TRUE) {
            return FALSE;
        } else {
            return TRUE;
        }
    }

    public static PythonBoolean valueOf(boolean result) {
        return (result) ? TRUE : FALSE;
    }

    @Override
    public String toString() {
        if (this == TRUE) {
            return "True";
        } else {
            return "False";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonBoolean that = (PythonBoolean) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
