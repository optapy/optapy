package org.optaplanner.python.translator.types;

import static org.optaplanner.python.translator.types.PythonInteger.INT_TYPE;

import java.util.List;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;

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
        // Unary
        FLOAT_TYPE.addMethod(PythonUnaryOperator.AS_BOOLEAN,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("asBoolean")),
                        PythonBoolean.BOOLEAN_TYPE));
        FLOAT_TYPE.addMethod(PythonUnaryOperator.AS_INT,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("asInteger")),
                        INT_TYPE));
        FLOAT_TYPE.addMethod(PythonUnaryOperator.POSITIVE,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("asFloat")),
                        FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonUnaryOperator.NEGATIVE,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("negative")),
                        FLOAT_TYPE));

        FLOAT_TYPE.addMethod(PythonUnaryOperator.ABS,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("abs")),
                        FLOAT_TYPE));

        // Binary
        FLOAT_TYPE.addMethod(PythonBinaryOperators.ADD,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("add", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.ADD,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("add", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("subtract", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("subtract", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.MULTIPLY,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("multiply", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.MULTIPLY,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("multiply", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("trueDivide", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("trueDivide", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("floorDivide", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("floorDivide", PythonFloat.class)),
                        INT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.MODULO,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("modulo", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.MODULO,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("modulo", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.POWER,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("power", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.POWER,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("power", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));

        // Inplace Binary (identical to Binary)
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_ADD,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("add", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_ADD,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("add", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("subtract", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("subtract", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MULTIPLY,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("multiply", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MULTIPLY,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("multiply", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("trueDivide", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_TRUE_DIVIDE,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("trueDivide", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("floorDivide", PythonInteger.class)),
                        INT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_FLOOR_DIVIDE,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("floorDivide", PythonFloat.class)),
                        INT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MODULO,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("modulo", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_MODULO,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("modulo", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_POWER,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("power", PythonInteger.class)),
                        FLOAT_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.INPLACE_POWER,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("power", PythonFloat.class)),
                        FLOAT_TYPE, FLOAT_TYPE));

        // Comparisons
        FLOAT_TYPE.addMethod(PythonBinaryOperators.EQUAL,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("equal", PythonInteger.class)),
                        PythonBoolean.BOOLEAN_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.EQUAL,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("equal", PythonFloat.class)),
                        PythonBoolean.BOOLEAN_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.NOT_EQUAL,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("notEqual", PythonInteger.class)),
                        PythonBoolean.BOOLEAN_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.NOT_EQUAL,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("notEqual", PythonFloat.class)),
                        PythonBoolean.BOOLEAN_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("lessThan", PythonInteger.class)),
                        PythonBoolean.BOOLEAN_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("lessThan", PythonFloat.class)),
                        PythonBoolean.BOOLEAN_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("lessThanOrEqual", PythonInteger.class)),
                        PythonBoolean.BOOLEAN_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("lessThanOrEqual", PythonFloat.class)),
                        PythonBoolean.BOOLEAN_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("greaterThan", PythonInteger.class)),
                        PythonBoolean.BOOLEAN_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                new PythonFunctionSignature(new MethodDescriptor(PythonFloat.class.getMethod("greaterThan", PythonFloat.class)),
                        PythonBoolean.BOOLEAN_TYPE, FLOAT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("greaterThanOrEqual", PythonInteger.class)),
                        PythonBoolean.BOOLEAN_TYPE, INT_TYPE));
        FLOAT_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonFloat.class.getMethod("greaterThanOrEqual", PythonFloat.class)),
                        PythonBoolean.BOOLEAN_TYPE, FLOAT_TYPE));
    }

    @Override
    public Number getValue() {
        return value;
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

    public PythonFloat modulo(PythonInteger other) {
        return new PythonFloat(value % other.value.doubleValue());
    }

    public PythonFloat modulo(PythonFloat other) {
        return new PythonFloat(value % other.value);
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
