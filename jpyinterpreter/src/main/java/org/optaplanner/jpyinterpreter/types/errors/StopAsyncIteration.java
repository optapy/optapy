package org.optaplanner.jpyinterpreter.types.errors;

import java.util.List;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;

/**
 * Error thrown when a Python async iterator has no more values to return.
 */
public class StopAsyncIteration extends PythonException {
    public static final PythonLikeType STOP_ASYNC_ITERATION_TYPE = new PythonLikeType("StopAsyncIteration",
            StopAsyncIteration.class, List.of(EXCEPTION_TYPE)),
            $TYPE = STOP_ASYNC_ITERATION_TYPE;

    static {
        STOP_ASYNC_ITERATION_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new StopAsyncIteration(STOP_ASYNC_ITERATION_TYPE, positionalArguments)));
    }

    private final PythonLikeObject value;

    public StopAsyncIteration() {
        this(PythonNone.INSTANCE);
    }

    public StopAsyncIteration(PythonLikeObject value) {
        this(STOP_ASYNC_ITERATION_TYPE, List.of(value));
    }

    public StopAsyncIteration(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
        if (args.size() > 0) {
            value = args.get(0);
        } else {
            value = PythonNone.INSTANCE;
        }
    }

    public PythonLikeObject getValue() {
        return value;
    }
}
