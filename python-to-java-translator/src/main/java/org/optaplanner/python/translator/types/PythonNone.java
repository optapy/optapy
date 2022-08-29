package org.optaplanner.python.translator.types;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.GlobalBuiltins;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;

public class PythonNone extends AbstractPythonLikeObject {
    public static final PythonNone INSTANCE;
    public static final PythonLikeType NONE_TYPE = new PythonLikeType("NoneType", PythonNone.class);

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(NONE_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        INSTANCE = new PythonNone();
        GlobalBuiltins.addBuiltinConstant("None", INSTANCE);
    }

    private static void registerMethods() throws NoSuchMethodException {
        NONE_TYPE.addMethod(PythonUnaryOperator.AS_BOOLEAN, PythonNone.class.getMethod("asBool"));
        NONE_TYPE.addMethod(PythonBinaryOperators.EQUAL, PythonNone.class.getMethod("equalsObject", PythonLikeObject.class));
        NONE_TYPE.addMethod(PythonBinaryOperators.NOT_EQUAL,
                PythonNone.class.getMethod("notEqualsObject", PythonLikeObject.class));
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
