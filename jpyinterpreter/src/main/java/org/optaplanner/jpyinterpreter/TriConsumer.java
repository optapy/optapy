package org.optaplanner.jpyinterpreter;

public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}
