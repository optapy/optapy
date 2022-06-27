package org.optaplanner.python.translator.types;

import java.util.List;
import java.util.Objects;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;

public class PythonBoolean extends PythonInteger {
    public final static PythonBoolean TRUE = new PythonBoolean(true);
    public final static PythonBoolean FALSE = new PythonBoolean(false);

    public static PythonLikeType BOOLEAN_TYPE = getBooleanType();

    public static PythonLikeType getBooleanType() {
        if (BOOLEAN_TYPE != null) {
            return BOOLEAN_TYPE;
        }
        BOOLEAN_TYPE = new PythonLikeType("bool", PythonBoolean.class, List.of(getIntType()));
        try {
            BOOLEAN_TYPE.addMethod(PythonUnaryOperator.AS_BOOLEAN,
                    new PythonFunctionSignature(new MethodDescriptor(
                            PythonBoolean.class.getMethod("asBoolean")),
                            BOOLEAN_TYPE));
            PythonOverloadImplementor.createDispatchesFor(BOOLEAN_TYPE);
            return BOOLEAN_TYPE;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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

    public PythonBoolean asBoolean() {
        return this;
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
