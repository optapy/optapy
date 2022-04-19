package org.optaplanner.optapy.translator.types;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.optaplanner.optapy.PythonLikeObject;

public class UnaryLambdaReference implements PythonLikeFunction {
    private final Function<PythonLikeObject, PythonLikeObject> lambda;
    private final Map<String, Integer> parameterNameToIndexMap;

    public UnaryLambdaReference(Function<PythonLikeObject, PythonLikeObject> lambda, Map<String, Integer> parameterNameToIndexMap) {
        this.lambda = lambda;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments, Map<PythonString, PythonLikeObject> namedArguments) {
        PythonLikeObject[] args = new PythonLikeObject[1];
        for (int i = 0; i < positionalArguments.size(); i++) {
            args[i] = positionalArguments.get(i);
        }

        if (namedArguments != null) {
            for (PythonString key : namedArguments.keySet()) {
                args[parameterNameToIndexMap.get(key.value)] = namedArguments.get(key);
            }
        }

        return lambda.apply(args[0]);
    }
}
