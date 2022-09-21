package org.optaplanner.python.translator.types.errors;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ImportError extends PythonBaseException {
    final public static PythonLikeType IMPORT_ERROR_TYPE =
            new PythonLikeType("ImportError", ImportError.class, List.of(PythonBaseException.BASE_EXCEPTION_TYPE)),
            $TYPE = IMPORT_ERROR_TYPE;

    static {
        IMPORT_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments) -> new ImportError(IMPORT_ERROR_TYPE, positionalArguments)));
    }

    public ImportError(PythonLikeType type) {
        super(type);
    }

    public ImportError(PythonLikeType type, String message) {
        super(type, message);
    }

    public ImportError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }
}