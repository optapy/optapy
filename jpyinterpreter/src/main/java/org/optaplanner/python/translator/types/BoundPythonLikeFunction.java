package org.optaplanner.python.translator.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;

public class BoundPythonLikeFunction implements PythonLikeFunction {
    private final PythonLikeObject instance;
    private final PythonLikeFunction function;

    public BoundPythonLikeFunction(PythonLikeObject instance, PythonLikeFunction function) {
        this.instance = instance;
        this.function = function;
    }

    public PythonLikeObject getInstance() {
        return instance;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        ArrayList<PythonLikeObject> actualPositionalArgs = new ArrayList<>(positionalArguments.size() + 1);
        actualPositionalArgs.add(instance);
        actualPositionalArgs.addAll(positionalArguments);
        return function.__call__(actualPositionalArgs, namedArguments);
    }
}
