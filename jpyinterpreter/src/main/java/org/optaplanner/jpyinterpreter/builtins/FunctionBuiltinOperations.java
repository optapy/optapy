package org.optaplanner.jpyinterpreter.builtins;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BoundPythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class FunctionBuiltinOperations {
    public static PythonLikeObject bindFunctionToInstance(final PythonLikeFunction function, final PythonLikeObject instance,
            final PythonLikeType type) {
        return new BoundPythonLikeFunction(instance, function);
    }

    public static PythonLikeObject bindFunctionToType(final PythonLikeFunction function, final PythonLikeObject instance,
            final PythonLikeType type) {
        return new BoundPythonLikeFunction(type, function);
    }
}
