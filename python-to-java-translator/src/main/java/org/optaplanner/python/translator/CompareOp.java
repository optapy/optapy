package org.optaplanner.python.translator;

public enum CompareOp {
    LESS_THAN(0),
    LESS_THAN_OR_EQUALS(1),
    EQUALS(2),
    NOT_EQUALS(3),
    GREATER_THAN(4),
    GREATER_THAN_OR_EQUALS(5);

    public final int id;

    CompareOp(int id) {
        this.id = id;
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
