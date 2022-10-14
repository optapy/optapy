package org.optaplanner.python.translator.types.errors.arithmetic;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * The base class for those built-in exceptions that are raised for various arithmetic errors
 */
public class ZeroDivisionError extends ArithmeticError {
    final public static PythonLikeType ZERO_DIVISION_ERROR_TYPE =
            new PythonLikeType("ZeroDivisionError", ZeroDivisionError.class, List.of(ARITHMETIC_ERROR_TYPE)),
            $TYPE = ZERO_DIVISION_ERROR_TYPE;

    static {
        ZERO_DIVISION_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new ZeroDivisionError(ZERO_DIVISION_ERROR_TYPE, positionalArguments)));
    }

    public ZeroDivisionError(PythonLikeType type) {
        super(type);
    }

    public ZeroDivisionError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ZeroDivisionError(PythonLikeType type, String message) {
        super(type, message);
    }
}
