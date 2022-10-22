package org.optaplanner.jpyinterpreter.types;

public class RecursionMarker extends AbstractPythonLikeObject {
    public static final RecursionMarker INSTANCE;
    public static final PythonLikeType $TYPE = BuiltinTypes.BASE_TYPE;

    static {
        INSTANCE = new RecursionMarker();
    }

    private RecursionMarker() {
        super(BuiltinTypes.BASE_TYPE);
    }

    @Override
    public String toString() {
        return "RecursionMarker";
    }
}
