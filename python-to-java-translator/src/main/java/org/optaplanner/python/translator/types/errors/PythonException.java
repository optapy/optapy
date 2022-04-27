package org.optaplanner.python.translator.types.errors;

import java.util.List;

import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Python class for general exceptions. Equivalent to Java's {@link RuntimeException}
 */
public class PythonException extends PythonBaseException {
    final static PythonLikeType EXCEPTION_TYPE =
            new PythonLikeType("Exception", List.of(PythonBaseException.BASE_EXCEPTION_TYPE));

    public PythonException(PythonLikeType type) {
        super(type);
    }
}
