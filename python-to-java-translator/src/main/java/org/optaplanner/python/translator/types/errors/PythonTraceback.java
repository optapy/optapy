package org.optaplanner.python.translator.types.errors;

import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Traceback of a Python Error.
 * TODO: Implement this
 */
public class PythonTraceback extends AbstractPythonLikeObject {
    public static final PythonLikeType TRACEBACK_TYPE = new PythonLikeType("traceback");

    public PythonTraceback() {
        super(TRACEBACK_TYPE);
    }
}
