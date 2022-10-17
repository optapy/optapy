package org.optaplanner.jpyinterpreter.types;

import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.builtins.GlobalBuiltins;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;

public class PythonNone extends AbstractPythonLikeObject {
    public static final PythonNone INSTANCE;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonNone::registerMethods);
        INSTANCE = new PythonNone();
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        BuiltinTypes.NONE_TYPE.addUnaryMethod(PythonUnaryOperator.AS_BOOLEAN, PythonNone.class.getMethod("asBool"));
        BuiltinTypes.NONE_TYPE.addBinaryMethod(PythonBinaryOperators.EQUAL,
                PythonNone.class.getMethod("equalsObject", PythonLikeObject.class));
        BuiltinTypes.NONE_TYPE.addBinaryMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonNone.class.getMethod("notEqualsObject", PythonLikeObject.class));
        GlobalBuiltins.addBuiltinConstant("None", INSTANCE);
        return BuiltinTypes.NONE_TYPE;
    }

    private PythonNone() {
        super(BuiltinTypes.NONE_TYPE);
    }

    @Override
    public String toString() {
        return "None";
    }

    public PythonBoolean asBool() {
        return PythonBoolean.FALSE;
    }

    public PythonBoolean equalsObject(PythonLikeObject other) {
        return PythonBoolean.valueOf(this == other);
    }

    public PythonBoolean notEqualsObject(PythonLikeObject other) {
        return PythonBoolean.valueOf(this != other);
    }
}
