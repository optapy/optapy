package org.optaplanner.jpyinterpreter.util.arguments;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.types.errors.TypeError;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.util.function.EightArgFunction;
import org.optaplanner.jpyinterpreter.util.function.ElevenArgFunction;
import org.optaplanner.jpyinterpreter.util.function.FifteenArgFunction;
import org.optaplanner.jpyinterpreter.util.function.FourteenArgFunction;
import org.optaplanner.jpyinterpreter.util.function.HexaFunction;
import org.optaplanner.jpyinterpreter.util.function.NineArgFunction;
import org.optaplanner.jpyinterpreter.util.function.PentaFunction;
import org.optaplanner.jpyinterpreter.util.function.QuadFunction;
import org.optaplanner.jpyinterpreter.util.function.SevenArgFunction;
import org.optaplanner.jpyinterpreter.util.function.SixteenArgFunction;
import org.optaplanner.jpyinterpreter.util.function.TenArgFunction;
import org.optaplanner.jpyinterpreter.util.function.ThirteenArgFunction;
import org.optaplanner.jpyinterpreter.util.function.TriFunction;
import org.optaplanner.jpyinterpreter.util.function.TwelveArgFunction;

public class ArgumentSpecTest {
    @SuppressWarnings("rawtypes")
    final Object[] SPEC_FUNCTIONS = new Object[] {
            (Supplier) ArgumentSpecTest::asTuple,
            (Function) ArgumentSpecTest::asTuple,
            (BiFunction) ArgumentSpecTest::asTuple,
            (TriFunction) ArgumentSpecTest::asTuple,
            (QuadFunction) ArgumentSpecTest::asTuple,
            (PentaFunction) ArgumentSpecTest::asTuple,
            (HexaFunction) ArgumentSpecTest::asTuple,
            (SevenArgFunction) ArgumentSpecTest::asTuple,
            (EightArgFunction) ArgumentSpecTest::asTuple,
            (NineArgFunction) ArgumentSpecTest::asTuple,
            (TenArgFunction) ArgumentSpecTest::asTuple,
            (ElevenArgFunction) ArgumentSpecTest::asTuple,
            (TwelveArgFunction) ArgumentSpecTest::asTuple,
            (ThirteenArgFunction) ArgumentSpecTest::asTuple,
            (FourteenArgFunction) ArgumentSpecTest::asTuple,
            (FifteenArgFunction) ArgumentSpecTest::asTuple,
            (SixteenArgFunction) ArgumentSpecTest::asTuple,
            (Function) a -> a,
            (Function) a -> a,
            (Function) a -> a,
            (Function) a -> a,
    };

    @Test
    @SuppressWarnings("unchecked")
    public void testSpec() {
        ArgumentSpec<?, ?, Object> current = (ArgumentSpec<?, ?, Object>) (ArgumentSpec<?, ?, ?>) ArgumentSpec
                .forFunctionReturning("myFunction", PythonLikeTuple.class);

        List<String> argumentNameList = new ArrayList<>();
        List<PythonInteger> argumentValueList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            List<PythonLikeObject> positionalArguments = new ArrayList<>(argumentValueList);
            Map<PythonString, PythonLikeObject> keywordArguments = new HashMap<>();

            current.apply(positionalArguments, keywordArguments, SPEC_FUNCTIONS[i]);
            while (!positionalArguments.isEmpty()) {
                int toRemove = positionalArguments.size() - 1;
                PythonLikeObject removed = positionalArguments.remove(toRemove);
                keywordArguments.put(PythonString.valueOf(argumentNameList.get(toRemove)), removed);
                List<PythonInteger> out =
                        (List<PythonInteger>) current.apply(positionalArguments, keywordArguments, SPEC_FUNCTIONS[i]);
                assertThat(out).containsExactlyElementsOf(argumentValueList);
            }

            current = (ArgumentSpec<?, ?, Object>) current.addArgument("arg" + i, PythonInteger.class);
            argumentNameList.add("arg" + i);
            argumentValueList.add(PythonInteger.valueOf(i));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSpecWithDefaults() {
        ArgumentSpec<?, ?, Object> current = (ArgumentSpec<?, ?, Object>) (ArgumentSpec<?, ?, ?>) ArgumentSpec
                .forFunctionReturning("myFunction", PythonLikeTuple.class);

        List<String> argumentNameList = new ArrayList<>();
        List<PythonInteger> argumentValueList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            for (int missingArgs = 0; missingArgs < i; missingArgs++) {
                List<PythonLikeObject> positionalArguments =
                        new ArrayList<>(argumentValueList.subList(0, argumentValueList.size() - missingArgs));
                Map<PythonString, PythonLikeObject> keywordArguments = new HashMap<>();

                current.apply(positionalArguments, keywordArguments, SPEC_FUNCTIONS[i]);
                while (!positionalArguments.isEmpty()) {
                    int toRemove = positionalArguments.size() - 1;
                    PythonLikeObject removed = positionalArguments.remove(toRemove);
                    keywordArguments.put(PythonString.valueOf(argumentNameList.get(toRemove)), removed);
                    List<PythonInteger> out =
                            (List<PythonInteger>) current.apply(positionalArguments, keywordArguments, SPEC_FUNCTIONS[i]);
                    List<PythonInteger> expected =
                            new ArrayList<>(argumentValueList.subList(0, argumentValueList.size() - missingArgs));
                    for (int j = i - missingArgs; j < i; j++) {
                        expected.add(PythonInteger.valueOf(-j));
                    }
                    assertThat(out).containsExactlyElementsOf(expected);
                }
            }

            current =
                    (ArgumentSpec<?, ?, Object>) current.addArgument("arg" + i, PythonInteger.class, PythonInteger.valueOf(-i));
            argumentNameList.add("arg" + i);
            argumentValueList.add(PythonInteger.valueOf(i));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSpecMissingArgument() {
        ArgumentSpec<?, ?, Object> current = (ArgumentSpec<?, ?, Object>) (ArgumentSpec<?, ?, ?>) ArgumentSpec
                .forFunctionReturning("myFunction", PythonLikeTuple.class)
                .addArgument("_arg0", PythonInteger.class);

        List<String> argumentNameList = new ArrayList<>();
        List<PythonInteger> argumentValueList = new ArrayList<>();

        for (int i = 0; i < 19; i++) {
            int functionIndex = i + 1;
            ArgumentSpec<?, ?, Object> finalCurrent = current;

            List<PythonLikeObject> positionalArguments = new ArrayList<>(argumentValueList);
            Map<PythonString, PythonLikeObject> keywordArguments = new HashMap<>();
            assertThatCode(() -> finalCurrent.apply(positionalArguments, keywordArguments, SPEC_FUNCTIONS[functionIndex]))
                    .isInstanceOf(TypeError.class)
                    .hasMessageContaining("myFunction() missing 1 required positional argument: '");
            while (!positionalArguments.isEmpty()) {
                int toRemove = positionalArguments.size() - 1;
                PythonLikeObject removed = positionalArguments.remove(toRemove);
                keywordArguments.put(PythonString.valueOf(argumentNameList.get(toRemove)), removed);

                assertThatCode(() -> finalCurrent.apply(positionalArguments, keywordArguments, SPEC_FUNCTIONS[functionIndex]))
                        .isInstanceOf(TypeError.class)
                        .hasMessageContaining("myFunction() missing 1 required positional argument: '");
                ;
            }

            current = (ArgumentSpec<?, ?, Object>) current.addArgument("arg" + i, PythonInteger.class);
            argumentNameList.add("arg" + i);
            argumentValueList.add(PythonInteger.valueOf(i));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSpecExtraArgument() {
        ArgumentSpec<?, ?, Object> current = (ArgumentSpec<?, ?, Object>) (ArgumentSpec<?, ?, ?>) ArgumentSpec
                .forFunctionReturning("myFunction", PythonLikeTuple.class);

        List<String> argumentNameList = new ArrayList<>();
        List<PythonInteger> argumentValueList = new ArrayList<>();
        argumentNameList.add("_arg0");
        argumentValueList.add(PythonInteger.valueOf(-1));

        for (int i = 0; i < 20; i++) {
            int functionIndex = i;
            ArgumentSpec<?, ?, Object> finalCurrent = current;

            List<PythonLikeObject> positionalArguments = new ArrayList<>(argumentValueList);
            Map<PythonString, PythonLikeObject> keywordArguments = new HashMap<>();
            String[] possibleErrorMessages = new String[2 + i];
            possibleErrorMessages[0] = "myFunction() takes " + i + " positional arguments but " + (i + 1) + " were given";
            possibleErrorMessages[1] = "myFunction() got an unexpected keyword argument '_arg0'";
            for (int arg = 0; arg < i; arg++) {
                possibleErrorMessages[2 + arg] = "myFunction() got multiple values for argument 'arg" + arg + "'";
            }

            assertThatCode(() -> finalCurrent.apply(positionalArguments, keywordArguments, SPEC_FUNCTIONS[functionIndex]))
                    .isInstanceOf(TypeError.class)
                    .extracting(Throwable::getMessage, as(InstanceOfAssertFactories.STRING))
                    .containsAnyOf(possibleErrorMessages);
            while (!positionalArguments.isEmpty()) {
                int toRemove = positionalArguments.size() - 1;
                PythonLikeObject removed = positionalArguments.remove(toRemove);
                keywordArguments.put(PythonString.valueOf(argumentNameList.get(toRemove)), removed);

                assertThatCode(() -> finalCurrent.apply(positionalArguments, keywordArguments, SPEC_FUNCTIONS[functionIndex]))
                        .isInstanceOf(TypeError.class)
                        .extracting(Throwable::getMessage, as(InstanceOfAssertFactories.STRING))
                        .containsAnyOf(possibleErrorMessages);
            }

            current = (ArgumentSpec<?, ?, Object>) current.addArgument("arg" + i, PythonInteger.class);
            argumentNameList.add("arg" + i);
            argumentValueList.add(PythonInteger.valueOf(i));
        }
    }

    @SuppressWarnings("unchecked")
    private static PythonLikeTuple asTuple(Object... items) {
        return PythonLikeTuple.fromList((List<PythonLikeObject>) (List<?>) List.of(items));
    }
}
