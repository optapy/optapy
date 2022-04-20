package org.optaplanner.optapy.translator.builtins;

import java.util.ArrayList;

import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.types.PythonLikeFunction;
import org.optaplanner.optapy.translator.types.PythonLikeType;

public class FunctionBuiltinOperations {
    public static PythonLikeObject bindFunctionToInstance(final PythonLikeFunction function, final PythonLikeObject instance, final PythonLikeType type) {
        return (PythonLikeFunction) (positionalArguments, namedArguments) -> {
            ArrayList<PythonLikeObject> actualPositionalArgs = new ArrayList<>(positionalArguments.size() + 1);
            actualPositionalArgs.add(instance);
            actualPositionalArgs.addAll(positionalArguments);
            return function.__call__(actualPositionalArgs, namedArguments);
        };
    }
}
