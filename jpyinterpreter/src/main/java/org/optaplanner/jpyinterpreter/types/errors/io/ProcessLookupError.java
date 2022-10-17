package org.optaplanner.jpyinterpreter.types.errors.io;

import java.util.List;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ProcessLookupError extends OSError {
    final public static PythonLikeType PROCESS_LOOKUP_ERROR_TYPE =
            new PythonLikeType("ProcessLookupError", ProcessLookupError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = PROCESS_LOOKUP_ERROR_TYPE;

    static {
        PROCESS_LOOKUP_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new ProcessLookupError(PROCESS_LOOKUP_ERROR_TYPE, positionalArguments)));
    }

    public ProcessLookupError(PythonLikeType type) {
        super(type);
    }

    public ProcessLookupError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ProcessLookupError(PythonLikeType type, String message) {
        super(type, message);
    }
}
