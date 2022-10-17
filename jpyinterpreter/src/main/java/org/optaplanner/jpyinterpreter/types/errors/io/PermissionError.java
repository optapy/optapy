package org.optaplanner.jpyinterpreter.types.errors.io;

import java.util.List;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class PermissionError extends OSError {
    final public static PythonLikeType PERMISSION_ERROR_TYPE =
            new PythonLikeType("PermissionError", PermissionError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = PERMISSION_ERROR_TYPE;

    static {
        PERMISSION_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new PermissionError(PERMISSION_ERROR_TYPE,
                        positionalArguments)));
    }

    public PermissionError(PythonLikeType type) {
        super(type);
    }

    public PermissionError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public PermissionError(PythonLikeType type, String message) {
        super(type, message);
    }
}
