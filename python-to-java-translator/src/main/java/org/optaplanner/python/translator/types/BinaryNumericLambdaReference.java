package org.optaplanner.python.translator.types;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.optaplanner.python.translator.PythonLikeObject;

public class BinaryNumericLambdaReference implements PythonLikeFunction {
    private final BiFunction<BigInteger, BigInteger, Number> longLambda;
    private final BiFunction<Double, Double, Number> doubleLambda;
    private final Map<String, Integer> parameterNameToIndexMap;

    public BinaryNumericLambdaReference(BiFunction<BigInteger, BigInteger, Number> longLambda,
            BiFunction<Double, Double, Number> doubleLambda) {
        this(longLambda, doubleLambda, Collections.emptyMap());
    }

    public BinaryNumericLambdaReference(BiFunction<BigInteger, BigInteger, Number> longLambda,
            BiFunction<Double, Double, Number> doubleLambda,
            Map<String, Integer> parameterNameToIndexMap) {
        this.longLambda = longLambda;
        this.doubleLambda = doubleLambda;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        PythonLikeObject[] args = new PythonLikeObject[2];
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
            for (PythonString key : namedArguments.keySet()) {
                args[parameterNameToIndexMap.get(key.value)] = namedArguments.get(key);
            }
        }

        Number result;
        if (args[0] instanceof PythonInteger && args[1] instanceof PythonInteger) {
            result = longLambda.apply(((PythonInteger) args[0]).value, ((PythonInteger) args[1]).value);
        } else {
            result = doubleLambda.apply(((PythonNumber) args[0]).getValue().doubleValue(),
                    ((PythonNumber) args[1]).getValue().doubleValue());
        }

        if (result instanceof BigInteger) {
            return PythonInteger.valueOf((BigInteger) result);
        } else {
            return PythonFloat.valueOf(result.doubleValue());
        }
    }
}
