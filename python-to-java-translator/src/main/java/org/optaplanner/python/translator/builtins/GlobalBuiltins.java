package org.optaplanner.python.translator.builtins;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.CPythonBackedPythonLikeObject;
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
import org.optaplanner.python.translator.types.PythonRange;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.AttributeError;
import org.optaplanner.python.translator.types.errors.StopIteration;
import org.optaplanner.python.translator.types.errors.ValueError;

public class GlobalBuiltins {
    final static StackWalker stackWalker = getInstance();

    private static StackWalker getInstance() {
        return StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
    }

    public static PythonLikeObject lookup(PythonInterpreter interpreter, String builtinName) {
        switch (builtinName) {
            case "abs":
                return UnaryDunderBuiltin.ABS;
            case "all":
                return ((PythonLikeFunction) GlobalBuiltins::all);
            case "any":
                return ((PythonLikeFunction) GlobalBuiltins::any);
            case "ascii":
                return ((PythonLikeFunction) GlobalBuiltins::ascii);
            case "bin":
                return ((PythonLikeFunction) GlobalBuiltins::bin);
            case "bool":
                return PythonBoolean.getBooleanType();
            case "callable":
                return ((PythonLikeFunction) GlobalBuiltins::callable);
            case "chr":
                return ((PythonLikeFunction) GlobalBuiltins::chr);
            case "delattr":
                return ((PythonLikeFunction) GlobalBuiltins::delattr);
            case "divmod":
                return BinaryDunderBuiltin.DIVMOD;
            case "dict":
                return PythonLikeDict.DICT_TYPE;
            case "enumerate":
                return ((PythonLikeFunction) GlobalBuiltins::enumerate);
            case "filter":
                return ((PythonLikeFunction) GlobalBuiltins::filter);
            case "float":
                return PythonFloat.FLOAT_TYPE;
            case "format":
                return ((PythonLikeFunction) GlobalBuiltins::format);
            case "getattr":
                return ((PythonLikeFunction) GlobalBuiltins::getattr);
            case "globals":
                return ((PythonLikeFunction) GlobalBuiltins::globals);
            case "hasattr":
                return ((PythonLikeFunction) GlobalBuiltins::hasattr);
            case "hash":
                return UnaryDunderBuiltin.HASH;
            case "hex":
                return ((PythonLikeFunction) GlobalBuiltins::hex);
            case "id":
                return ((PythonLikeFunction) GlobalBuiltins::id);
            case "input":
                return GlobalBuiltins.input(interpreter);
            case "int":
                return PythonInteger.getIntType();
            case "isinstance":
                return ((PythonLikeFunction) GlobalBuiltins::isinstance);
            case "issubclass":
                return ((PythonLikeFunction) GlobalBuiltins::issubclass);
            case "iter":
                return UnaryDunderBuiltin.ITERATOR; // TODO: Iterator with sentinel value
            case "len":
                return UnaryDunderBuiltin.LENGTH;
            case "list":
                return PythonLikeList.LIST_TYPE;
            case "locals":
                return ((PythonLikeFunction) GlobalBuiltins::locals);
            case "map":
                return ((PythonLikeFunction) GlobalBuiltins::map);
            case "min":
                return ((PythonLikeFunction) GlobalBuiltins::min);
            case "max":
                return ((PythonLikeFunction) GlobalBuiltins::max);
            case "next":
                return UnaryDunderBuiltin.NEXT;
            case "object":
                return PythonLikeType.getBaseType();
            case "oct":
                return ((PythonLikeFunction) GlobalBuiltins::oct);
            case "ord":
                return ((PythonLikeFunction) GlobalBuiltins::ord);
            case "pow":
                return ((PythonLikeFunction) GlobalBuiltins::pow);
            case "print":
                return GlobalBuiltins.print(interpreter);
            case "range":
                return PythonRange.RANGE_TYPE;
            case "repr":
                return UnaryDunderBuiltin.REPRESENTATION;
            case "set":
                return PythonLikeSet.SET_TYPE;
            case "setattr":
                return ((PythonLikeFunction) GlobalBuiltins::setattr);
            case "str":
                return PythonString.STRING_TYPE;
            case "tuple":
                return PythonLikeTuple.TUPLE_TYPE;
            case "type":
                return PythonLikeType.getTypeType();
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

    public static PythonString ascii(List<PythonLikeObject> positionalArgs, Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            object = positionalArgs.get(0);
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("object"))) {
            object = keywordArgs.get(PythonString.valueOf("object"));
        } else {
            throw new ValueError("ascii expects 1 argument, got " + positionalArgs.size());
        }

        PythonString reprString = (PythonString) UnaryDunderBuiltin.REPRESENTATION.invoke(object);
        String asciiString = reprString.value.codePoints().flatMap((character) -> {
            if (character < 128) {
                return IntStream.of(character);
            } else {
                // \Uxxxxxxxx
                IntStream.Builder builder = IntStream.builder().add('\\').add('U');
                String hexString = Integer.toHexString(character);
                for (int i = 8; i > hexString.length(); i--) {
                    builder.add('0');
                }
                hexString.codePoints().forEach(builder);
                return builder.build();
            }
        }).collect(StringBuilder::new,
                StringBuilder::appendCodePoint,
                StringBuilder::append).toString();

        return PythonString.valueOf(asciiString);
    }

    public static PythonString bin(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            object = positionalArgs.get(0);
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("x"))) {
            object = keywordArgs.get(PythonString.valueOf("x"));
        } else {
            throw new ValueError("bin expects 1 argument, got " + positionalArgs.size());
        }

        PythonInteger integer;

        if (object instanceof PythonInteger) {
            integer = (PythonInteger) object;
        } else {
            integer = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(object);
        }

        String binaryString = integer.value.toString(2);

        if (binaryString.startsWith("-")) {
            return PythonString.valueOf("-0b" + binaryString.substring(1));
        } else {
            return PythonString.valueOf("0b" + binaryString);
        }
    }

    public static PythonBoolean callable(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            object = positionalArgs.get(0);
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("object"))) {
            object = keywordArgs.get(PythonString.valueOf("object"));
        } else {
            throw new ValueError("callable expects 1 argument, got " + positionalArgs.size());
        }

        return PythonBoolean.valueOf(object instanceof PythonLikeFunction);
    }

    public static PythonString chr(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            object = positionalArgs.get(0);
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("i"))) {
            object = keywordArgs.get(PythonString.valueOf("i"));
        } else {
            throw new ValueError("chr expects 1 argument, got " + positionalArgs.size());
        }

        PythonInteger integer = (PythonInteger) object;

        if (integer.value.compareTo(BigInteger.valueOf(0x10FFFF)) > 0 || integer.value.compareTo(BigInteger.ZERO) < 0) {
            throw new ValueError("Integer (" + integer + ") outside valid range for chr (0 through 1,114,111)");
        }

        return PythonString.valueOf(Character.toString(integer.value.intValueExact()));
    }

    public static PythonNone delattr(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;
        PythonString name;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 2) {
            object = positionalArgs.get(0);
            name = (PythonString) positionalArgs.get(1);
        } else if (positionalArgs.size() == 1 && keywordArgs.containsKey(PythonString.valueOf("name"))) {
            object = positionalArgs.get(0);
            name = (PythonString) keywordArgs.get(PythonString.valueOf("name"));
        } else if (positionalArgs.size() == 0 && keywordArgs.containsKey(PythonString.valueOf("object"))
                && keywordArgs.containsKey(PythonString.valueOf("name"))) {
            object = keywordArgs.get(PythonString.valueOf("object"));
            name = (PythonString) keywordArgs.get(PythonString.valueOf("name"));
        } else {
            throw new ValueError("delattr expects 2 argument, got " + positionalArgs.size());
        }

        object.__deleteAttribute(name.value);

        return PythonNone.INSTANCE;
    }

    public static PythonLikeObject enumerate(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject iterable;
        PythonLikeObject start = PythonInteger.valueOf(0);

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 2) {
            iterable = positionalArgs.get(0);
            start = positionalArgs.get(1);
        } else if (positionalArgs.size() == 1) {
            iterable = positionalArgs.get(0);
            if (keywordArgs.containsKey(PythonString.valueOf("start"))) {
                start = keywordArgs.get(PythonString.valueOf("start"));
            }
        } else if (positionalArgs.size() == 0 && keywordArgs.containsKey(PythonString.valueOf("iterable"))) {
            iterable = keywordArgs.get(PythonString.valueOf("iterable"));
            if (keywordArgs.containsKey(PythonString.valueOf("start"))) {
                start = keywordArgs.get(PythonString.valueOf("start"));
            }
        } else {
            throw new ValueError("enumerate expects 1 or 2 argument, got " + positionalArgs.size());
        }

        final PythonLikeObject iterator = UnaryDunderBuiltin.ITERATOR.invoke(iterable);
        final AtomicReference<PythonLikeObject> currentValue = new AtomicReference(null);
        final AtomicReference<PythonLikeObject> currentIndex = new AtomicReference(start);
        final AtomicBoolean shouldCallNext = new AtomicBoolean(true);

        return new PythonIterator(new Iterator<PythonLikeObject>() {
            @Override
            public boolean hasNext() {
                if (shouldCallNext.get()) {
                    try {
                        currentValue.set(UnaryDunderBuiltin.NEXT.invoke(iterator));
                    } catch (StopIteration e) {
                        currentValue.set(null);
                        shouldCallNext.set(false);
                        return false;
                    }
                    shouldCallNext.set(false);
                    return true;
                } else {
                    // we already called next
                    return currentValue.get() != null;
                }
            }

            @Override
            public PythonLikeObject next() {
                PythonLikeObject value;
                if (currentValue.get() != null) {
                    shouldCallNext.set(true);
                    value = currentValue.get();
                    currentValue.set(null);
                } else {
                    value = UnaryDunderBuiltin.NEXT.invoke(iterator);
                    shouldCallNext.set(true);
                }
                PythonLikeObject index = currentIndex.get();
                currentIndex.set(BinaryDunderBuiltin.ADD.invoke(index, PythonInteger.valueOf(1)));
                return PythonLikeTuple.fromList(List.of(index, value));
            }
        });
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

    public static PythonLikeObject format(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject toFormat;
        PythonLikeObject formatSpec = PythonString.valueOf("");

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 2 && keywordArgs.isEmpty()) {
            toFormat = positionalArgs.get(0);
            formatSpec = positionalArgs.get(1);
        } else if (positionalArgs.size() == 1) {
            toFormat = positionalArgs.get(0);
            if (keywordArgs.containsKey(PythonString.valueOf("format_spec"))) {
                formatSpec = keywordArgs.get(PythonString.valueOf("format_spec"));
            }
        } else if (positionalArgs.size() == 0 && keywordArgs.containsKey(PythonString.valueOf("value"))) {
            toFormat = keywordArgs.get(PythonString.valueOf("value"));
            if (keywordArgs.containsKey(PythonString.valueOf("format_spec"))) {
                formatSpec = keywordArgs.get(PythonString.valueOf("format_spec"));
            }
        } else {
            throw new ValueError("format expects 1 or 2 arguments, got " + positionalArgs.size());
        }

        return ObjectBuiltinOperations.format(toFormat, formatSpec);
    }

    public static PythonLikeObject getattr(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;
        PythonString name;
        PythonLikeObject defaultValue = null;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 3) {
            object = positionalArgs.get(0);
            name = (PythonString) positionalArgs.get(1);
            defaultValue = positionalArgs.get(2);
        } else if (positionalArgs.size() == 2) {
            object = positionalArgs.get(0);
            name = (PythonString) positionalArgs.get(1);
            defaultValue = keywordArgs.get(PythonString.valueOf("default"));
        } else if (positionalArgs.size() == 1 && keywordArgs.containsKey(PythonString.valueOf("name"))) {
            object = positionalArgs.get(0);
            name = (PythonString) keywordArgs.get(PythonString.valueOf("name"));
            defaultValue = keywordArgs.get(PythonString.valueOf("default"));
        } else if (positionalArgs.size() == 0 && keywordArgs.containsKey(PythonString.valueOf("object"))
                && keywordArgs.containsKey(PythonString.valueOf("name"))) {
            object = keywordArgs.get(PythonString.valueOf("object"));
            name = (PythonString) keywordArgs.get(PythonString.valueOf("name"));
            defaultValue = keywordArgs.get(PythonString.valueOf("default"));
        } else {
            throw new ValueError("getattr expects 2 or 3 arguments, got " + positionalArgs.size());
        }

        PythonLikeFunction getAttribute = (PythonLikeFunction) object.__getType().__getAttributeOrError("__getattribute__");

        try {
            return getAttribute.__call__(List.of(object, name), null);
        } catch (AttributeError attributeError) {
            if (defaultValue != null) {
                return defaultValue;
            } else {
                throw attributeError;
            }
        }
    }

    public static PythonLikeDict globals(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (!positionalArgs.isEmpty() && keywordArgs.isEmpty()) {
            throw new ValueError("globals expects 0 arguments, got " + positionalArgs.size());
        }
        Class<?> callerClass = stackWalker.getCallerClass();

        try {
            Map globalsMap =
                    (Map) callerClass.getField(PythonBytecodeToJavaBytecodeTranslator.GLOBALS_MAP_STATIC_FIELD_NAME).get(null);
            return PythonLikeDict.mirror(globalsMap);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Caller is not a generated class");
        }
    }

    public static PythonBoolean hasattr(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        try {
            getattr(positionalArgs, keywordArgs);
            return PythonBoolean.TRUE;
        } catch (AttributeError error) {
            return PythonBoolean.FALSE;
        }
    }

    public static PythonString hex(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            object = positionalArgs.get(0);
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("x"))) {
            object = keywordArgs.get(PythonString.valueOf("x"));
        } else {
            throw new ValueError("hex expects 1 argument, got " + positionalArgs.size());
        }

        PythonInteger integer;

        if (object instanceof PythonInteger) {
            integer = (PythonInteger) object;
        } else {
            integer = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(object);
        }

        String hexString = integer.value.toString(16);

        if (hexString.startsWith("-")) {
            return PythonString.valueOf("-0x" + hexString.substring(1));
        } else {
            return PythonString.valueOf("0x" + hexString);
        }
    }

    public static PythonInteger id(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            object = positionalArgs.get(0);
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("object"))) {
            object = keywordArgs.get(PythonString.valueOf("object"));
        } else {
            throw new ValueError("id expects 1 argument, got " + positionalArgs.size());
        }

        if (object instanceof CPythonBackedPythonLikeObject) {
            CPythonBackedPythonLikeObject cPythonBackedPythonLikeObject = (CPythonBackedPythonLikeObject) object;
            if (cPythonBackedPythonLikeObject.$cpythonId != null) {
                return cPythonBackedPythonLikeObject.$cpythonId;
            }
        }

        return PythonInteger.valueOf(System.identityHashCode(object));
    }

    public static PythonLikeFunction input(PythonInterpreter interpreter) {
        return (positionalArguments, namedArguments) -> {
            PythonString prompt = null;
            if (positionalArguments.size() == 1) {
                prompt = (PythonString) positionalArguments.get(0);
            } else if (positionalArguments.size() == 0 && namedArguments.containsKey(PythonString.valueOf("prompt"))) {
                prompt = (PythonString) namedArguments.get(PythonString.valueOf("prompt"));
            } else {
                throw new ValueError("input expects 0 or 1 arguments, got " + positionalArguments.size());
            }

            if (prompt != null) {
                interpreter.write(prompt.value);
            }

            String line = interpreter.readLine();

            return PythonString.valueOf(line);
        };
    }

    public static PythonBoolean isinstance(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;
        PythonLikeObject classInfo;

        if (positionalArgs.size() == 2) {
            object = positionalArgs.get(0);
            classInfo = positionalArgs.get(1);
        } else if (positionalArgs.size() == 1 && keywordArgs.containsKey(PythonString.valueOf("classinfo"))) {
            object = positionalArgs.get(0);
            classInfo = keywordArgs.get(PythonString.valueOf("classinfo"));
        } else if (positionalArgs.size() == 0 && keywordArgs.containsKey(PythonString.valueOf("object"))
                && keywordArgs.containsKey(PythonString.valueOf("classinfo"))) {
            object = keywordArgs.get(PythonString.valueOf("object"));
            classInfo = keywordArgs.get(PythonString.valueOf("classinfo"));
        } else {
            throw new ValueError("isinstance expects 2 arguments, got " + positionalArgs.size());
        }

        if (classInfo instanceof PythonLikeType) {
            return PythonBoolean.valueOf(((PythonLikeType) classInfo).isInstance(object));
        } else if (classInfo instanceof List) {
            for (PythonLikeObject possibleType : (List<PythonLikeObject>) classInfo) {
                if (isinstance(List.of(object, possibleType), null).getBooleanValue()) {
                    return PythonBoolean.TRUE;
                }
            }
            return PythonBoolean.FALSE;
        } else {
            throw new ValueError("classInfo (" + classInfo + ") is not a tuple of types or a type"); // TODO: Use TypeError
        }
    }

    public static PythonBoolean issubclass(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeType type;
        PythonLikeObject classInfo;

        if (positionalArgs.size() == 2) {
            type = (PythonLikeType) positionalArgs.get(0);
            classInfo = positionalArgs.get(1);
        } else if (positionalArgs.size() == 1 && keywordArgs.containsKey(PythonString.valueOf("classinfo"))) {
            type = (PythonLikeType) positionalArgs.get(0);
            classInfo = keywordArgs.get(PythonString.valueOf("classinfo"));
        } else if (positionalArgs.size() == 0 && keywordArgs.containsKey(PythonString.valueOf("class"))
                && keywordArgs.containsKey(PythonString.valueOf("classinfo"))) {
            type = (PythonLikeType) keywordArgs.get(PythonString.valueOf("class"));
            classInfo = keywordArgs.get(PythonString.valueOf("classinfo"));
        } else {
            throw new ValueError("isinstance expects 2 arguments, got " + positionalArgs.size());
        }

        if (classInfo instanceof PythonLikeType) {
            return PythonBoolean.valueOf(type.isSubclassOf((PythonLikeType) classInfo));
        } else if (classInfo instanceof List) {
            for (PythonLikeObject possibleType : (List<PythonLikeObject>) classInfo) {
                if (issubclass(List.of(type, possibleType), null).getBooleanValue()) {
                    return PythonBoolean.TRUE;
                }
            }
            return PythonBoolean.FALSE;
        } else {
            throw new ValueError("classInfo (" + classInfo + ") is not a tuple of types or a type"); // TODO: Use TypeError
        }
    }

    public static PythonLikeDict locals(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        throw new ValueError("builtin locals is not supported when executed in Java bytecode");
    }

    public static PythonIterator map(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeFunction function;
        List<PythonLikeObject> iterableList = new ArrayList<>();

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() >= 2 && keywordArgs.isEmpty()) {
            function = (PythonLikeFunction) positionalArgs.get(0);
            iterableList = positionalArgs.subList(1, positionalArgs.size());
        } else if (positionalArgs.size() == 1 && keywordArgs.containsKey(PythonString.valueOf("iterable"))) {
            function = (PythonLikeFunction) positionalArgs.get(0);
            iterableList.add(keywordArgs.get(PythonString.valueOf("iterable")));
        } else if (positionalArgs.size() == 0
                && keywordArgs.containsKey(PythonString.valueOf("function"))
                && keywordArgs.containsKey(PythonString.valueOf("iterable"))) {
            function = (PythonLikeFunction) keywordArgs.get(PythonString.valueOf("function"));
            iterableList.add(keywordArgs.get(PythonString.valueOf("iterable")));
        } else {
            throw new ValueError("map expects at least 2 argument, got " + positionalArgs.size());
        }

        final List<Iterator> iteratorList = new ArrayList<>(iterableList.size());

        for (PythonLikeObject iterable : iterableList) {
            Iterator iterator;
            if (iterable instanceof Collection) {
                iterator = ((Collection) iterable).iterator();
            } else if (iterable instanceof Iterator) {
                iterator = (Iterator) iterable;
            } else {
                iterator = (Iterator) UnaryDunderBuiltin.ITERATOR.invoke(iterable);
            }
            iteratorList.add(iterator);
        }

        Iterator<List<PythonLikeObject>> iteratorIterator = new Iterator<List<PythonLikeObject>>() {
            @Override
            public boolean hasNext() {
                return iteratorList.stream().allMatch(Iterator::hasNext);
            }

            @Override
            public List<PythonLikeObject> next() {
                List<PythonLikeObject> out = new ArrayList<>(iteratorList.size());
                for (Iterator iterator : iteratorList) {
                    out.add((PythonLikeObject) iterator.next());
                }
                return out;
            }
        };

        return new PythonIterator(StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iteratorIterator, Spliterator.ORDERED),
                false)
                .map(element -> function.__call__(element, null))
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

    public static PythonString oct(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            object = positionalArgs.get(0);
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("x"))) {
            object = keywordArgs.get(PythonString.valueOf("x"));
        } else {
            throw new ValueError("oct expects 1 argument, got " + positionalArgs.size());
        }

        PythonInteger integer;

        if (object instanceof PythonInteger) {
            integer = (PythonInteger) object;
        } else {
            integer = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(object);
        }

        String octString = integer.value.toString(8);

        if (octString.startsWith("-")) {
            return PythonString.valueOf("-0o" + octString.substring(1));
        } else {
            return PythonString.valueOf("0o" + octString);
        }
    }

    public static PythonInteger ord(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonString character;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 1 && keywordArgs.isEmpty()) {
            character = (PythonString) positionalArgs.get(0);
        } else if (positionalArgs.isEmpty() && keywordArgs.size() == 1
                && keywordArgs.containsKey(PythonString.valueOf("c"))) {
            character = (PythonString) keywordArgs.get(PythonString.valueOf("c"));
        } else {
            throw new ValueError("ord expects 1 argument, got " + positionalArgs.size());
        }

        if (character.length() != 1) {
            throw new ValueError("String \"" + character + "\" does not represent a single character");
        }

        return PythonInteger.valueOf(character.value.charAt(0));
    }

    public static PythonLikeObject pow(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject base;
        PythonLikeObject exp;
        PythonLikeObject mod = null;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 3 && keywordArgs.isEmpty()) {
            base = positionalArgs.get(0);
            exp = positionalArgs.get(1);
            mod = positionalArgs.get(2);
        } else if (positionalArgs.size() == 2) {
            base = positionalArgs.get(0);
            exp = positionalArgs.get(1);
            mod = keywordArgs.get(PythonString.valueOf("mod"));
        } else if (positionalArgs.size() == 1 && keywordArgs.containsKey(PythonString.valueOf("exp"))) {
            base = positionalArgs.get(0);
            exp = keywordArgs.get(PythonString.valueOf("exp"));
            mod = keywordArgs.get(PythonString.valueOf("mod"));
        } else if (positionalArgs.isEmpty() && keywordArgs.containsKey(PythonString.valueOf("base"))
                && keywordArgs.containsKey(PythonString.valueOf("exp"))) {
            base = keywordArgs.get(PythonString.valueOf("base"));
            exp = keywordArgs.get(PythonString.valueOf("exp"));
            mod = keywordArgs.get(PythonString.valueOf("mod"));
        } else {
            throw new ValueError("pow expects 2 or 3 arguments, got " + positionalArgs.size());
        }

        if (mod == null) {
            return BinaryDunderBuiltin.POWER.invoke(base, exp);
        } else {
            return TernaryDunderBuiltin.POWER.invoke(base, exp, mod);
        }
    }

    public static PythonLikeFunction print(PythonInterpreter interpreter) {

        return (positionalArgs, keywordArgs) -> {
            List<PythonLikeObject> objects = positionalArgs;
            String sep;
            if (!keywordArgs.containsKey(PythonString.valueOf("sep"))
                    || keywordArgs.get(PythonString.valueOf("sep")) == PythonNone.INSTANCE) {
                sep = " ";
            } else {
                sep = ((PythonString) keywordArgs.get(PythonString.valueOf("sep"))).value;
            }
            String end;
            if (!keywordArgs.containsKey(PythonString.valueOf("end"))
                    || keywordArgs.get(PythonString.valueOf("end")) == PythonNone.INSTANCE) {
                end = "\n";
            } else {
                end = ((PythonString) keywordArgs.get(PythonString.valueOf("end"))).value;
            }
            // TODO: support file keyword arg

            boolean flush;
            if (!keywordArgs.containsKey(PythonString.valueOf("flush"))
                    || keywordArgs.get(PythonString.valueOf("flush")) == PythonNone.INSTANCE) {
                flush = false;
            } else {
                flush = ((PythonBoolean) keywordArgs.get(PythonString.valueOf("flush"))).getBooleanValue();
            }

            for (int i = 0; i < objects.size() - 1; i++) {
                interpreter.write(UnaryDunderBuiltin.STR.invoke(objects.get(i)).toString());
                interpreter.write(sep);
            }
            if (!objects.isEmpty()) {
                interpreter.write(UnaryDunderBuiltin.STR.invoke(objects.get(objects.size() - 1)).toString());
            }
            interpreter.write(end);

            if (flush) {
                System.out.flush();
            }

            return PythonNone.INSTANCE;
        };
    };

    public static PythonLikeObject setattr(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> keywordArgs) {
        PythonLikeObject object;
        PythonString name;
        PythonLikeObject value;

        keywordArgs = (keywordArgs != null) ? keywordArgs : Map.of();

        if (positionalArgs.size() == 3) {
            object = positionalArgs.get(0);
            name = (PythonString) positionalArgs.get(1);
            value = positionalArgs.get(2);
        } else if (positionalArgs.size() == 2 && keywordArgs.containsKey(PythonString.valueOf("value"))) {
            object = positionalArgs.get(0);
            name = (PythonString) positionalArgs.get(1);
            value = keywordArgs.get(PythonString.valueOf("value"));
        } else if (positionalArgs.size() == 1 && keywordArgs.containsKey(PythonString.valueOf("name")) &&
                keywordArgs.containsKey(PythonString.valueOf("value"))) {
            object = positionalArgs.get(0);
            name = (PythonString) keywordArgs.get(PythonString.valueOf("name"));
            value = keywordArgs.get(PythonString.valueOf("value"));
        } else if (positionalArgs.size() == 0 && keywordArgs.containsKey(PythonString.valueOf("object")) &&
                keywordArgs.containsKey(PythonString.valueOf("name")) &&
                keywordArgs.containsKey(PythonString.valueOf("value"))) {
            object = keywordArgs.get(PythonString.valueOf("object"));
            name = (PythonString) keywordArgs.get(PythonString.valueOf("name"));
            value = keywordArgs.get(PythonString.valueOf("value"));
        } else {
            throw new ValueError("setattr expects 2 or 3 arguments, got " + positionalArgs.size());
        }

        return TernaryDunderBuiltin.SETATTR.invoke(object, name, value);
    }
}
