package org.optaplanner.jpyinterpreter.util.arguments;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;

public class ZeroArgumentSpec<Out_ extends PythonLikeObject>
        extends ArgumentSpec<Out_, OneArgumentSpec<Out_, ?>, Supplier<Out_>> {

    protected ZeroArgumentSpec(String functionName) {
        super(functionName);
    }

    @Override
    protected <ArgumentType_ extends PythonLikeObject> OneArgumentSpec<Out_, ArgumentType_> addArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentKind argumentKind, ArgumentType_ defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex) {
        return new OneArgumentSpec<>(argumentName, argumentType, argumentKind, defaultValue,
                extraPositionalsArgumentIndex, extraKeywordsArgumentIndex, this);
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> OneArgumentSpec<Out_, ArgumentType_> addArgument(String argumentName,
            Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_AND_KEYWORD, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> OneArgumentSpec<Out_, ArgumentType_>
            addPositionalOnlyArgument(String argumentName, Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_ONLY, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> OneArgumentSpec<Out_, ArgumentType_>
            addKeywordOnlyArgument(String argumentName, Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.KEYWORD_ONLY, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> OneArgumentSpec<Out_, ArgumentType_> addArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_AND_KEYWORD, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> OneArgumentSpec<Out_, ArgumentType_>
            addPositionalOnlyArgument(String argumentName, Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_ONLY, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> OneArgumentSpec<Out_, ArgumentType_>
            addKeywordOnlyArgument(String argumentName, Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.KEYWORD_ONLY, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public OneArgumentSpec<Out_, PythonLikeTuple> addExtraPositionalVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeTuple.class, ArgumentKind.VARARGS, null,
                Optional.of(getArgCount()), Optional.empty());
    }

    @Override
    public OneArgumentSpec<Out_, PythonLikeDict> addExtraKeywordVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeDict.class, ArgumentKind.VARARGS, null,
                Optional.empty(), Optional.of(getArgCount()));
    }

    @Override
    public Out_ apply(List<PythonLikeObject> positionalArgumentList,
            Map<PythonString, PythonLikeObject> keywordArgumentMap,
            Supplier<Out_> extractor) {
        extractArgumentList(positionalArgumentList, keywordArgumentMap);
        return extractor.get();
    }
}
