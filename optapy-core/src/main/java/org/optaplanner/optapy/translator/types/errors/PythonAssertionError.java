package org.optaplanner.optapy.translator.types.errors;

import java.util.List;

import org.optaplanner.optapy.translator.types.PythonLikeType;

public class PythonAssertionError extends PythonException {
    public static final PythonLikeType ASSERTION_ERROR_TYPE = new PythonLikeType("AssertionError", List.of(PythonException.EXCEPTION_TYPE));

    public PythonAssertionError() {
        super(ASSERTION_ERROR_TYPE);
    }
}
