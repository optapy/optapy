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
    PythonLikeObject __call__(List<PythonLikeObject> positionalArguments, Map<String, PythonLikeObject> namedArguments);

    @Override
    default PythonLikeObject __getattribute__(String attributeName) {
        throw new NoSuchElementException();
    }

    @Override
    default void __setattribute__(String attributeName, PythonLikeObject value) {
        throw new UnsupportedOperationException();
    }

    @Override
    default PythonLikeType __type__() {
        return FUNCTION_TYPE;
    }
}
