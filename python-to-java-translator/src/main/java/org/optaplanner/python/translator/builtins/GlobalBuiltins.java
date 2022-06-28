package org.optaplanner.python.translator.builtins;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.UnaryLambdaReference;
import org.optaplanner.python.translator.types.errors.ValueError;

public class GlobalBuiltins {
    public static PythonLikeObject lookup(PythonInterpreter interpreter, String builtinName) {
        switch (builtinName) {
            case "len":
                return new UnaryLambdaReference(
                        a -> ((PythonLikeFunction) (a.__getType().__getAttributeOrError("__len__"))).__call__(List.of(a),
                                Map.of()),
                        Map.of());
            case "iter":
                return new UnaryLambdaReference(
                        a -> ((PythonLikeFunction) (a.__getType().__getAttributeOrError("__iter__"))).__call__(List.of(a),
                                Map.of()),
                        Map.of());
            case "next":
                return new UnaryLambdaReference(
                        a -> ((PythonLikeFunction) (a.__getType().__getAttributeOrError("__next__"))).__call__(List.of(a),
                                Map.of()),
                        Map.of());
            case "int":
                return new UnaryLambdaReference(
                        a -> ((PythonLikeFunction) (a.__getType().__getAttributeOrError("__int__"))).__call__(List.of(a),
                                Map.of()),
                        Map.of());
            case "min":
                return ((PythonLikeFunction) GlobalBuiltins::min);
            case "max":
                return ((PythonLikeFunction) GlobalBuiltins::max);
            case "print":
                return new UnaryLambdaReference(object -> {
                    interpreter.print(object);
                    return PythonNone.INSTANCE;
                }, Map.of());
            default:
                return null;
        }
    }

    public static PythonLikeObject min(List<PythonLikeObject> positionalArgs, Map<PythonString, PythonLikeObject> keywordArgs) {
        if (positionalArgs.isEmpty()) {
            PythonLikeObject defaultValue = keywordArgs.get(PythonString.valueOf("default"));
            if (!keywordArgs.containsKey(PythonString.valueOf("default"))) {
                throw new ValueError("No arguments were passed to min, and no default was provided");
            }
            return defaultValue;
        } else if (positionalArgs.size() == 1) {
            Iterator<Comparable> iterator = (Iterator<Comparable>) ((PythonLikeFunction) (positionalArgs.get(0).__getType()
                    .__getAttributeOrError("__iter__"))).__call__(List.of(positionalArgs.get(0)),
                            Map.of());
            Comparable min = null;
            for (Iterator<Comparable> it = iterator; it.hasNext();) {
                Comparable item = it.next();
                if (min == null) {
                    min = item;
                } else {
                    if (item.compareTo(min) < 0) {
                        min = item;
                    }
                }
            }
            if (min == null) {
                PythonLikeObject defaultValue = keywordArgs.get(PythonString.valueOf("default"));
                if (!keywordArgs.containsKey(PythonString.valueOf("default"))) {
                    throw new ValueError("Iterable is empty, and no default was provided");
                }
                return defaultValue;
            } else {
                return (PythonLikeObject) min;
            }
        } else {
            Comparable min = (Comparable) positionalArgs.get(0);
            for (PythonLikeObject item : positionalArgs) {
                Comparable comparableItem = (Comparable) item;
                if (comparableItem.compareTo(min) < 0) {
                    min = comparableItem;
                }
            }
            return (PythonLikeObject) min;
        }
    }

    public static PythonLikeObject max(List<PythonLikeObject> positionalArgs, Map<PythonString, PythonLikeObject> keywordArgs) {
        if (positionalArgs.isEmpty()) {
            PythonLikeObject defaultValue = keywordArgs.get(PythonString.valueOf("default"));
            if (!keywordArgs.containsKey(PythonString.valueOf("default"))) {
                throw new ValueError("No arguments were passed to max, and no default was provided");
            }
            return defaultValue;
        } else if (positionalArgs.size() == 1) {
            Iterator<Comparable> iterator = (Iterator<Comparable>) ((PythonLikeFunction) (positionalArgs.get(0).__getType()
                    .__getAttributeOrError("__iter__"))).__call__(List.of(positionalArgs.get(0)),
                            Map.of());
            Comparable max = null;
            for (Iterator<Comparable> it = iterator; it.hasNext();) {
                Comparable item = it.next();
                if (max == null) {
                    max = item;
                } else {
                    if (item.compareTo(max) > 0) {
                        max = item;
                    }
                }
            }
            if (max == null) {
                PythonLikeObject defaultValue = keywordArgs.get(PythonString.valueOf("default"));
                if (!keywordArgs.containsKey(PythonString.valueOf("default"))) {
                    throw new ValueError("Iterable is empty, and no default was provided");
                }
                return defaultValue;
            } else {
                return (PythonLikeObject) max;
            }
        } else {
            Comparable max = (Comparable) positionalArgs.get(0);
            for (PythonLikeObject item : positionalArgs) {
                Comparable comparableItem = (Comparable) item;
                if (comparableItem.compareTo(max) > 0) {
                    max = comparableItem;
                }
            }
            return (PythonLikeObject) max;
        }
    }

    public static PythonLikeObject lookupOrError(PythonInterpreter interpreter, String builtinName) {
        PythonLikeObject out = lookup(interpreter, builtinName);
        if (out == null) {
            throw new IllegalArgumentException(builtinName + " does not exist in global scope");
        }
        return out;
    }
}
