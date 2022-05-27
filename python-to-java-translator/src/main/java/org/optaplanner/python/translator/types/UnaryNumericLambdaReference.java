package org.optaplanner.python.translator.types;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.optaplanner.python.translator.PythonLikeObject;

public class UnaryNumericLambdaReference implements PythonLikeFunction {
    private final Function<BigInteger, Number> longLambda;
    private final Function<Double, Number> doubleLambda;
    private final Map<String, Integer> parameterNameToIndexMap;

    public UnaryNumericLambdaReference(Function<BigInteger, Number> longLambda,
            Function<Double, Number> doubleLambda) {
        this(longLambda, doubleLambda, Collections.emptyMap());
    }

    public UnaryNumericLambdaReference(Function<BigInteger, Number> longLambda,
            Function<Double, Number> doubleLambda,
            Map<String, Integer> parameterNameToIndexMap) {
        this.longLambda = longLambda;
        this.doubleLambda = doubleLambda;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        PythonLikeObject[] args = new PythonLikeObject[1];
        for (int i = 0; i < positionalArguments.size(); i++) {
            args[i] = positionalArguments.get(i);
        }

        if (namedArguments != null) {
            for (PythonString key : namedArguments.keySet()) {
                args[parameterNameToIndexMap.get(key.value)] = namedArguments.get(key);
            }
        }

        Number result;
        if (args[0] instanceof PythonInteger) {
            result = longLambda.apply(((PythonInteger) args[0]).value);
        } else {
            result = doubleLambda.apply(((PythonNumber) args[0]).getValue().doubleValue());
        }

        if (result instanceof BigInteger) {
            return PythonInteger.valueOf((BigInteger) result);
        } else {
            return PythonFloat.valueOf(result.doubleValue());
        }
    }
}
