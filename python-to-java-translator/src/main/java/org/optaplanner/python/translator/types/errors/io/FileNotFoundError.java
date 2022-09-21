package org.optaplanner.python.translator.types.errors.io;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class FileNotFoundError extends OSError {
    final public static PythonLikeType FILE_NOT_FOUND_ERROR_TYPE =
            new PythonLikeType("FileNotFoundError", FileNotFoundError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = FILE_NOT_FOUND_ERROR_TYPE;

    static {
        FILE_NOT_FOUND_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments) -> new FileNotFoundError(FILE_NOT_FOUND_ERROR_TYPE, positionalArguments)));
    }

    public FileNotFoundError(PythonLikeType type) {
        super(type);
    }

    public FileNotFoundError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public FileNotFoundError(PythonLikeType type, String message) {
        super(type, message);
    }
}