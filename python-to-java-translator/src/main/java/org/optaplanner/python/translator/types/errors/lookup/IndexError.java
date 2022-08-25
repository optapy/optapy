package org.optaplanner.python.translator.types.errors.lookup;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * The base class for the exceptions that are raised when a key or index used on a mapping or sequence is invalid.
 */
public class IndexError extends LookupError {
    final public static PythonLikeType INDEX_ERROR_TYPE =
            new PythonLikeType("IndexError", IndexError.class, List.of(LOOKUP_ERROR_TYPE)),
            $TYPE = INDEX_ERROR_TYPE;

    static {
        INDEX_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments) -> new IndexError(INDEX_ERROR_TYPE, positionalArguments)));
    }

    public IndexError(PythonLikeType type) {
        super(type);
    }

    public IndexError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public IndexError(PythonLikeType type, String message) {
        super(type, message);
    }
}
