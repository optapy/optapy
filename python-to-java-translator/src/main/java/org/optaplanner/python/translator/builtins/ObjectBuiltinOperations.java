package org.optaplanner.python.translator.builtins;

import java.util.Formatter;
import java.util.List;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonTernaryOperators;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.AttributeError;

public class ObjectBuiltinOperations {
    public static PythonLikeObject getAttribute(PythonLikeObject object, String name) {
        PythonLikeObject objectResult = object.__getAttributeOrNull(name);
        if (objectResult != null) {
            return objectResult;
        }

        PythonLikeType type = object.__getType();
        PythonLikeObject typeResult = type.__getAttributeOrNull(name);
        if (typeResult != null) {
            PythonLikeObject maybeDescriptor = typeResult.__getAttributeOrNull(PythonTernaryOperators.GET.dunderMethod);
            if (maybeDescriptor == null) {
                maybeDescriptor = typeResult.__getType().__getAttributeOrNull(PythonTernaryOperators.GET.dunderMethod);
            }

            if (maybeDescriptor != null) {
                if (!(maybeDescriptor instanceof PythonLikeFunction)) {
                    throw new UnsupportedOperationException("'" + maybeDescriptor.__getType() + "' is not callable");
                }
                PythonLikeFunction descriptor = (PythonLikeFunction) maybeDescriptor;
                return descriptor.__call__(List.of(typeResult, object, PythonNone.INSTANCE), null);
            }
            return typeResult;
        }

        throw new AttributeError("object '" + object + "' does not have attribute '" + name + "'");
    }

    public static void setAttribute(PythonLikeObject object, String name, PythonLikeObject value) {
        object.__setAttribute(name, value);
    }

    public static void deleteAttribute(PythonLikeObject object, String name) {
        object.__deleteAttribute(name);
    }

    // Based on https://github.com/python/cpython/blob/d44815cabc0a8d9932df2fa95cb374eadddb7c17/Objects/abstract.c#L768-L827
    public static PythonLikeObject format(PythonLikeObject object, PythonLikeObject formatString) {
        // TODO: Create TypeError and throw it instead of RuntimeError
        // TODO: Implement https://peps.python.org/pep-3101/#standard-format-specifiers
        if (formatString != PythonNone.INSTANCE && !(formatString instanceof PythonString)) {
            throw new RuntimeException("Format specifier must be a string, not " + formatString.__getType());
        }

        // Fast path for common types.
        if (formatString == PythonNone.INSTANCE || ((PythonString) formatString).value.length() == 0) {
            if (object instanceof PythonString) {
                return object;
            }
            if (object instanceof PythonInteger) {
                return new PythonString(object.toString());
            }
        }

        // If no format_spec is provided, use an empty string
        if (formatString == PythonNone.INSTANCE) {
            formatString = new PythonString("");
        }

        PythonLikeObject method = object.__getType().__getAttributeOrError(PythonBinaryOperators.FORMAT.dunderMethod);
        if (!(method instanceof PythonLikeFunction)) {
            throw new RuntimeException(
                    "Type " + object.__getType() + " __format__ attribute must be a function, not " + method.__getType());
        }

        PythonLikeObject result = ((PythonLikeFunction) method).__call__(List.of(object, formatString), null);

        if (!(result instanceof PythonString)) {
            throw new RuntimeException("__format__ must return a str, not " + result.__getType());
        }

        return result;
    }

    public static PythonString formatPythonObject(PythonLikeObject object, PythonLikeObject formatString) {
        String javaFormatString = getJavaNumberFormatString(((PythonString) formatString).value);
        return new PythonString(new Formatter().format(javaFormatString, object.toString()).toString());
    }

    public static String getJavaNumberFormatString(String pythonFormatString) {
        // Python format
        // [[fill]align][sign][#][0][minimumwidth][.precision][type]
        // Java format
        //  %[argument_index$][flags][width][.precision]conversion
        return "%1$" + pythonFormatString;
    }
}
