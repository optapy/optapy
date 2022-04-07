package org.optaplanner.optapy.translator.types;

import java.util.Map;

public class PythonNone extends AbstractPythonLikeObject {
    public static final PythonNone INSTANCE;
    private static final PythonLikeType NONE_TYPE = new PythonLikeType("NoneType");

    static {
        NONE_TYPE.__dir__.put("__bool__", new UnaryLambdaReference(self -> PythonBoolean.FALSE, Map.of()));
        NONE_TYPE.__dir__.put("__eq__", new BinaryLambdaReference((self, other) -> PythonBoolean.valueOf(self == other), Map.of()));
        NONE_TYPE.__dir__.put("__neq__", new BinaryLambdaReference((self, other) -> PythonBoolean.valueOf(self != other), Map.of()));
        INSTANCE = new PythonNone();
    }

    private PythonNone() {
        super(NONE_TYPE);
    }

    @Override
    public String toString() {
        return "None";
    }
}
