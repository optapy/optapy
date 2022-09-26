package org.optaplanner.python.translator.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.builtins.BinaryDunderBuiltin;
import org.optaplanner.python.translator.builtins.GlobalBuiltins;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.errors.lookup.KeyError;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class StringFormatter {
    final static String IDENTIFIER = "(?:(?:\\p{javaUnicodeIdentifierStart}|_)\\p{javaUnicodeIdentifierPart}*)";
    final static String ARG_NAME = "(?<argName>" + IDENTIFIER + "|\\d+)?";
    final static String ATTRIBUTE_NAME = IDENTIFIER;
    final static String ELEMENT_INDEX = "[^]]+";
    final static String ITEM_NAME = "(?:(?:\\." + ATTRIBUTE_NAME + ")|(?:\\[" + ELEMENT_INDEX + "\\]))";
    final static String FIELD_NAME = "(?<fieldName>" + ARG_NAME + "(" + ITEM_NAME + ")*)?";
    final static String CONVERSION = "(?:!(?<conversion>[rsa]))?";
    final static String FORMAT_SPEC = "(?::(?<formatSpec>[^{}]*))?";

    final static Pattern REPLACEMENT_FIELD_PATTERN = Pattern.compile("\\{" +
            FIELD_NAME +
            CONVERSION +
            FORMAT_SPEC +
            "}|(?<literal>\\{\\{|}})");

    final static Pattern INDEX_CHAIN_PART_PATTERN = Pattern.compile(ITEM_NAME);

    public static String format(String text, List<PythonLikeObject> positionalArguments,
            Map<? extends PythonLikeObject, PythonLikeObject> namedArguments) {
        Matcher matcher = REPLACEMENT_FIELD_PATTERN.matcher(text);
        StringBuilder out = new StringBuilder();
        int start = 0;
        int implicitField = 0;

        while (matcher.find()) {
            out.append(text, start, matcher.start());
            start = matcher.end();

            String literal = matcher.group("literal");
            if (literal != null) {
                switch (literal) {
                    case "{{":
                        out.append("{");
                        continue;
                    case "}}":
                        out.append("}");
                        continue;
                    default:
                        throw new IllegalStateException("Unhandled literal: " + literal);
                }
            }

            String argName = matcher.group("argName");

            PythonLikeObject toConvert;

            if (positionalArguments != null) {
                if (argName == null) {
                    if (implicitField >= positionalArguments.size()) {
                        throw new ValueError(
                                "(" + implicitField + ") is larger than sequence length (" + positionalArguments.size() + ")");
                    }
                    toConvert = positionalArguments.get(implicitField);
                    implicitField++;
                } else {
                    try {
                        int argumentIndex = Integer.parseInt(argName);
                        if (argumentIndex >= positionalArguments.size()) {
                            throw new ValueError("(" + implicitField + ") is larger than sequence length ("
                                    + positionalArguments.size() + ")");
                        }
                        toConvert = positionalArguments.get(argumentIndex);
                    } catch (NumberFormatException e) {
                        if (namedArguments == null) {
                            throw new ValueError("(" + argName + ") cannot be used to index a sequence");
                        } else {
                            toConvert = namedArguments.get(PythonString.valueOf(argName));
                        }
                    }
                }
            } else {
                toConvert = namedArguments.get(PythonString.valueOf(argName));
            }

            if (toConvert == null) {
                throw new KeyError(argName);
            }

            toConvert = getFinalObjectInChain(toConvert, matcher.group("fieldName"));

            String conversion = matcher.group("conversion");
            if (conversion != null) {
                switch (conversion) {
                    case "s":
                        toConvert = UnaryDunderBuiltin.STR.invoke(toConvert);
                        break;
                    case "r":
                        toConvert = UnaryDunderBuiltin.REPRESENTATION.invoke(toConvert);
                        break;
                    case "a":
                        toConvert = GlobalBuiltins.ascii(List.of(toConvert), null);
                        break;
                }
            }

            String formatSpec = Objects.requireNonNullElse(matcher.group("formatSpec"), "");
            out.append(BinaryDunderBuiltin.FORMAT.invoke(toConvert, PythonString.valueOf(formatSpec)));
        }
        out.append(text.substring(start));
        return out.toString();
    }

    private static PythonLikeObject getFinalObjectInChain(PythonLikeObject chainStart, String chain) {
        if (chain == null) {
            return chainStart;
        }

        PythonLikeObject current = chainStart;
        Matcher matcher = INDEX_CHAIN_PART_PATTERN.matcher(chain);

        while (matcher.find()) {
            String result = matcher.group();
            if (result.startsWith(".")) {
                String attributeName = result.substring(1);
                current = BinaryDunderBuiltin.GET_ATTRIBUTE.invoke(current, PythonString.valueOf(attributeName));
            } else {
                String index = result.substring(1, result.length() - 1);
                try {
                    int intIndex = Integer.parseInt(index);
                    current = BinaryDunderBuiltin.GET_ITEM.invoke(current, PythonInteger.valueOf(intIndex));
                } catch (NumberFormatException e) {
                    current = BinaryDunderBuiltin.GET_ITEM.invoke(current, PythonString.valueOf(index));
                }
            }
        }
        return current;
    }

    public static void addGroupings(StringBuilder out, DefaultFormatSpec formatSpec, int groupSize) {
        if (formatSpec.groupingOption.isEmpty()) {
            return;
        }

        if (groupSize <= 0) {
            throw new ValueError(
                    "Invalid format spec: grouping option now allowed for conversion type " + formatSpec.conversionType);
        }

        int decimalSeperator = out.indexOf(".");
        char seperator;
        switch (formatSpec.groupingOption.get()) {
            case COMMA:
                seperator = ',';
                break;
            case UNDERSCORE:
                seperator = '_';
                break;
            default:
                throw new IllegalStateException("Unhandled case: " + formatSpec.groupingOption.get());
        }

        int index;
        if (decimalSeperator != -1) {
            index = decimalSeperator - 1;
        } else {
            index = out.length() - 1;
        }

        int groupIndex = 0;
        while (index >= 0 && out.charAt(index) != '-') {
            groupIndex++;
            if (groupIndex == groupSize) {
                out.insert(index, seperator);
                groupIndex = 0;
            }
            index--;
        }
    }

    public static void align(StringBuilder out, DefaultFormatSpec formatSpec,
            DefaultFormatSpec.AlignmentOption defaultAlignment) {
        if (formatSpec.width.isPresent()) {
            switch (formatSpec.alignment.orElse(defaultAlignment)) {
                case LEFT_ALIGN:
                    leftAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case RIGHT_ALIGN:
                    rightAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case RESPECT_SIGN_RIGHT_ALIGN:
                    respectSignRightAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case CENTER_ALIGN:
                    center(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
            }
        }
    }

    public static void alignWithPrefixRespectingSign(StringBuilder out, String prefix, DefaultFormatSpec formatSpec,
            DefaultFormatSpec.AlignmentOption defaultAlignment) {
        int insertPosition = (out.charAt(0) == '+' || out.charAt(0) == '-' || out.charAt(0) == ' ') ? 1 : 0;
        if (formatSpec.width.isPresent()) {
            switch (formatSpec.alignment.orElse(defaultAlignment)) {
                case LEFT_ALIGN:
                    out.insert(insertPosition, prefix);
                    leftAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case RIGHT_ALIGN:
                    out.insert(insertPosition, prefix);
                    rightAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case RESPECT_SIGN_RIGHT_ALIGN:
                    respectSignRightAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    out.insert(insertPosition, prefix);
                    break;
                case CENTER_ALIGN:
                    out.insert(insertPosition, prefix);
                    center(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
            }
        } else {
            out.insert(insertPosition, prefix);
        }
    }

    public static void leftAlign(StringBuilder builder, String fillCharAsString, int width) {
        if (width <= builder.length()) {
            return;
        }

        int rightPadding = width - builder.length();
        builder.append(fillCharAsString.repeat(rightPadding));
    }

    public static void rightAlign(StringBuilder builder, String fillCharAsString, int width) {
        if (width <= builder.length()) {
            return;
        }

        int leftPadding = width - builder.length();
        builder.insert(0, fillCharAsString.repeat(leftPadding));
    }

    public static void respectSignRightAlign(StringBuilder builder, String fillCharAsString, int width) {
        if (width <= builder.length()) {
            return;
        }

        int leftPadding = width - builder.length();
        if (builder.length() >= 1 && (builder.charAt(0) == '+' || builder.charAt(0) == '-' || builder.charAt(0) == ' ')) {
            builder.insert(1, fillCharAsString.repeat(leftPadding));
        } else {
            builder.insert(0, fillCharAsString.repeat(leftPadding));
        }
    }

    public static void center(StringBuilder builder, String fillCharAsString, int width) {
        if (width <= builder.length()) {
            return;
        }
        int extraWidth = width - builder.length();
        int rightPadding = extraWidth / 2;
        // left padding get extra character if extraWidth is odd
        int leftPadding = rightPadding + (extraWidth & 1); // x & 1 == x % 2

        builder.insert(0, fillCharAsString.repeat(leftPadding))
                .append(fillCharAsString.repeat(rightPadding));
    }
}
