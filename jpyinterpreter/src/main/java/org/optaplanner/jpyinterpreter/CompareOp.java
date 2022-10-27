package org.optaplanner.jpyinterpreter;

public enum CompareOp {
    LESS_THAN(0, "__lt__"),
    LESS_THAN_OR_EQUALS(1, "__le__"),
    EQUALS(2, "__eq__"),
    NOT_EQUALS(3, "__ne__"),
    GREATER_THAN(4, "__gt__"),
    GREATER_THAN_OR_EQUALS(5, "__ge__");

    public final int id;
    public final String dunderMethod;

    CompareOp(int id, String dunderMethod) {
        this.id = id;
        this.dunderMethod = dunderMethod;
    }

    public static CompareOp getOpForDunderMethod(String dunderMethod) {
        for (CompareOp op : CompareOp.values()) {
            if (op.dunderMethod.equals(dunderMethod)) {
                return op;
            }
        }
        throw new IllegalArgumentException("No Op corresponds to dunder method (" + dunderMethod + ")");
    }

    public static CompareOp getOp(int id) {
        for (CompareOp op : CompareOp.values()) {
            if (op.id == id) {
                return op;
            }
        }
        throw new IllegalArgumentException("No Op corresponds to id (" + id + ")");
    }
}
