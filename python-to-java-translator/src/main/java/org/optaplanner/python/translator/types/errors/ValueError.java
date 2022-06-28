package org.optaplanner.python.translator.types.errors;

import java.util.List;

import org.optaplanner.python.translator.types.PythonLikeType;

public class ValueError extends PythonException {
    public final static PythonLikeType VALUE_ERROR_TYPE =
            new PythonLikeType("ValueError", ValueError.class, List.of(PythonException.EXCEPTION_TYPE)),
            $TYPE = VALUE_ERROR_TYPE;

    public ValueError() {
        super(VALUE_ERROR_TYPE);
    }

    public ValueError(String message) {
        super(VALUE_ERROR_TYPE, message);
    }
}
