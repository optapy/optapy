package org.optaplanner.optapy.translator.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.implementors.JavaPythonTypeConversionImplementor;

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
            return JavaPythonTypeConversionImplementor.wrapJavaObject(method.invoke(self, args));
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
            out[i] = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(parameterTypes[i], argument);
        }

        for (String key : namedArguments.keySet()) {
            int index = parameterNameToIndexMap.get(key);
            PythonLikeObject argument = namedArguments.get(key);
            out[index] = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(parameterTypes[index], argument);
        }

        return out;
    }
}
