package org.optaplanner.python.translator.types.errors.syntax;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class IndentationError extends SyntaxError {
    final public static PythonLikeType INDENTATION_ERROR_TYPE =
            new PythonLikeType("IndentationError", IndentationError.class, List.of(SYNTAX_ERROR_TYPE)),
            $TYPE = INDENTATION_ERROR_TYPE;

    static {
        INDENTATION_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments) -> new IndentationError(INDENTATION_ERROR_TYPE, positionalArguments)));
    }

    public IndentationError(PythonLikeType type) {
        super(type);
    }

    public IndentationError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public IndentationError(PythonLikeType type, String message) {
        super(type, message);
    }
}