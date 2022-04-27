package org.optaplanner.python.translator.types;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonTernaryOperators;
import org.optaplanner.python.translator.builtins.FunctionBuiltinOperations;

public interface PythonLikeFunction extends PythonLikeObject {

    PythonLikeType FUNCTION_TYPE = new PythonLikeType("function", type -> {
        try {
            type.__dir__.put(PythonTernaryOperators.GET.dunderMethod,
                    new JavaMethodReference(
                            FunctionBuiltinOperations.class.getMethod("bindFunctionToInstance", PythonLikeFunction.class,
                                    PythonLikeObject.class, PythonLikeType.class),
                            Map.of("self", 0, "obj", 1, "objtype", 2)));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    });

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
