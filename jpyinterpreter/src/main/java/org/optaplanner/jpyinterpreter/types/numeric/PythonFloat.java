package org.optaplanner.jpyinterpreter.types.numeric;

import static org.optaplanner.jpyinterpreter.types.numeric.PythonInteger.ZERO;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.NotImplemented;
import org.optaplanner.jpyinterpreter.types.PythonLikeComparable;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.types.errors.TypeError;
import org.optaplanner.jpyinterpreter.types.errors.ValueError;
import org.optaplanner.jpyinterpreter.types.errors.arithmetic.ZeroDivisionError;
import org.optaplanner.jpyinterpreter.util.DefaultFormatSpec;
import org.optaplanner.jpyinterpreter.util.StringFormatter;

public class PythonFloat extends AbstractPythonLikeObject implements PythonNumber {
    public final double value;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonFloat::registerMethods);
    }

    public PythonFloat(double value) {
        super(BuiltinTypes.FLOAT_TYPE);
        this.value = value;
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        PythonLikeComparable.setup(BuiltinTypes.FLOAT_TYPE);

        // Constructor
        BuiltinTypes.FLOAT_TYPE.setConstructor((positionalArguments, namedArguments, callerInstance) -> {
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
                    return asFloatFunction.$call(List.of(value), Map.of(), null);
                }
            } else {
                throw new ValueError("float takes 0 or 1 arguments, got " + positionalArguments.size());
            }
        });
        // Unary
        BuiltinTypes.FLOAT_TYPE.addUnaryMethod(PythonUnaryOperator.AS_BOOLEAN, PythonFloat.class.getMethod("asBoolean"));
        BuiltinTypes.FLOAT_TYPE.addUnaryMethod(PythonUnaryOperator.AS_INT, PythonFloat.class.getMethod("asInteger"));
        BuiltinTypes.FLOAT_TYPE.addUnaryMethod(PythonUnaryOperator.POSITIVE, PythonFloat.class.getMethod("asFloat"));
        BuiltinTypes.FLOAT_TYPE.addUnaryMethod(PythonUnaryOperator.NEGATIVE, PythonFloat.class.getMethod("negative"));
        BuiltinTypes.FLOAT_TYPE.addUnaryMethod(PythonUnaryOperator.ABS, PythonFloat.class.getMethod("abs"));
        BuiltinTypes.FLOAT_TYPE.addUnaryMethod(PythonUnaryOperator.HASH, PythonFloat.class.getMethod("$method$__hash__"));

        // Binary
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.ADD,
                PythonFloat.class.getMethod("add", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.ADD,
                PythonFloat.class.getMethod("add", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.ADD,
                PythonFloat.class.getMethod("add", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.SUBTRACT,
                PythonFloat.class.getMethod("subtract", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.SUBTRACT,
                PythonFloat.class.getMethod("subtract", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.SUBTRACT,
                PythonFloat.class.getMethod("subtract", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MULTIPLY,
                PythonFloat.class.getMethod("multiply", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MULTIPLY,
                PythonFloat.class.getMethod("multiply", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MULTIPLY,
                PythonFloat.class.getMethod("multiply", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                PythonFloat.class.getMethod("trueDivide", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                PythonFloat.class.getMethod("trueDivide", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                PythonFloat.class.getMethod("trueDivide", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                PythonFloat.class.getMethod("floorDivide", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                PythonFloat.class.getMethod("floorDivide", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                PythonFloat.class.getMethod("floorDivide", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.DIVMOD,
                PythonFloat.class.getMethod("divmod", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.DIVMOD,
                PythonFloat.class.getMethod("divmod", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.DIVMOD,
                PythonFloat.class.getMethod("divmod", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MODULO,
                PythonFloat.class.getMethod("modulo", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MODULO,
                PythonFloat.class.getMethod("modulo", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MODULO,
                PythonFloat.class.getMethod("modulo", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.POWER,
                PythonFloat.class.getMethod("power", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.POWER,
                PythonFloat.class.getMethod("power", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.POWER,
                PythonFloat.class.getMethod("power", PythonFloat.class));

        // Comparisons
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.EQUAL,
                PythonFloat.class.getMethod("equal", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.EQUAL,
                PythonFloat.class.getMethod("equal", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.EQUAL,
                PythonFloat.class.getMethod("equal", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonFloat.class.getMethod("notEqual", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonFloat.class.getMethod("notEqual", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonFloat.class.getMethod("notEqual", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN,
                PythonFloat.class.getMethod("lessThan", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN,
                PythonFloat.class.getMethod("lessThan", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN,
                PythonFloat.class.getMethod("lessThan", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("lessThanOrEqual", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("lessThanOrEqual", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("lessThanOrEqual", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN,
                PythonFloat.class.getMethod("greaterThan", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN,
                PythonFloat.class.getMethod("greaterThan", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN,
                PythonFloat.class.getMethod("greaterThan", PythonFloat.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("greaterThanOrEqual", PythonLikeObject.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("greaterThanOrEqual", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonFloat.class.getMethod("greaterThanOrEqual", PythonFloat.class));

        // Other
        BuiltinTypes.FLOAT_TYPE.addMethod("__round__", PythonFloat.class.getMethod("round"));
        BuiltinTypes.FLOAT_TYPE.addMethod("__round__", PythonFloat.class.getMethod("round", PythonInteger.class));
        BuiltinTypes.FLOAT_TYPE.addBinaryMethod(PythonBinaryOperators.FORMAT,
                PythonFloat.class.getMethod("$method$__format__"));
        BuiltinTypes.FLOAT_TYPE.addBinaryMethod(PythonBinaryOperators.FORMAT,
                PythonFloat.class.getMethod("$method$__format__", PythonLikeObject.class));

        return BuiltinTypes.FLOAT_TYPE;
    }

    @Override
    public Number getValue() {
        return value;
    }

    public PythonLikeTuple asFraction() {
        final BigInteger FIVE = BigInteger.valueOf(5L);

        BigDecimal bigDecimal = new BigDecimal(value);
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
        return $method$__hash__().value.intValue();
    }

    @Override
    public PythonInteger $method$__hash__() {
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

    public PythonLikeObject add(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return add((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return add((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonFloat add(PythonInteger other) {
        return new PythonFloat(value + other.value.doubleValue());
    }

    public PythonFloat add(PythonFloat other) {
        return new PythonFloat(value + other.value);
    }

    public PythonLikeObject subtract(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return subtract((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return subtract((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonFloat subtract(PythonInteger other) {
        return new PythonFloat(value - other.value.doubleValue());
    }

    public PythonFloat subtract(PythonFloat other) {
        return new PythonFloat(value - other.value);
    }

    public PythonLikeObject multiply(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return multiply((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return multiply((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonFloat multiply(PythonInteger other) {
        return new PythonFloat(value * other.value.doubleValue());
    }

    public PythonFloat multiply(PythonFloat other) {
        return new PythonFloat(value * other.value);
    }

    public PythonLikeObject trueDivide(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return trueDivide((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return trueDivide((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonFloat trueDivide(PythonInteger other) {
        if (other.value.equals(BigInteger.ZERO)) {
            throw new ZeroDivisionError("float division");
        }
        return new PythonFloat(value / other.value.doubleValue());
    }

    public PythonFloat trueDivide(PythonFloat other) {
        if (other.value == 0) {
            throw new ZeroDivisionError("float division");
        }
        return new PythonFloat(value / other.value);
    }

    public PythonLikeObject floorDivide(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return floorDivide((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return floorDivide((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonFloat floorDivide(PythonInteger other) {
        if (other.value.equals(BigInteger.ZERO)) {
            throw new ZeroDivisionError("float division");
        }
        return new PythonFloat(new BigDecimal(value)
                .divideToIntegralValue(new BigDecimal(other.value))
                .doubleValue());
    }

    public PythonFloat floorDivide(PythonFloat other) {
        if (other.value == 0) {
            throw new ZeroDivisionError("float division");
        }
        return PythonFloat.valueOf(Math.floor(value / other.value));
    }

    public PythonFloat ceilDivide(PythonInteger other) {
        if (other.value.equals(BigInteger.ZERO)) {
            throw new ZeroDivisionError("float division");
        }
        return new PythonFloat(new BigDecimal(value)
                .divide(new BigDecimal(other.value), RoundingMode.CEILING)
                .doubleValue());
    }

    public PythonFloat ceilDivide(PythonFloat other) {
        if (other.value == 0) {
            throw new ZeroDivisionError("float division");
        }
        return PythonFloat.valueOf(Math.ceil(value / other.value));
    }

    public PythonLikeObject modulo(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return modulo((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return modulo((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonFloat modulo(PythonInteger other) {
        int remainderSign = other.compareTo(ZERO);

        if (remainderSign == 0) {
            throw new ZeroDivisionError("float modulo");
        } else if (remainderSign > 0) {
            double remainder = value % other.value.doubleValue();
            if (remainder < 0) {
                remainder = remainder + other.value.doubleValue();
            }
            return new PythonFloat(remainder);
        } else {
            double remainder = value % other.value.doubleValue();
            if (remainder > 0) {
                remainder = remainder + other.value.doubleValue();
            }
            return new PythonFloat(remainder);
        }
    }

    public PythonFloat modulo(PythonFloat other) {
        if (other.value == 0) {
            throw new ZeroDivisionError("float modulo");
        } else if (other.value > 0) {
            double remainder = value % other.value;
            if (remainder < 0) {
                remainder = remainder + other.value;
            }
            return new PythonFloat(remainder);
        } else {
            double remainder = value % other.value;
            if (remainder > 0) {
                remainder = remainder + other.value;
            }
            return new PythonFloat(remainder);
        }
    }

    public PythonLikeObject divmod(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return divmod((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return divmod((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonLikeTuple divmod(PythonInteger other) {
        PythonFloat quotient;

        if (value < 0 == other.value.compareTo(BigInteger.ZERO) < 0) {
            // Same sign, use floor division
            quotient = floorDivide(other);
        } else {
            // Different sign, use ceil division
            quotient = ceilDivide(other);
        }
        PythonInteger.valueOf(Math.round(value / other.value.doubleValue()));
        double remainder = value % other.value.doubleValue();

        // Python remainder has sign of divisor
        if (other.value.compareTo(BigInteger.ZERO) < 0) {
            if (remainder > 0) {
                quotient = quotient.subtract(PythonInteger.ONE);
                remainder = remainder + other.value.doubleValue();
            }
        } else {
            if (remainder < 0) {
                quotient = quotient.subtract(PythonInteger.ONE);
                remainder = remainder + other.value.doubleValue();
            }
        }
        return PythonLikeTuple.fromList(List.of(quotient, new PythonFloat(remainder)));
    }

    public PythonLikeTuple divmod(PythonFloat other) {
        PythonFloat quotient;

        if (value < 0 == other.value < 0) {
            // Same sign, use floor division
            quotient = floorDivide(other);
        } else {
            // Different sign, use ceil division
            quotient = ceilDivide(other);
        }
        double remainder = value % other.value;

        // Python remainder has sign of divisor
        if (other.value < 0) {
            if (remainder > 0) {
                quotient = quotient.subtract(PythonInteger.ONE);
                remainder = remainder + other.value;
            }
        } else {
            if (remainder < 0) {
                quotient = quotient.subtract(PythonInteger.ONE);
                remainder = remainder + other.value;
            }
        }
        return PythonLikeTuple.fromList(List.of(quotient, new PythonFloat(remainder)));
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
        if (digitsAfterDecimal.equals(ZERO)) {
            return round();
        }

        BigDecimal asDecimal = new BigDecimal(value);
        return new PythonFloat(
                asDecimal.setScale(digitsAfterDecimal.value.intValueExact(), RoundingMode.HALF_EVEN).doubleValue());
    }

    public PythonLikeObject power(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return power((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return power((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonFloat power(PythonInteger other) {
        return new PythonFloat(Math.pow(value, other.value.doubleValue()));
    }

    public PythonFloat power(PythonFloat other) {
        return new PythonFloat(Math.pow(value, other.value));
    }

    public PythonLikeObject equal(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return equal((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return equal((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonLikeObject notEqual(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return notEqual((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return notEqual((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonLikeObject lessThan(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return lessThan((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return lessThan((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonLikeObject greaterThan(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return greaterThan((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return greaterThan((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonLikeObject lessThanOrEqual(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return lessThanOrEqual((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return lessThanOrEqual((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonLikeObject greaterThanOrEqual(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return greaterThanOrEqual((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return greaterThanOrEqual((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
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

    public PythonString format() {
        return PythonString.valueOf(Double.toString(value));
    }

    private DecimalFormat getNumberFormat(DefaultFormatSpec formatSpec) {
        DecimalFormat numberFormat = new DecimalFormat();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        boolean isUppercase = false;

        switch (formatSpec.conversionType.orElse(DefaultFormatSpec.ConversionType.LOWERCASE_GENERAL)) {
            case UPPERCASE_GENERAL:
            case UPPERCASE_SCIENTIFIC_NOTATION:
            case UPPERCASE_FIXED_POINT:
                isUppercase = true;
                break;
        }

        if (isUppercase) {
            symbols.setExponentSeparator("E");
            symbols.setInfinity("INF");
            symbols.setNaN("NAN");
        } else {
            symbols.setExponentSeparator("e");
            symbols.setInfinity("inf");
            symbols.setNaN("nan");
        }

        if (formatSpec.groupingOption.isPresent()) {
            switch (formatSpec.groupingOption.get()) {
                case COMMA:
                    symbols.setGroupingSeparator(',');
                    break;
                case UNDERSCORE:
                    symbols.setGroupingSeparator('_');
                    break;
            }
        }

        if (formatSpec.conversionType.orElse(null) == DefaultFormatSpec.ConversionType.LOCALE_SENSITIVE) {
            symbols.setGroupingSeparator(DecimalFormatSymbols.getInstance().getGroupingSeparator());
        }
        numberFormat.setDecimalFormatSymbols(symbols);

        switch (formatSpec.conversionType.orElse(DefaultFormatSpec.ConversionType.LOWERCASE_GENERAL)) {
            case LOWERCASE_SCIENTIFIC_NOTATION:
            case UPPERCASE_SCIENTIFIC_NOTATION:
                numberFormat.applyPattern("0." + "#".repeat(formatSpec.getPrecisionOrDefault()) + "E00");
                break;

            case LOWERCASE_FIXED_POINT:
            case UPPERCASE_FIXED_POINT:
                if (formatSpec.groupingOption.isPresent()) {
                    numberFormat.applyPattern("#,##0." + "0".repeat(formatSpec.getPrecisionOrDefault()));
                } else {
                    numberFormat.applyPattern("0." + "0".repeat(formatSpec.getPrecisionOrDefault()));
                }
                break;

            case LOCALE_SENSITIVE:
            case LOWERCASE_GENERAL:
            case UPPERCASE_GENERAL:
                BigDecimal asBigDecimal = new BigDecimal(value);
                // total digits - digits to the right of the decimal point
                int exponent;
                if (asBigDecimal.precision() == asBigDecimal.scale() + 1) {
                    exponent = -asBigDecimal.scale();
                } else {
                    exponent = asBigDecimal.precision() - asBigDecimal.scale() - 1;
                }

                if (-4 < exponent || exponent >= formatSpec.getPrecisionOrDefault()) {
                    if (formatSpec.conversionType.isEmpty()) {
                        numberFormat.applyPattern("0.0" + "#".repeat(formatSpec.getPrecisionOrDefault() - 1) + "E00");
                    } else {
                        numberFormat.applyPattern("0." + "#".repeat(formatSpec.getPrecisionOrDefault()) + "E00");
                    }
                } else {
                    if (formatSpec.groupingOption.isPresent() ||
                            formatSpec.conversionType.orElse(null) == DefaultFormatSpec.ConversionType.LOCALE_SENSITIVE) {
                        if (formatSpec.conversionType.isEmpty()) {
                            numberFormat.applyPattern("#,##0.0" + "#".repeat(formatSpec.getPrecisionOrDefault() - 1));
                        } else {
                            numberFormat.applyPattern("#,##0." + "#".repeat(formatSpec.getPrecisionOrDefault()));
                        }
                    } else {
                        if (formatSpec.conversionType.isEmpty()) {
                            numberFormat.applyPattern("0.0" + "#".repeat(formatSpec.getPrecisionOrDefault() - 1));
                        } else {
                            numberFormat.applyPattern("0." + "#".repeat(formatSpec.getPrecisionOrDefault()));
                        }
                    }
                }
            case PERCENTAGE:
                if (formatSpec.groupingOption.isPresent()) {
                    numberFormat.applyPattern("#,##0." + "0".repeat(formatSpec.getPrecisionOrDefault()) + "%");
                } else {
                    numberFormat.applyPattern("0." + "0".repeat(formatSpec.getPrecisionOrDefault()) + "%");
                }
                break;
            default:
                throw new ValueError("Invalid conversion for float type: " + formatSpec.conversionType);
        }

        switch (formatSpec.signOption.orElse(DefaultFormatSpec.SignOption.ONLY_NEGATIVE_NUMBERS)) {
            case ALWAYS_SIGN:
                numberFormat.setPositivePrefix("+");
                numberFormat.setNegativePrefix("-");
                break;
            case ONLY_NEGATIVE_NUMBERS:
                numberFormat.setPositivePrefix("");
                numberFormat.setNegativePrefix("-");
                break;
            case SPACE_FOR_POSITIVE_NUMBERS:
                numberFormat.setPositivePrefix(" ");
                numberFormat.setNegativePrefix("-");
                break;
        }

        numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
        numberFormat.setDecimalSeparatorAlwaysShown(formatSpec.useAlternateForm);

        return numberFormat;
    }

    public PythonString $method$__format__(PythonLikeObject specObject) {
        PythonString spec;
        if (specObject == PythonNone.INSTANCE) {
            spec = PythonString.EMPTY;
        } else if (specObject instanceof PythonString) {
            spec = (PythonString) specObject;
        } else {
            throw new TypeError("__format__ argument 0 has incorrect type (expecting str or None)");
        }
        DefaultFormatSpec formatSpec = DefaultFormatSpec.fromSpec(spec);

        StringBuilder out = new StringBuilder();
        NumberFormat numberFormat = getNumberFormat(formatSpec);

        out.append(numberFormat.format(value));
        StringFormatter.align(out, formatSpec, DefaultFormatSpec.AlignmentOption.RIGHT_ALIGN);
        return PythonString.valueOf(out.toString());
    }
}
