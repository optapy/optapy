package org.optaplanner.python.translator.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.errors.ValueError;

public class PythonFloat extends AbstractPythonLikeObject implements PythonNumber {
    final double value;

    public final static PythonLikeType FLOAT_TYPE = new PythonLikeType("float", PythonFloat.class, List.of(NUMBER_TYPE));

    static {
        try {
            PythonLikeComparable.setup(FLOAT_TYPE);
            PythonNumericOperations.setup(FLOAT_TYPE.__dir__);
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(FLOAT_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public PythonFloat(double value) {
        super(FLOAT_TYPE);
        this.value = value;
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        FLOAT_TYPE.setConstructor((positionalArguments, namedArguments) -> {
            if (positionalArguments.isEmpty()) {
                return new PythonFloat(0.0);
            } else if (positionalArguments.size() == 1) {
                PythonLikeObject value = positionalArguments.get(0);
                if (value instanceof PythonInteger) {
                    return ((PythonInteger) value).asFloat();
                } else if (value instanceof PythonFloat) {
                    return value;
                } else {
                    PythonLikeType valueType = value.__getType();
                    PythonLikeFunction asFloatFunction = (PythonLikeFunction) (valueType.__getAttributeOrError("__float__"));
                    return asFloatFunction.__call__(List.of(value), null);
                }
            } else {
                throw new ValueError("float takes 0 or 1 arguments, got " + positionalArguments.size());
            }
        });
        // Unary
        FLOAT_TYPE.addMethod(PythonUnaryOperator.AS_BOOLEAN, PythonFloat.class.getMethod("asBoolean"));
        FLOAT_TYPE.addMethod(PythonUnaryOperator.AS_INT, PythonFloat.class.getMethod("asInteger"));
        FLOAT_TYPE.addMethod(PythonUnaryOperator.POSITIVE, PythonFloat.class.getMethod("asFloat"));
        FLOAT_TYPE.addMethod(PythonUnaryOperator.NEGATIVE, PythonFloat.class.getMethod("negative"));
        FLOAT_TYPE.addMethod(PythonUnaryOperator.ABS, PythonFloat.class.getMethod("abs"));
        FLOAT_TYPE.addMethod(PythonUnaryOperator.HASH, PythonFloat.class.getMethod("computePythonHash"));

        // Binary
        FLOAT_TYPE.addMethod(PythonBinaryOperators.ADD, PythonFloat.class.getMethod("add", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.ADD, PythonFloat.class.getMethod("add", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.SUBTRACT, PythonFloat.class.getMethod("subtract", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.SUBTRACT, PythonFloat.class.getMethod("subtract", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.MULTIPLY, PythonFloat.class.getMethod("multiply", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.MULTIPLY, PythonFloat.class.getMethod("multiply", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE, PythonFloat.class.getMethod("trueDivide", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE, PythonFloat.class.getMethod("trueDivide", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                PythonFloat.class.getMethod("floorDivide", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.FLOOR_DIVIDE, PythonFloat.class.getMethod("floorDivide", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.DIVMOD, PythonFloat.class.getMethod("divmod", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.DIVMOD, PythonFloat.class.getMethod("divmod", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.MODULO, PythonFloat.class.getMethod("modulo", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.MODULO, PythonFloat.class.getMethod("modulo", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.POWER, PythonFloat.class.getMethod("power", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.POWER, PythonFloat.class.getMethod("power", PythonFloat.class));

        // Inplace Binary (identical to Binary)
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_ADD, PythonFloat.class.getMethod("add", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_ADD, PythonFloat.class.getMethod("add", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                PythonFloat.class.getMethod("subtract", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                PythonFloat.class.getMethod("subtract", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MULTIPLY,
                PythonFloat.class.getMethod("multiply", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MULTIPLY,
                PythonFloat.class.getMethod("multiply", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_TRUE_DIVIDE,
                PythonFloat.class.getMethod("trueDivide", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_TRUE_DIVIDE,
                PythonFloat.class.getMethod("trueDivide", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_FLOOR_DIVIDE,
                PythonFloat.class.getMethod("floorDivide", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_FLOOR_DIVIDE,
                PythonFloat.class.getMethod("floorDivide", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MODULO, PythonFloat.class.getMethod("modulo", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MODULO, PythonFloat.class.getMethod("modulo", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_POWER, PythonFloat.class.getMethod("power", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_POWER, PythonFloat.class.getMethod("power", PythonFloat.class));

        // Comparisons
        FLOAT_TYPE.addMethod(PythonBinaryOperators.EQUAL, PythonFloat.class.getMethod("equal", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.EQUAL, PythonFloat.class.getMethod("equal", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.NOT_EQUAL, PythonFloat.class.getMethod("notEqual", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.NOT_EQUAL, PythonFloat.class.getMethod("notEqual", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN, PythonFloat.class.getMethod("lessThan", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN, PythonFloat.class.getMethod("lessThan", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("lessThanOrEqual", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("lessThanOrEqual", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                PythonFloat.class.getMethod("greaterThan", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN, PythonFloat.class.getMethod("greaterThan", PythonFloat.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("greaterThanOrEqual", PythonInteger.class));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("greaterThanOrEqual", PythonFloat.class));

        // Other
        FLOAT_TYPE.addMethod("__round__", PythonFloat.class.getMethod("round"));
        FLOAT_TYPE.addMethod("__round__", PythonFloat.class.getMethod("round", PythonInteger.class));
    }

    @Override
    public Number getValue() {
        return value;
    }

    public PythonLikeTuple asFraction() {
        final BigInteger FIVE = BigInteger.valueOf(5L);

        BigDecimal bigDecimal = BigDecimal.valueOf(value);
        BigInteger numerator = bigDecimal.movePointRight(bigDecimal.scale()).toBigIntegerExact();
        BigInteger denominator;
        if (bigDecimal.scale() < 0) {
            denominator = BigInteger.ONE;
            numerator = numerator.multiply(BigInteger.TEN.pow(-bigDecimal.scale()));
        } else {
            denominator = BigInteger.TEN.pow(bigDecimal.scale());
        }

        // denominator is a multiple of 10, thus only have 5 and 2 as prime factors

        while (denominator.remainder(BigInteger.TWO).equals(BigInteger.ZERO)
                && numerator.remainder(BigInteger.TWO).equals(BigInteger.ZERO)) {
            denominator = denominator.shiftRight(1);
            numerator = numerator.shiftRight(1);
        }

        while (denominator.remainder(FIVE).equals(BigInteger.ZERO) && numerator.remainder(FIVE).equals(BigInteger.ZERO)) {
            denominator = denominator.divide(FIVE);
            numerator = numerator.divide(FIVE);
        }

        return PythonLikeTuple.fromList(List.of(PythonInteger.valueOf(numerator), PythonInteger.valueOf(denominator)));
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue() == value;
        } else if (o instanceof PythonFloat) {
            return ((PythonFloat) o).value == value;
        } else if (o instanceof PythonInteger) {
            return ((PythonInteger) o).getValue().doubleValue() == value;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    public PythonInteger computePythonHash() {
        if (Double.isNaN(value)) {
            return PythonInteger.valueOf(hashCode());
        } else if (Double.isInfinite(value)) {
            if (value > 0) {
                return INFINITY_HASH_VALUE;
            } else {
                return INFINITY_HASH_VALUE.negative();
            }
        }
        PythonLikeTuple fractionTuple = asFraction();
        return PythonNumber.computeHash((PythonInteger) fractionTuple.get(0), (PythonInteger) fractionTuple.get(1));
    }

    public static PythonFloat valueOf(float value) {
        return new PythonFloat(value);
    }

    public static PythonFloat valueOf(double value) {
        return new PythonFloat(value);
    }

    public PythonBoolean asBoolean() {
        return value == 0.0 ? PythonBoolean.FALSE : PythonBoolean.TRUE;
    }

    public PythonInteger asInteger() {
        return new PythonInteger((long) Math.floor(value));
    }

    public PythonFloat asFloat() {
        return this;
    }

    public PythonFloat negative() {
        return new PythonFloat(-value);
    }

    public PythonFloat abs() {
        return new PythonFloat(Math.abs(value));
    }

    public PythonFloat add(PythonInteger other) {
        return new PythonFloat(value + other.value.doubleValue());
    }

    public PythonFloat add(PythonFloat other) {
        return new PythonFloat(value + other.value);
    }

    public PythonFloat subtract(PythonInteger other) {
        return new PythonFloat(value - other.value.doubleValue());
    }

    public PythonFloat subtract(PythonFloat other) {
        return new PythonFloat(value - other.value);
    }

    public PythonFloat multiply(PythonInteger other) {
        return new PythonFloat(value * other.value.doubleValue());
    }

    public PythonFloat multiply(PythonFloat other) {
        return new PythonFloat(value * other.value);
    }

    public PythonFloat trueDivide(PythonInteger other) {
        return new PythonFloat(value / other.value.doubleValue());
    }

    public PythonFloat trueDivide(PythonFloat other) {
        return new PythonFloat(value / other.value);
    }

    public PythonInteger floorDivide(PythonInteger other) {
        return new PythonInteger((long) Math.floor(value / other.value.doubleValue()));
    }

    public PythonInteger floorDivide(PythonFloat other) {
        return PythonInteger.valueOf((long) Math.floor(value / other.value));
    }

    public PythonInteger ceilDivide(PythonInteger other) {
        return new PythonInteger((long) Math.ceil(value / other.value.doubleValue()));
    }

    public PythonInteger ceilDivide(PythonFloat other) {
        return PythonInteger.valueOf((long) Math.ceil(value / other.value));
    }

    public PythonFloat modulo(PythonInteger other) {
        return new PythonFloat(value % other.value.doubleValue());
    }

    public PythonFloat modulo(PythonFloat other) {
        return new PythonFloat(value % other.value);
    }

    public PythonLikeTuple divmod(PythonInteger other) {
        PythonInteger quotient;

        if (value < 0 == other.value.compareTo(BigInteger.ZERO) < 0) {
            // Same sign, use floor division
            quotient = floorDivide(other);
        } else {
            // Different sign, use ceil division
            quotient = ceilDivide(other);
        }
        PythonInteger.valueOf(Math.round(value / other.value.doubleValue()));
        PythonFloat remainder = modulo(other);

        // Python remainder has sign of divisor
        if (other.value.compareTo(BigInteger.ZERO) < 0) {
            if (remainder.value > 0) {
                quotient = quotient.subtract(PythonInteger.ONE);
                remainder = remainder.add(other);
            }
        } else {
            if (remainder.value < 0) {
                quotient = quotient.subtract(PythonInteger.ONE);
                remainder = remainder.add(other);
            }
        }
        return PythonLikeTuple.fromList(List.of(quotient.asFloat(),
                remainder));
    }

    public PythonLikeTuple divmod(PythonFloat other) {
        PythonInteger quotient;

        if (value < 0 == other.value < 0) {
            // Same sign, use floor division
            quotient = floorDivide(other);
        } else {
            // Different sign, use ceil division
            quotient = ceilDivide(other);
        }
        PythonFloat remainder = modulo(other);

        // Python remainder has sign of divisor
        if (other.value < 0) {
            if (remainder.value > 0) {
                quotient = quotient.subtract(PythonInteger.ONE);
                remainder = remainder.add(other);
            }
        } else {
            if (remainder.value < 0) {
                quotient = quotient.subtract(PythonInteger.ONE);
                remainder = remainder.add(other);
            }
        }
        return PythonLikeTuple.fromList(List.of(quotient.asFloat(),
                remainder));
    }

    public PythonInteger round() {
        if (value % 1.0 == 0.5) {
            long floor = (long) Math.floor(value);
            if (floor % 2 == 0) {
                return PythonInteger.valueOf(floor);
            } else {
                return PythonInteger.valueOf(floor + 1);
            }
        }
        return PythonInteger.valueOf(Math.round(value));
    }

    public PythonNumber round(PythonInteger digitsAfterDecimal) {
        if (digitsAfterDecimal.equals(PythonInteger.ZERO)) {
            return round();
        }

        BigDecimal asDecimal = BigDecimal.valueOf(value);
        return new PythonFloat(
                asDecimal.setScale(digitsAfterDecimal.value.intValueExact(), RoundingMode.HALF_EVEN).doubleValue());
    }

    public PythonFloat power(PythonInteger other) {
        return new PythonFloat(Math.pow(value, other.value.doubleValue()));
    }

    public PythonFloat power(PythonFloat other) {
        return new PythonFloat(Math.pow(value, other.value));
    }

    public PythonBoolean equal(PythonInteger other) {
        return PythonBoolean.valueOf(value == other.value.doubleValue());
    }

    public PythonBoolean notEqual(PythonInteger other) {
        return PythonBoolean.valueOf(value != other.value.doubleValue());
    }

    public PythonBoolean lessThan(PythonInteger other) {
        return PythonBoolean.valueOf(value < other.value.doubleValue());
    }

    public PythonBoolean lessThanOrEqual(PythonInteger other) {
        return PythonBoolean.valueOf(value <= other.value.doubleValue());
    }

    public PythonBoolean greaterThan(PythonInteger other) {
        return PythonBoolean.valueOf(value > other.value.doubleValue());
    }

    public PythonBoolean greaterThanOrEqual(PythonInteger other) {
        return PythonBoolean.valueOf(value >= other.value.doubleValue());
    }

    public PythonBoolean equal(PythonFloat other) {
        return PythonBoolean.valueOf(value == other.value);
    }

    public PythonBoolean notEqual(PythonFloat other) {
        return PythonBoolean.valueOf(value != other.value);
    }

    public PythonBoolean lessThan(PythonFloat other) {
        return PythonBoolean.valueOf(value < other.value);
    }

    public PythonBoolean lessThanOrEqual(PythonFloat other) {
        return PythonBoolean.valueOf(value <= other.value);
    }

    public PythonBoolean greaterThan(PythonFloat other) {
        return PythonBoolean.valueOf(value > other.value);
    }

    public PythonBoolean greaterThanOrEqual(PythonFloat other) {
        return PythonBoolean.valueOf(value >= other.value);
    }
}
