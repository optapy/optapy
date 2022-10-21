package org.optaplanner.jpyinterpreter.util.arguments;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.util.function.NineArgFunction;

public class NineArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_>
        extends
        ArgumentSpec<Out_, TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, ?>, NineArgFunction<Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, Out_>> {

    protected NineArgumentSpec(String argumentName, Class<?> argumentType, ArgumentKind argumentKind, Arg9_ defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex,
            ArgumentSpec<?, ?, ?> previousSpec) {
        super(argumentName, argumentType, argumentKind, defaultValue, extraPositionalsArgumentIndex, extraKeywordsArgumentIndex,
                previousSpec);
    }

    @Override
    protected <ArgumentType_ extends PythonLikeObject>
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, ArgumentType_> addArgument(
                    String argumentName, Class<ArgumentType_> argumentType, ArgumentKind argumentKind,
                    ArgumentType_ defaultValue,
                    Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex) {
        return new TenArgumentSpec<>(argumentName, argumentType, argumentKind, defaultValue,
                extraPositionalsArgumentIndex, extraKeywordsArgumentIndex, this);
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject>
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, ArgumentType_>
            addArgument(String argumentName, Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_AND_KEYWORD, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject>
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, ArgumentType_>
            addPositionalOnlyArgument(String argumentName, Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_ONLY, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject>
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, ArgumentType_>
            addKeywordOnlyArgument(String argumentName, Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.KEYWORD_ONLY, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject>
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, ArgumentType_>
            addArgument(String argumentName, Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_AND_KEYWORD, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject>
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, ArgumentType_>
            addPositionalOnlyArgument(String argumentName, Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_ONLY, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject>
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, ArgumentType_>
            addKeywordOnlyArgument(String argumentName, Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.KEYWORD_ONLY, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, PythonLikeTuple>
            addExtraPositionalVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeTuple.class, ArgumentKind.VARARGS, null,
                Optional.of(getArgCount()), Optional.empty());
    }

    @Override
    public
            TenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, PythonLikeDict>
            addExtraKeywordVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeDict.class, ArgumentKind.VARARGS, null,
                Optional.empty(), Optional.of(getArgCount()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Out_ apply(List<PythonLikeObject> positionalArgumentList,
            Map<PythonString, PythonLikeObject> keywordArgumentMap,
            NineArgFunction<Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, Out_> extractor) {
        List<PythonLikeObject> argumentList = extractArgumentList(positionalArgumentList, keywordArgumentMap);
        return extractor.apply((Arg1_) argumentList.get(0), (Arg2_) argumentList.get(1),
                (Arg3_) argumentList.get(2), (Arg4_) argumentList.get(3),
                (Arg5_) argumentList.get(4), (Arg6_) argumentList.get(5),
                (Arg7_) argumentList.get(6), (Arg8_) argumentList.get(7),
                (Arg9_) argumentList.get(8));
    }
}
