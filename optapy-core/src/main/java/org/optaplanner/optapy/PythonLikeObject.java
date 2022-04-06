package org.optaplanner.optapy;

import java.util.NoSuchElementException;

import javax.naming.directory.NoSuchAttributeException;

import org.optaplanner.optapy.translator.types.PythonLikeType;

/**
 * Represents an Object that can be interacted with like a Python Object.
 * Unlike {@link PythonObject}, a PythonLikeObject can refer to a Java
 * Object.
 */
public interface PythonLikeObject {

    /**
     * Gets an attribute by name.
     *
     * @param attributeName Name of the attribute to get
     * @return The attribute of the object that corresponds with attributeName
     * @throws NoSuchElementException if the attribute does not exist
     */
    PythonLikeObject __getattribute__(String attributeName);

    /**
     * Sets an attribute by name.
     *
     * @param attributeName Name of the attribute to set
     * @param value Value to set the attribute to
     */
    void __setattribute__(String attributeName, PythonLikeObject value);

    /**
     * Returns the type describing the object
     *
     * @return the type describing the object
     */
    PythonLikeType __type__();
}
