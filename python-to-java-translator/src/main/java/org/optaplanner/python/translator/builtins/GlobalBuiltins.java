package org.optaplanner.python.translator.builtins;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.UnaryLambdaReference;

public class GlobalBuiltins {
    public static PythonLikeObject lookup(String builtinName) {
        switch (builtinName) {
            case "len":
                return new UnaryLambdaReference(a -> ((PythonLikeFunction)
                        (a.__getType().__getAttributeOrError("__len__"))).__call__(List.of(a), Map.of()),
                                                Map.of());
            default:
                return null;
        }
    }

    public static PythonLikeObject lookupOrError(String builtinName) {
        PythonLikeObject out = lookup(builtinName);
        if (out == null) {
            throw new IllegalArgumentException(builtinName + " does not exist in global scope");
        }
        return out;
    }
}
