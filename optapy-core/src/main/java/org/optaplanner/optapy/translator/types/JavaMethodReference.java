package org.optaplanner.optapy.translator.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;

public class JavaMethodReference implements PythonLikeFunction {
    private final Method method;
    private final Map<String, Integer> parameterNameToIndexMap;

    public JavaMethodReference(Method method, Map<String, Integer> parameterNameToIndexMap) {
        this.method = method;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments, Map<String, PythonLikeObject> namedArguments) {
        Object self;
        Object[] args;
        if (Modifier.isStatic(method.getModifiers())) {
            self = null;
            args = unwrapPrimitiveArguments(positionalArguments, namedArguments);
        } else {
            self = positionalArguments.get(0);
            args = unwrapPrimitiveArguments(positionalArguments.subList(1, positionalArguments.size()), namedArguments);
        }
        try {
            return wrapResult(method.invoke(self, args));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method (" + method + ") is not accessible.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Object[] unwrapPrimitiveArguments(List<PythonLikeObject> positionalArguments,
                                              Map<String, PythonLikeObject> namedArguments) {
        namedArguments = (namedArguments != null)? namedArguments : Map.of();
        Object[] out = new Object[method.getParameterCount()];
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < positionalArguments.size(); i++) {
            PythonLikeObject argument = positionalArguments.get(i);
            out[i] = unwrapPrimitiveObject(parameterTypes[i], argument);
        }

        for (String key : namedArguments.keySet()) {
            int index = parameterNameToIndexMap.get(key);
            PythonLikeObject argument = namedArguments.get(key);
            out[index] = unwrapPrimitiveObject(parameterTypes[index], argument);
        }

        return out;
    }

    private PythonLikeObject wrapResult(Object result) {
        if (null == result) {
            return PythonNone.INSTANCE;
        }

        if (result instanceof PythonLikeObject) {
            return (PythonLikeObject) result;
        }

        Class<?> type = result.getClass();

        if (boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
            return PythonBoolean.valueOf((boolean) result);
        } else if (byte.class.isAssignableFrom(type)) {
            return new PythonInteger((byte) result);
        } else if (char.class.isAssignableFrom(type)) {
            // TODO: PythonCharacter class?
            return new PythonInteger((char) result);
        } else if (short.class.isAssignableFrom(type)) {
            return new PythonInteger((short) result);
        } else if (int.class.isAssignableFrom(type)) {
            return new PythonInteger((int) result);
        } else if (long.class.isAssignableFrom(type)) {
            return new PythonInteger((long) result);
        } else if (float.class.isAssignableFrom(type)) {
            return new PythonFloat((float) result);
        } else if (double.class.isAssignableFrom(type)) {
            return new PythonFloat((double) result);
        } else if (Iterator.class.isAssignableFrom(type)) {
            return new PythonIterator((Iterator) result);
        } else {
            throw new IllegalStateException("Unhandled type (" + type + ").");
        }
    }

    private static Object unwrapPrimitiveObject(Class<?> type, PythonLikeObject argument) {
        if (boolean.class.equals(type)) {
            return ((PythonBoolean) argument).getValue();
        } else if (byte.class.equals(type)) {
            return ((PythonNumber) argument).getValue().byteValue();
        } else if (char.class.equals(type)) {
            // TODO: PythonCharacter class?
            return ((PythonNumber) argument).getValue();
        } else if (short.class.equals(type)) {
            return ((PythonNumber) argument).getValue().shortValue();
        } else if (int.class.equals(type)) {
            return ((PythonNumber) argument).getValue().intValue();
        } else if (long.class.equals(type)) {
            return ((PythonNumber) argument).getValue().longValue();
        } else if (float.class.equals(type)) {
            return ((PythonNumber) argument).getValue().floatValue();
        } else if (double.class.equals(type)) {
            return ((PythonNumber) argument).getValue().doubleValue();
        } else {
            return argument;
        }
    }
}
