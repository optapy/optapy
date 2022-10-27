package org.optaplanner.jpyinterpreter.types;

import org.optaplanner.jpyinterpreter.MethodDescriptor;
import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonFunctionSignature;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;

public interface PythonLikeComparable<T> extends Comparable<T> {
    static void setup(PythonLikeType type) {
        try {
            type.addBinaryMethod(PythonBinaryOperators.LESS_THAN, new PythonFunctionSignature(
                    new MethodDescriptor(PythonLikeComparable.class.getMethod("lessThan", Object.class)),
                    BuiltinTypes.BOOLEAN_TYPE, type));
            type.addBinaryMethod(PythonBinaryOperators.GREATER_THAN, new PythonFunctionSignature(
                    new MethodDescriptor(PythonLikeComparable.class.getMethod("greaterThan", Object.class)),
                    BuiltinTypes.BOOLEAN_TYPE, type));
            type.addBinaryMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL, new PythonFunctionSignature(
                    new MethodDescriptor(PythonLikeComparable.class.getMethod("lessThanOrEqual", Object.class)),
                    BuiltinTypes.BOOLEAN_TYPE, type));
            type.addBinaryMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL, new PythonFunctionSignature(
                    new MethodDescriptor(PythonLikeComparable.class.getMethod("greaterThanOrEqual", Object.class)),
                    BuiltinTypes.BOOLEAN_TYPE, type));
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
