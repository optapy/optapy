package org.optaplanner.optapy.translator.types;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class PythonBoolean extends AbstractPythonLikeObject {
    public final static PythonBoolean TRUE;
    public final static PythonBoolean FALSE;

    private final static PythonLikeType BOOLEAN_TYPE = new PythonLikeType("bool");

    static {
        BOOLEAN_TYPE.__dir__.put("__bool__", new UnaryLambdaReference(self -> self, Map.of()));
        PythonNumericOperations.setup(BOOLEAN_TYPE.__dir__);
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
        return (result)? TRUE : FALSE;
    }

    @Override
    public String toString() {
        if (this == TRUE) {
            return "True";
        } else {
            return "False";
        }
    }
}
