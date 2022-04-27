package org.optaplanner.optapy.translator.types.errors;

import java.util.List;

import org.optaplanner.optapy.translator.types.PythonLikeType;

/**
 * Error thrown when a Python iterator has no more values to return.
 */
public class StopIteration extends PythonException {
    public static final PythonLikeType STOP_ITERATION_TYPE = new PythonLikeType("StopIteration", List.of(PythonException.EXCEPTION_TYPE));

    public StopIteration() {
        super(STOP_ITERATION_TYPE);
    }
}
