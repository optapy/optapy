package org.optaplanner.optapy.translator.builtins;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.PythonTernaryOperators;
import org.optaplanner.optapy.translator.types.PythonLikeFunction;
import org.optaplanner.optapy.translator.types.PythonLikeType;
import org.optaplanner.optapy.translator.types.PythonNone;

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

        // TODO: Raise AttributeError instead of NoSuchElementException
        throw new NoSuchElementException("object '" + object + "' does not have attribute '" + name + "'");
    }
    public static void setAttribute(PythonLikeObject object, String name, PythonLikeObject value) {
        object.__setAttribute(name, value);
    }

    public static void deleteAttribute(PythonLikeObject object, String name) {
        object.__deleteAttribute(name);
    }
}
