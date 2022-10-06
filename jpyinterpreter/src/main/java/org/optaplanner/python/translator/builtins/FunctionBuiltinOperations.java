package org.optaplanner.python.translator.builtins;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.BoundPythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;

public class FunctionBuiltinOperations {
    public static PythonLikeObject bindFunctionToInstance(final PythonLikeFunction function, final PythonLikeObject instance,
            final PythonLikeType type) {
        return new BoundPythonLikeFunction(instance, function);
    }
}
