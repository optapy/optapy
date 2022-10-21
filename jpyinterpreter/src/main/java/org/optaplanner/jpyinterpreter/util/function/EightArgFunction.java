package org.optaplanner.jpyinterpreter.util.function;

public interface EightArgFunction<A_, B_, C_, D_, E_, F_, G_, H_, Result_> {
    Result_ apply(A_ a, B_ b, C_ c, D_ d, E_ e, F_ f, G_ g, H_ h);
}
