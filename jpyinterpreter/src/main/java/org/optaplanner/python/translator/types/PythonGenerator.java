package org.optaplanner.python.translator.types;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.NONE_TYPE;

import java.util.Iterator;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.errors.GeneratorExit;
import org.optaplanner.python.translator.types.errors.PythonBaseException;

public abstract class PythonGenerator extends AbstractPythonLikeObject implements Iterator<PythonLikeObject> {
    public static PythonLikeType GENERATOR_TYPE = new PythonLikeType("generator", PythonGenerator.class);
    public static PythonLikeType $TYPE = GENERATOR_TYPE;
    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(GENERATOR_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        GENERATOR_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("asPythonIterator")),
                        GENERATOR_TYPE));
        GENERATOR_TYPE.addUnaryMethod(PythonUnaryOperator.NEXT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("next")),
                        BASE_TYPE));
        GENERATOR_TYPE.addMethod("send",
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("send", PythonLikeObject.class)),
                        BASE_TYPE, BASE_TYPE));
        GENERATOR_TYPE.addMethod("throw",
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("throwValue", Throwable.class)),
                        BASE_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE));
        GENERATOR_TYPE.addMethod("close",
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("close")),
                        BASE_TYPE));
    }

    public PythonLikeObject sentValue;
    public Throwable thrownValue;

    public PythonGenerator() {
        super(GENERATOR_TYPE);
        sentValue = NONE_TYPE;
        thrownValue = null;
    }

    public PythonNone close() {
        this.thrownValue = new GeneratorExit();
        next();
        return PythonNone.INSTANCE;
    }

    public PythonLikeObject send(PythonLikeObject sentValue) {
        this.sentValue = sentValue;
        return next();
    }

    public PythonLikeObject throwValue(Throwable thrownValue) {
        this.thrownValue = thrownValue;
        return next();
    }

    public PythonGenerator asPythonIterator() {
        return this;
    }
}
