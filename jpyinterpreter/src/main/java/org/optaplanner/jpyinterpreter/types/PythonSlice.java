package org.optaplanner.jpyinterpreter.types;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.builtins.UnaryDunderBuiltin;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.types.errors.ValueError;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;

public class PythonSlice extends AbstractPythonLikeObject {
    public static PythonLikeType SLICE_TYPE = new PythonLikeType("slice", PythonSlice.class);
    public static PythonLikeType $TYPE = SLICE_TYPE;

    public final PythonLikeObject start;
    public final PythonLikeObject stop;
    public final PythonLikeObject step;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonSlice::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        SLICE_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
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
        SLICE_TYPE.addUnaryMethod(PythonUnaryOperator.HASH, PythonSlice.class.getMethod("pythonHash"));

        // Binary
        SLICE_TYPE.addBinaryMethod(PythonBinaryOperators.EQUAL, PythonSlice.class.getMethod("pythonEquals", PythonSlice.class));

        // Other methods
        SLICE_TYPE.addMethod("indices", PythonSlice.class.getMethod("indices", PythonInteger.class));

        return SLICE_TYPE;
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

    /**
     * Convert index into a index for a sequence of length {@code length}. May be outside the range
     * [0, length - 1]. Use for indexing into a sequence.
     *
     * @param index The given index
     * @param length The length
     * @return index, if index in [0, length -1]; length - index, if index < 0.
     */
    public static int asIntIndexForLength(PythonInteger index, int length) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            return length + indexAsInt;
        } else {
            return indexAsInt;
        }
    }

    /**
     * Convert index into a VALID start index for a sequence of length {@code length}. bounding it to the
     * range [0, length - 1]. Use for sequence operations that need to search an portion of a sequence.
     *
     * @param index The given index
     * @param length The length
     * @return index, if index in [0, length -1]; length - index, if index < 0 and -index <= length;
     *         otherwise 0 (if the index represent a position before 0) or length - 1 (if the index represent a
     *         position after the sequence).
     */
    public static int asValidStartIntIndexForLength(PythonInteger index, int length) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            return Math.max(0, Math.min(length - 1, length + indexAsInt));
        } else {
            return Math.max(0, Math.min(length - 1, indexAsInt));
        }
    }

    /**
     * Convert index into a VALID end index for a sequence of length {@code length}. bounding it to the
     * range [0, length]. Use for sequence operations that need to search an portion of a sequence.
     *
     * @param index The given index
     * @param length The length
     * @return index, if index in [0, length]; length - index, if index < 0 and -index <= length + 1;
     *         otherwise 0 (if the index represent a position before 0) or length (if the index represent a
     *         position after the sequence).
     */
    public static int asValidEndIntIndexForLength(PythonInteger index, int length) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            return Math.max(0, Math.min(length, length + indexAsInt));
        } else {
            return Math.max(0, Math.min(length, indexAsInt));
        }
    }

    public PythonLikeTuple indices(PythonInteger sequenceLength) {
        PythonInteger startIndex, stopIndex, strideLength;
        boolean isReversed = false;

        if (this.step == PythonNone.INSTANCE) {
            strideLength = PythonInteger.ONE;
        } else if (this.step instanceof PythonInteger) {
            strideLength = (PythonInteger) this.step;
            isReversed = strideLength.compareTo(PythonInteger.ZERO) < 0;
        } else {
            strideLength = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start);
            isReversed = strideLength.compareTo(PythonInteger.ZERO) < 0;
        }

        if (strideLength.value.intValueExact() == 0) {
            throw new ValueError("stride length cannot be zero");
        }

        if (start instanceof PythonInteger) {
            startIndex = (PythonInteger) start;
        } else if (start == PythonNone.INSTANCE) {
            startIndex = isReversed ? PythonInteger.valueOf(sequenceLength.value.subtract(BigInteger.ONE)) : PythonInteger.ZERO;
        } else {
            startIndex = ((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start));
        }

        if (startIndex.compareTo(PythonInteger.ZERO) < 0) {
            startIndex = sequenceLength.add(startIndex);
        }

        if (!isReversed && startIndex.value.compareTo(sequenceLength.value) > 0) {
            startIndex = sequenceLength;
        } else if (isReversed && startIndex.value.compareTo(sequenceLength.value.subtract(BigInteger.ONE)) > 0) {
            startIndex = PythonInteger.valueOf(sequenceLength.value.subtract(BigInteger.ONE));
        }

        if (stop instanceof PythonInteger) {
            stopIndex = (PythonInteger) stop;
        } else if (stop == PythonNone.INSTANCE) {
            stopIndex =
                    isReversed ? PythonInteger.valueOf(sequenceLength.value.negate().subtract(BigInteger.ONE)) : sequenceLength;
        } else {
            stopIndex = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(stop);
        }

        if (stopIndex.compareTo(PythonInteger.ZERO) < 0) {
            stopIndex = sequenceLength.add(stopIndex);
        }

        if (!isReversed && stopIndex.value.compareTo(sequenceLength.value) > 0) {
            stopIndex = sequenceLength;
        } else if (isReversed && stopIndex.value.compareTo(sequenceLength.value.subtract(BigInteger.ONE)) > 0) {
            stopIndex = PythonInteger.valueOf(sequenceLength.value.subtract(BigInteger.ONE));
        }

        return PythonLikeTuple.fromList(List.of(startIndex, stopIndex, strideLength));
    }

    public int getStartIndex(int length) {
        int startIndex;
        boolean isReversed = getStrideLength() < 0;

        if (start instanceof PythonInteger) {
            startIndex = ((PythonInteger) start).value.intValueExact();
        } else if (start == PythonNone.INSTANCE) {
            startIndex = isReversed ? length - 1 : 0;
        } else {
            startIndex = ((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start)).value.intValueExact();
        }

        if (startIndex < 0) {
            startIndex = length + startIndex;
        }

        if (!isReversed && startIndex > length) {
            startIndex = length;
        } else if (isReversed && startIndex > length - 1) {
            startIndex = length - 1;
        }

        return startIndex;
    }

    public int getStopIndex(int length) {
        int stopIndex;
        boolean isReversed = getStrideLength() < 0;

        if (stop instanceof PythonInteger) {
            stopIndex = ((PythonInteger) stop).value.intValueExact();
        } else if (stop == PythonNone.INSTANCE) {
            stopIndex = isReversed ? -length - 1 : length; // use -length - 1 so length - stopIndex = -1
        } else {
            stopIndex = ((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(stop)).value.intValueExact();
        }

        if (stopIndex < 0) {
            stopIndex = length + stopIndex;
        }

        if (!isReversed && stopIndex > length) {
            stopIndex = length;
        } else if (isReversed && stopIndex > length - 1) {
            stopIndex = length - 1;
        }

        return stopIndex;
    }

    public int getStrideLength() {
        PythonInteger strideLength;

        if (step instanceof PythonInteger) {
            strideLength = (PythonInteger) step;
        } else if (step == PythonNone.INSTANCE) {
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

    public void iterate(int length, SliceConsumer consumer) {
        int startIndex, stopIndex, strideLength;
        boolean isReversed = false;

        if (this.step == PythonNone.INSTANCE) {
            strideLength = 1;
        } else if (this.step instanceof PythonInteger) {
            strideLength = ((PythonInteger) this.step).value.intValueExact();
            isReversed = strideLength < 0;
        } else {
            strideLength = ((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start)).value.intValueExact();
            isReversed = strideLength < 0;
        }

        if (strideLength == 0) {
            throw new ValueError("stride length cannot be zero");
        }

        if (start instanceof PythonInteger) {
            startIndex = ((PythonInteger) start).value.intValueExact();
        } else if (start == PythonNone.INSTANCE) {
            startIndex = isReversed ? length - 1 : 0;
        } else {
            startIndex = ((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start)).value.intValueExact();
        }

        if (startIndex < 0) {
            startIndex = length + startIndex;
        }

        if (!isReversed && startIndex > length) {
            startIndex = length;
        } else if (isReversed && startIndex > length - 1) {
            startIndex = length - 1;
        }

        if (stop instanceof PythonInteger) {
            stopIndex = ((PythonInteger) stop).value.intValueExact();
        } else if (stop == PythonNone.INSTANCE) {
            stopIndex = isReversed ? -length - 1 : length; // use -length - 1 so length - stopIndex = -1
        } else {
            stopIndex = ((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(stop)).value.intValueExact();
        }

        if (stopIndex < 0) {
            stopIndex = length + stopIndex;
        }

        if (!isReversed && stopIndex > length) {
            stopIndex = length;
        } else if (isReversed && stopIndex > length - 1) {
            stopIndex = length - 1;
        }

        int step = 0;
        if (isReversed) {
            for (int i = startIndex; i > stopIndex; i += strideLength) {
                consumer.accept(i, step);
                step++;
            }
        } else {
            for (int i = startIndex; i < stopIndex; i += strideLength) {
                consumer.accept(i, step);
                step++;
            }
        }
    }

    private boolean isReversed() {
        return getStrideLength() < 0;
    }

    public int getSliceSize(int length) {
        int start = getStartIndex(length);
        int stop = getStopIndex(length);
        int span = stop - start;
        int strideLength = getStrideLength();

        // ceil division
        return span / strideLength + (span % strideLength == 0 ? 0 : 1);
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

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    public interface SliceConsumer {
        void accept(int index, int step);
    }
}
