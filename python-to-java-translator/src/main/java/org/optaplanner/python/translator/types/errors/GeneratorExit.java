package org.optaplanner.python.translator.types.errors;

import java.util.List;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;

/**
 * Error thrown when a user of a generator indicates a Generator should close
 */
public class GeneratorExit extends PythonException {
    public static final PythonLikeType GENERATOR_EXIT_TYPE = new PythonLikeType("GeneratorExit",
            GeneratorExit.class, List.of(EXCEPTION_TYPE)),
            $TYPE = GENERATOR_EXIT_TYPE;

    static {
        GENERATOR_EXIT_TYPE.setConstructor(
                ((positionalArguments, namedArguments) -> new GeneratorExit(GENERATOR_EXIT_TYPE, positionalArguments)));
    }

    private final PythonLikeObject value;

    public GeneratorExit() {
        this(PythonNone.INSTANCE);
    }

    public GeneratorExit(PythonLikeObject value) {
        this(GENERATOR_EXIT_TYPE, List.of(value));
    }

    public GeneratorExit(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
        if (args.size() > 0) {
            value = args.get(0);
        } else {
            value = PythonNone.INSTANCE;
        }
    }
}
