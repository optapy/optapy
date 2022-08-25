package org.optaplanner.python.translator.types.errors.io;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.errors.PythonBaseException;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class SystemError extends PythonBaseException {
    final public static PythonLikeType SYSTEM_ERROR_TYPE =
            new PythonLikeType("SystemError", SystemError.class, List.of(PythonBaseException.BASE_EXCEPTION_TYPE)),
            $TYPE = SYSTEM_ERROR_TYPE;

    static {
        SYSTEM_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments) -> new SystemError(SYSTEM_ERROR_TYPE, positionalArguments)));
    }

    public SystemError(PythonLikeType type) {
        super(type);
    }

    public SystemError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public SystemError(PythonLikeType type, String message) {
        super(type, message);
    }
}
