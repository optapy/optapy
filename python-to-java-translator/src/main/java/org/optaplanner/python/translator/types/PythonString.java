package org.optaplanner.python.translator.types;

import java.math.BigInteger;
import java.util.Map;

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
            STRING_TYPE.__dir__.put("__len__", new JavaMethodReference(PythonString.class.getMethod("length"),
                    Map.of()));
            STRING_TYPE.__dir__.put(PythonBinaryOperators.GET_ITEM.getDunderMethod(),
                    new JavaMethodReference(PythonString.class.getMethod("getCharAt", PythonInteger.class),
                            Map.of()));
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
        STRING_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonString.class.getMethod("getCharAt", PythonInteger.class));
        STRING_TYPE.addMethod(PythonUnaryOperator.REPRESENTATION, PythonString.class.getMethod("repr"));
        STRING_TYPE.addMethod(PythonUnaryOperator.AS_STRING, PythonString.class.getMethod("asString"));
        STRING_TYPE.addMethod(PythonUnaryOperator.ITERATOR, PythonString.class.getMethod("getIterator"));
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

    public PythonString getCharAt(PythonInteger position) {
        if (position.value.compareTo(BigInteger.valueOf(value.length())) >= 0) {
            throw new IndexOutOfBoundsException("position " + position + " larger than string length " + value.length());
        }
        return new PythonString(Character.toString(value.charAt(position.value.intValue())));
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
