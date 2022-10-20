package org.optaplanner.jpyinterpreter.util.arguments;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.TriFunction;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;

public class ThreeArgumentSpec<Out_ extends PythonLikeObject, Arg1_ extends PythonLikeObject, Arg2_ extends PythonLikeObject, Arg3_ extends PythonLikeObject>
        extends ArgumentSpec<Out_, FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, ?>, TriFunction<Arg1_, Arg2_, Arg3_, Out_>> {

    protected ThreeArgumentSpec(String argumentName, Class<?> argumentType, ArgumentKind argumentKind, Arg3_ defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex,
            ArgumentSpec<?, ?, ?> previousSpec) {
        super(argumentName, argumentType, argumentKind, defaultValue, extraPositionalsArgumentIndex, extraKeywordsArgumentIndex,
                previousSpec);
    }

    @Override
    protected <ArgumentType_ extends PythonLikeObject> FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, ArgumentType_> addArgument(
            String argumentName, Class<ArgumentType_> argumentType, ArgumentKind argumentKind, ArgumentType_ defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex) {
        return new FourArgumentSpec<>(argumentName, argumentType, argumentKind, defaultValue,
                extraPositionalsArgumentIndex, extraKeywordsArgumentIndex, this);
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, ArgumentType_>
            addArgument(String argumentName, Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_AND_KEYWORD, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, ArgumentType_>
            addPositionalOnlyArgument(String argumentName, Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_ONLY, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, ArgumentType_>
            addKeywordOnlyArgument(String argumentName, Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.KEYWORD_ONLY, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, ArgumentType_>
            addArgument(String argumentName, Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_AND_KEYWORD, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, ArgumentType_>
            addPositionalOnlyArgument(String argumentName, Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_ONLY, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, ArgumentType_>
            addKeywordOnlyArgument(String argumentName, Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.KEYWORD_ONLY, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, PythonLikeTuple> addExtraPositionalVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeTuple.class, ArgumentKind.VARARGS, null,
                Optional.of(getArgCount()), Optional.empty());
    }

    @Override
    public FourArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, PythonLikeDict> addExtraKeywordVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeDict.class, ArgumentKind.VARARGS, null,
                Optional.empty(), Optional.of(getArgCount()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Out_ apply(List<PythonLikeObject> positionalArgumentList,
            Map<PythonString, PythonLikeObject> keywordArgumentMap,
            TriFunction<Arg1_, Arg2_, Arg3_, Out_> extractor) {
        List<PythonLikeObject> argumentList = extractArgumentList(positionalArgumentList, keywordArgumentMap);
        return extractor.apply((Arg1_) argumentList.get(0), (Arg2_) argumentList.get(1),
                (Arg3_) argumentList.get(2));
    }
}
