package org.optaplanner.python.translator.builtins;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.UnaryLambdaReference;

public class GlobalBuiltins {
    public static PythonLikeObject lookup(PythonInterpreter interpreter, String builtinName) {
        switch (builtinName) {
            case "len":
                return new UnaryLambdaReference(a -> ((PythonLikeFunction)
                        (a.__getType().__getAttributeOrError("__len__"))).__call__(List.of(a), Map.of()),
                                                Map.of());
            case "print":
                return new UnaryLambdaReference(object -> {
                    interpreter.print(object);
                    return PythonNone.INSTANCE;
                }, Map.of());
            default:
                return null;
        }
    }

    public static PythonLikeObject lookupOrError(PythonInterpreter interpreter, String builtinName) {
        PythonLikeObject out = lookup(interpreter, builtinName);
        if (out == null) {
            throw new IllegalArgumentException(builtinName + " does not exist in global scope");
        }
        return out;
    }
}
