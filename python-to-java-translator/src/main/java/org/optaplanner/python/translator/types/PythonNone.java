package org.optaplanner.python.translator.types;

import static org.optaplanner.python.translator.types.BuiltinTypes.NONE_TYPE;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.GlobalBuiltins;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;

public class PythonNone extends AbstractPythonLikeObject {
    public static final PythonNone INSTANCE;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonNone::registerMethods);
        INSTANCE = new PythonNone();
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        NONE_TYPE.addUnaryMethod(PythonUnaryOperator.AS_BOOLEAN, PythonNone.class.getMethod("asBool"));
        NONE_TYPE.addBinaryMethod(PythonBinaryOperators.EQUAL,
                PythonNone.class.getMethod("equalsObject", PythonLikeObject.class));
        NONE_TYPE.addBinaryMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonNone.class.getMethod("notEqualsObject", PythonLikeObject.class));
        GlobalBuiltins.addBuiltinConstant("None", INSTANCE);
        return NONE_TYPE;
    }

    private PythonNone() {
        super(NONE_TYPE);
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
