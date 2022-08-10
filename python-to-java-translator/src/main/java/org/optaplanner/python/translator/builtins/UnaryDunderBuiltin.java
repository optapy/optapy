package org.optaplanner.python.translator.builtins;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.ValueError;

public class UnaryDunderBuiltin implements PythonLikeFunction {
    private final String DUNDER_METHOD_NAME;

    public static final UnaryDunderBuiltin ABS = new UnaryDunderBuiltin(PythonUnaryOperator.ABS);
    public static final UnaryDunderBuiltin INT = new UnaryDunderBuiltin(PythonUnaryOperator.AS_INT);
    public static final UnaryDunderBuiltin ITERATOR = new UnaryDunderBuiltin(PythonUnaryOperator.ITERATOR);
    public static final UnaryDunderBuiltin LENGTH = new UnaryDunderBuiltin(PythonUnaryOperator.LENGTH);
    public static final UnaryDunderBuiltin NEXT = new UnaryDunderBuiltin(PythonUnaryOperator.NEXT);
    public static final UnaryDunderBuiltin REPRESENTATION = new UnaryDunderBuiltin(PythonUnaryOperator.REPRESENTATION);

    public UnaryDunderBuiltin(String dunderMethodName) {
        DUNDER_METHOD_NAME = dunderMethodName;
    }

    public UnaryDunderBuiltin(PythonUnaryOperator operator) {
        DUNDER_METHOD_NAME = operator.getDunderMethod();
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();

        if (positionalArguments.size() != 1 || !namedArguments.isEmpty()) {
            throw new ValueError("Function " + DUNDER_METHOD_NAME + " expects 1 positional argument");
        }
        PythonLikeObject object = positionalArguments.get(0);
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.__getType().__getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.__call__(List.of(object), Map.of());
    }

    public PythonLikeObject invoke(PythonLikeObject object) {
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.__getType().__getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.__call__(List.of(object), Map.of());
    }
}
