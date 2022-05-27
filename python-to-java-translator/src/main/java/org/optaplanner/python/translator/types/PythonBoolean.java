package org.optaplanner.python.translator.types;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PythonBoolean extends PythonInteger {
    public final static PythonBoolean TRUE;
    public final static PythonBoolean FALSE;

    public final static PythonLikeType BOOLEAN_TYPE = new PythonLikeType("bool", PythonBoolean.class, List.of(INT_TYPE));

    static {
        BOOLEAN_TYPE.__dir__.put("__bool__", new UnaryLambdaReference(self -> self, Map.of()));
        TRUE = new PythonBoolean(true);
        FALSE = new PythonBoolean(false);
    }

    private final boolean booleanValue;

    private PythonBoolean(boolean booleanValue) {
        super(booleanValue ? 1L : 0L);
        this.booleanValue = booleanValue;
    }

    public boolean getBooleanValue() {
        return booleanValue;
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
    public PythonLikeType __getType() {
        return BOOLEAN_TYPE;
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
        return booleanValue == that.booleanValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValue);
    }
}
