package org.optaplanner.python.translator.types;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class PythonSlice extends AbstractPythonLikeObject {
    public static PythonLikeType SLICE_TYPE = new PythonLikeType("slice", PythonSlice.class);
    public static PythonLikeType $TYPE = SLICE_TYPE;

    public final PythonLikeObject start;
    public final PythonLikeObject stop;
    public final PythonLikeObject step;

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(SLICE_TYPE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        SLICE_TYPE.setConstructor(((positionalArguments, namedArguments) -> {
            PythonLikeObject start;
            PythonLikeObject stop;
            PythonLikeObject step;

            namedArguments = (namedArguments != null) ? namedArguments : Map.of();

            if (positionalArguments.size() == 3) {
                start = positionalArguments.get(0);
                stop = positionalArguments.get(1);
                step = positionalArguments.get(2);
            } else if (positionalArguments.size() == 2) {
                start = positionalArguments.get(0);
                stop = positionalArguments.get(1);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonNone.INSTANCE);
            } else if (positionalArguments.size() == 1 && namedArguments.containsKey(PythonString.valueOf("stop"))) {
                start = positionalArguments.get(0);
                stop = namedArguments.getOrDefault(PythonString.valueOf("stop"), PythonNone.INSTANCE);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonNone.INSTANCE);
            } else if (positionalArguments.size() == 1) {
                stop = positionalArguments.get(0);
                start = PythonInteger.valueOf(0);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonNone.INSTANCE);
            } else if (positionalArguments.isEmpty()) {
                start = namedArguments.getOrDefault(PythonString.valueOf("start"), PythonInteger.valueOf(0));
                stop = namedArguments.getOrDefault(PythonString.valueOf("stop"), PythonNone.INSTANCE);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonNone.INSTANCE);
            } else {
                throw new ValueError("slice expects 1 to 3 arguments, got " + positionalArguments.size());
            }

            return new PythonSlice(start, stop, step);
        }));

        // Unary
        SLICE_TYPE.addMethod(PythonUnaryOperator.HASH, PythonSlice.class.getMethod("pythonHash"));

        // Binary
        SLICE_TYPE.addMethod(PythonBinaryOperators.EQUAL, PythonSlice.class.getMethod("pythonEquals", PythonSlice.class));

        // Other methods
        SLICE_TYPE.addMethod("indices", PythonSlice.class.getMethod("indices", PythonInteger.class));
    }

    public PythonSlice(PythonLikeObject start, PythonLikeObject stop, PythonLikeObject step) {
        super(SLICE_TYPE);
        this.start = start;
        this.stop = stop;
        this.step = step;

        __setAttribute("start", (start != null) ? start : PythonNone.INSTANCE);
        __setAttribute("stop", (stop != null) ? stop : PythonNone.INSTANCE);
        __setAttribute("step", (step != null) ? step : PythonNone.INSTANCE);
    }

    public PythonLikeTuple indices(PythonInteger sequenceLength) {
        PythonInteger startIndex, stopIndex, strideLength;

        if (start instanceof PythonInteger) {
            startIndex = (PythonInteger) start;
        } else if (start instanceof PythonNone) {
            startIndex = PythonInteger.ZERO;
        } else {
            startIndex = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start);
        }

        if (stop instanceof PythonInteger) {
            stopIndex = (PythonInteger) stop;
        } else if (stop instanceof PythonNone) {
            stopIndex = sequenceLength.subtract(PythonInteger.ONE);
        } else {
            stopIndex = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start);
        }

        if (step instanceof PythonInteger) {
            strideLength = (PythonInteger) step;
        } else if (step instanceof PythonNone) {
            strideLength = PythonInteger.ONE;
        } else {
            strideLength = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start);
        }

        if (startIndex.compareTo(PythonInteger.ZERO) < 0) {
            startIndex = sequenceLength.add(startIndex);
        }

        if (stopIndex.compareTo(PythonInteger.ZERO) < 0) {
            stopIndex = sequenceLength.add(stopIndex);
        }

        if (strideLength.value.intValueExact() == 0) {
            throw new ValueError("stride length cannot be zero");
        }

        return PythonLikeTuple.fromList(List.of(startIndex, stopIndex, strideLength));
    }

    public int getStartIndex(int size) {
        PythonInteger startIndex;

        if (start instanceof PythonInteger) {
            startIndex = (PythonInteger) start;
        } else if (start instanceof PythonNone) {
            startIndex = PythonInteger.ZERO;
        } else {
            startIndex = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start);
        }

        if (startIndex.compareTo(PythonInteger.ZERO) < 0) {
            return size + startIndex.value.intValueExact();
        } else {
            return startIndex.value.intValueExact();
        }
    }

    public int getStopIndex(int size) {
        PythonInteger stopIndex;

        if (stop instanceof PythonInteger) {
            stopIndex = (PythonInteger) stop;
        } else if (stop instanceof PythonNone) {
            stopIndex = PythonInteger.ZERO;
        } else {
            stopIndex = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(stop);
        }

        if (stopIndex.compareTo(PythonInteger.ZERO) < 0) {
            return size + stopIndex.value.intValueExact();
        } else {
            return stopIndex.value.intValueExact();
        }
    }

    public int getStrideLength() {
        PythonInteger strideLength;

        if (step instanceof PythonInteger) {
            strideLength = (PythonInteger) step;
        } else if (step instanceof PythonNone) {
            strideLength = PythonInteger.ONE;
        } else {
            strideLength = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(step);
        }

        int out = strideLength.value.intValueExact();

        if (out == 0) {
            throw new ValueError("stride length cannot be zero");
        }

        return strideLength.value.intValueExact();
    }

    public PythonBoolean pythonEquals(PythonSlice other) {
        return PythonBoolean.valueOf(this.equals(other));
    }

    public PythonInteger pythonHash() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonSlice that = (PythonSlice) o;
        return Objects.equals(start, that.start) && Objects.equals(stop, that.stop) && Objects.equals(step, that.step);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, stop, step);
    }
}
