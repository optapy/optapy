package org.optaplanner.python.translator;

public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}
