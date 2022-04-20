package org.optaplanner.optapy.translator.types;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.optaplanner.optapy.PythonLikeObject;

public interface PythonLikeFunction extends PythonLikeObject {

    PythonLikeType FUNCTION_TYPE = new PythonLikeType("function");

    /**
     * Calls the function with positional arguments and named arguments.
     *
     * @param positionalArguments Positional arguments
     * @param namedArguments Named arguments
     * @return The function result
     */
    PythonLikeObject __call__(List<PythonLikeObject> positionalArguments, Map<PythonString, PythonLikeObject> namedArguments);

    @Override
    default PythonLikeObject __getAttributeOrNull(String attributeName) {
        return null;
    }

    @Override
    default void __setAttribute(String attributeName, PythonLikeObject value) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void __deleteAttribute(String attributeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    default PythonLikeType __getType() {
        return FUNCTION_TYPE;
    }
}
