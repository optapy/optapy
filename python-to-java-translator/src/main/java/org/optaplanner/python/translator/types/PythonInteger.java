package org.optaplanner.python.translator.types;

import static org.optaplanner.python.translator.types.PythonFloat.FLOAT_TYPE;

import java.math.BigInteger;
import java.util.List;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.errors.ValueError;

public class PythonInteger extends AbstractPythonLikeObject implements PythonNumber {
    final BigInteger value;

    public static PythonLikeType INT_TYPE = getIntType();
    public final static PythonInteger ZERO = new PythonInteger(BigInteger.ZERO);

    public static PythonLikeType getIntType() {
        if (INT_TYPE != null) {
            return INT_TYPE;
        }
        INT_TYPE = new PythonLikeType("int", PythonInteger.class, List.of(NUMBER_TYPE));
        try {
            PythonLikeComparable.setup(INT_TYPE);
            PythonNumericOperations.setup(INT_TYPE.__dir__);
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(INT_TYPE);
            return INT_TYPE;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        INT_TYPE.setConstructor(((positionalArguments, namedArguments) -> {
            if (positionalArguments.size() == 0) {
                return PythonInteger.valueOf(0);
            } else if (positionalArguments.size() == 1) {
                PythonLikeObject value = positionalArguments.get(0);
                if (value instanceof PythonInteger) {
                    return value;
                } else if (value instanceof PythonFloat) {
                    return ((PythonFloat) value).asInteger();
                } else {
                    PythonLikeType valueType = value.__getType();
                    PythonLikeFunction asIntFunction = (PythonLikeFunction) (valueType.__getAttributeOrError("__int__"));
                    return asIntFunction.__call__(List.of(value), null);
                }
            } else {
                throw new ValueError("int expects 0 or 1 arguments, got " + positionalArguments.size());
            }
        }));
        // Unary
        INT_TYPE.addMethod(PythonUnaryOperator.AS_BOOLEAN,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("asBoolean")),
                        PythonBoolean.getBooleanType()));
        INT_TYPE.addMethod(PythonUnaryOperator.AS_INT,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("asInteger")),
                        INT_TYPE));
        INT_TYPE.addMethod(PythonUnaryOperator.AS_FLOAT,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("asFloat")),
                        FLOAT_TYPE));
        INT_TYPE.addMethod(PythonUnaryOperator.AS_INDEX,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("asInteger")),
                        INT_TYPE));
        INT_TYPE.addMethod(PythonUnaryOperator.POSITIVE,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("asInteger")),
                        INT_TYPE));
        INT_TYPE.addMethod(PythonUnaryOperator.NEGATIVE,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("negative")),
                        INT_TYPE));
        INT_TYPE.addMethod(PythonUnaryOperator.INVERT,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("invert")),
                        INT_TYPE));
        INT_TYPE.addMethod(PythonUnaryOperator.ABS,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("abs")),
                        INT_TYPE));

        // Binary
        INT_TYPE.addMethod(PythonBinaryOperators.ADD,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("add", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.ADD,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("add", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("subtract", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("subtract", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.MULTIPLY,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("multiply", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.MULTIPLY,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("multiply", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("trueDivide", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("trueDivide", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("floorDivide", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("floorDivide", PythonFloat.class)),
                        INT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.MODULO,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("modulo", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.MODULO,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("modulo", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.POWER,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("power", PythonInteger.class)),
                        NUMBER_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.POWER,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("power", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.LSHIFT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("shiftLeft", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.RSHIFT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("shiftRight", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.AND,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("bitwiseAnd", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.OR,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("bitwiseOr", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.XOR,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("bitwiseXor", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));

        // Inplace Binary (identical to Binary)
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_ADD,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("add", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_ADD,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("add", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("subtract", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("subtract", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MULTIPLY,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("multiply", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MULTIPLY,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("multiply", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("trueDivide", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("trueDivide", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("floorDivide", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("floorDivide", PythonFloat.class)),
                        INT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MODULO,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("modulo", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MODULO,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("modulo", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_POWER,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("power", PythonInteger.class)),
                        NUMBER_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_POWER,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("power", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_LSHIFT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("shiftLeft", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_RSHIFT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("shiftRight", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_AND,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("bitwiseAnd", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_OR,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("bitwiseOr", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.INPLACE_XOR,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("bitwiseXor", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));

        // Comparisons
        INT_TYPE.addMethod(PythonBinaryOperators.EQUAL,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("equal", PythonInteger.class)),
                        PythonBoolean.getBooleanType(), INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.EQUAL,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("equal", PythonFloat.class)),
                        PythonBoolean.getBooleanType(), FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.NOT_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("notEqual", PythonInteger.class)),
                        PythonBoolean.getBooleanType(), INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.NOT_EQUAL,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("notEqual", PythonFloat.class)),
                        PythonBoolean.getBooleanType(), FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("lessThan", PythonInteger.class)),
                        PythonBoolean.getBooleanType(), INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN,
                new PythonFunctionSignature(new MethodDescriptor(PythonInteger.class.getMethod("lessThan", PythonFloat.class)),
                        PythonBoolean.getBooleanType(), FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("lessThanOrEqual", PythonInteger.class)),
                        PythonBoolean.getBooleanType(), INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("lessThanOrEqual", PythonFloat.class)),
                        PythonBoolean.getBooleanType(), FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("greaterThan", PythonInteger.class)),
                        PythonBoolean.getBooleanType(), INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("greaterThan", PythonFloat.class)),
                        PythonBoolean.getBooleanType(), FLOAT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("greaterThanOrEqual", PythonInteger.class)),
                        PythonBoolean.getBooleanType(), INT_TYPE));
        INT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonInteger.class.getMethod("greaterThanOrEqual", PythonFloat.class)),
                        PythonBoolean.getBooleanType(), FLOAT_TYPE));
    }

    public PythonInteger(PythonLikeType type) {
        super(type);
        this.value = BigInteger.ZERO;
    }

    public PythonInteger(PythonLikeType type, BigInteger value) {
        super(type);
        this.value = value;
    }

    public PythonInteger(long value) {
        this(BigInteger.valueOf(value));
    }

    public PythonInteger(BigInteger value) {
        super(getIntType());
        this.value = value;
    }

    @Override
    public Number getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Number) {
            return value.equals(BigInteger.valueOf(((Number) o).longValue()));
        } else if (o instanceof PythonInteger) {
            return ((PythonInteger) o).value.equals(value);
        } else if (o instanceof PythonFloat) {
            return value.doubleValue() == ((PythonFloat) o).value;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static PythonInteger valueOf(byte value) {
        return new PythonInteger(value);
    }

    public static PythonInteger valueOf(short value) {
        return new PythonInteger(value);
    }

    public static PythonInteger valueOf(int value) {
        return new PythonInteger(value);
    }

    public static PythonInteger valueOf(long value) {
        return new PythonInteger(value);
    }

    public static PythonInteger valueOf(BigInteger value) {
        return new PythonInteger(value);
    }

    public PythonBoolean asBoolean() {
        return value.signum() == 0 ? PythonBoolean.FALSE : PythonBoolean.TRUE;
    }

    public PythonInteger asInteger() {
        return this;
    }

    public PythonFloat asFloat() {
        return new PythonFloat(value.doubleValue());
    }

    public PythonInteger negative() {
        return new PythonInteger(value.negate());
    }

    public PythonInteger invert() {
        return new PythonInteger(value.add(BigInteger.ONE).negate());
    }

    public PythonInteger abs() {
        return new PythonInteger(value.abs());
    }

    public PythonInteger add(PythonInteger other) {
        return new PythonInteger(value.add(other.value));
    }

    public PythonFloat add(PythonFloat other) {
        return new PythonFloat(value.doubleValue() + other.value);
    }

    public PythonInteger subtract(PythonInteger other) {
        return new PythonInteger(value.subtract(other.value));
    }

    public PythonFloat subtract(PythonFloat other) {
        return new PythonFloat(value.doubleValue() - other.value);
    }

    public PythonInteger multiply(PythonInteger other) {
        return new PythonInteger(value.multiply(other.value));
    }

    public PythonFloat multiply(PythonFloat other) {
        return new PythonFloat(value.doubleValue() * other.value);
    }

    public PythonFloat trueDivide(PythonInteger other) {
        return new PythonFloat(value.doubleValue() / other.value.doubleValue());
    }

    public PythonFloat trueDivide(PythonFloat other) {
        return new PythonFloat(value.doubleValue() / other.value);
    }

    public PythonInteger floorDivide(PythonInteger other) {
        return new PythonInteger(value.divide(other.value));
    }

    public PythonInteger floorDivide(PythonFloat other) {
        return PythonInteger.valueOf((long) Math.floor(value.doubleValue() / other.value));
    }

    public PythonInteger modulo(PythonInteger other) {
        return new PythonInteger(value.remainder(other.value));
    }

    public PythonFloat modulo(PythonFloat other) {
        return new PythonFloat(value.doubleValue() % other.value);
    }

    public PythonNumber power(PythonInteger other) {
        if (other.value.signum() >= 0) {
            return new PythonInteger(value.pow(other.value.intValueExact()));
        }
        return new PythonFloat(Math.pow(value.doubleValue(), other.value.doubleValue()));
    }

    public PythonFloat power(PythonFloat other) {
        return new PythonFloat(Math.pow(value.doubleValue(), other.value));
    }

    public PythonInteger shiftLeft(PythonInteger other) {
        return new PythonInteger(value.shiftLeft(other.value.intValueExact()));
    }

    public PythonInteger shiftRight(PythonInteger other) {
        return new PythonInteger(value.shiftRight(other.value.intValueExact()));
    }

    public PythonInteger bitwiseAnd(PythonInteger other) {
        return new PythonInteger(value.and(other.value));
    }

    public PythonInteger bitwiseOr(PythonInteger other) {
        return new PythonInteger(value.or(other.value));
    }

    public PythonInteger bitwiseXor(PythonInteger other) {
        return new PythonInteger(value.xor(other.value));
    }

    public PythonBoolean equal(PythonInteger other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) == 0);
    }

    public PythonBoolean notEqual(PythonInteger other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) != 0);
    }

    public PythonBoolean lessThan(PythonInteger other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) < 0);
    }

    public PythonBoolean lessThanOrEqual(PythonInteger other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) <= 0);
    }

    public PythonBoolean greaterThan(PythonInteger other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) > 0);
    }

    public PythonBoolean greaterThanOrEqual(PythonInteger other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) >= 0);
    }

    public PythonBoolean equal(PythonFloat other) {
        return PythonBoolean.valueOf(value.doubleValue() == other.value);
    }

    public PythonBoolean notEqual(PythonFloat other) {
        return PythonBoolean.valueOf(value.doubleValue() != other.value);
    }

    public PythonBoolean lessThan(PythonFloat other) {
        return PythonBoolean.valueOf(value.doubleValue() < other.value);
    }

    public PythonBoolean lessThanOrEqual(PythonFloat other) {
        return PythonBoolean.valueOf(value.doubleValue() <= other.value);
    }

    public PythonBoolean greaterThan(PythonFloat other) {
        return PythonBoolean.valueOf(value.doubleValue() > other.value);
    }

    public PythonBoolean greaterThanOrEqual(PythonFloat other) {
        return PythonBoolean.valueOf(value.doubleValue() >= other.value);
    }
}
