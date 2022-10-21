package org.optaplanner.jpyinterpreter.util.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.types.errors.TypeError;

public abstract class ArgumentSpec<Out_, NextSpec_ extends ArgumentSpec<?, ?, ?>, Extractor_> {
    private final String functionName;
    private final List<String> argumentNameList;
    private final List<Class<?>> argumentTypeList;
    private final List<ArgumentKind> argumentKindList;
    private final List<Object> argumentDefaultList;
    private final Optional<Integer> extraPositionalsArgumentIndex;
    private final Optional<Integer> extraKeywordsArgumentIndex;

    private final int numberOfPositionalArguments;
    private final int requiredPositionalArguments;

    protected ArgumentSpec(String functionName) {
        this.functionName = functionName + "()";
        requiredPositionalArguments = 0;
        numberOfPositionalArguments = 0;
        argumentNameList = List.of();
        argumentTypeList = List.of();
        argumentKindList = List.of();
        argumentDefaultList = List.of();
        extraPositionalsArgumentIndex = Optional.empty();
        extraKeywordsArgumentIndex = Optional.empty();
    }

    protected ArgumentSpec(String argumentName, Class<?> argumentType, ArgumentKind argumentKind, Object defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex,
            ArgumentSpec<?, ?, ?> previousSpec) {
        functionName = previousSpec.functionName;

        if (previousSpec.numberOfPositionalArguments < previousSpec.getArgCount()) {
            numberOfPositionalArguments = previousSpec.numberOfPositionalArguments;
        } else {
            if (argumentKind.allowPositional) {
                numberOfPositionalArguments = previousSpec.getArgCount() + 1;
            } else {
                numberOfPositionalArguments = previousSpec.getArgCount();
            }
        }

        if (argumentKind == ArgumentKind.POSITIONAL_ONLY) {
            if (previousSpec.requiredPositionalArguments != previousSpec.getArgCount()) {
                throw new IllegalArgumentException("All required positional arguments must come before all other arguments");
            } else {
                requiredPositionalArguments = previousSpec.getArgCount() + 1;
            }
        } else {
            requiredPositionalArguments = previousSpec.requiredPositionalArguments;
        }

        argumentNameList = new ArrayList<>(previousSpec.argumentNameList.size() + 1);
        argumentTypeList = new ArrayList<>(previousSpec.argumentTypeList.size() + 1);
        argumentKindList = new ArrayList<>(previousSpec.argumentKindList.size() + 1);
        argumentDefaultList = new ArrayList<>(previousSpec.argumentDefaultList.size() + 1);

        argumentNameList.addAll(previousSpec.argumentNameList);
        argumentNameList.add(argumentName);

        argumentTypeList.addAll(previousSpec.argumentTypeList);
        argumentTypeList.add(argumentType);

        argumentKindList.addAll(previousSpec.argumentKindList);
        argumentKindList.add(argumentKind);

        argumentDefaultList.addAll(previousSpec.argumentDefaultList);
        argumentDefaultList.add(defaultValue);

        if (extraPositionalsArgumentIndex.isPresent() && previousSpec.extraPositionalsArgumentIndex.isPresent()) {
            throw new IllegalArgumentException("Multiple positional vararg arguments");
        }
        if (previousSpec.extraPositionalsArgumentIndex.isPresent()) {
            extraPositionalsArgumentIndex = previousSpec.extraPositionalsArgumentIndex;
        }

        if (extraKeywordsArgumentIndex.isPresent() && previousSpec.extraKeywordsArgumentIndex.isPresent()) {
            throw new IllegalArgumentException("Multiple keyword vararg arguments");
        }
        if (previousSpec.extraKeywordsArgumentIndex.isPresent()) {
            extraKeywordsArgumentIndex = previousSpec.extraKeywordsArgumentIndex;
        }

        this.extraPositionalsArgumentIndex = extraPositionalsArgumentIndex;
        this.extraKeywordsArgumentIndex = extraKeywordsArgumentIndex;
    }

    public static <T extends PythonLikeObject> ZeroArgumentSpec<T> forFunctionReturning(String functionName,
            Class<T> outClass) {
        return new ZeroArgumentSpec<>(functionName);
    }

    protected final int getArgCount() {
        return argumentNameList.size();
    }

    protected final List<PythonLikeObject> extractArgumentList(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> keywordArguments) {
        List<PythonLikeObject> out = new ArrayList<>(argumentNameList.size());

        if (positionalArguments.size() > numberOfPositionalArguments &&
                extraPositionalsArgumentIndex.isEmpty()) {
            throw new TypeError(functionName + " takes " + numberOfPositionalArguments + " positional arguments but "
                    + positionalArguments.size() + " were given");
        }

        if (positionalArguments.size() < requiredPositionalArguments) {
            int missing = (requiredPositionalArguments - positionalArguments.size());
            String argumentString = (missing == 1) ? "argument" : "arguments";
            List<String> missingArgumentNames = argumentNameList.subList(argumentNameList.size() - missing,
                    argumentNameList.size());
            throw new TypeError(functionName + " missing " + (requiredPositionalArguments - positionalArguments.size()) +
                    " required positional " + argumentString + ": '" + String.join("', ", missingArgumentNames) + "'");
        }

        out.addAll(positionalArguments.subList(0, Math.min(numberOfPositionalArguments, positionalArguments.size())));
        for (int i = positionalArguments.size(); i < argumentNameList.size(); i++) {
            out.add(null);
        }

        int remaining = argumentNameList.size() - positionalArguments.size();

        if (extraPositionalsArgumentIndex.isPresent()) {
            remaining--;
            out.set(extraPositionalsArgumentIndex.get(),
                    PythonLikeTuple
                            .fromList(positionalArguments.subList(numberOfPositionalArguments, positionalArguments.size())));
        }

        PythonLikeDict extraKeywordArguments = new PythonLikeDict();
        for (Map.Entry<PythonString, PythonLikeObject> keywordArgument : keywordArguments.entrySet()) {
            PythonString argumentName = keywordArgument.getKey();

            int position = argumentNameList.indexOf(argumentName.value);
            if (position == -1) {
                if (extraKeywordsArgumentIndex.isPresent()) {
                    extraKeywordArguments.put(argumentName, keywordArgument.getValue());
                    continue;
                } else {
                    throw new TypeError(functionName + " got an unexpected keyword argument " + argumentName.repr().value);
                }
            }

            if (out.get(position) != null) {
                throw new TypeError(functionName + " got multiple values for argument " + argumentName.repr().value);
            }

            if (!argumentKindList.get(position).allowKeyword) {
                throw new TypeError(functionName + " got some positional-only arguments passed as keyword arguments: "
                        + argumentName.repr().value);
            }

            remaining--;
            out.set(position, keywordArgument.getValue());
        }

        if (extraKeywordsArgumentIndex.isPresent()) {
            remaining--;
            out.set(extraKeywordsArgumentIndex.get(),
                    extraKeywordArguments);
        }

        if (remaining > 0) {
            List<Integer> missing = new ArrayList<>(remaining);
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i) == null) {
                    if (argumentDefaultList.get(i) != null) {
                        out.set(i, (PythonLikeObject) argumentDefaultList.get(i));
                        remaining--;
                    } else {
                        missing.add(i);
                    }
                }
            }

            if (remaining > 0) {
                if (missing.stream().anyMatch(index -> argumentKindList.get(index).allowPositional)) {
                    List<String> missingAllowsPositional = new ArrayList<>(remaining);
                    for (int index : missing) {
                        if (argumentKindList.get(index).allowPositional) {
                            missingAllowsPositional.add(argumentNameList.get(index));
                        }
                    }
                    String argumentString = (missingAllowsPositional.size() == 1) ? "argument" : "arguments";
                    throw new TypeError(functionName + " missing " + remaining + " required positional " + argumentString
                            + ": '" + String.join("', ", missingAllowsPositional) + "'");
                } else {
                    List<String> missingKeywordOnly = new ArrayList<>(remaining);
                    for (int index : missing) {
                        missingKeywordOnly.add(argumentNameList.get(index));
                    }
                    String argumentString = (missingKeywordOnly.size() == 1) ? "argument" : "arguments";
                    throw new TypeError(functionName + " missing " + remaining + " required keyword-only " + argumentString
                            + ": '" + String.join("', ", missingKeywordOnly) + "'");
                }
            }
        }

        for (int i = 0; i < argumentNameList.size(); i++) {
            if (!argumentTypeList.get(i).isInstance(out.get(i))) {
                throw new TypeError(functionName + "'s argument '" + argumentNameList.get(i) + "' has incorrect type: " +
                        "'" + argumentNameList.get(i) + "' must be a " +
                        JavaPythonTypeConversionImplementor.getPythonLikeType(argumentTypeList.get(i)) +
                        " (got " + JavaPythonTypeConversionImplementor.getPythonLikeType(out.get(i).getClass()) + " instead)");
            }
        }

        return out;
    }

    protected abstract <ArgumentType_ extends PythonLikeObject> NextSpec_ addArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentKind argumentKind, ArgumentType_ defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex);

    public abstract <ArgumentType_ extends PythonLikeObject> NextSpec_ addArgument(String argumentName,
            Class<ArgumentType_> argumentType);

    public abstract <ArgumentType_ extends PythonLikeObject> NextSpec_ addPositionalOnlyArgument(String argumentName,
            Class<ArgumentType_> argumentType);

    public abstract <ArgumentType_ extends PythonLikeObject> NextSpec_ addKeywordOnlyArgument(String argumentName,
            Class<ArgumentType_> argumentType);

    public abstract NextSpec_ addExtraPositionalVarArgument(String argumentName);

    public abstract NextSpec_ addExtraKeywordVarArgument(String argumentName);

    public abstract <ArgumentType_ extends PythonLikeObject> NextSpec_ addArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentType_ defaultValue);

    public abstract <ArgumentType_ extends PythonLikeObject> NextSpec_ addPositionalOnlyArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentType_ defaultValue);

    public abstract <ArgumentType_ extends PythonLikeObject> NextSpec_ addKeywordOnlyArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentType_ defaultValue);

    public abstract Out_ apply(List<PythonLikeObject> positionalArgumentList,
            Map<PythonString, PythonLikeObject> keywordArgumentMap,
            Extractor_ extractor);

    public final PythonLikeFunction asStaticPythonLikeFunction(final Extractor_ extractor) {
        return new PythonLikeFunction() {
            @Override
            public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
                    Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
                return (PythonLikeObject) apply(positionalArguments, namedArguments, extractor);
            }

            @Override
            public PythonLikeType __getType() {
                return BuiltinTypes.STATIC_FUNCTION_TYPE;
            }
        };
    }

    public final PythonLikeFunction asClassPythonLikeFunction(final Extractor_ extractor) {
        return new PythonLikeFunction() {
            @Override
            public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
                    Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
                return (PythonLikeObject) apply(positionalArguments, namedArguments, extractor);
            }

            @Override
            public PythonLikeType __getType() {
                return BuiltinTypes.CLASS_FUNCTION_TYPE;
            }
        };
    }

    public final PythonLikeFunction asVirtualPythonLikeFunction(final Extractor_ extractor) {
        return (positionalArguments, namedArguments,
                callerInstance) -> (PythonLikeObject) apply(positionalArguments, namedArguments, extractor);
    }
}
