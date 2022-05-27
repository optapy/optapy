package org.optaplanner.python.translator.types.errors;

import java.util.List;

import org.optaplanner.python.translator.types.PythonLikeType;

public class PythonAssertionError extends PythonException {
    public static final PythonLikeType ASSERTION_ERROR_TYPE = new PythonLikeType("AssertionError",
            PythonAssertionError.class,
            List.of(EXCEPTION_TYPE));

    public PythonAssertionError() {
        super(ASSERTION_ERROR_TYPE);
    }
}
