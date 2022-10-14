package org.optaplanner.python.translator.types.errors.warning;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

public class BytesWarning extends Warning {
    public final static PythonLikeType BYTES_WARNING_TYPE =
            new PythonLikeType("BytesWarning", BytesWarning.class, List.of(WARNING_TYPE)),
            $TYPE = BYTES_WARNING_TYPE;

    static {
        BYTES_WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new BytesWarning(BYTES_WARNING_TYPE,
                        positionalArguments)));
    }

    public BytesWarning() {
        super(BYTES_WARNING_TYPE);
    }

    public BytesWarning(String message) {
        super(BYTES_WARNING_TYPE, message);
    }

    public BytesWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public BytesWarning(PythonLikeType type) {
        super(type);
    }

    public BytesWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
