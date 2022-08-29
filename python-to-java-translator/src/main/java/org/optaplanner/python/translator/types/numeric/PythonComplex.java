package org.optaplanner.python.translator.types.numeric;

import java.util.List;

import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

public class PythonComplex extends AbstractPythonLikeObject implements PythonNumber {
    final PythonNumber real;
    final PythonNumber imaginary;

    public final static PythonLikeType COMPLEX_TYPE = new PythonLikeType("complex", PythonComplex.class, List.of(NUMBER_TYPE));

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(COMPLEX_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public PythonComplex(PythonNumber real, PythonNumber imaginary) {
        super(COMPLEX_TYPE);
        this.real = real;
        this.imaginary = imaginary;
    }

    private static void registerMethods() throws NoSuchMethodException {
        // TODO
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
