package org.optaplanner.jpyinterpreter;

public interface TriFunction<A, B, C, Result_> {
    Result_ apply(A a, B b, C c);
}
