package org.optaplanner.jpyinterpreter.util.function;

public interface HexaFunction<A_, B_, C_, D_, E_, F_, Result_> {
    Result_ apply(A_ a, B_ b, C_ c, D_ d, E_ e, F_ f);
}
