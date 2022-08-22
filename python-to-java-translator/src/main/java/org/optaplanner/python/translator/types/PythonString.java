package org.optaplanner.python.translator.types;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.errors.ValueError;

public class PythonString extends AbstractPythonLikeObject implements PythonLikeComparable<PythonString> {
    public final String value;

    public final static PythonLikeType STRING_TYPE = new PythonLikeType("str", PythonString.class);

    static {
        try {
            PythonLikeComparable.setup(STRING_TYPE);
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(STRING_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        STRING_TYPE.setConstructor((positionalArguments, namedArguments) -> {
            if (positionalArguments.size() == 1) {
                return UnaryDunderBuiltin.STR.invoke(positionalArguments.get(0));
            } else if (positionalArguments.size() == 3) {
                // TODO Support byte array strings
                throw new ValueError("three argument str not supported");
            } else {
                throw new ValueError("str expects");
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
        return value;
    }

    public int length() {
        return value.length();
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(value.length());
    }

    public PythonString getCharAt(PythonInteger position) {
        int index;
        if (position.compareTo(PythonInteger.ZERO) < 0) {
            index = value.length() - position.value.intValueExact();
        } else {
            index = position.value.intValueExact();
        }

        if (index >= value.length()) {
            throw new IndexOutOfBoundsException("position " + position + " larger than string length " + value.length());
        } else if (index < 0) {
            throw new IndexOutOfBoundsException("position " + position + " is less than 0");
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

    public PythonIterator getIterator() {
        return new PythonIterator(value.chars().mapToObj(charVal -> new PythonString(Character.toString(charVal)))
                .iterator());
    }

    @Override
    public int compareTo(PythonString pythonString) {
        return value.compareTo(pythonString.value);
    }

    public PythonString repr() {
        return PythonString.valueOf("'" + value
                .replaceAll("\n", "\\\\n")
                .replaceAll("\t", "\\\\t") + "'");
    }

    public PythonString asString() {
        return this;
    }
}
