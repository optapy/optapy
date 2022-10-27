package org.optaplanner.jpyinterpreter.types.numeric;

import java.util.List;

import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class PythonComplex extends AbstractPythonLikeObject implements PythonNumber {
    final PythonNumber real;
    final PythonNumber imaginary;

    public final static PythonLikeType COMPLEX_TYPE = new PythonLikeType("complex", PythonComplex.class, List.of(NUMBER_TYPE));

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonComplex::registerMethods);
    }

    public PythonComplex(PythonNumber real, PythonNumber imaginary) {
        super(COMPLEX_TYPE);
        this.real = real;
        this.imaginary = imaginary;
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // TODO
        return COMPLEX_TYPE;
    }

    public PythonComplex valueOf(PythonNumber real, PythonNumber imaginary) {
        return new PythonComplex(real, imaginary);
    }

    @Override
    public Number getValue() {
        return (real.getValue().doubleValue() * real.getValue().doubleValue()) +
                (imaginary.getValue().doubleValue() * imaginary.getValue().doubleValue());
    }

    public PythonNumber getReal() {
        return real;
    }

    public PythonNumber getImaginary() {
        return imaginary;
    }
}
