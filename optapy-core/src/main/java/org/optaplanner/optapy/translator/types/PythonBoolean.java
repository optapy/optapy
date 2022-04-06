package org.optaplanner.optapy.translator.types;

import java.util.Collections;
import java.util.function.Function;

public class PythonBoolean extends AbstractPythonLikeObject {
    public static PythonBoolean TRUE = new PythonBoolean(true);
    public static PythonBoolean FALSE = new PythonBoolean(false);

    private final static PythonLikeType BOOLEAN_TYPE = new PythonLikeType("bool");

    static {
        try {
            BOOLEAN_TYPE.__setattribute__("__bool__", new JavaMethodReference(Function.class.getMethod("identity"),
                                                                              Collections.emptyMap()));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
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
}
