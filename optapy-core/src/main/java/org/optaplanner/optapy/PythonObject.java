package org.optaplanner.optapy;

import java.util.Map;

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

    /**
     * The Map of references that the planning solution that contains this
     * object refers to. Used in solution cloning.
     *
     * @return The map used to store references.
     */
    Map<Number, Object> get__optapy_reference_map();
}
