package org.optaplanner.jpyinterpreter.types.errors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;

/**
 * Python base class for all exceptions. Equivalent to Java's {@link Throwable}.
 */
public class PythonBaseException extends RuntimeException implements PythonLikeObject {
    final public static PythonLikeType BASE_EXCEPTION_TYPE = new PythonLikeType("BaseException", PythonBaseException.class),
            $TYPE = BASE_EXCEPTION_TYPE;

    static {
        BASE_EXCEPTION_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new PythonBaseException(BASE_EXCEPTION_TYPE,
                        positionalArguments)));
    }

    Map<String, PythonLikeObject> dict;

    final PythonLikeType type;
    final List<PythonLikeObject> args;

    private static String getMessageFromArgs(List<PythonLikeObject> args) {
        if (args.size() < 1) {
            return null;
        }

        if (args.get(0) instanceof PythonString) {
            return ((PythonString) args.get(0)).getValue();
        }

        return null;
    }

    public PythonBaseException(PythonLikeType type) {
        this(type, List.of());
    }

    public PythonBaseException(PythonLikeType type, List<PythonLikeObject> args) {
        super(getMessageFromArgs(args));
        this.type = type;
        this.args = args;
        this.dict = new HashMap<>();
        __setAttribute("args", PythonLikeTuple.fromList(args));
        __setAttribute("__cause__", PythonNone.INSTANCE);
    }

    public PythonBaseException(PythonLikeType type, String message) {
        super(message);
        this.type = type;
        this.args = List.of(PythonString.valueOf(message));
        this.dict = new HashMap<>();
        __setAttribute("args", PythonLikeTuple.fromList(args));
        __setAttribute("__cause__", PythonNone.INSTANCE);
    }

    /**
     * Python errors are supposed to be extremely low cost, to the point you are encouraged
     * to write code with try...except instead of if...then. See
     * <a href="https://docs.python.org/3/glossary.html#term-EAFP">the Python glossary</a>
     * for more information.
     *
     * @return this
     */
    @Override
    public Throwable fillInStackTrace() {
        // Do nothing
        return this;
    }

    @Override
    public Throwable initCause(Throwable cause) {
        super.initCause(cause);
        __setAttribute("__cause__", (PythonLikeObject) cause);
        return this;
    }

    @Override
    public PythonLikeObject __getAttributeOrNull(String attributeName) {
        return dict.get(attributeName);
    }

    @Override
    public void __setAttribute(String attributeName, PythonLikeObject value) {
        dict.put(attributeName, value);
    }

    @Override
    public void __deleteAttribute(String attributeName) {
        dict.remove(attributeName);
    }

    public PythonLikeTuple $getArgs() {
        return (PythonLikeTuple) __getAttributeOrError("args");
    }

    @Override
    public PythonLikeType __getType() {
        return type;
    }
}
