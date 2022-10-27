package org.optaplanner.jpyinterpreter.types.errors.io;

import java.util.List;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class FileExistsError extends OSError {
    final public static PythonLikeType FILE_EXISTS_ERROR_TYPE =
            new PythonLikeType("FileExistsError", FileExistsError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = FILE_EXISTS_ERROR_TYPE;

    static {
        FILE_EXISTS_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new FileExistsError(FILE_EXISTS_ERROR_TYPE,
                        positionalArguments)));
    }

    public FileExistsError(PythonLikeType type) {
        super(type);
    }

    public FileExistsError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public FileExistsError(PythonLikeType type, String message) {
        super(type, message);
    }
}
