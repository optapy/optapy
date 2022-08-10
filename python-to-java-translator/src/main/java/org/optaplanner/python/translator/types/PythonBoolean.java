package org.optaplanner.python.translator.types;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.errors.ValueError;

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
            BOOLEAN_TYPE.setConstructor(((positionalArguments, namedArguments) -> {
                if (!namedArguments.isEmpty()) {
                    throw new ValueError("bool does not take named arguments");
                }
                if (positionalArguments.isEmpty()) {
                    return FALSE;
                } else if (positionalArguments.size() == 1) {
                    return PythonBoolean.valueOf(PythonBoolean.isTruthful(positionalArguments.get(0)));
                } else {
                    throw new ValueError("bool expects 0 or 1 arguments, got " + positionalArguments.size());
                }
            }));
            PythonOverloadImplementor.createDispatchesFor(BOOLEAN_TYPE);
            return BOOLEAN_TYPE;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final boolean booleanValue;

    private PythonBoolean(boolean booleanValue) {
        super(BOOLEAN_TYPE, booleanValue ? BigInteger.ONE : BigInteger.ZERO);
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

    public static boolean isTruthful(PythonLikeObject tested) {
        if (tested instanceof PythonBoolean) {
            return tested == TRUE;
        } else if (tested instanceof PythonInteger) {
            return ((PythonInteger) tested).asBoolean() == TRUE;
        } else if (tested instanceof PythonFloat) {
            return ((PythonFloat) tested).asBoolean() == TRUE;
        } else if (tested instanceof PythonNone) {
            return false;
        } else if (tested instanceof Collection) {
            return ((Collection<?>) tested).size() == 0;
        } else if (tested instanceof Map) {
            return ((Map<?, ?>) tested).size() == 0;
        } else {
            PythonLikeType testedType = tested.__getType();
            PythonLikeFunction boolMethod = (PythonLikeFunction) testedType.__getAttributeOrNull("__bool__");
            if (boolMethod != null) {
                return isTruthful(boolMethod.__call__(List.of(tested), Map.of()));
            }

            PythonLikeFunction lenMethod = (PythonLikeFunction) testedType.__getAttributeOrNull("__len__");
            if (lenMethod != null) {
                return isTruthful(lenMethod.__call__(List.of(tested), Map.of()));
            }

            return true;
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
