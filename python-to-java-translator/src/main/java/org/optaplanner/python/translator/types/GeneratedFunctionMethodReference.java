package org.optaplanner.python.translator.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;

public class GeneratedFunctionMethodReference implements PythonLikeFunction {

    private final Object instance;

    private final Method method;
    private final Map<String, Integer> parameterNameToIndexMap;

    public GeneratedFunctionMethodReference(Object instance, Method method, Map<String, Integer> parameterNameToIndexMap) {
        this.instance = instance;
        this.method = method;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        Object[] args = unwrapPrimitiveArguments(positionalArguments, namedArguments);
        try {
            return (PythonLikeObject) method.invoke(instance, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method (" + method + ") is not accessible.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Object[] unwrapPrimitiveArguments(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();
        Object[] out = new Object[method.getParameterCount()];

        for (int i = 0; i < positionalArguments.size(); i++) {
            PythonLikeObject argument = positionalArguments.get(i);
            out[i] = argument;
        }

        for (PythonString key : namedArguments.keySet()) {
            int index = parameterNameToIndexMap.get(key.value);
            PythonLikeObject argument = namedArguments.get(key);
            out[index] = argument;
        }

        return out;
    }
}
