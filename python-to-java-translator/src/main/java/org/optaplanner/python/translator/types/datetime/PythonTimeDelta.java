package org.optaplanner.python.translator.types.datetime;

import static org.optaplanner.python.translator.types.PythonBoolean.BOOLEAN_TYPE;
import static org.optaplanner.python.translator.types.PythonFloat.FLOAT_TYPE;
import static org.optaplanner.python.translator.types.PythonInteger.INT_TYPE;
import static org.optaplanner.python.translator.types.PythonString.STRING_TYPE;

import java.time.Duration;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonBoolean;
import org.optaplanner.python.translator.types.PythonFloat;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonLikeComparable;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;

/**
 * Python docs: <a href="https://docs.python.org/3/library/datetime.html#timedelta-objects">timedelta-objects</a>
 */
public class PythonTimeDelta extends AbstractPythonLikeObject implements Comparable<PythonTimeDelta> {
    private static final int NANOS_IN_SECOND = 1_000_000_000;
    private static final int SECONDS_IN_DAY = 86400; // 24 * 60 * 60

    public static PythonLikeType TIME_DELTA_TYPE = new PythonLikeType("timedelta",
            PythonTimeDelta.class);

    static {
        try {
            PythonLikeComparable.setup(TIME_DELTA_TYPE.__dir__);
            registerMethods();

            TIME_DELTA_TYPE.__setAttribute("min", new PythonTimeDelta(Duration.ofDays(-999999999)));
            TIME_DELTA_TYPE.__setAttribute("max", new PythonTimeDelta(Duration.ofDays(1000000000)
                    .minusNanos(1000)));
            TIME_DELTA_TYPE.__setAttribute("resolution", new PythonTimeDelta(Duration.ofNanos(1000)));

            TIME_DELTA_TYPE.setConstructor(((positionalArguments, namedArguments) -> {
                // days=0, seconds=0, microseconds=0, milliseconds=0, minutes=0, hours=0, weeks=0
                long days = 0L, seconds = 0L, microseconds = 0L, milliseconds = 0L, minutes = 0L, hours = 0L, weeks = 0L;

                if (positionalArguments.size() >= 1) {
                    days = ((PythonInteger) positionalArguments.get(0)).getValue().longValue();
                }

                if (positionalArguments.size() >= 2) {
                    seconds = ((PythonInteger) positionalArguments.get(1)).getValue().longValue();
                }

                if (positionalArguments.size() >= 3) {
                    microseconds = ((PythonInteger) positionalArguments.get(2)).getValue().longValue();
                }

                if (positionalArguments.size() >= 4) {
                    milliseconds = ((PythonInteger) positionalArguments.get(3)).getValue().longValue();
                }

                if (positionalArguments.size() >= 5) {
                    minutes = ((PythonInteger) positionalArguments.get(4)).getValue().longValue();
                }

                if (positionalArguments.size() >= 6) {
                    hours = ((PythonInteger) positionalArguments.get(5)).getValue().longValue();
                }

                if (positionalArguments.size() >= 7) {
                    weeks = ((PythonInteger) positionalArguments.get(6)).getValue().longValue();
                }

                PythonString daysKey = PythonString.valueOf("days");
                if (namedArguments.containsKey(daysKey)) {
                    days = ((PythonInteger) namedArguments.get(daysKey)).getValue().longValue();
                }

                PythonString secondsKey = PythonString.valueOf("seconds");
                if (namedArguments.containsKey(secondsKey)) {
                    seconds = ((PythonInteger) namedArguments.get(secondsKey)).getValue().longValue();
                }

                PythonString microsecondsKey = PythonString.valueOf("microseconds");
                if (namedArguments.containsKey(microsecondsKey)) {
                    microseconds = ((PythonInteger) namedArguments.get(microsecondsKey)).getValue().longValue();
                }

                PythonString millisecondsKey = PythonString.valueOf("milliseconds");
                if (namedArguments.containsKey(millisecondsKey)) {
                    milliseconds = ((PythonInteger) namedArguments.get(millisecondsKey)).getValue().longValue();
                }

                PythonString minutesKey = PythonString.valueOf("minutes");
                if (namedArguments.containsKey(minutesKey)) {
                    minutes = ((PythonInteger) namedArguments.get(minutesKey)).getValue().longValue();
                }

                PythonString hoursKey = PythonString.valueOf("hours");
                if (namedArguments.containsKey(hoursKey)) {
                    hours = ((PythonInteger) namedArguments.get(hoursKey)).getValue().longValue();
                }

                PythonString weeksKey = PythonString.valueOf("weeks");
                if (namedArguments.containsKey(weeksKey)) {
                    weeks = ((PythonInteger) namedArguments.get(weeksKey)).getValue().longValue();
                }

                return new PythonTimeDelta(Duration.ofDays(days)
                        .plusSeconds(seconds)
                        .plusNanos(microseconds * 1000L)
                        .plusMillis(milliseconds)
                        .plusMinutes(minutes)
                        .plusHours(hours)
                        .plusDays(7L * weeks));
            }));

            PythonOverloadImplementor.createDispatchesFor(TIME_DELTA_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Unary
        TIME_DELTA_TYPE.addMethod(PythonUnaryOperator.POSITIVE,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("pos")),
                        TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonUnaryOperator.NEGATIVE,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("negate")),
                        TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonUnaryOperator.ABS,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("abs")),
                        TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonUnaryOperator.AS_BOOLEAN,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("isZero")),
                        BOOLEAN_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonUnaryOperator.AS_STRING,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("toPythonString")),
                        STRING_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonUnaryOperator.REPRESENTATION,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("toPythonRepr")),
                        STRING_TYPE));

        // Binary
        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.ADD,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("add_time_delta", PythonTimeDelta.class)),
                        TIME_DELTA_TYPE, TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("subtract_time_delta", PythonTimeDelta.class)),
                        TIME_DELTA_TYPE, TIME_DELTA_TYPE));

        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.MULTIPLY,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("get_integer_multiple", PythonInteger.class)),
                        TIME_DELTA_TYPE, INT_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.MULTIPLY,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("get_float_multiple", PythonFloat.class)),
                        TIME_DELTA_TYPE, FLOAT_TYPE));

        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("divide_time_delta", PythonTimeDelta.class)),
                        FLOAT_TYPE, TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("divide_integer", PythonInteger.class)),
                        TIME_DELTA_TYPE, INT_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("divide_float", PythonFloat.class)),
                        TIME_DELTA_TYPE, FLOAT_TYPE));

        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("floor_divide_time_delta", PythonTimeDelta.class)),
                        INT_TYPE, TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("floor_divide_integer", PythonInteger.class)),
                        TIME_DELTA_TYPE, INT_TYPE));

        TIME_DELTA_TYPE.addMethod(PythonBinaryOperators.MODULO,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("remainder_time_delta", PythonTimeDelta.class)),
                        TIME_DELTA_TYPE, TIME_DELTA_TYPE));
    }

    final Duration duration;

    public final PythonInteger days;
    public final PythonInteger seconds;
    public final PythonInteger microseconds;

    public PythonTimeDelta(Duration duration) {
        super(TIME_DELTA_TYPE);
        this.duration = duration;

        days = PythonInteger.valueOf(duration.toDays());
        seconds = PythonInteger.valueOf(Math.abs(duration.toSeconds() % SECONDS_IN_DAY));
        microseconds = PythonInteger.valueOf(duration.toNanosPart() / 1000);
    }

    public static PythonTimeDelta of(int days, int seconds, int microseconds) {
        return new PythonTimeDelta(Duration.ofDays(days).plusSeconds(seconds)
                .plusNanos(microseconds * 1000L));
    }

    public PythonFloat total_seconds() {
        return PythonFloat.valueOf((double) duration.toNanos() / NANOS_IN_SECOND);
    }

    public PythonTimeDelta add_time_delta(PythonTimeDelta other) {
        return new PythonTimeDelta(duration.plus(other.duration));
    }

    public PythonTimeDelta subtract_time_delta(PythonTimeDelta other) {
        return new PythonTimeDelta(duration.minus(other.duration));
    }

    public PythonTimeDelta get_integer_multiple(PythonInteger multiple) {
        return new PythonTimeDelta(duration.multipliedBy(multiple.getValue().longValue()));
    }

    public PythonTimeDelta get_float_multiple(PythonFloat multiple) {
        double multipleAsDouble = multiple.getValue().doubleValue();
        long flooredMultiple = (long) Math.floor(multipleAsDouble);
        double fractionalPart = multipleAsDouble - flooredMultiple;
        long nanos = duration.toNanos();
        double fractionalNanos = fractionalPart * nanos;
        long fractionalNanosInMicroResolution = Math.round(fractionalNanos / 1000) * 1000;

        return new PythonTimeDelta(duration.multipliedBy(flooredMultiple)
                .plus(Duration.ofNanos(fractionalNanosInMicroResolution)));
    }

    public PythonFloat divide_time_delta(PythonTimeDelta divisor) {
        return PythonFloat.valueOf((double) duration.toNanos() / divisor.duration.toNanos());
    }

    public PythonTimeDelta divide_integer(PythonInteger divisor) {
        return new PythonTimeDelta(duration.dividedBy(divisor.getValue().longValue()));
    }

    public PythonTimeDelta divide_float(PythonFloat divisor) {
        double fractionalNanos = duration.toNanos() / divisor.getValue().doubleValue();
        return new PythonTimeDelta(Duration.ofNanos(Math.round(fractionalNanos / 1000) * 1000));
    }

    public PythonInteger floor_divide_time_delta(PythonTimeDelta divisor) {
        return PythonInteger.valueOf(duration.dividedBy(divisor.duration));
    }

    public PythonTimeDelta floor_divide_integer(PythonInteger divisor) {
        return new PythonTimeDelta(duration.dividedBy(divisor.getValue().longValue()));
    }

    public PythonTimeDelta remainder_time_delta(PythonTimeDelta divisor) {
        return new PythonTimeDelta(duration.minus(divisor.duration.multipliedBy(
                duration.dividedBy(divisor.duration))));
    }

    public PythonTimeDelta pos() {
        return this;
    }

    public PythonTimeDelta negate() {
        return new PythonTimeDelta(duration.negated());
    }

    public PythonTimeDelta abs() {
        return new PythonTimeDelta(duration.abs());
    }

    public PythonString toPythonString() {
        return new PythonString(toString());
    }

    public PythonString toPythonRepr() {
        return PythonString.valueOf(
                "datetime.timedelta(days=" + days + ", seconds=" + seconds + ", " + "microseconds=" + microseconds + ")");
    }

    public PythonBoolean isZero() {
        return PythonBoolean.valueOf(duration.isZero());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        long daysPart = duration.toDaysPart();

        if (daysPart != 0) {
            out.append(daysPart);
            out.append(" day");
            if (daysPart > 1 || daysPart < -1) {
                out.append('s');
            }
            out.append(", ");
        }
        int hours = Math.abs(duration.toHoursPart());
        out.append(hours);
        out.append(':');

        int minutes = Math.abs(duration.toMinutesPart());
        out.append(String.format("%02d", minutes));
        out.append(':');

        int seconds = Math.abs(duration.toSecondsPart());
        out.append(String.format("%02d", seconds));

        int micros = duration.toNanosPart() / 1000;
        if (micros != 0) {
            out.append(String.format(".%06d", micros));
        }

        return out.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonTimeDelta that = (PythonTimeDelta) o;
        return duration.equals(that.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }

    @Override
    public int compareTo(PythonTimeDelta pythonTimeDelta) {
        return duration.compareTo(pythonTimeDelta.duration);
    }
}
