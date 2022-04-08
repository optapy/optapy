package org.optaplanner.optapy.translator.types;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.optaplanner.optapy.PythonLikeObject;

public class UnaryNumericLambdaReference implements PythonLikeFunction {
    private final Function<Long, Number> longLambda;
    private final Function<Double, Number> doubleLambda;
    private final Map<String, Integer> parameterNameToIndexMap;

    public UnaryNumericLambdaReference(Function<Long, Number> longLambda,
                                       Function<Double, Number> doubleLambda) {
        this(longLambda, doubleLambda, Collections.emptyMap());
    }

    public UnaryNumericLambdaReference(Function<Long, Number> longLambda,
                                       Function<Double, Number> doubleLambda,
                                       Map<String, Integer> parameterNameToIndexMap) {
        this.longLambda = longLambda;
        this.doubleLambda = doubleLambda;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments, Map<String, PythonLikeObject> namedArguments) {
        PythonLikeObject[] args = new PythonLikeObject[1];
        for (int i = 0; i < positionalArguments.size(); i++) {
            args[i] = positionalArguments.get(i);

            // Yes, booleans in Python can be treated as number in Python (ex: True + True = 2)
            if (args[i] instanceof PythonBoolean) {
                if (((PythonBoolean) args[i]).getValue()) {
                    args[i] = PythonInteger.valueOf(1L);
                } else {
                    args[i] = PythonInteger.valueOf(0L);
                }
            }
        }

        if (namedArguments != null) {
            for (String key : namedArguments.keySet()) {
                args[parameterNameToIndexMap.get(key)] = namedArguments.get(key);
            }
        }

        Number result;
        if (args[0] instanceof PythonInteger) {
            result = longLambda.apply(((PythonInteger) args[0]).value);
        } else {
            result = doubleLambda.apply(((PythonNumber) args[0]).getValue().doubleValue());
        }

        if (result instanceof Long) {
            return PythonInteger.valueOf(result.longValue());
        } else {
            return PythonFloat.valueOf(result.doubleValue());
        }
    }
}