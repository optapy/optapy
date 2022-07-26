package org.optaplanner.python.translator.types.errors;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;

/**
 * Error thrown when a Python iterator has no more values to return.
 */
public class StopIteration extends PythonException {
    public static final PythonLikeType STOP_ITERATION_TYPE = new PythonLikeType("StopIteration",
            StopIteration.class, List.of(EXCEPTION_TYPE)),
            $TYPE = STOP_ITERATION_TYPE;

    private final PythonLikeObject value;

    public StopIteration() {
        this(PythonNone.INSTANCE);
    }

    public StopIteration(PythonLikeObject value) {
        super(STOP_ITERATION_TYPE);
        this.value = value;
    }

    public PythonLikeObject getValue() {
        return value;
    }
}
