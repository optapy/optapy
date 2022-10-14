package org.optaplanner.python.translator.types.numeric;

import static org.optaplanner.python.translator.types.BuiltinTypes.INT_TYPE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.NotImplemented;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;
import org.optaplanner.python.translator.types.errors.TypeError;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.util.DefaultFormatSpec;
import org.optaplanner.python.translator.util.StringFormatter;

public class PythonInteger extends AbstractPythonLikeObject implements PythonNumber {
    private static final BigInteger MIN_BYTE = BigInteger.valueOf(0);
    private static final BigInteger MAX_BYTE = BigInteger.valueOf(255);

    public final BigInteger value;

    public final static PythonInteger ZERO = new PythonInteger(BigInteger.ZERO);
    public final static PythonInteger ONE = new PythonInteger(BigInteger.ONE);
    public final static PythonInteger TWO = new PythonInteger(BigInteger.TWO);

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonInteger::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        INT_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
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
                    return asIntFunction.$call(List.of(value), Map.of(), null);
                }
            } else {
                throw new ValueError("int expects 0 or 1 arguments, got " + positionalArguments.size());
            }
        }));
        // Unary
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.AS_BOOLEAN, PythonInteger.class.getMethod("asBoolean"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.AS_INT, PythonInteger.class.getMethod("asInteger"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.AS_FLOAT, PythonInteger.class.getMethod("asFloat"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.AS_INDEX, PythonInteger.class.getMethod("asInteger"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.POSITIVE, PythonInteger.class.getMethod("asInteger"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.NEGATIVE, PythonInteger.class.getMethod("negative"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.INVERT, PythonInteger.class.getMethod("invert"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.ABS, PythonInteger.class.getMethod("abs"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION, PythonInteger.class.getMethod("asString"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING, PythonInteger.class.getMethod("asString"));
        INT_TYPE.addUnaryMethod(PythonUnaryOperator.HASH, PythonInteger.class.getMethod("$method$__hash__"));

        // Binary
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.ADD, PythonInteger.class.getMethod("add", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.ADD, PythonInteger.class.getMethod("add", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.ADD, PythonInteger.class.getMethod("add", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.SUBTRACT,
                PythonInteger.class.getMethod("subtract", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.SUBTRACT,
                PythonInteger.class.getMethod("subtract", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.SUBTRACT,
                PythonInteger.class.getMethod("subtract", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MULTIPLY,
                PythonInteger.class.getMethod("multiply", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MULTIPLY,
                PythonInteger.class.getMethod("multiply", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MULTIPLY,
                PythonInteger.class.getMethod("multiply", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                PythonInteger.class.getMethod("trueDivide", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                PythonInteger.class.getMethod("trueDivide", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                PythonInteger.class.getMethod("trueDivide", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                PythonInteger.class.getMethod("floorDivide", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                PythonInteger.class.getMethod("floorDivide", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                PythonInteger.class.getMethod("floorDivide", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.DIVMOD,
                PythonInteger.class.getMethod("divmod", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.DIVMOD, PythonInteger.class.getMethod("divmod", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MODULO,
                PythonInteger.class.getMethod("modulo", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MODULO,
                PythonInteger.class.getMethod("modulo", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.MODULO, PythonInteger.class.getMethod("modulo", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.POWER,
                PythonInteger.class.getMethod("power", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.POWER, PythonInteger.class.getMethod("power", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.POWER, PythonInteger.class.getMethod("power", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LSHIFT,
                PythonInteger.class.getMethod("shiftLeft", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LSHIFT,
                PythonInteger.class.getMethod("shiftLeft", PythonInteger.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.RSHIFT,
                PythonInteger.class.getMethod("shiftRight", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.RSHIFT,
                PythonInteger.class.getMethod("shiftRight", PythonInteger.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.AND,
                PythonInteger.class.getMethod("bitwiseAnd", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.AND,
                PythonInteger.class.getMethod("bitwiseAnd", PythonInteger.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.OR,
                PythonInteger.class.getMethod("bitwiseOr", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.OR, PythonInteger.class.getMethod("bitwiseOr", PythonInteger.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.XOR,
                PythonInteger.class.getMethod("bitwiseXor", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.XOR,
                PythonInteger.class.getMethod("bitwiseXor", PythonInteger.class));

        // Inplace Binary (identical to Binary)
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_ADD,
                PythonInteger.class.getMethod("add", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_ADD,
                PythonInteger.class.getMethod("add", PythonFloat.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                PythonInteger.class.getMethod("subtract", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                PythonInteger.class.getMethod("subtract", PythonFloat.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_MULTIPLY,
                PythonInteger.class.getMethod("multiply", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_MULTIPLY,
                PythonInteger.class.getMethod("multiply", PythonFloat.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_TRUE_DIVIDE,
                PythonInteger.class.getMethod("trueDivide", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_TRUE_DIVIDE,
                PythonInteger.class.getMethod("trueDivide", PythonFloat.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_FLOOR_DIVIDE,
                PythonInteger.class.getMethod("floorDivide", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_FLOOR_DIVIDE,
                PythonInteger.class.getMethod("floorDivide", PythonFloat.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_MODULO,
                PythonInteger.class.getMethod("modulo", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_MODULO,
                PythonInteger.class.getMethod("modulo", PythonFloat.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_POWER,
                PythonInteger.class.getMethod("power", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_POWER,
                PythonInteger.class.getMethod("power", PythonFloat.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_LSHIFT,
                PythonInteger.class.getMethod("shiftLeft", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_RSHIFT,
                PythonInteger.class.getMethod("shiftRight", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_AND,
                PythonInteger.class.getMethod("bitwiseAnd", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_OR,
                PythonInteger.class.getMethod("bitwiseOr", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.INPLACE_XOR,
                PythonInteger.class.getMethod("bitwiseXor", PythonInteger.class));

        // Ternary
        INT_TYPE.addBinaryMethod(PythonBinaryOperators.POWER,
                PythonInteger.class.getMethod("power", PythonInteger.class, PythonInteger.class));

        // Comparisons
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.EQUAL,
                PythonInteger.class.getMethod("equal", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.EQUAL, PythonInteger.class.getMethod("equal", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.EQUAL, PythonInteger.class.getMethod("equal", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonInteger.class.getMethod("notEqual", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonInteger.class.getMethod("notEqual", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonInteger.class.getMethod("notEqual", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN,
                PythonInteger.class.getMethod("lessThan", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN,
                PythonInteger.class.getMethod("lessThan", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN,
                PythonInteger.class.getMethod("lessThan", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonInteger.class.getMethod("lessThanOrEqual", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonInteger.class.getMethod("lessThanOrEqual", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonInteger.class.getMethod("lessThanOrEqual", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN,
                PythonInteger.class.getMethod("greaterThan", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN,
                PythonInteger.class.getMethod("greaterThan", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN,
                PythonInteger.class.getMethod("greaterThan", PythonFloat.class));

        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonInteger.class.getMethod("greaterThanOrEqual", PythonLikeObject.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonInteger.class.getMethod("greaterThanOrEqual", PythonInteger.class));
        INT_TYPE.addLeftBinaryMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonInteger.class.getMethod("greaterThanOrEqual", PythonFloat.class));

        // Other
        INT_TYPE.addMethod("__round__", PythonInteger.class.getMethod("round"));
        INT_TYPE.addMethod("__round__", PythonInteger.class.getMethod("round", PythonInteger.class));
        INT_TYPE.addBinaryMethod(PythonBinaryOperators.FORMAT, PythonInteger.class.getMethod("$method$__format__"));
        INT_TYPE.addBinaryMethod(PythonBinaryOperators.FORMAT,
                PythonInteger.class.getMethod("$method$__format__", PythonLikeObject.class));

        return INT_TYPE;
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
        super(INT_TYPE);
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

    public byte asByte() {
        if (value.compareTo(MIN_BYTE) < 0 || value.compareTo(MAX_BYTE) > 0) {
            throw new ValueError(value + " cannot represent a byte because it outside the range [0, 255].");
        }
        return value.byteValue();
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
        return $method$__hash__().value.intValue();
    }

    public PythonInteger $method$__hash__() {
        return PythonNumber.computeHash(this, ONE);
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

    public PythonLikeObject add(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return add((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return add((PythonFloat) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonInteger add(PythonInteger other) {
        return new PythonInteger(value.add(other.value));
    }

    public PythonFloat add(PythonFloat other) {
        return new PythonFloat(value.doubleValue() + other.value);
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

    public PythonInteger subtract(PythonInteger other) {
        return new PythonInteger(value.subtract(other.value));
    }

    public PythonFloat subtract(PythonFloat other) {
        return new PythonFloat(value.doubleValue() - other.value);
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

    public PythonInteger multiply(PythonInteger other) {
        return new PythonInteger(value.multiply(other.value));
    }

    public PythonFloat multiply(PythonFloat other) {
        return new PythonFloat(value.doubleValue() * other.value);
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
        return new PythonFloat(value.doubleValue() / other.value.doubleValue());
    }

    public PythonFloat trueDivide(PythonFloat other) {
        return new PythonFloat(value.doubleValue() / other.value);
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

    public PythonInteger floorDivide(PythonInteger other) {
        return new PythonInteger(value.divide(other.value));
    }

    public PythonFloat floorDivide(PythonFloat other) {
        return PythonFloat.valueOf(new BigDecimal(value)
                .divideToIntegralValue(BigDecimal.valueOf(other.value))
                .doubleValue());
    }

    public PythonFloat ceilDivide(PythonFloat other) {
        return PythonFloat.valueOf(new BigDecimal(value)
                .divide(BigDecimal.valueOf(other.value), RoundingMode.CEILING)
                .doubleValue());
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

    public PythonInteger modulo(PythonInteger other) {
        return new PythonInteger(value.remainder(other.value));
    }

    public PythonFloat modulo(PythonFloat other) {
        return new PythonFloat(value.doubleValue() % other.value);
    }

    public PythonLikeTuple divmod(PythonInteger other) {
        BigInteger[] result = value.divideAndRemainder(other.value);

        // Python remainder has sign of divisor
        if (other.value.compareTo(BigInteger.ZERO) < 0) {
            if (result[1].compareTo(BigInteger.ZERO) > 0) {
                result[0] = result[0].subtract(BigInteger.ONE);
                result[1] = result[1].add(other.value);
            }
        } else {
            if (result[1].compareTo(BigInteger.ZERO) < 0) {
                result[0] = result[0].subtract(BigInteger.ONE);
                result[1] = result[1].add(other.value);
            }
        }
        return PythonLikeTuple.fromList(List.of(PythonInteger.valueOf(result[0]),
                PythonInteger.valueOf(result[1])));
    }

    public PythonLikeTuple divmod(PythonFloat other) {
        PythonFloat quotient;

        if (value.compareTo(BigInteger.ZERO) < 0 == other.value < 0) {
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
        return this;
    }

    public PythonInteger round(PythonInteger digitsAfterDecimal) {
        if (digitsAfterDecimal.compareTo(PythonInteger.ZERO) >= 0) {
            return this;
        }

        BigInteger powerOfTen = BigInteger.TEN.pow(-digitsAfterDecimal.value.intValueExact());
        BigInteger halfPowerOfTen = powerOfTen.shiftRight(1);
        BigInteger remainder = value.mod(powerOfTen);

        BigInteger previous = value.subtract(remainder);
        BigInteger next = value.add(powerOfTen.subtract(remainder));

        if (remainder.equals(halfPowerOfTen)) {
            if (previous.divide(powerOfTen).mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                // previous even
                return PythonInteger.valueOf(previous);
            } else {
                // next even
                return PythonInteger.valueOf(next);
            }
        } else if (remainder.compareTo(halfPowerOfTen) < 0) {
            // previous closer
            return PythonInteger.valueOf(previous);
        } else {
            // next closer
            return PythonInteger.valueOf(next);
        }
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

    public PythonNumber power(PythonInteger other) {
        if (other.value.signum() >= 0) {
            return new PythonInteger(value.pow(other.value.intValueExact()));
        }
        return new PythonFloat(Math.pow(value.doubleValue(), other.value.doubleValue()));
    }

    public PythonInteger power(PythonInteger exponent, PythonInteger modulus) {
        return PythonInteger.valueOf(value.modPow(exponent.value, modulus.value));
    }

    public PythonFloat power(PythonFloat other) {
        return new PythonFloat(Math.pow(value.doubleValue(), other.value));
    }

    public PythonLikeObject shiftLeft(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return shiftLeft((PythonInteger) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonInteger shiftLeft(PythonInteger other) {
        return new PythonInteger(value.shiftLeft(other.value.intValueExact()));
    }

    public PythonLikeObject shiftRight(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return shiftRight((PythonInteger) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonInteger shiftRight(PythonInteger other) {
        return new PythonInteger(value.shiftRight(other.value.intValueExact()));
    }

    public PythonLikeObject bitwiseAnd(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return bitwiseAnd((PythonInteger) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonInteger bitwiseAnd(PythonInteger other) {
        return new PythonInteger(value.and(other.value));
    }

    public PythonLikeObject bitwiseOr(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return bitwiseOr((PythonInteger) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonInteger bitwiseOr(PythonInteger other) {
        return new PythonInteger(value.or(other.value));
    }

    public PythonLikeObject bitwiseXor(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return bitwiseXor((PythonInteger) other);
        } else {
            return NotImplemented.INSTANCE;
        }
    }

    public PythonInteger bitwiseXor(PythonInteger other) {
        return new PythonInteger(value.xor(other.value));
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

    public PythonLikeObject lessThanOrEqual(PythonLikeObject other) {
        if (other instanceof PythonInteger) {
            return lessThanOrEqual((PythonInteger) other);
        } else if (other instanceof PythonFloat) {
            return lessThanOrEqual((PythonFloat) other);
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

    public PythonString asString() {
        return PythonString.valueOf(value.toString());
    }

    public PythonString $method$__format__() {
        return PythonString.valueOf(value.toString());
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

        String alternateFormPrefix = null;
        int groupSize;

        if (formatSpec.precision.isPresent()) {
            throw new ValueError("Precision not allowed in integer format specifier");
        }
        switch (formatSpec.conversionType.orElse(DefaultFormatSpec.ConversionType.DECIMAL)) {
            case BINARY:
                alternateFormPrefix = "0b";
                groupSize = 4;
                out.append(value.toString(2));
                break;
            case OCTAL:
                alternateFormPrefix = "0o";
                groupSize = 4;
                out.append(value.toString(8));
                break;
            case DECIMAL:
                out.append(value.toString(10));
                groupSize = 3;
                break;
            case LOWERCASE_HEX:
                alternateFormPrefix = "0x";
                groupSize = 4;
                out.append(value.toString(16));
                break;
            case UPPERCASE_HEX:
                alternateFormPrefix = "0X";
                groupSize = 4;
                out.append(value.toString(16).toUpperCase());
                break;
            case CHARACTER:
                groupSize = -1;
                out.appendCodePoint(value.intValueExact());
                break;
            case LOCALE_SENSITIVE:
                groupSize = -1;
                NumberFormat.getIntegerInstance().format(value);
                break;
            default:
                throw new ValueError("Invalid format spec for int: " + spec);
        }

        StringFormatter.addGroupings(out, formatSpec, groupSize);
        switch (formatSpec.signOption.orElse(DefaultFormatSpec.SignOption.ONLY_NEGATIVE_NUMBERS)) {
            case ALWAYS_SIGN:
                if (out.charAt(0) != '-') {
                    out.insert(0, '+');
                }
                break;
            case ONLY_NEGATIVE_NUMBERS:
                break;
            case SPACE_FOR_POSITIVE_NUMBERS:
                if (out.charAt(0) != '-') {
                    out.insert(0, ' ');
                }
                break;
            default:
                throw new IllegalStateException("Unhandled case: " + formatSpec.signOption);
        }

        if (formatSpec.useAlternateForm && alternateFormPrefix != null) {
            StringFormatter.alignWithPrefixRespectingSign(out, alternateFormPrefix, formatSpec,
                    DefaultFormatSpec.AlignmentOption.RIGHT_ALIGN);
        } else {
            StringFormatter.align(out, formatSpec, DefaultFormatSpec.AlignmentOption.RIGHT_ALIGN);
        }

        return PythonString.valueOf(out.toString());
    }
}
