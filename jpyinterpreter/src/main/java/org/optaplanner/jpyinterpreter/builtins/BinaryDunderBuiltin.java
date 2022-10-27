package org.optaplanner.jpyinterpreter.builtins;

import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.errors.ValueError;

public class BinaryDunderBuiltin implements PythonLikeFunction {
    private final String DUNDER_METHOD_NAME;

    public static final BinaryDunderBuiltin DIVMOD = new BinaryDunderBuiltin(PythonBinaryOperators.DIVMOD);
    public static final BinaryDunderBuiltin ADD = new BinaryDunderBuiltin(PythonBinaryOperators.ADD);
    public static final BinaryDunderBuiltin LESS_THAN = new BinaryDunderBuiltin(PythonBinaryOperators.LESS_THAN);
    public static final BinaryDunderBuiltin GET_ITEM = new BinaryDunderBuiltin(PythonBinaryOperators.GET_ITEM);
    public static final BinaryDunderBuiltin GET_ATTRIBUTE = new BinaryDunderBuiltin(PythonBinaryOperators.GET_ATTRIBUTE);
    public static final BinaryDunderBuiltin POWER = new BinaryDunderBuiltin(PythonBinaryOperators.POWER);
    public static final BinaryDunderBuiltin FORMAT = new BinaryDunderBuiltin(PythonBinaryOperators.FORMAT);

    public BinaryDunderBuiltin(String dunderMethodName) {
        DUNDER_METHOD_NAME = dunderMethodName;
    }

    public BinaryDunderBuiltin(PythonBinaryOperators operator) {
        DUNDER_METHOD_NAME = operator.getDunderMethod();
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();

        if (positionalArguments.size() != 2) {
            throw new ValueError("Function " + DUNDER_METHOD_NAME + " expects 2 positional arguments");
        }

        PythonLikeObject object = positionalArguments.get(0);
        PythonLikeObject arg = positionalArguments.get(1);
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.__getType().__getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.$call(List.of(object, arg), Map.of(), null);
    }

    public PythonLikeObject invoke(PythonLikeObject object, PythonLikeObject arg) {
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.__getType().__getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.$call(List.of(object, arg), Map.of(), null);
    }
}
