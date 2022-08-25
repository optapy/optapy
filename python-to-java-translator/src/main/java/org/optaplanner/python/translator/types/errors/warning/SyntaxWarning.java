package org.optaplanner.python.translator.types.errors.warning;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

public class SyntaxWarning extends Warning {
    public final static PythonLikeType SYNTAX_WARNING_TYPE =
            new PythonLikeType("SyntaxWarning", SyntaxWarning.class, List.of(WARNING_TYPE)),
            $TYPE = SYNTAX_WARNING_TYPE;

    static {
        SYNTAX_WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments) -> new SyntaxWarning(SYNTAX_WARNING_TYPE, positionalArguments)));
    }

    public SyntaxWarning() {
        super(SYNTAX_WARNING_TYPE);
    }

    public SyntaxWarning(String message) {
        super(SYNTAX_WARNING_TYPE, message);
    }

    public SyntaxWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public SyntaxWarning(PythonLikeType type) {
        super(type);
    }

    public SyntaxWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
