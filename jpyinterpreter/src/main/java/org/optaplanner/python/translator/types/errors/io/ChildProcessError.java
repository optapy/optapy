package org.optaplanner.python.translator.types.errors.io;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ChildProcessError extends OSError {
    final public static PythonLikeType CHILD_PROCESS_ERROR_TYPE =
            new PythonLikeType("ChildProcessError", ChildProcessError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = CHILD_PROCESS_ERROR_TYPE;

    static {
        CHILD_PROCESS_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments) -> new ChildProcessError(CHILD_PROCESS_ERROR_TYPE, positionalArguments)));
    }

    public ChildProcessError(PythonLikeType type) {
        super(type);
    }

    public ChildProcessError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ChildProcessError(PythonLikeType type, String message) {
        super(type, message);
    }
}
