package org.optaplanner.optapy.translator.types;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.optaplanner.optapy.PythonLikeObject;

public class BinaryNumericLambdaReference implements PythonLikeFunction {
    private final BiFunction<Long, Long, Number> longLambda;
    private final BiFunction<Double, Double, Number> doubleLambda;
    private final Map<String, Integer> parameterNameToIndexMap;

    public BinaryNumericLambdaReference(BiFunction<Long, Long, Number> longLambda,
                                        BiFunction<Double, Double, Number> doubleLambda) {
        this(longLambda, doubleLambda, Collections.emptyMap());
    }

    public BinaryNumericLambdaReference(BiFunction<Long, Long, Number> longLambda,
                                        BiFunction<Double, Double, Number> doubleLambda,
                                        Map<String, Integer> parameterNameToIndexMap) {
        this.longLambda = longLambda;
        this.doubleLambda = doubleLambda;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments, Map<String, PythonLikeObject> namedArguments) {
        PythonLikeObject[] args = new PythonLikeObject[2];
        for (int i = 0; i < positionalArguments.size(); i++) {
            args[i] = positionalArguments.get(i);
        }

        if (namedArguments != null) {
            for (String key : namedArguments.keySet()) {
                args[parameterNameToIndexMap.get(key)] = namedArguments.get(key);
            }
        }

        Number result;
        if (args[0] instanceof PythonInteger && args[1] instanceof PythonInteger) {
            result = longLambda.apply(((PythonInteger) args[0]).value, ((PythonInteger) args[1]).value);
        } else {
            result = doubleLambda.apply(((PythonNumber) args[0]).getValue().doubleValue(),
                                        ((PythonNumber) args[1]).getValue().doubleValue());
        }

        if (result instanceof Long) {
            return PythonInteger.valueOf(result.longValue());
        } else {
            return PythonFloat.valueOf(result.doubleValue());
        }
    }
}
