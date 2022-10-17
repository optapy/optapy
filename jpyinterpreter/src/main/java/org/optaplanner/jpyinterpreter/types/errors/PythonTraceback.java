package org.optaplanner.jpyinterpreter.types.errors;

import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

/**
 * Traceback of a Python Error.
 * TODO: Implement this
 */
public class PythonTraceback extends AbstractPythonLikeObject {
    public static final PythonLikeType TRACEBACK_TYPE = new PythonLikeType("traceback", PythonTraceback.class),
            $TYPE = TRACEBACK_TYPE;

    public PythonTraceback() {
        super(TRACEBACK_TYPE);
    }
}
