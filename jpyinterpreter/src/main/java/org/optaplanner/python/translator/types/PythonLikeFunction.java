package org.optaplanner.python.translator.types;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;

public interface PythonLikeFunction extends PythonLikeObject {
    static PythonLikeType getStaticFunctionType() {
        return BuiltinTypes.STATIC_FUNCTION_TYPE;
    }

    static PythonLikeType getFunctionType() {
        return BuiltinTypes.FUNCTION_TYPE;
    }

    static PythonLikeType getClassFunctionType() {
        return BuiltinTypes.CLASS_FUNCTION_TYPE;
    }

    /**
     * Calls the function with positional arguments and named arguments.
     *
     * @param positionalArguments Positional arguments
     * @param namedArguments Named arguments
     * @return The function result
     */
    PythonLikeObject __call__(List<PythonLikeObject> positionalArguments, Map<PythonString, PythonLikeObject> namedArguments);

    // TODO: Replace __call__ with $call
    // needed since __call__ not accessible in Python via JPype
    default PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        return __call__(positionalArguments, namedArguments);
    }

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
        return BuiltinTypes.FUNCTION_TYPE;
    }
}
