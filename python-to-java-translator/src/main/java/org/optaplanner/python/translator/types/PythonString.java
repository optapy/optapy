package org.optaplanner.python.translator.types;

import static org.optaplanner.python.translator.types.BuiltinTypes.STRING_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.BinaryDunderBuiltin;
import org.optaplanner.python.translator.builtins.GlobalBuiltins;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.collections.PythonIterator;
import org.optaplanner.python.translator.types.collections.PythonLikeDict;
import org.optaplanner.python.translator.types.collections.PythonLikeList;
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;
import org.optaplanner.python.translator.types.errors.TypeError;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.errors.lookup.IndexError;
import org.optaplanner.python.translator.types.errors.lookup.LookupError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonFloat;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class PythonString extends AbstractPythonLikeObject implements PythonLikeComparable<PythonString> {
    public final String value;

    public final static PythonString EMPTY = new PythonString("");
    private final static Pattern FORMAT_REGEX = Pattern.compile("\\{(.*?)\\}");

    /**
     * Pattern that matches conversion specifiers for the "%" operator. See
     * <a href="https://docs.python.org/3/library/stdtypes.html#printf-style-string-formatting">
     * Python printf-style String Formatting documentation</a> for details.
     */
    private final static Pattern PRINTF_FORMAT_REGEX = Pattern.compile("%(?:(?<key>\\([^()]+\\))?" +
                                                                             "(?<flags>[#0\\-+ ]*)?" +
                                                                             "(?<minWidth>\\*|\\d+)?" +
                                                                             "(?<precision>\\.(?:\\*|\\d+))?" +
                                                                             "[hlL]?" + // ignored length modifier
                                                                             "(?<type>[diouxXeEfFgGcrsa%])|.*)"
            );

    private enum PrintfConversionType {
        SIGNED_INTEGER_DECIMAL("d", "i", "u"),
        SIGNED_INTEGER_OCTAL("o"),
        SIGNED_HEXADECIMAL_LOWERCASE("x"),
        SIGNED_HEXADECIMAL_UPPERCASE("X"),
        FLOATING_POINT_EXPONENTIAL_LOWERCASE("e"),
        FLOATING_POINT_EXPONENTIAL_UPPERCASE("E"),
        FLOATING_POINT_DECIMAL("f", "F"),
        FLOATING_POINT_DECIMAL_OR_EXPONENTIAL_LOWERCASE("g"),
        FLOATING_POINT_DECIMAL_OR_EXPONENTIAL_UPPERCASE("G"),
        SINGLE_CHARACTER("c"),
        REPR_STRING("r"),
        STR_STRING("s"),
        ASCII_STRING("a"),
        LITERAL_PERCENT("%");

        final String[] matchedCharacters;
        PrintfConversionType(String... matchedCharacters) {
            this.matchedCharacters = matchedCharacters;
        }

        public static PrintfConversionType getConversionType(Matcher matcher) {
            String conversion = matcher.group("type");

            if (conversion == null) {
                throw new ValueError("Invalid specifier at position " + matcher.start() + " in string ");
            }

            for (PrintfConversionType conversionType : PrintfConversionType.values()) {
                for (String matchedCharacter : conversionType.matchedCharacters) {
                    if (matchedCharacter.equals(conversion)) {
                        return conversionType;
                    }
                }
            }
            throw new IllegalStateException("Conversion (" + conversion + ") does not match any defined conversions");
        }
    }

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonString::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        PythonLikeComparable.setup(STRING_TYPE);
        STRING_TYPE.setConstructor((positionalArguments, namedArguments) -> {
            if (positionalArguments.size() == 1) {
                return UnaryDunderBuiltin.STR.invoke(positionalArguments.get(0));
            } else if (positionalArguments.size() == 3) {
                // TODO Support byte array strings
                throw new ValueError("three argument str not supported");
            } else {
                throw new ValueError("str expects 1 or 3 arguments, got " + positionalArguments.size());
            }
        });

        // Unary
        STRING_TYPE.addMethod(PythonUnaryOperator.REPRESENTATION, PythonString.class.getMethod("repr"));
        STRING_TYPE.addMethod(PythonUnaryOperator.AS_STRING, PythonString.class.getMethod("asString"));
        STRING_TYPE.addMethod(PythonUnaryOperator.ITERATOR, PythonString.class.getMethod("getIterator"));
        STRING_TYPE.addMethod(PythonUnaryOperator.LENGTH, PythonString.class.getMethod("getLength"));

        // Binary
        STRING_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonString.class.getMethod("getCharAt", PythonInteger.class));
        STRING_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonString.class.getMethod("getSubstring", PythonSlice.class));
        STRING_TYPE.addMethod(PythonBinaryOperators.CONTAINS,
                PythonString.class.getMethod("containsSubstring", PythonString.class));
        STRING_TYPE.addMethod(PythonBinaryOperators.ADD, PythonString.class.getMethod("concat", PythonString.class));
        STRING_TYPE.addMethod(PythonBinaryOperators.MULTIPLY, PythonString.class.getMethod("repeat", PythonInteger.class));
        STRING_TYPE.addMethod(PythonBinaryOperators.MODULO, PythonString.class.getMethod("interpolate", PythonLikeObject.class));
        STRING_TYPE.addMethod(PythonBinaryOperators.MODULO, PythonString.class.getMethod("interpolate", PythonLikeTuple.class));
        STRING_TYPE.addMethod(PythonBinaryOperators.MODULO, PythonString.class.getMethod("interpolate", PythonLikeDict.class));

        // Other
        STRING_TYPE.addMethod("capitalize", PythonString.class.getMethod("capitalize"));
        STRING_TYPE.addMethod("casefold", PythonString.class.getMethod("casefold"));

        STRING_TYPE.addMethod("center", PythonString.class.getMethod("center", PythonInteger.class));
        STRING_TYPE.addMethod("center", PythonString.class.getMethod("center", PythonInteger.class, PythonString.class));

        STRING_TYPE.addMethod("count", PythonString.class.getMethod("count", PythonString.class));
        STRING_TYPE.addMethod("count", PythonString.class.getMethod("count", PythonString.class, PythonInteger.class));
        STRING_TYPE.addMethod("count",
                PythonString.class.getMethod("count", PythonString.class, PythonInteger.class, PythonInteger.class));

        // TODO: encode

        STRING_TYPE.addMethod("endswith", PythonString.class.getMethod("endsWith", PythonString.class));
        STRING_TYPE.addMethod("endswith", PythonString.class.getMethod("endsWith", PythonLikeTuple.class));
        STRING_TYPE.addMethod("endswith", PythonString.class.getMethod("endsWith", PythonString.class, PythonInteger.class));
        STRING_TYPE.addMethod("endswith", PythonString.class.getMethod("endsWith", PythonLikeTuple.class, PythonInteger.class));
        STRING_TYPE.addMethod("endswith",
                PythonString.class.getMethod("endsWith", PythonString.class, PythonInteger.class, PythonInteger.class));
        STRING_TYPE.addMethod("endswith",
                PythonString.class.getMethod("endsWith", PythonLikeTuple.class, PythonInteger.class, PythonInteger.class));

        STRING_TYPE.addMethod("expandtabs", PythonString.class.getMethod("expandTabs"));
        STRING_TYPE.addMethod("expandtabs", PythonString.class.getMethod("expandTabs", PythonInteger.class));

        STRING_TYPE.addMethod("find", PythonString.class.getMethod("findSubstringIndex", PythonString.class));
        STRING_TYPE.addMethod("find",
                PythonString.class.getMethod("findSubstringIndex", PythonString.class, PythonInteger.class));
        STRING_TYPE.addMethod("find", PythonString.class.getMethod("findSubstringIndex", PythonString.class,
                PythonInteger.class, PythonInteger.class));

        // TODO: format

        STRING_TYPE.addMethod("format_map", PythonString.class.getMethod("formatMap", PythonLikeDict.class));

        STRING_TYPE.addMethod("index", PythonString.class.getMethod("findSubstringIndexOrError", PythonString.class));
        STRING_TYPE.addMethod("index",
                PythonString.class.getMethod("findSubstringIndexOrError", PythonString.class, PythonInteger.class));
        STRING_TYPE.addMethod("index", PythonString.class.getMethod("findSubstringIndexOrError", PythonString.class,
                PythonInteger.class, PythonInteger.class));

        STRING_TYPE.addMethod("isalnum", PythonString.class.getMethod("isAlphaNumeric"));
        STRING_TYPE.addMethod("isalpha", PythonString.class.getMethod("isAlpha"));
        STRING_TYPE.addMethod("isascii", PythonString.class.getMethod("isAscii"));
        STRING_TYPE.addMethod("isdecimal", PythonString.class.getMethod("isDecimal"));
        STRING_TYPE.addMethod("isdigit", PythonString.class.getMethod("isDigit"));
        STRING_TYPE.addMethod("isidentifier", PythonString.class.getMethod("isIdentifier"));
        STRING_TYPE.addMethod("islower", PythonString.class.getMethod("isLower"));
        STRING_TYPE.addMethod("isnumeric", PythonString.class.getMethod("isNumeric"));
        STRING_TYPE.addMethod("isprintable", PythonString.class.getMethod("isPrintable"));
        STRING_TYPE.addMethod("isspace", PythonString.class.getMethod("isSpace"));
        STRING_TYPE.addMethod("istitle", PythonString.class.getMethod("isTitle"));
        STRING_TYPE.addMethod("isupper", PythonString.class.getMethod("isUpper"));

        STRING_TYPE.addMethod("join", PythonString.class.getMethod("join", PythonLikeObject.class));

        STRING_TYPE.addMethod("ljust", PythonString.class.getMethod("leftJustify", PythonInteger.class));
        STRING_TYPE.addMethod("ljust", PythonString.class.getMethod("leftJustify", PythonInteger.class, PythonString.class));

        STRING_TYPE.addMethod("lower", PythonString.class.getMethod("lower"));

        STRING_TYPE.addMethod("lstrip", PythonString.class.getMethod("leftStrip"));
        STRING_TYPE.addMethod("lstrip", PythonString.class.getMethod("leftStrip", PythonNone.class));
        STRING_TYPE.addMethod("lstrip", PythonString.class.getMethod("leftStrip", PythonString.class));

        // TODO: maketrans

        STRING_TYPE.addMethod("partition", PythonString.class.getMethod("partition", PythonString.class));

        STRING_TYPE.addMethod("removeprefix", PythonString.class.getMethod("removePrefix", PythonString.class));

        STRING_TYPE.addMethod("removesuffix", PythonString.class.getMethod("removeSuffix", PythonString.class));

        STRING_TYPE.addMethod("replace", PythonString.class.getMethod("replaceAll", PythonString.class, PythonString.class));
        STRING_TYPE.addMethod("replace",
                PythonString.class.getMethod("replaceUpToCount", PythonString.class, PythonString.class, PythonInteger.class));

        STRING_TYPE.addMethod("rfind", PythonString.class.getMethod("rightFindSubstringIndex", PythonString.class));
        STRING_TYPE.addMethod("rfind",
                PythonString.class.getMethod("rightFindSubstringIndex", PythonString.class, PythonInteger.class));
        STRING_TYPE.addMethod("rfind", PythonString.class.getMethod("rightFindSubstringIndex", PythonString.class,
                PythonInteger.class, PythonInteger.class));

        STRING_TYPE.addMethod("rindex", PythonString.class.getMethod("rightFindSubstringIndexOrError", PythonString.class));
        STRING_TYPE.addMethod("rindex",
                PythonString.class.getMethod("rightFindSubstringIndexOrError", PythonString.class, PythonInteger.class));
        STRING_TYPE.addMethod("rindex", PythonString.class.getMethod("rightFindSubstringIndexOrError", PythonString.class,
                PythonInteger.class, PythonInteger.class));

        STRING_TYPE.addMethod("rjust", PythonString.class.getMethod("rightJustify", PythonInteger.class));
        STRING_TYPE.addMethod("rjust", PythonString.class.getMethod("rightJustify", PythonInteger.class, PythonString.class));

        STRING_TYPE.addMethod("rpartition", PythonString.class.getMethod("rightPartition", PythonString.class));

        STRING_TYPE.addMethod("rsplit", PythonString.class.getMethod("rightSplit"));
        STRING_TYPE.addMethod("rsplit", PythonString.class.getMethod("rightSplit", PythonNone.class));
        STRING_TYPE.addMethod("rsplit", PythonString.class.getMethod("rightSplit", PythonString.class));
        STRING_TYPE.addMethod("rsplit", PythonString.class.getMethod("rightSplit", PythonNone.class, PythonInteger.class));
        STRING_TYPE.addMethod("rsplit", PythonString.class.getMethod("rightSplit", PythonString.class, PythonInteger.class));

        STRING_TYPE.addMethod("rstrip", PythonString.class.getMethod("rightStrip"));
        STRING_TYPE.addMethod("rstrip", PythonString.class.getMethod("rightStrip", PythonNone.class));
        STRING_TYPE.addMethod("rstrip", PythonString.class.getMethod("rightStrip", PythonString.class));

        STRING_TYPE.addMethod("split", PythonString.class.getMethod("split"));
        STRING_TYPE.addMethod("split", PythonString.class.getMethod("split", PythonNone.class));
        STRING_TYPE.addMethod("split", PythonString.class.getMethod("split", PythonString.class));
        STRING_TYPE.addMethod("split", PythonString.class.getMethod("split", PythonNone.class, PythonInteger.class));
        STRING_TYPE.addMethod("split", PythonString.class.getMethod("split", PythonString.class, PythonInteger.class));

        STRING_TYPE.addMethod("splitlines", PythonString.class.getMethod("splitLines"));
        STRING_TYPE.addMethod("splitlines", PythonString.class.getMethod("splitLines", PythonBoolean.class));

        STRING_TYPE.addMethod("startswith", PythonString.class.getMethod("startsWith", PythonString.class));
        STRING_TYPE.addMethod("startswith", PythonString.class.getMethod("startsWith", PythonLikeTuple.class));
        STRING_TYPE.addMethod("startswith",
                PythonString.class.getMethod("startsWith", PythonString.class, PythonInteger.class));
        STRING_TYPE.addMethod("startswith",
                PythonString.class.getMethod("startsWith", PythonLikeTuple.class, PythonInteger.class));
        STRING_TYPE.addMethod("startswith",
                PythonString.class.getMethod("startsWith", PythonString.class, PythonInteger.class, PythonInteger.class));
        STRING_TYPE.addMethod("startswith",
                PythonString.class.getMethod("startsWith", PythonLikeTuple.class, PythonInteger.class, PythonInteger.class));

        STRING_TYPE.addMethod("strip", PythonString.class.getMethod("strip"));
        STRING_TYPE.addMethod("strip", PythonString.class.getMethod("strip", PythonNone.class));
        STRING_TYPE.addMethod("strip", PythonString.class.getMethod("strip", PythonString.class));

        STRING_TYPE.addMethod("swapcase", PythonString.class.getMethod("swapCase"));

        STRING_TYPE.addMethod("title", PythonString.class.getMethod("title"));

        STRING_TYPE.addMethod("translate", PythonString.class.getMethod("translate", PythonLikeObject.class));

        STRING_TYPE.addMethod("upper", PythonString.class.getMethod("upper"));

        STRING_TYPE.addMethod("zfill", PythonString.class.getMethod("zfill", PythonInteger.class));

        return STRING_TYPE;
    }

    public PythonString(String value) {
        super(STRING_TYPE);
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof String) {
            return value.equals(o);
        } else if (o instanceof PythonString) {
            return ((PythonString) o).value.equals(value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static PythonString valueOf(String value) {
        return new PythonString(value);
    }

    public String getValue() {
        if (value.equals("\0a")) {
            throw new IllegalStateException("OKAY: " + value);
        }
        return value;
    }

    public int length() {
        return value.length();
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(value.length());
    }

    public PythonString getCharAt(PythonInteger position) {
        int index = PythonSlice.asIntIndexForLength(position, value.length());

        if (index >= value.length()) {
            throw new IndexError("position " + position + " larger than string length " + value.length());
        } else if (index < 0) {
            throw new IndexError("position " + position + " is less than 0");
        }

        return new PythonString(Character.toString(value.charAt(index)));
    }

    public PythonString getSubstring(PythonSlice slice) {
        int length = value.length();
        int start = slice.getStartIndex(length);
        int stop = slice.getStopIndex(length);
        int step = slice.getStrideLength();

        if (step == 1) {
            if (stop <= start) {
                return PythonString.valueOf("");
            } else {
                return PythonString.valueOf(value.substring(start, stop));
            }
        } else {
            StringBuilder out = new StringBuilder();
            if (step > 0) {
                for (int i = start; i < stop; i += step) {
                    out.append(value.charAt(i));
                }
            } else {
                for (int i = start; i > stop; i += step) {
                    out.append(value.charAt(i));
                }
            }
            return PythonString.valueOf(out.toString());
        }
    }

    public PythonBoolean containsSubstring(PythonString substring) {
        return PythonBoolean.valueOf(value.contains(substring.value));
    }

    public PythonString concat(PythonString other) {
        if (value.isEmpty()) {
            return other;
        } else if (other.value.isEmpty()) {
            return this;
        } else {
            return PythonString.valueOf(value + other.value);
        }
    }

    public PythonString repeat(PythonInteger times) {
        int timesAsInt = times.value.intValueExact();

        if (timesAsInt <= 0) {
            return EMPTY;
        }

        if (timesAsInt == 1) {
            return this;
        }

        return PythonString.valueOf(value.repeat(timesAsInt));
    }

    public PythonIterator getIterator() {
        return new PythonIterator(value.chars().mapToObj(charVal -> new PythonString(Character.toString(charVal)))
                .iterator());
    }

    public PythonString capitalize() {
        if (value.isEmpty()) {
            return this;
        }
        return PythonString.valueOf(Character.toTitleCase(value.charAt(0)) + value.substring(1).toLowerCase());
    }

    public PythonString title() {
        if (value.isEmpty()) {
            return this;
        }

        int length = value.length();
        boolean previousIsWordBoundary = true;

        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            if (previousIsWordBoundary) {
                out.append(Character.toTitleCase(character));
            } else {
                out.append(Character.toLowerCase(character));
            }

            previousIsWordBoundary = !Character.isAlphabetic(character);
        }

        return PythonString.valueOf(out.toString());
    }

    public PythonString casefold() {
        // This will work for the majority of cases, but fail for some cases
        return PythonString.valueOf(value.toUpperCase().toLowerCase());
    }

    public PythonString swapCase() {
        return PythonString.valueOf(value.codePoints()
                .map(CharacterCase::swapCase)
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint, StringBuilder::append)
                .toString());
    }

    public PythonString lower() {
        return PythonString.valueOf(value.toLowerCase());
    }

    public PythonString upper() {
        return PythonString.valueOf(value.toUpperCase());
    }

    public PythonString center(PythonInteger width) {
        return center(width, PythonString.valueOf(" "));
    }

    public PythonString center(PythonInteger width, PythonString fillChar) {
        int widthAsInt = width.value.intValueExact();
        if (widthAsInt <= value.length()) {
            return this;
        }
        int extraWidth = widthAsInt - value.length();
        int rightPadding = extraWidth / 2;
        // left padding get extra character if extraWidth is odd
        int leftPadding = rightPadding + (extraWidth & 1); // x & 1 == x % 2

        if (fillChar.value.length() != 1) {
            throw new TypeError("The fill character must be exactly one character long");
        }

        String fillCharAsString = fillChar.value;

        return PythonString.valueOf(fillCharAsString.repeat(leftPadding) +
                value +
                fillCharAsString.repeat(rightPadding));
    }

    public PythonString rightJustify(PythonInteger width) {
        return rightJustify(width, PythonString.valueOf(" "));
    }

    public PythonString rightJustify(PythonInteger width, PythonString fillChar) {
        int widthAsInt = width.value.intValueExact();
        if (widthAsInt <= value.length()) {
            return this;
        }
        int leftPadding = widthAsInt - value.length();

        if (fillChar.value.length() != 1) {
            throw new TypeError("The fill character must be exactly one character long");
        }

        return PythonString.valueOf(fillChar.value.repeat(leftPadding) + value);
    }

    public PythonString leftJustify(PythonInteger width) {
        return leftJustify(width, PythonString.valueOf(" "));
    }

    public PythonString leftJustify(PythonInteger width, PythonString fillChar) {
        int widthAsInt = width.value.intValueExact();
        if (widthAsInt <= value.length()) {
            return this;
        }
        int rightPadding = widthAsInt - value.length();

        if (fillChar.value.length() != 1) {
            throw new TypeError("The fill character must be exactly one character long");
        }

        return PythonString.valueOf(value + fillChar.value.repeat(rightPadding));
    }

    public PythonInteger count(PythonString sub) {
        Matcher matcher = Pattern.compile(Pattern.quote(sub.value)).matcher(value);
        return PythonInteger.valueOf(matcher.results().count());
    }

    public PythonInteger count(PythonString sub, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());

        Matcher matcher = Pattern.compile(Pattern.quote(sub.value)).matcher(value.substring(startIndex));
        return PythonInteger.valueOf(matcher.results().count());
    }

    public PythonInteger count(PythonString sub, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        Matcher matcher = Pattern.compile(Pattern.quote(sub.value)).matcher(value.substring(startIndex, endIndex));
        return PythonInteger.valueOf(matcher.results().count());
    }

    // TODO: encode https://docs.python.org/3/library/stdtypes.html#str.encode

    public PythonBoolean startsWith(PythonString prefix) {
        return PythonBoolean.valueOf(value.startsWith(prefix.value));
    }

    public PythonBoolean startsWith(PythonLikeTuple prefixTuple) {
        for (PythonLikeObject maybePrefix : prefixTuple) {
            if (!(maybePrefix instanceof PythonString)) {
                throw new TypeError("tuple for endswith must only contain str, not int");
            }

            PythonString prefix = (PythonString) maybePrefix;
            if (value.startsWith(prefix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean startsWith(PythonString prefix, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        return PythonBoolean.valueOf(value.substring(startIndex).startsWith(prefix.value));
    }

    public PythonBoolean startsWith(PythonLikeTuple prefixTuple, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        String toCheck = value.substring(startIndex);
        for (PythonLikeObject maybePrefix : prefixTuple) {
            if (!(maybePrefix instanceof PythonString)) {
                throw new TypeError("tuple for endswith must only contain str, not int");
            }

            PythonString prefix = (PythonString) maybePrefix;
            if (toCheck.startsWith(prefix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean startsWith(PythonString prefix, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());
        return PythonBoolean.valueOf(value.substring(startIndex, endIndex).startsWith(prefix.value));
    }

    public PythonBoolean startsWith(PythonLikeTuple prefixTuple, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        String toCheck = value.substring(startIndex, endIndex);
        for (PythonLikeObject maybePrefix : prefixTuple) {
            if (!(maybePrefix instanceof PythonString)) {
                throw new TypeError("tuple for endswith must only contain str, not int");
            }

            PythonString prefix = (PythonString) maybePrefix;
            if (toCheck.startsWith(prefix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonString suffix) {
        return PythonBoolean.valueOf(value.endsWith(suffix.value));
    }

    public PythonBoolean endsWith(PythonLikeTuple suffixTuple) {
        for (PythonLikeObject maybeSuffix : suffixTuple) {
            if (!(maybeSuffix instanceof PythonString)) {
                throw new TypeError("tuple for endswith must only contain str, not int");
            }

            PythonString suffix = (PythonString) maybeSuffix;
            if (value.endsWith(suffix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonString suffix, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        return PythonBoolean.valueOf(value.substring(startIndex).endsWith(suffix.value));
    }

    public PythonBoolean endsWith(PythonLikeTuple suffixTuple, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        String toCheck = value.substring(startIndex);
        for (PythonLikeObject maybeSuffix : suffixTuple) {
            if (!(maybeSuffix instanceof PythonString)) {
                throw new TypeError("tuple for endswith must only contain str, not int");
            }

            PythonString suffix = (PythonString) maybeSuffix;
            if (toCheck.endsWith(suffix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonString suffix, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        return PythonBoolean.valueOf(value.substring(startIndex, endIndex).endsWith(suffix.value));
    }

    public PythonBoolean endsWith(PythonLikeTuple suffixTuple, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        String toCheck = value.substring(startIndex, endIndex);
        for (PythonLikeObject maybeSuffix : suffixTuple) {
            if (!(maybeSuffix instanceof PythonString)) {
                throw new TypeError("tuple for endswith must only contain str, not int");
            }

            PythonString suffix = (PythonString) maybeSuffix;
            if (toCheck.endsWith(suffix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonString expandTabs() {
        return expandTabs(PythonInteger.valueOf(8));
    }

    public PythonString expandTabs(PythonInteger tabsize) {
        int tabsizeAsInt = tabsize.value.intValueExact();

        int column = 0;
        int length = value.length();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            if (character == '\n' || character == '\r') {
                builder.append(character);
                column = 0;
                continue;
            }

            if (character == '\t') {
                int remainder = tabsizeAsInt - (column % tabsizeAsInt);
                builder.append(" ".repeat(remainder));
                column += remainder;
                continue;
            }

            builder.append(character);
            column++;
        }

        return PythonString.valueOf(builder.toString());
    }

    public PythonInteger findSubstringIndex(PythonString substring) {
        return PythonInteger.valueOf(value.indexOf(substring.value));
    }

    public PythonInteger findSubstringIndex(PythonString substring, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int result = value.indexOf(substring.value, startIndex);

        return PythonInteger.valueOf(result);
    }

    public PythonInteger findSubstringIndex(PythonString substring, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        int result = value.substring(startIndex, endIndex).indexOf(substring.value);
        return PythonInteger.valueOf(result < 0 ? result : result + startIndex);
    }

    public PythonInteger rightFindSubstringIndex(PythonString substring) {
        return PythonInteger.valueOf(value.lastIndexOf(substring.value));
    }

    public PythonInteger rightFindSubstringIndex(PythonString substring, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int result = value.substring(startIndex).lastIndexOf(substring.value);

        return PythonInteger.valueOf(result < 0 ? result : result + startIndex);
    }

    public PythonInteger rightFindSubstringIndex(PythonString substring, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());
        int result = value.substring(startIndex, endIndex).lastIndexOf(substring.value);

        return PythonInteger.valueOf(result < 0 ? result : result + startIndex);
    }

    public PythonString format(List<PythonLikeObject> positionalArguments, Map<PythonString, PythonLikeObject> namedArguments) {
        return PythonString.valueOf(FORMAT_REGEX.splitAsStream(value)
                .map(part -> {
                    Matcher matcher = FORMAT_REGEX.matcher(part);
                    if (matcher.find()) {
                        String beforeArgument = part.substring(0, matcher.start());
                        String argumentIdentifier = matcher.group(1);

                        boolean isPositional = true;
                        for (int i = 0; i < part.length(); i++) {
                            char character = part.charAt(i);
                            if (!Character.isDigit(character)) {
                                isPositional = false;
                                break;
                            }
                        }
                        if (isPositional) {
                            int position = Integer.parseInt(argumentIdentifier);
                            return beforeArgument + UnaryDunderBuiltin.STR.invoke(positionalArguments.get(position));
                        } else {
                            PythonString identifierName = PythonString.valueOf(argumentIdentifier);
                            return beforeArgument + UnaryDunderBuiltin.STR.invoke(namedArguments.get(identifierName));
                        }
                    } else {
                        return part;
                    }
                }).collect(Collectors.joining()));
    }

    public PythonString formatMap(PythonLikeDict dict) {
        return format(List.of(), (Map) dict);
    }

    public PythonString interpolate(PythonLikeObject object) {
        return interpolate(PythonLikeTuple.fromList(List.of(object)));
    }

    public PythonString interpolate(PythonLikeTuple tuple) {
        Matcher matcher = PRINTF_FORMAT_REGEX.matcher(value);

        StringBuilder out = new StringBuilder();
        int start = 0;
        int currentElement = 0;

        while (matcher.find()) {
            out.append(value, start, matcher.start());
            start = matcher.end();

            String key = matcher.group("key");
            if (key != null) {
                throw new TypeError("format requires a mapping");
            }

            String flags = matcher.group("flags");
            String minWidth = matcher.group("minWidth");
            String precisionString = matcher.group("precision");

            PrintfConversionType conversionType = PrintfConversionType.getConversionType(matcher);

            if (conversionType != PrintfConversionType.LITERAL_PERCENT) {
                if (tuple.size() <= currentElement) {
                    throw new TypeError("not enough arguments for format string");
                }

                PythonLikeObject toConvert = tuple.get(currentElement);

                currentElement++;

                if ("*".equals(minWidth)) {
                    if (tuple.size() <= currentElement) {
                        throw new TypeError("not enough arguments for format string");
                    }
                    minWidth = ((PythonString) UnaryDunderBuiltin.STR.invoke(tuple.get(currentElement))).value;
                    currentElement++;
                }

                if ("*".equals(precisionString)) {
                    if (tuple.size() <= currentElement) {
                        throw new TypeError("not enough arguments for format string");
                    }
                    precisionString = ((PythonString) UnaryDunderBuiltin.STR.invoke(tuple.get(currentElement))).value;
                    currentElement++;
                }

                Optional<Integer> maybePrecision, maybeWidth;
                if (precisionString != null) {
                    maybePrecision = Optional.of(Integer.parseInt(precisionString.substring(1)));
                } else {
                    maybePrecision = Optional.empty();
                }

                if (minWidth != null) {
                    maybeWidth = Optional.of(Integer.parseInt(minWidth));
                } else {
                    maybeWidth = Optional.empty();
                }
                out.append(performInterpolateConversion(flags, maybeWidth, maybePrecision, conversionType, toConvert));
            } else {
                out.append("%");
            }
        }

        out.append(value.substring(start));
        return PythonString.valueOf(out.toString());
    }

    public PythonString interpolate(PythonLikeDict dict) {
        Matcher matcher = PRINTF_FORMAT_REGEX.matcher(value);

        StringBuilder out = new StringBuilder();
        int start = 0;
        while (matcher.find()) {
            out.append(value, start, matcher.start());
            start = matcher.end();

            PrintfConversionType conversionType = PrintfConversionType.getConversionType(matcher);

            if (conversionType != PrintfConversionType.LITERAL_PERCENT) {
                String key = matcher.group("key");
                if (key == null) {
                    throw new ValueError("When a dict is used for the interpolation operator, all conversions must have parenthesised keys");
                }
                key = key.substring(1, key.length() - 1);

                String flags = matcher.group("flags");
                String minWidth = matcher.group("minWidth");
                String precisionString = matcher.group("precision");

                if ("*".equals(minWidth)) {
                    throw new ValueError("* cannot be used for minimum field width when a dict is used for the interpolation operator");
                }

                if ("*".equals(precisionString)) {
                    throw new ValueError("* cannot be used for precision when a dict is used for the interpolation operator");
                }

                PythonLikeObject toConvert = dict.getItemOrError(PythonString.valueOf(key));
                Optional<Integer> maybePrecision, maybeWidth;
                if (precisionString != null) {
                    maybePrecision = Optional.of(Integer.parseInt(precisionString.substring(1)));
                } else {
                    maybePrecision = Optional.empty();
                }

                if (minWidth != null) {
                    maybeWidth = Optional.of(Integer.parseInt(minWidth));
                } else {
                    maybeWidth = Optional.empty();
                }

                out.append(performInterpolateConversion(flags, maybeWidth, maybePrecision, conversionType, toConvert));
            } else {
                out.append("%");
            }
        }

        out.append(value.substring(start));
        return PythonString.valueOf(out.toString());
    }

    private BigDecimal getBigDecimalWithPrecision(BigDecimal number, Optional<Integer> precision) {
        int currentScale = number.scale();
        int currentPrecision = number.precision();
        int precisionDelta = precision.orElse(6) - currentPrecision;
        return number.setScale(currentScale + precisionDelta, RoundingMode.HALF_EVEN);
    }

    private String getUppercaseEngineeringString(BigDecimal number, Optional<Integer> precision) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        printStream.printf("%1." + (precision.orElse(6) - 1) + "E", number);
        return out.toString();
    }

    private String performInterpolateConversion(String flags, Optional<Integer> maybeWidth, Optional<Integer>  maybePrecision, PrintfConversionType conversionType,
                                                PythonLikeObject toConvert) {
        boolean useAlternateForm = flags.contains("#");
        boolean isZeroPadded = flags.contains("0");
        boolean isLeftAdjusted = flags.contains("-");
        if (isLeftAdjusted) {
            isZeroPadded = false;
        }

        boolean putSpaceBeforePositiveNumber = flags.contains(" ");
        boolean putSignBeforeConversion = flags.contains("+");
        if (putSignBeforeConversion) {
            putSpaceBeforePositiveNumber = false;
        }


        String result;
        switch (conversionType) {
            case SIGNED_INTEGER_DECIMAL: {
                if (toConvert instanceof PythonFloat) {
                    toConvert = ((PythonFloat) toConvert).asInteger();
                }
                if (!(toConvert instanceof PythonInteger)) {
                    throw new TypeError("%d format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                result = ((PythonInteger) toConvert).value.toString(10);
                break;
            }
            case SIGNED_INTEGER_OCTAL: {
                if (toConvert instanceof PythonFloat) {
                    toConvert = ((PythonFloat) toConvert).asInteger();
                }
                if (!(toConvert instanceof PythonInteger)) {
                    throw new TypeError("%o format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                result = ((PythonInteger) toConvert).value.toString(8);
                if (useAlternateForm) {
                    result = (result.startsWith("-"))? "-0o" + result.substring(1) : "0o" + result;
                }
                break;
            }
            case SIGNED_HEXADECIMAL_LOWERCASE: {
                if (toConvert instanceof PythonFloat) {
                    toConvert = ((PythonFloat) toConvert).asInteger();
                }
                if (!(toConvert instanceof PythonInteger)) {
                    throw new TypeError("%x format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                result = ((PythonInteger) toConvert).value.toString(16);
                if (useAlternateForm) {
                    result = (result.startsWith("-"))? "-0x" + result.substring(1) : "0x" + result;
                }
                break;
            }
            case SIGNED_HEXADECIMAL_UPPERCASE: {
                if (toConvert instanceof PythonFloat) {
                    toConvert = ((PythonFloat) toConvert).asInteger();
                }
                if (!(toConvert instanceof PythonInteger)) {
                    throw new TypeError("%X format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                result = ((PythonInteger) toConvert).value.toString(16).toUpperCase();
                if (useAlternateForm) {
                    result = (result.startsWith("-"))? "-0X" + result.substring(1) : "0X" + result;
                }
                break;
            }
            case FLOATING_POINT_EXPONENTIAL_LOWERCASE: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%e format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                result = getUppercaseEngineeringString(value, maybePrecision.map(precision -> precision + 1)
                        .or(() -> Optional.of(7))).toLowerCase();
                if (useAlternateForm && !result.contains(".")) {
                    result = result + ".0";
                }
                break;
            }
            case FLOATING_POINT_EXPONENTIAL_UPPERCASE: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%E format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                result = getUppercaseEngineeringString(value, maybePrecision.map(precision -> precision + 1)
                        .or(() -> Optional.of(7)));
                if (useAlternateForm && !result.contains(".")) {
                    result = result + ".0";
                }
                break;
            }
            case FLOATING_POINT_DECIMAL: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%f format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                BigDecimal valueWithPrecision = value.setScale(maybePrecision.orElse(6), RoundingMode.HALF_EVEN);
                result = valueWithPrecision.toPlainString();
                if (useAlternateForm && !result.contains(".")) {
                    result = result + ".0";
                }
                break;
            }
            case FLOATING_POINT_DECIMAL_OR_EXPONENTIAL_LOWERCASE: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%g format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                BigDecimal valueWithPrecision;

                if (value.scale() > 4 || value.precision() >= maybePrecision.orElse(6)) {
                    valueWithPrecision = getBigDecimalWithPrecision(value, maybePrecision);
                    result = getUppercaseEngineeringString(valueWithPrecision, maybePrecision).toLowerCase();
                } else {
                    valueWithPrecision = value.setScale(maybePrecision.orElse(6), RoundingMode.HALF_EVEN);
                    result = valueWithPrecision.toPlainString();
                }

                if (result.length() >= 3 && result.charAt(result.length() - 3) == 'e') {
                    result = result.substring(0, result.length() - 1) + "0" + result.charAt(result.length() - 1);
                }
                break;
            }
            case FLOATING_POINT_DECIMAL_OR_EXPONENTIAL_UPPERCASE: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%G format: a real number is required, not " + toConvert.__getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                BigDecimal valueWithPrecision;

                if (value.scale() > 4 || value.precision() >= maybePrecision.orElse(6)) {
                    valueWithPrecision = getBigDecimalWithPrecision(value, maybePrecision);
                    result = getUppercaseEngineeringString(valueWithPrecision, maybePrecision);
                } else {
                    valueWithPrecision = value.setScale(maybePrecision.orElse(6), RoundingMode.HALF_EVEN);
                    result = valueWithPrecision.toPlainString();
                }
                break;
            }
            case SINGLE_CHARACTER: {
                if (toConvert instanceof PythonString) {
                    PythonString convertedCharacter = (PythonString) toConvert;
                    if (convertedCharacter.value.length() != 1) {
                        throw new ValueError("c specifier can only take an integer or single character string");
                    }
                    result = convertedCharacter.value;
                } else {
                    result = Character.toString(((PythonInteger) toConvert).value.intValueExact());
                }
                break;
            }
            case REPR_STRING: {
                result = ((PythonString) UnaryDunderBuiltin.REPRESENTATION.invoke(toConvert)).value;
                break;
            }
            case STR_STRING: {
                result = ((PythonString) UnaryDunderBuiltin.STR.invoke(toConvert)).value;
                break;
            }
            case ASCII_STRING:{
                result = GlobalBuiltins.ascii(List.of(toConvert), null).value;
                break;
            }
            case LITERAL_PERCENT: {
                result = "%";
                break;
            }
            default:
                throw new IllegalStateException("Unhandled case: " + conversionType);
        }

        if (putSignBeforeConversion && !(result.startsWith("+") || result.startsWith("-"))) {
            result = "+" + result;
        }

        if (putSpaceBeforePositiveNumber && !(result.startsWith("-"))) {
            result = " " + result;
        }

        if (maybeWidth.isPresent() && maybeWidth.get() > result.length()) {
            int padding = maybeWidth.get() - result.length();
            if (isZeroPadded) {
                if (result.startsWith("+") || result.startsWith("-")) {
                    result = result.charAt(0) + "0".repeat(padding) + result.substring(1);
                } else {
                    result = "0".repeat(padding) + result;
                }
            } else if (isLeftAdjusted) {
                result = result + " ".repeat(padding);
            }
        }

        return result;
    }

    public PythonInteger findSubstringIndexOrError(PythonString substring) {
        int result = value.indexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result);
    }

    public PythonInteger findSubstringIndexOrError(PythonString substring, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());

        int result = value.indexOf(substring.value, startIndex);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result);
    }

    public PythonInteger findSubstringIndexOrError(PythonString substring, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        int result = value.substring(startIndex, endIndex).indexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result + startIndex);
    }

    public PythonInteger rightFindSubstringIndexOrError(PythonString substring) {
        int result = value.lastIndexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result);
    }

    public PythonInteger rightFindSubstringIndexOrError(PythonString substring, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());

        int result = value.substring(startIndex).lastIndexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result + startIndex);
    }

    public PythonInteger rightFindSubstringIndexOrError(PythonString substring, PythonInteger start,
            PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        int result = value.substring(startIndex, endIndex).lastIndexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result + startIndex);
    }

    private PythonBoolean allCharactersHaveProperty(IntPredicate predicate) {
        int length = value.length();
        if (length == 0) {
            return PythonBoolean.FALSE;
        }

        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            if (!predicate.test(character)) {
                return PythonBoolean.FALSE;
            }
        }

        return PythonBoolean.TRUE;
    }

    public PythonBoolean isAlphaNumeric() {
        return allCharactersHaveProperty(
                character -> Character.isLetter(character) || Character.getNumericValue(character) != -1);
    }

    public PythonBoolean isAlpha() {
        return allCharactersHaveProperty(Character::isLetter);
    }

    public PythonBoolean isAscii() {
        if (value.isEmpty()) {
            return PythonBoolean.TRUE;
        }
        return allCharactersHaveProperty(character -> character <= 127);
    }

    public PythonBoolean isDecimal() {
        return allCharactersHaveProperty(Character::isDigit);
    }

    private boolean isSuperscriptOrSubscript(int character) {
        String characterName = Character.getName(character);
        return characterName.contains("SUPERSCRIPT") || characterName.contains("SUBSCRIPT");
    }

    public PythonBoolean isDigit() {
        return allCharactersHaveProperty(character -> {
            if (Character.getType(character) == Character.DECIMAL_DIGIT_NUMBER) {
                return true;
            }
            return Character.isDigit(character) || (Character.getNumericValue(character) >= 0 &&
                    isSuperscriptOrSubscript(character));
        });
    }

    public PythonBoolean isIdentifier() {
        int length = value.length();
        if (length == 0) {
            return PythonBoolean.FALSE;
        }

        char firstChar = value.charAt(0);
        if (!isPythonIdentifierStart(firstChar)) {
            return PythonBoolean.FALSE;
        }

        for (int i = 1; i < length; i++) {
            char character = value.charAt(i);

            if (!isPythonIdentifierPart(character)) {
                return PythonBoolean.FALSE;
            }
        }

        return PythonBoolean.TRUE;
    }

    private static boolean isPythonIdentifierStart(char character) {
        if (Character.isLetter(character)) {
            return true;
        }
        if (Character.getType(character) == Character.LETTER_NUMBER) {
            return true;
        }

        switch (character) {
            case '_':
            case 0x1885:
            case 0x1886:
            case 0x2118:
            case 0x212E:
            case 0x309B:
            case 0x309C:
                return true;
            default:
                return false;
        }
    }

    private static boolean isPythonIdentifierPart(char character) {
        if (isPythonIdentifierStart(character)) {
            return true;
        }
        switch (Character.getType(character)) {
            case Character.NON_SPACING_MARK:
            case Character.COMBINING_SPACING_MARK:
            case Character.DECIMAL_DIGIT_NUMBER:
            case Character.CONNECTOR_PUNCTUATION:
                return true;
        }

        switch (character) {
            case 0x00B7:
            case 0x0387:
            case 0x19DA:
                return true;
            default:
                return character >= 0x1369 && character <= 0x1371;
        }
    }

    private static boolean hasCase(int character) {
        return Character.isUpperCase(character) ||
                Character.isLowerCase(character) ||
                Character.isTitleCase(character);
    }

    private boolean hasCaseCharacters() {
        return !allCharactersHaveProperty(character -> !hasCase(character)).getBooleanValue();
    }

    public PythonBoolean isLower() {
        if (hasCaseCharacters()) {
            return allCharactersHaveProperty(character -> !hasCase(character) || Character.isLowerCase(character));
        } else {
            return PythonBoolean.FALSE;
        }
    }

    public PythonBoolean isNumeric() {
        return allCharactersHaveProperty(character -> {
            switch (Character.getType(character)) {
                case Character.OTHER_NUMBER:
                case Character.DECIMAL_DIGIT_NUMBER:
                    return true;
                default:
                    return !Character.isLetter(character) && Character.getNumericValue(character) != -1;
            }
        });
    }

    private static boolean isCharacterPrintable(int character) {
        if (character == ' ') {
            return true;
        }
        switch (Character.getType(character)) {
            // Others
            case Character.PRIVATE_USE:
            case Character.FORMAT:
            case Character.CONTROL:
            case Character.UNASSIGNED:

                // Separators
            case Character.SPACE_SEPARATOR:
            case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR:
                return false;

            default:
                return true;
        }
    }

    public PythonBoolean isPrintable() {
        if (value.isEmpty()) {
            return PythonBoolean.TRUE;
        }

        return allCharactersHaveProperty(PythonString::isCharacterPrintable);
    }

    public PythonBoolean isSpace() {
        return PythonBoolean.valueOf(!value.isEmpty() && value.isBlank());
    }

    public PythonBoolean isUpper() {
        if (hasCaseCharacters()) {
            return allCharactersHaveProperty(character -> !hasCase(character) || Character.isUpperCase(character));
        } else {
            return PythonBoolean.FALSE;
        }
    }

    private enum CharacterCase {
        UNCASED,
        LOWER,
        UPPER;

        public static CharacterCase getCase(int character) {
            if (Character.isLowerCase(character)) {
                return LOWER;
            } else if (Character.isUpperCase(character)) {
                return UPPER;
            } else {
                return UNCASED;
            }
        }

        public static int swapCase(int character) {
            if (Character.isLowerCase(character)) {
                return Character.toUpperCase(character);
            } else if (Character.isUpperCase(character)) {
                return Character.toLowerCase(character);
            }
            return character;
        }
    }

    public PythonBoolean isTitle() {
        int length = value.length();
        if (length == 0) {
            return PythonBoolean.FALSE;
        }

        CharacterCase previousType = CharacterCase.UNCASED;
        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            CharacterCase characterCase = CharacterCase.getCase(character);
            if (characterCase == CharacterCase.UNCASED && Character.isLetter(character)) {
                return PythonBoolean.FALSE;
            }

            switch (previousType) {
                case UNCASED:
                    if (characterCase != CharacterCase.UPPER) {
                        return PythonBoolean.FALSE;
                    }
                    break;
                case UPPER:
                case LOWER:
                    if (characterCase == CharacterCase.UPPER) {
                        return PythonBoolean.FALSE;
                    }
                    break;
            }
            previousType = characterCase;
        }
        return PythonBoolean.TRUE;
    }

    public PythonString join(PythonLikeObject iterable) {
        PythonIterator iterator = (PythonIterator) UnaryDunderBuiltin.ITERATOR.invoke(iterable);
        int index = 0;
        StringBuilder out = new StringBuilder();

        while (iterator.hasNext()) {
            PythonLikeObject maybeString = iterator.nextPythonItem();
            if (!(maybeString instanceof PythonString)) {
                throw new TypeError("sequence item " + index + ": expected str instance, "
                        + maybeString.__getType().getTypeName() + " found");
            }
            PythonString string = (PythonString) maybeString;
            out.append(string.value);
            if (iterator.hasNext()) {
                out.append(value);
            }
            index++;
        }

        return PythonString.valueOf(out.toString());
    }

    public PythonString strip() {
        return PythonString.valueOf(value.strip());
    }

    public PythonString strip(PythonNone ignored) {
        return strip();
    }

    public PythonString strip(PythonString toStrip) {
        int length = value.length();

        int start = 0;
        int end = length - 1;

        for (; start < length; start++) {
            if (toStrip.value.indexOf(value.charAt(start)) == -1) {
                break;
            }
        }

        if (start == length) {
            return EMPTY;
        }

        for (; end >= start; end--) {
            if (toStrip.value.indexOf(value.charAt(end)) == -1) {
                break;
            }
        }

        return PythonString.valueOf(value.substring(start, end + 1));
    }

    public PythonString leftStrip() {
        return PythonString.valueOf(value.stripLeading());
    }

    public PythonString leftStrip(PythonNone ignored) {
        return leftStrip();
    }

    public PythonString leftStrip(PythonString toStrip) {
        int length = value.length();
        for (int i = 0; i < length; i++) {
            if (toStrip.value.indexOf(value.charAt(i)) == -1) {
                return PythonString.valueOf(value.substring(i));
            }
        }
        return EMPTY;
    }

    public PythonString rightStrip() {
        return PythonString.valueOf(value.stripTrailing());
    }

    public PythonString rightStrip(PythonNone ignored) {
        return rightStrip();
    }

    public PythonString rightStrip(PythonString toStrip) {
        int length = value.length();
        for (int i = length - 1; i >= 0; i--) {
            if (toStrip.value.indexOf(value.charAt(i)) == -1) {
                return PythonString.valueOf(value.substring(0, i + 1));
            }
        }
        return EMPTY;
    }

    public PythonLikeTuple partition(PythonString seperator) {
        int firstIndex = value.indexOf(seperator.value);
        if (firstIndex != -1) {
            return PythonLikeTuple.fromList(List.of(
                    PythonString.valueOf(value.substring(0, firstIndex)),
                    seperator,
                    PythonString.valueOf(value.substring(firstIndex + seperator.value.length()))));
        } else {
            return PythonLikeTuple.fromList(List.of(
                    this,
                    EMPTY,
                    EMPTY));
        }
    }

    public PythonLikeTuple rightPartition(PythonString seperator) {
        int lastIndex = value.lastIndexOf(seperator.value);
        if (lastIndex != -1) {
            return PythonLikeTuple.fromList(List.of(
                    PythonString.valueOf(value.substring(0, lastIndex)),
                    seperator,
                    PythonString.valueOf(value.substring(lastIndex + seperator.value.length()))));
        } else {
            return PythonLikeTuple.fromList(List.of(
                    EMPTY,
                    EMPTY,
                    this));
        }
    }

    public PythonString removePrefix(PythonString prefix) {
        if (value.startsWith(prefix.value)) {
            return new PythonString(value.substring(prefix.value.length()));
        }
        return this;
    }

    public PythonString removeSuffix(PythonString suffix) {
        if (value.endsWith(suffix.value)) {
            return new PythonString(value.substring(0, value.length() - suffix.value.length()));
        }
        return this;
    }

    public PythonString replaceAll(PythonString old, PythonString replacement) {
        return PythonString.valueOf(value.replaceAll(Pattern.quote(old.value), replacement.value));
    }

    public PythonString replaceUpToCount(PythonString old, PythonString replacement, PythonInteger count) {
        int countAsInt = count.value.intValueExact();
        if (countAsInt < 0) { // negative count act the same as replace all
            return replaceAll(old, replacement);
        }

        Matcher matcher = Pattern.compile(Pattern.quote(old.value)).matcher(value);
        StringBuilder out = new StringBuilder();
        int start = 0;
        while (countAsInt > 0) {
            if (matcher.find()) {
                out.append(value, start, matcher.start());
                out.append(replacement.value);
                start = matcher.end();
            } else {
                break;
            }
            countAsInt--;
        }
        out.append(value.substring(start));
        return PythonString.valueOf(out.toString());
    }

    public PythonLikeList<PythonString> split() {
        return Arrays.stream(value.stripLeading().split("\\s+"))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> split(PythonNone ignored) {
        return split();
    }

    public PythonLikeList<PythonString> split(PythonString seperator) {
        return Arrays.stream(value.split(Pattern.quote(seperator.value), -1))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> split(PythonString seperator, PythonInteger maxSplits) {
        int maxSplitsAsInt = maxSplits.value.intValueExact();
        if (maxSplitsAsInt == -1) {
            return split(seperator);
        }

        return Arrays.stream(value.split(Pattern.quote(seperator.value), maxSplitsAsInt + 1))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> split(PythonNone ignored, PythonInteger maxSplits) {
        int maxSplitsAsInt = maxSplits.value.intValueExact();
        if (maxSplitsAsInt == -1) {
            return split();
        }

        return Arrays.stream(value.stripLeading().split("\\s+", maxSplitsAsInt + 1))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> rightSplit() {
        return split();
    }

    public PythonLikeList<PythonString> rightSplit(PythonNone ignored) {
        return split();
    }

    public PythonLikeList<PythonString> rightSplit(PythonString seperator) {
        return split(seperator);
    }

    public PythonLikeList<PythonString> rightSplit(PythonString seperator, PythonInteger maxSplits) {
        int maxSplitsAsInt = maxSplits.value.intValueExact();
        if (maxSplitsAsInt == -1) {
            return split(seperator);
        }

        String reversedValue = new StringBuilder(value.stripTrailing()).reverse().toString();
        String reversedSeperator = new StringBuilder(seperator.value).reverse().toString();

        return Arrays.stream(reversedValue.split(Pattern.quote(reversedSeperator), maxSplitsAsInt + 1))
                .map(reversedPart -> PythonString.valueOf(new StringBuilder(reversedPart).reverse().toString()))
                .collect(Collectors.collectingAndThen(Collectors.toCollection(PythonLikeList::new), l -> {
                    Collections.reverse(l);
                    return l;
                }));
    }

    public PythonLikeList<PythonString> rightSplit(PythonNone ignored, PythonInteger maxSplits) {
        int maxSplitsAsInt = maxSplits.value.intValueExact();
        if (maxSplitsAsInt == -1) {
            return split();
        }

        String reversedValue = new StringBuilder(value.stripTrailing()).reverse().toString();

        return Arrays.stream(reversedValue.split("\\s+", maxSplitsAsInt + 1))
                .map(reversedPart -> PythonString.valueOf(new StringBuilder(reversedPart).reverse().toString()))
                .collect(Collectors.collectingAndThen(Collectors.toCollection(PythonLikeList::new), l -> {
                    Collections.reverse(l);
                    return l;
                }));
    }

    public PythonLikeList<PythonString> splitLines() {
        if (value.isEmpty()) {
            return new PythonLikeList<>();
        }
        return Arrays.stream(value.split("\\R"))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> splitLines(PythonBoolean keepEnds) {
        if (!keepEnds.getBooleanValue()) {
            return splitLines();
        }

        // Use lookahead so the newline is included in the result
        return Arrays.stream(value.split("(?<=\\R)"))
                .map(PythonString::valueOf)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(PythonLikeList::new), l -> {
                    int i;
                    for (i = 0; i < l.size() - 1; i++) {
                        // lookbehind cause it to split \r\n into two seperate
                        // lines; need to combine consecutive lines where the first ends with \r
                        // and the second starts with \n to get expected behavior
                        if (l.get(i).value.endsWith("\r") && l.get(i + 1).value.startsWith("\n")) {
                            l.set(i, PythonString.valueOf(l.get(i).value + l.remove(i + 1).value));
                            i--;
                        }
                    }

                    // Remove trailing empty string
                    // i = l.size() - 1
                    if (!l.isEmpty() && l.get(i).value.isEmpty()) {
                        l.remove(i);
                    }

                    return l;
                }));
    }

    public PythonString translate(PythonLikeObject object) {
        return PythonString.valueOf(value.codePoints()
                .flatMap(codePoint -> {
                    try {
                        PythonLikeObject translated =
                                BinaryDunderBuiltin.GET_ITEM.invoke(object, PythonInteger.valueOf(codePoint));
                        if (translated == PythonNone.INSTANCE) {
                            return IntStream.empty();
                        }

                        if (translated instanceof PythonInteger) {
                            return IntStream.of(((PythonInteger) translated).value.intValueExact());
                        }

                        if (translated instanceof PythonString) {
                            return ((PythonString) translated).value.codePoints();
                        }

                        throw new TypeError("character mapping must return integer, None or str");
                    } catch (LookupError e) {
                        return IntStream.of(codePoint);
                    }
                }).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
    }

    public PythonString zfill(PythonInteger width) {
        int widthAsInt = width.value.intValueExact();
        if (widthAsInt <= value.length()) {
            return this;
        }

        int leftPadding = widthAsInt - value.length();
        if (!value.isEmpty() && (value.charAt(0) == '+' || value.charAt(0) == '-')) {
            return PythonString.valueOf(value.charAt(0) + "0".repeat(leftPadding) + value.substring(1));
        } else {
            return PythonString.valueOf("0".repeat(leftPadding) + value);
        }
    }

    @Override
    public int compareTo(PythonString pythonString) {
        return value.compareTo(pythonString.value);
    }

    public PythonString repr() {
        return PythonString.valueOf("'" + value.codePoints()
                                            .flatMap(character -> {
                                                if (character == '\\') {
                                                    return IntStream.of('\\', '\\');
                                                }
                                                if (isCharacterPrintable(character)) {
                                                    return IntStream.of(character);
                                                } else {
                                                    switch (character) {
                                                        case '\r': return IntStream.of('\\', 'r');
                                                        case '\n': return IntStream.of('\\', 'n');
                                                        case '\t': return IntStream.of('\\', 't');
                                                        default: {
                                                            if (character < 0xFFFF) {
                                                                return String.format("u%04x", character).codePoints();
                                                            } else {
                                                                return String.format("U%08x", character).codePoints();
                                                            }

                                                        }
                                                    }
                                                }
                                            })
                                            .collect(StringBuilder::new,
                                                     StringBuilder::appendCodePoint, StringBuilder::append)
                                            .toString() + "'");
    }

    public PythonString asString() {
        return this;
    }
}