package org.optaplanner.jpyinterpreter.types;

import java.util.Iterator;

import org.optaplanner.jpyinterpreter.MethodDescriptor;
import org.optaplanner.jpyinterpreter.PythonFunctionSignature;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.types.errors.GeneratorExit;
import org.optaplanner.jpyinterpreter.types.errors.PythonBaseException;

public abstract class PythonGenerator extends AbstractPythonLikeObject implements Iterator<PythonLikeObject> {
    public static PythonLikeType $TYPE = BuiltinTypes.GENERATOR_TYPE;
    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonGenerator::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        BuiltinTypes.GENERATOR_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("asPythonIterator")),
                        BuiltinTypes.GENERATOR_TYPE));
        BuiltinTypes.GENERATOR_TYPE.addUnaryMethod(PythonUnaryOperator.NEXT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("next")),
                        BuiltinTypes.BASE_TYPE));
        BuiltinTypes.GENERATOR_TYPE.addMethod("send",
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("send", PythonLikeObject.class)),
                        BuiltinTypes.BASE_TYPE, BuiltinTypes.BASE_TYPE));
        BuiltinTypes.GENERATOR_TYPE.addMethod("throw",
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("throwValue", Throwable.class)),
                        BuiltinTypes.BASE_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE));
        BuiltinTypes.GENERATOR_TYPE.addMethod("close",
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonGenerator.class.getMethod("close")),
                        BuiltinTypes.BASE_TYPE));

        return BuiltinTypes.GENERATOR_TYPE;
    }

    public PythonLikeObject sentValue;
    public Throwable thrownValue;

    public PythonGenerator() {
        super(BuiltinTypes.GENERATOR_TYPE);
        sentValue = PythonNone.INSTANCE;
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
