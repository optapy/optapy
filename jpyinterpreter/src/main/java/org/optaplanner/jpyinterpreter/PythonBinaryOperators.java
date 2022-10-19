package org.optaplanner.jpyinterpreter;

import java.util.Optional;

/**
 * The list of all Python Binary Operators, which are performed
 * by calling the left operand's corresponding dunder method on the
 * right operand
 *
 * ex: a.__add__(b)
 */
public enum PythonBinaryOperators {
    // Comparable operations ( from https://docs.python.org/3/reference/datamodel.html#object.__lt__ )
    LESS_THAN("<", "__lt__", "__gt__", true),
    LESS_THAN_OR_EQUAL("<=", "__le__", "__ge__", true),
    GREATER_THAN(">", "__gt__", "__lt__", true),
    GREATER_THAN_OR_EQUAL(">=", "__ge__", "__le__", true),
    EQUAL("==", "__eq__", "__eq__", true),
    NOT_EQUAL("!=", "__ne__", "__ne__", true),

    // Numeric binary operations ( from https://docs.python.org/3/reference/datamodel.html#emulating-numeric-types )
    POWER("**", "__pow__", "__rpow__"),
    MULTIPLY("*", "__mul__", "__rmul__"),
    MATRIX_MULTIPLY("@", "__matmul__", "__rmatmul__"),
    FLOOR_DIVIDE("//", "__floordiv__", "__rfloordiv__"),
    TRUE_DIVIDE("/", "__truediv__", "__rtruediv__"),
    MODULO("%", "__mod__", "__rmod__"),
    ADD("+", "__add__", "__radd__"),
    SUBTRACT("-", "__sub__", "__rsub__"),
    LSHIFT("<<", "__lshift__", "__rlshift__"),
    RSHIFT(">>", "__rshift__", "__rrshift__"),
    AND("&", "__and__", "__rand__"),
    XOR("^", "__xor__", "__rxor__"),
    OR("|", "__or__", "__ror__"),

    // In-place numeric binary operations variations
    INPLACE_POWER("**=", "__ipow__", PythonBinaryOperators.POWER),
    INPLACE_MULTIPLY("*=", "__imul__", PythonBinaryOperators.MULTIPLY),
    INPLACE_MATRIX_MULTIPLY("@=", "__imatmul__", PythonBinaryOperators.MATRIX_MULTIPLY),
    INPLACE_FLOOR_DIVIDE("//=", "__ifloordiv__", PythonBinaryOperators.FLOOR_DIVIDE),
    INPLACE_TRUE_DIVIDE("/=", "__itruediv__", PythonBinaryOperators.TRUE_DIVIDE),
    INPLACE_MODULO("%=", "__imod__", PythonBinaryOperators.MODULO),
    INPLACE_ADD("+=", "__iadd__", PythonBinaryOperators.ADD),
    INPLACE_SUBTRACT("-=", "__isub__", PythonBinaryOperators.SUBTRACT),
    INPLACE_LSHIFT("<<=", "__ilshift__", PythonBinaryOperators.LSHIFT),
    INPLACE_RSHIFT(">>=", "__irshift__", PythonBinaryOperators.RSHIFT),
    INPLACE_AND("&=", "__iand__", PythonBinaryOperators.AND),
    INPLACE_XOR("^=", "__ixor__", PythonBinaryOperators.XOR),
    INPLACE_OR("|=", "__ior__", PythonBinaryOperators.OR),

    // List operations
    // https://docs.python.org/3/reference/datamodel.html#object.__getitem__
    GET_ITEM("", "__getitem__"),
    DELETE_ITEM("", "__delitem__"),

    // Generator operations
    // https://docs.python.org/3/reference/expressions.html#generator-iterator-methods
    SEND("", "send"),
    THROW("", "throw"),

    // Membership operations
    // https://docs.python.org/3/reference/expressions.html#membership-test-operations
    CONTAINS("", "__contains__"),

    // Descriptor operations
    // https://docs.python.org/3/howto/descriptor.html
    DELETE("", "__delete__"),

    // Attribute access
    GET_ATTRIBUTE("", "__getattribute__"),
    GET_ATTRIBUTE_NOT_IN_SLOTS("", "__getattr__"),
    DELETE_ATTRIBUTE("", "__delattr__"),

    // Format string: https://peps.python.org/pep-3101/
    FORMAT("", "__format__"),

    // global builtins
    DIVMOD("", "__divmod__");

    public final String operatorSymbol;
    public final String dunderMethod;
    public final String rightDunderMethod;
    public final boolean isComparisonMethod;
    public final PythonBinaryOperators fallbackOperation;

    PythonBinaryOperators(String operatorSymbol, String dunderMethod) {
        this.operatorSymbol = operatorSymbol;
        this.dunderMethod = dunderMethod;
        this.rightDunderMethod = null;
        this.isComparisonMethod = false;
        this.fallbackOperation = null;
    }

    PythonBinaryOperators(String operatorSymbol, String dunderMethod, String rightDunderMethod) {
        this.operatorSymbol = operatorSymbol;
        this.dunderMethod = dunderMethod;
        this.rightDunderMethod = rightDunderMethod;
        this.isComparisonMethod = false;
        this.fallbackOperation = null;
    }

    PythonBinaryOperators(String operatorSymbol, String dunderMethod, String rightDunderMethod, boolean isComparisonMethod) {
        this.operatorSymbol = operatorSymbol;
        this.dunderMethod = dunderMethod;
        this.rightDunderMethod = rightDunderMethod;
        this.isComparisonMethod = isComparisonMethod;
        this.fallbackOperation = null;
    }

    PythonBinaryOperators(String operatorSymbol, String dunderMethod, PythonBinaryOperators fallbackOperation) {
        this.operatorSymbol = operatorSymbol;
        this.dunderMethod = dunderMethod;
        this.rightDunderMethod = null;
        this.isComparisonMethod = false;
        this.fallbackOperation = fallbackOperation;
    }

    public String getOperatorSymbol() {
        return operatorSymbol;
    }

    public String getDunderMethod() {
        return dunderMethod;
    }

    public String getRightDunderMethod() {
        return rightDunderMethod;
    }

    public boolean hasRightDunderMethod() {
        return rightDunderMethod != null;
    }

    public boolean isComparisonMethod() {
        return isComparisonMethod;
    }

    public Optional<PythonBinaryOperators> getFallbackOperation() {
        return Optional.ofNullable(fallbackOperation);
    }
}
