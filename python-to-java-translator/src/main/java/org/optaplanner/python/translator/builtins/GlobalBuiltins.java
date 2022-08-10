package org.optaplanner.python.translator.builtins;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonBoolean;
import org.optaplanner.python.translator.types.PythonFloat;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonIterator;
import org.optaplanner.python.translator.types.PythonLikeDict;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeList;
import org.optaplanner.python.translator.types.PythonLikeSet;
import org.optaplanner.python.translator.types.PythonLikeTuple;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.UnaryLambdaReference;
import org.optaplanner.python.translator.types.errors.ValueError;

public class GlobalBuiltins {
    public static PythonLikeObject lookup(PythonInterpreter interpreter, String builtinName) {
        switch (builtinName) {
            case "abs":
                return UnaryDunderBuiltin.ABS;
            case "all":
                return ((PythonLikeFunction) GlobalBuiltins::all);
            case "any":
                return ((PythonLikeFunction) GlobalBuiltins::any);
            case "bool":
                return PythonBoolean.getBooleanType();
            case "dict":
                return PythonLikeDict.DICT_TYPE;
            case "filter":
                return ((PythonLikeFunction) GlobalBuiltins::filter);
            case "float":
                return PythonFloat.FLOAT_TYPE;
            case "int":
                return PythonInteger.getIntType();
            case "iter":
                return UnaryDunderBuiltin.ITERATOR; // TODO: Iterator with sentinel value
            case "len":
                return UnaryDunderBuiltin.LENGTH;
            case "list":
                return PythonLikeList.LIST_TYPE;
            case "min":
                return ((PythonLikeFunction) GlobalBuiltins::min);
            case "max":
                return ((PythonLikeFunction) GlobalBuiltins::max);
            case "next":
                return UnaryDunderBuiltin.NEXT;
            case "repr":
                return UnaryDunderBuiltin.REPRESENTATION;
            case "set":
                return PythonLikeSet.SET_TYPE;
            case "str":
                return PythonString.STRING_TYPE;
            case "tuple":
                return PythonLikeTuple.TUPLE_TYPE;
            case "type":
                return PythonLikeType.getTypeType();
            case "print":
                return new UnaryLambdaReference(object -> {
                    interpreter.print(object);
                    return PythonNone.INSTANCE;
                }, Map.of());
            default:
                return null;
        }
    }

    public static PythonLikeObject lookupOrError(PythonInterpreter interpreter, String builtinName) {
        PythonLikeObject out = lookup(interpreter, builtinName);
        if (out == null) {
            throw new IllegalArgumentException(builtinName + " does not exist in global scope");
        }
        return out;
    }

    public static PythonBoolean all(List<PythonLikeObject> positionalArgs, Map<PythonString, PythonLikeObject> keywordArgs) {
        Iterator<PythonLikeObject> iterator;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(positionalArgs.get(0));
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("iterable"))) {
            iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR
                    .invoke(keywordArgs.get(PythonString.valueOf("iterable")));
        } else {
            throw new ValueError("all expects 1 argument, got " + positionalArgs.size());
        }

        while (iterator.hasNext()) {
            PythonLikeObject element = iterator.next();
            if (!PythonBoolean.isTruthful(element)) {
                return PythonBoolean.FALSE;
            }
        }

        return PythonBoolean.TRUE;
    }

    public static PythonBoolean any(List<PythonLikeObject> positionalArgs, Map<PythonString, PythonLikeObject> keywordArgs) {
        Iterator<PythonLikeObject> iterator;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(positionalArgs.get(0));
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("iterable"))) {
            iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR
                    .invoke(keywordArgs.get(PythonString.valueOf("iterable")));
        } else {
            throw new ValueError("any expects 1 argument, got " + positionalArgs.size());
        }

        while (iterator.hasNext()) {
            PythonLikeObject element = iterator.next();
            if (PythonBoolean.isTruthful(element)) {
                return PythonBoolean.TRUE;
            }
        }

        return PythonBoolean.FALSE;
    }

    public static PythonIterator filter(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject function;
        PythonLikeObject iterable;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 2 && keywordArgs.isEmpty()) {
            function = positionalArgs.get(0);
            iterable = positionalArgs.get(1);
        } else if (positionalArgs.size() == 1) {
            function = positionalArgs.get(0);
            iterable = keywordArgs.get(PythonString.valueOf("iterable"));
            if (iterable == null) {
                throw new ValueError("iterable is None");
            }
        } else if (positionalArgs.size() == 0) {
            function = keywordArgs.get(PythonString.valueOf("function"));
            iterable = keywordArgs.get(PythonString.valueOf("iterable"));
            if (iterable == null) {
                throw new ValueError("iterable is None");
            }

            if (function == null) {
                function = PythonNone.INSTANCE;
            }
        } else {
            throw new ValueError("filter expects 2 argument, got " + positionalArgs.size());
        }

        Iterator iterator;
        if (iterable instanceof Collection) {
            iterator = ((Collection) iterable).iterator();
        } else if (iterable instanceof Iterator) {
            iterator = (Iterator) iterable;
        } else {
            iterator = (Iterator) UnaryDunderBuiltin.ITERATOR.invoke(iterable);
        }

        PythonLikeFunction predicate;

        if (function == PythonNone.INSTANCE) {
            predicate = (pos, keywords) -> pos.get(0);
        } else {
            predicate = (PythonLikeFunction) function;
        }

        return new PythonIterator(StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false)
                .filter(element -> PythonBoolean.isTruthful(predicate.__call__(List.of((PythonLikeObject) element), null)))
                .iterator());
    }

    public static PythonLikeObject min(List<PythonLikeObject> positionalArgs, Map<PythonString, PythonLikeObject> keywordArgs) {
        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

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
        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

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
}
