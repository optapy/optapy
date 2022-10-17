package org.optaplanner.jpyinterpreter.types.errors;

import java.util.List;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class NameError extends PythonException {
    public final static PythonLikeType NAME_ERROR_TYPE =
            new PythonLikeType("NameError", NameError.class, List.of(EXCEPTION_TYPE)),
            $TYPE = NAME_ERROR_TYPE;

    static {
        NAME_ERROR_TYPE
                .setConstructor(((positionalArguments, namedArguments, callerInstance) -> new NameError(NAME_ERROR_TYPE,
                        positionalArguments)));
    }

    public NameError() {
        super(NAME_ERROR_TYPE);
    }

    public NameError(String message) {
        super(NAME_ERROR_TYPE, message);
    }

    public NameError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public NameError(PythonLikeType type) {
        super(type);
    }

    public NameError(PythonLikeType type, String message) {
        super(type, message);
    }
}
