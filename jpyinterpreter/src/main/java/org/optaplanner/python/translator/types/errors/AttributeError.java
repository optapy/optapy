package org.optaplanner.python.translator.types.errors;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

public class AttributeError extends PythonException {
    public final static PythonLikeType ATTRIBUTE_ERROR_TYPE =
            new PythonLikeType("AttributeError", AttributeError.class, List.of(EXCEPTION_TYPE)),
            $TYPE = ATTRIBUTE_ERROR_TYPE;

    static {
        ATTRIBUTE_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments) -> new AttributeError(ATTRIBUTE_ERROR_TYPE, positionalArguments)));
    }

    public AttributeError() {
        super(ATTRIBUTE_ERROR_TYPE);
    }

    public AttributeError(String message) {
        super(ATTRIBUTE_ERROR_TYPE, message);
    }

    public AttributeError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

}
