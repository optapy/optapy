package org.optaplanner.python.translator.builtins;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonTernaryOperators;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.ValueError;

public class TernaryDunderBuiltin implements PythonLikeFunction {
    private final String DUNDER_METHOD_NAME;

    public static final TernaryDunderBuiltin POWER = new TernaryDunderBuiltin("__pow__");
    public static final TernaryDunderBuiltin SETATTR = new TernaryDunderBuiltin(PythonTernaryOperators.SET_ATTRIBUTE);
    public static final TernaryDunderBuiltin GET_DESCRIPTOR = new TernaryDunderBuiltin(PythonTernaryOperators.GET);

    public TernaryDunderBuiltin(String dunderMethodName) {
        DUNDER_METHOD_NAME = dunderMethodName;
    }

    public TernaryDunderBuiltin(PythonTernaryOperators operator) {
        DUNDER_METHOD_NAME = operator.getDunderMethod();
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();

        if (positionalArguments.size() != 3 || !namedArguments.isEmpty()) {
            throw new ValueError("Function " + DUNDER_METHOD_NAME + " expects 3 positional arguments");
        }

        PythonLikeObject object = positionalArguments.get(0);
        PythonLikeObject arg1 = positionalArguments.get(1);
        PythonLikeObject arg2 = positionalArguments.get(2);
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.__getType().__getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.__call__(List.of(object, arg1, arg2), Map.of());
    }

    public PythonLikeObject invoke(PythonLikeObject object, PythonLikeObject arg1, PythonLikeObject arg2) {
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.__getType().__getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.__call__(List.of(object, arg1, arg2), Map.of());
    }
}
