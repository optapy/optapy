package org.optaplanner.python.translator.types.errors.io.connection;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.errors.io.OSError;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ConnectionError extends OSError {
    final public static PythonLikeType CONNECTION_ERROR_TYPE =
            new PythonLikeType("ConnectionError", ConnectionError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = CONNECTION_ERROR_TYPE;

    static {
        CONNECTION_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new ConnectionError(CONNECTION_ERROR_TYPE,
                        positionalArguments)));
    }

    public ConnectionError(PythonLikeType type) {
        super(type);
    }

    public ConnectionError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ConnectionError(PythonLikeType type, String message) {
        super(type, message);
    }
}
