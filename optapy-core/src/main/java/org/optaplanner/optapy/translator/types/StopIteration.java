package org.optaplanner.optapy.translator.types;

public class StopIteration extends RuntimeException {
    public static StopIteration INSTANCE = new StopIteration();

    @Override
    public Throwable fillInStackTrace() {
        // Do nothing
        return this;
    }
}
