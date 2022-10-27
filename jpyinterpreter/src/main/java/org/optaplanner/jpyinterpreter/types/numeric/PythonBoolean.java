package org.optaplanner.jpyinterpreter.types.numeric;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.optaplanner.jpyinterpreter.MethodDescriptor;
import org.optaplanner.jpyinterpreter.PythonFunctionSignature;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.builtins.GlobalBuiltins;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.errors.ValueError;

public class PythonBoolean extends PythonInteger {
    public final static PythonBoolean TRUE = new PythonBoolean(true);
    public final static PythonBoolean FALSE = new PythonBoolean(false);

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonBoolean::registerMethods);
    }

    public static PythonLikeType registerMethods() {
        try {
            BuiltinTypes.BOOLEAN_TYPE.addUnaryMethod(PythonUnaryOperator.AS_BOOLEAN,
                    new PythonFunctionSignature(new MethodDescriptor(
                            PythonBoolean.class.getMethod("asBoolean")),
                            BuiltinTypes.BOOLEAN_TYPE));
            BuiltinTypes.BOOLEAN_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                    new PythonFunctionSignature(new MethodDescriptor(
                            PythonBoolean.class.getMethod("asString")),
                            BuiltinTypes.STRING_TYPE));
            BuiltinTypes.BOOLEAN_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                    new PythonFunctionSignature(new MethodDescriptor(
                            PythonBoolean.class.getMethod("asString")),
                            BuiltinTypes.STRING_TYPE));
            BuiltinTypes.BOOLEAN_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
                if (namedArguments.size() > 1) {
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

            GlobalBuiltins.addBuiltinConstant("True", TRUE);
            GlobalBuiltins.addBuiltinConstant("False", FALSE);
            return BuiltinTypes.BOOLEAN_TYPE;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final boolean booleanValue;

    private PythonBoolean(boolean booleanValue) {
        super(BuiltinTypes.BOOLEAN_TYPE, booleanValue ? BigInteger.ONE : BigInteger.ZERO);
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
                return isTruthful(boolMethod.$call(List.of(tested), Map.of(), null));
            }

            PythonLikeFunction lenMethod = (PythonLikeFunction) testedType.__getAttributeOrNull("__len__");
            if (lenMethod != null) {
                return isTruthful(lenMethod.$call(List.of(tested), Map.of(), null));
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
        return BuiltinTypes.BOOLEAN_TYPE;
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

    public PythonString asString() {
        return PythonString.valueOf(toString());
    }
}
