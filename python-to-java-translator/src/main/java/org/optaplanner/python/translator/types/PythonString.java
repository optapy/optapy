package org.optaplanner.python.translator.types;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Map;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonUnaryOperator;

public class PythonString extends AbstractPythonLikeObject implements Comparable<PythonString> {
    public final String value;

    public final static PythonLikeType STRING_TYPE = new PythonLikeType("str", PythonString.class);

    static {
        try {
            PythonLikeComparable.setup(STRING_TYPE.__dir__);
            STRING_TYPE.__dir__.put("__len__", new JavaMethodReference(PythonString.class.getMethod("length"),
                    Map.of()));
            STRING_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, new PythonFunctionSignature(
                    new MethodDescriptor(PythonString.class.getMethod("getCharAt", PythonInteger.class)),
                    STRING_TYPE, PythonInteger.INT_TYPE
            ));
            STRING_TYPE.__dir__.put(PythonBinaryOperators.GET_ITEM.getDunderMethod(),
                                    new JavaMethodReference(PythonString.class.getMethod("getCharAt", PythonInteger.class),
                                                            Map.of()));
            STRING_TYPE.addMethod(PythonUnaryOperator.ITERATOR, new PythonFunctionSignature(
                    new MethodDescriptor(PythonString.class.getMethod("getIterator")),
                    PythonIterator.ITERATOR_TYPE
            ));
            STRING_TYPE.__dir__.put(PythonUnaryOperator.ITERATOR.getDunderMethod(),
                                    new JavaMethodReference(PythonString.class.getMethod("getIterator"),
                                                            Map.of()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
}
