package org.optaplanner.jpyinterpreter.util.arguments;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.util.function.SixteenArgFunction;

public class SixteenArgumentSpec<Out_, Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, Arg10_, Arg11_, Arg12_, Arg13_, Arg14_, Arg15_, Arg16_>
        extends
        ArgumentSpec<Out_, GenericArgumentSpec<Out_>, SixteenArgFunction<Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, Arg10_, Arg11_, Arg12_, Arg13_, Arg14_, Arg15_, Arg16_, Out_>> {

    protected SixteenArgumentSpec(String argumentName, Class<?> argumentType, ArgumentKind argumentKind, Arg16_ defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex,
            ArgumentSpec<?, ?, ?> previousSpec) {
        super(argumentName, argumentType, argumentKind, defaultValue, extraPositionalsArgumentIndex, extraKeywordsArgumentIndex,
                previousSpec);
    }

    @Override
    protected <ArgumentType_ extends PythonLikeObject> GenericArgumentSpec<Out_> addArgument(
            String argumentName, Class<ArgumentType_> argumentType, ArgumentKind argumentKind, ArgumentType_ defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex) {
        return new GenericArgumentSpec<>(argumentName, argumentType, argumentKind, defaultValue,
                extraPositionalsArgumentIndex, extraKeywordsArgumentIndex, this);
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> GenericArgumentSpec<Out_> addArgument(String argumentName,
            Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_AND_KEYWORD, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> GenericArgumentSpec<Out_> addPositionalOnlyArgument(String argumentName,
            Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_ONLY, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> GenericArgumentSpec<Out_> addKeywordOnlyArgument(String argumentName,
            Class<ArgumentType_> argumentType) {
        return addArgument(argumentName, argumentType, ArgumentKind.KEYWORD_ONLY, null,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> GenericArgumentSpec<Out_> addArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_AND_KEYWORD, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> GenericArgumentSpec<Out_> addPositionalOnlyArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.POSITIONAL_ONLY, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public <ArgumentType_ extends PythonLikeObject> GenericArgumentSpec<Out_> addKeywordOnlyArgument(String argumentName,
            Class<ArgumentType_> argumentType, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentType, ArgumentKind.KEYWORD_ONLY, defaultValue,
                Optional.empty(), Optional.empty());
    }

    @Override
    public GenericArgumentSpec<Out_> addExtraPositionalVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeTuple.class, ArgumentKind.VARARGS, null,
                Optional.of(getArgCount()), Optional.empty());
    }

    @Override
    public GenericArgumentSpec<Out_> addExtraKeywordVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeDict.class, ArgumentKind.VARARGS, null,
                Optional.empty(), Optional.of(getArgCount()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Out_ apply(List<PythonLikeObject> positionalArgumentList,
            Map<PythonString, PythonLikeObject> keywordArgumentMap,
            SixteenArgFunction<Arg1_, Arg2_, Arg3_, Arg4_, Arg5_, Arg6_, Arg7_, Arg8_, Arg9_, Arg10_, Arg11_, Arg12_, Arg13_, Arg14_, Arg15_, Arg16_, Out_> extractor) {
        List<PythonLikeObject> argumentList = extractArgumentList(positionalArgumentList, keywordArgumentMap);
        return extractor.apply((Arg1_) argumentList.get(0), (Arg2_) argumentList.get(1),
                (Arg3_) argumentList.get(2), (Arg4_) argumentList.get(3),
                (Arg5_) argumentList.get(4), (Arg6_) argumentList.get(5),
                (Arg7_) argumentList.get(6), (Arg8_) argumentList.get(7),
                (Arg9_) argumentList.get(8), (Arg10_) argumentList.get(9),
                (Arg11_) argumentList.get(10), (Arg12_) argumentList.get(11),
                (Arg13_) argumentList.get(12), (Arg14_) argumentList.get(13),
                (Arg15_) argumentList.get(14), (Arg16_) argumentList.get(15));
    }
}
