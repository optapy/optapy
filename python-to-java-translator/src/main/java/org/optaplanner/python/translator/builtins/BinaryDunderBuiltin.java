package org.optaplanner.python.translator.builtins;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.ValueError;

public class BinaryDunderBuiltin implements PythonLikeFunction {
    private final String DUNDER_METHOD_NAME;

    public static final BinaryDunderBuiltin DIVMOD = new BinaryDunderBuiltin(PythonBinaryOperators.DIVMOD);
    public static final BinaryDunderBuiltin ADD = new BinaryDunderBuiltin(PythonBinaryOperators.ADD);
    public static final BinaryDunderBuiltin LESS_THAN = new BinaryDunderBuiltin(PythonBinaryOperators.LESS_THAN);
    public static final BinaryDunderBuiltin GET_ITEM = new BinaryDunderBuiltin(PythonBinaryOperators.GET_ITEM);
    public static final BinaryDunderBuiltin POWER = new BinaryDunderBuiltin(PythonBinaryOperators.POWER);
    public static final BinaryDunderBuiltin FORMAT = new BinaryDunderBuiltin(PythonBinaryOperators.FORMAT);

    public BinaryDunderBuiltin(String dunderMethodName) {
        DUNDER_METHOD_NAME = dunderMethodName;
    }

    public BinaryDunderBuiltin(PythonBinaryOperators operator) {
        DUNDER_METHOD_NAME = operator.getDunderMethod();
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();

        if (positionalArguments.size() != 2 || !namedArguments.isEmpty()) {
            throw new ValueError("Function " + DUNDER_METHOD_NAME + " expects 2 positional arguments");
        }

        PythonLikeObject object = positionalArguments.get(0);
        PythonLikeObject arg = positionalArguments.get(1);
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.__getType().__getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.__call__(List.of(object, arg), Map.of());
    }

    public PythonLikeObject invoke(PythonLikeObject object, PythonLikeObject arg) {
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.__getType().__getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.__call__(List.of(object, arg), Map.of());
    }
}