package org.optaplanner.optapy;

import java.util.Map;
import java.util.Set;

import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;

/**
 * A PythonObject holds a reference to {@link OpaquePythonReference}.
 * Its internal state and fields are mapped to the {@link OpaquePythonReference}.
 */
public interface PythonObject extends PythonLikeObject {

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

    void forceUpdate();

    void readFromPythonObject(Set doneSet, Map<Number, Object> referenceMap);

    void visitIds(Map<Number, Object> referenceMap);

    void $setFields(OpaquePythonReference reference, Number id, Map referenceMap, TriFunction setter);
}
