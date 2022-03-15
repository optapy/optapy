package org.optaplanner.optapy.translator;

/**
 * The list of all Python Binary Operators, which are performed
 * by calling the left operand's corresponding dunder method on the
 * right operand
 *
 * ex: a.__add__(b)
 */
public enum PythonBinaryOperators {
    // Comparable operations ( from https://docs.python.org/3/reference/datamodel.html#object.__lt__ )
    LESS_THAN("__lt__"),
    LESS_THAN_OR_EQUAL("__le__"),
    GREATER_THAN("__gt__"),
    GREATER_THAN_OR_EQUAL("__ge__"),
    EQUAL("__eq__"),
    NOT_EQUAL("__ne__"),

    // Numeric binary operations ( from https://docs.python.org/3/reference/datamodel.html#emulating-numeric-types )
    POWER("__pow__"),
    MULTIPLY("__mul__"),
    MATRIX_MULTIPLY("__matmul__"),
    FLOOR_DIVIDE("__floordiv__"),
    TRUE_DIVIDE("__truediv__"),
    MODULO("__mod__"),
    ADD("__add__"),
    SUBTRACT("__sub__"),
    LSHIFT("__lshift__"),
    RSHIFT("__rshift__"),
    AND("__and__"),
    XOR("__xor__"),
    OR("__or__"),

    // In-place numeric binary operations variations
    INPLACE_POWER("__ipow__"),
    INPLACE_MULTIPLY("__imul__"),
    INPLACE_MATRIX_MULTIPLY("__imatmul__"),
    INPLACE_FLOOR_DIVIDE("__ifloordiv__"),
    INPLACE_TRUE_DIVIDE("__itruediv__"),
    INPLACE_MODULO("__imod__"),
    INPLACE_ADD("__iadd__"),
    INPLACE_SUBTRACT("__isub__"),
    INPLACE_LSHIFT("__ilshift__"),
    INPLACE_RSHIFT("__irshift__"),
    INPLACE_AND("__iand__"),
    INPLACE_XOR("__ixor__"),
    INPLACE_OR("__ior__"),

    // List operations
    // https://docs.python.org/3/reference/datamodel.html#object.__getitem__
    GET_ITEM("__getitem__")
    ;

    final String dunderMethod;

    PythonBinaryOperators(String dunderMethod) {
        this.dunderMethod = dunderMethod;
    }

    public String getDunderMethod() {
        return dunderMethod;
    }
}
