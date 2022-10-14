package org.optaplanner.python.translator.types.wrappers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonString;

public class JavaMethodReference implements PythonLikeFunction {
    private final Method method;
    private final Map<String, Integer> parameterNameToIndexMap;

    public JavaMethodReference(Method method, Map<String, Integer> parameterNameToIndexMap) {
        this.method = method;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    @Override
    public PythonLikeObject __call__(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        Object self;
        Object[] args;
        if (Modifier.isStatic(method.getModifiers())) {
            self = null;
            args = unwrapPrimitiveArguments(positionalArguments, namedArguments);
        } else {
            self = positionalArguments.get(0);
            if (self instanceof JavaObjectWrapper) { // unwrap wrapped Java Objects
                self = ((JavaObjectWrapper) self).getWrappedObject();
            }
            args = unwrapPrimitiveArguments(positionalArguments.subList(1, positionalArguments.size()), namedArguments);
        }
        try {
            return JavaPythonTypeConversionImplementor.wrapJavaObject(method.invoke(self, args));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method (" + method + ") is not accessible.", e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private Object[] unwrapPrimitiveArguments(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();
        Object[] out = new Object[method.getParameterCount()];
        Class<?>[] parameterTypes = method.getParameterTypes();

        for (int i = 0; i < positionalArguments.size(); i++) {
            PythonLikeObject argument = positionalArguments.get(i);
            out[i] = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(parameterTypes[i], argument);
        }

        for (PythonString key : namedArguments.keySet()) {
            if (key == PythonString.CALLER_INSTANCE_KEY) {
                continue;
            }
            int index = parameterNameToIndexMap.get(key.value);
            PythonLikeObject argument = namedArguments.get(key);
            out[index] = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(parameterTypes[index], argument);
        }

        return out;
    }
}
