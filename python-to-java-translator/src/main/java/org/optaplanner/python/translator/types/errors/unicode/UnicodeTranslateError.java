package org.optaplanner.python.translator.types.errors.unicode;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

public class UnicodeTranslateError extends UnicodeError {
    public final static PythonLikeType UNICODE_TRANSLATE_ERROR_TYPE =
            new PythonLikeType("UnicodeTranslateError", UnicodeTranslateError.class, List.of(UNICODE_ERROR_TYPE)),
            $TYPE = UNICODE_TRANSLATE_ERROR_TYPE;

    static {
        UNICODE_TRANSLATE_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments) -> new UnicodeTranslateError(UNICODE_TRANSLATE_ERROR_TYPE, positionalArguments)));
    }

    public UnicodeTranslateError() {
        super(UNICODE_TRANSLATE_ERROR_TYPE);
    }

    public UnicodeTranslateError(String message) {
        super(UNICODE_TRANSLATE_ERROR_TYPE, message);
    }

    public UnicodeTranslateError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public UnicodeTranslateError(PythonLikeType type) {
        super(type);
    }

    public UnicodeTranslateError(PythonLikeType type, String message) {
        super(type, message);
    }
}