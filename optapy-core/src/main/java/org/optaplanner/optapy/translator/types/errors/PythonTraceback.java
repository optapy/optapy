package org.optaplanner.optapy.translator.types.errors;

import org.optaplanner.optapy.translator.types.AbstractPythonLikeObject;
import org.optaplanner.optapy.translator.types.PythonLikeType;

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
