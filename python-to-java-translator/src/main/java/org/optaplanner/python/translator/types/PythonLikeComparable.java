package org.optaplanner.python.translator.types;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;

public interface PythonLikeComparable<T> extends Comparable<T> {
    static void setup(PythonLikeType type) {
        try {
            type.addMethod(PythonBinaryOperators.LESS_THAN, new PythonFunctionSignature(
                    new MethodDescriptor(PythonLikeComparable.class.getMethod("lessThan", Object.class)),
                    PythonBoolean.getBooleanType(), type));
            type.addMethod(PythonBinaryOperators.GREATER_THAN, new PythonFunctionSignature(
                    new MethodDescriptor(PythonLikeComparable.class.getMethod("greaterThan", Object.class)),
                    PythonBoolean.getBooleanType(), type));
            type.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL, new PythonFunctionSignature(
                    new MethodDescriptor(PythonLikeComparable.class.getMethod("lessThanOrEqual", Object.class)),
                    PythonBoolean.getBooleanType(), type));
            type.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL, new PythonFunctionSignature(
                    new MethodDescriptor(PythonLikeComparable.class.getMethod("greaterThanOrEqual", Object.class)),
                    PythonBoolean.getBooleanType(), type));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    default PythonBoolean lessThan(T other) {
        return PythonBoolean.valueOf(compareTo(other) < 0);
    }

    default PythonBoolean greaterThan(T other) {
        return PythonBoolean.valueOf(compareTo(other) > 0);
    }

    default PythonBoolean lessThanOrEqual(T other) {
        return PythonBoolean.valueOf(compareTo(other) <= 0);
    }

    default PythonBoolean greaterThanOrEqual(T other) {
        return PythonBoolean.valueOf(compareTo(other) >= 0);
    }
}
