package org.optaplanner.python.translator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.optaplanner.python.translator.types.BuiltinTypes.BOOLEAN_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.INT_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.STRING_TYPE;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeFunction;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class PythonOverloadImplementorTest {

    @Test
    public void testSingleOverload() throws NoSuchMethodException {
        SingleOverload.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                SingleOverload.class.getMethod("overload")), STRING_TYPE));
        PythonOverloadImplementor.createDispatchesFor(SingleOverload.TYPE);

        SingleOverload instance = new SingleOverload();
        PythonLikeFunction overload = (PythonLikeFunction) SingleOverload.TYPE.__getAttributeOrError("overload");
        assertThat(overload.__call__(List.of(instance), Map.of())).isEqualTo(PythonString.valueOf("1"));
    }

    @Test
    public void testDifferentArgCountOverloads() throws NoSuchMethodException {
        DifferentArgCountOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgCountOverloads.class.getMethod("overload")), STRING_TYPE));
        DifferentArgCountOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgCountOverloads.class.getMethod("overload", PythonInteger.class)), INT_TYPE, INT_TYPE));
        PythonOverloadImplementor.createDispatchesFor(DifferentArgCountOverloads.TYPE);

        DifferentArgCountOverloads instance = new DifferentArgCountOverloads();
        PythonLikeFunction overload = (PythonLikeFunction) DifferentArgCountOverloads.TYPE.__getAttributeOrError("overload");
        assertThat(overload.__call__(List.of(instance), Map.of())).isEqualTo(PythonString.valueOf("1"));
        assertThat(overload.__call__(List.of(instance, PythonInteger.valueOf(2)), Map.of()))
                .isEqualTo(PythonInteger.valueOf(2));
        assertThat(overload.__call__(List.of(instance, PythonInteger.valueOf(3)), Map.of()))
                .isEqualTo(PythonInteger.valueOf(3));
    }

    @Test
    public void testDifferentArgTypeOverloads() throws NoSuchMethodException {
        DifferentArgTypeOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgTypeOverloads.class.getMethod("overload", PythonString.class)), STRING_TYPE, STRING_TYPE));
        DifferentArgTypeOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgTypeOverloads.class.getMethod("overload", PythonInteger.class)), INT_TYPE, INT_TYPE));
        DifferentArgTypeOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgTypeOverloads.class.getMethod("overload", PythonBoolean.class)), BOOLEAN_TYPE, BOOLEAN_TYPE));
        PythonOverloadImplementor.createDispatchesFor(DifferentArgTypeOverloads.TYPE);

        DifferentArgTypeOverloads instance = new DifferentArgTypeOverloads();
        PythonLikeFunction overload = (PythonLikeFunction) DifferentArgTypeOverloads.TYPE.__getAttributeOrError("overload");
        assertThat(overload.__call__(List.of(instance, PythonString.valueOf("1")), Map.of()))
                .isEqualTo(PythonString.valueOf("1"));

        assertThat(overload.__call__(List.of(instance, PythonInteger.valueOf(2)), Map.of()))
                .isEqualTo(PythonInteger.valueOf(2));
        assertThat(overload.__call__(List.of(instance, PythonInteger.valueOf(3)), Map.of()))
                .isEqualTo(PythonInteger.valueOf(3));

        assertThat(overload.__call__(List.of(instance, PythonBoolean.TRUE), Map.of())).isEqualTo(PythonBoolean.FALSE);
        assertThat(overload.__call__(List.of(instance, PythonBoolean.FALSE), Map.of())).isEqualTo(PythonBoolean.TRUE);
    }

    @Test
    public void testVariousOverloads() throws NoSuchMethodException {
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload")), STRING_TYPE));
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload", PythonString.class)), STRING_TYPE, STRING_TYPE));
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload", PythonInteger.class)), INT_TYPE, INT_TYPE));
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload", PythonString.class, PythonString.class)), STRING_TYPE, STRING_TYPE,
                STRING_TYPE));
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload", PythonInteger.class, PythonInteger.class)), INT_TYPE, INT_TYPE,
                INT_TYPE));
        PythonOverloadImplementor.createDispatchesFor(VariousOverloads.TYPE);

        VariousOverloads instance = new VariousOverloads();
        PythonLikeFunction overload = (PythonLikeFunction) VariousOverloads.TYPE.__getAttributeOrError("overload");
        assertThat(overload.__call__(List.of(instance), Map.of())).isEqualTo(PythonString.valueOf("1"));
        assertThat(overload.__call__(List.of(instance, PythonString.valueOf("a")), Map.of()))
                .isEqualTo(PythonString.valueOf("a 1"));
        assertThat(overload.__call__(List.of(instance, PythonInteger.valueOf(2)), Map.of()))
                .isEqualTo(PythonInteger.valueOf(2));
        assertThat(overload.__call__(List.of(instance, PythonString.valueOf("a"), PythonString.valueOf("b")), Map.of()))
                .isEqualTo(PythonString.valueOf("a b"));
        assertThat(overload.__call__(List.of(instance, PythonInteger.valueOf(1), PythonInteger.valueOf(2)), Map.of()))
                .isEqualTo(PythonInteger.valueOf(3));
    }

    public static class SingleOverload extends AbstractPythonLikeObject {
        static PythonLikeType TYPE = new PythonLikeType("single-overload", SingleOverload.class);

        public SingleOverload() {
            super(TYPE);
        }

        public PythonString overload() {
            return PythonString.valueOf("1");
        }
    }

    public static class DifferentArgCountOverloads extends AbstractPythonLikeObject {
        static PythonLikeType TYPE = new PythonLikeType("different-arg-count-overloads", DifferentArgCountOverloads.class);

        public DifferentArgCountOverloads() {
            super(TYPE);
        }

        public PythonString overload() {
            return PythonString.valueOf("1");
        }

        public PythonInteger overload(PythonInteger arg) {
            return arg;
        }
    }

    public static class DifferentArgTypeOverloads extends AbstractPythonLikeObject {
        static PythonLikeType TYPE = new PythonLikeType("different-arg-type-overloads", DifferentArgTypeOverloads.class);

        public DifferentArgTypeOverloads() {
            super(TYPE);
        }

        public PythonString overload(PythonString string) {
            return string;
        }

        public PythonInteger overload(PythonInteger integer) {
            return integer;
        }

        public PythonBoolean overload(PythonBoolean bool) {
            return bool.not();
        }
    }

    public static class VariousOverloads extends AbstractPythonLikeObject {
        static PythonLikeType TYPE = new PythonLikeType("various-overloads", VariousOverloads.class);

        public VariousOverloads() {
            super(TYPE);
        }

        public PythonString overload() {
            return PythonString.valueOf("1");
        }

        public PythonString overload(PythonString string) {
            return PythonString.valueOf(string.getValue() + " 1");
        }

        public PythonInteger overload(PythonInteger integer) {
            return integer;
        }

        public PythonString overload(PythonString a, PythonString b) {
            return PythonString.valueOf(a.getValue() + " " + b.getValue());
        }

        public PythonInteger overload(PythonInteger a, PythonInteger b) {
            return PythonInteger.valueOf(a.getValue().intValue() + b.getValue().intValue());
        }
    }
}