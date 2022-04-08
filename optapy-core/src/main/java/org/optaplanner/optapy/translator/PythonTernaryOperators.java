package org.optaplanner.optapy.translator;

/**
 * The list of all Python Ternary Operators, which take
 * self and two other arguments.
 *
 * ex: a.__setitem__(key, value)
 */
public enum PythonTernaryOperators {
    SET_ITEM("__setitem__");

    final String dunderMethod;

    PythonTernaryOperators(String dunderMethod) {
        this.dunderMethod = dunderMethod;
    }

    public String getDunderMethod() {
        return dunderMethod;
    }
}
