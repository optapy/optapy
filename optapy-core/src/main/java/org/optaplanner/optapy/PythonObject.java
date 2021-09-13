package org.optaplanner.optapy;

/**
 * A PythonObject holds a reference to {@link OpaquePythonReference}.
 * Its internal state and fields are mapped to the {@link OpaquePythonReference}.
 */
public interface PythonObject {

    /**
     * The {@link OpaquePythonReference} that this PythonObject
     * represents. Used in Python code to read/modify the Python Object.
     *
     * @return An opaque pointer to the Python Object represented by
     *         this PythonObject.
     */
    OpaquePythonReference get__optapy_Id();
}
