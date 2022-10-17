package org.optaplanner.jpyinterpreter.types.datetime;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.MethodDescriptor;
import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonFunctionSignature;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeComparable;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonFloat;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.types.numeric.PythonNumber;

/**
 * Python docs: <a href="https://docs.python.org/3/library/datetime.html#timedelta-objects">timedelta-objects</a>
 */
public class PythonTimeDelta extends AbstractPythonLikeObject implements PythonLikeComparable<PythonTimeDelta> {
    private static final int NANOS_IN_SECOND = 1_000_000_000;
    private static final int SECONDS_IN_DAY = 86400; // 24 * 60 * 60

    public static PythonLikeType TIME_DELTA_TYPE = new PythonLikeType("timedelta",
            PythonTimeDelta.class);

    public static PythonLikeType $TYPE = TIME_DELTA_TYPE;

    static {
        try {
            PythonLikeComparable.setup(TIME_DELTA_TYPE);
            registerMethods();

            TIME_DELTA_TYPE.__setAttribute("min", new PythonTimeDelta(Duration.ofDays(-999999999)));
            TIME_DELTA_TYPE.__setAttribute("max", new PythonTimeDelta(Duration.ofDays(1000000000)
                    .minusNanos(1000)));
            TIME_DELTA_TYPE.__setAttribute("resolution", new PythonTimeDelta(Duration.ofNanos(1000)));

            TIME_DELTA_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
                // days=0, seconds=0, microseconds=0, milliseconds=0, minutes=0, hours=0, weeks=0
                namedArguments = (namedArguments != null) ? namedArguments : Map.of();
                PythonNumber days = PythonInteger.ZERO, seconds = PythonInteger.ZERO, microseconds = PythonInteger.ZERO,
                        milliseconds = PythonInteger.ZERO, minutes = PythonInteger.ZERO, hours = PythonInteger.ZERO,
                        weeks = PythonInteger.ZERO;

                if (positionalArguments.size() >= 1) {
                    days = (PythonNumber) positionalArguments.get(0);
                }

                if (positionalArguments.size() >= 2) {
                    seconds = (PythonNumber) positionalArguments.get(1);
                }

                if (positionalArguments.size() >= 3) {
                    microseconds = (PythonNumber) positionalArguments.get(2);
                }

                if (positionalArguments.size() >= 4) {
                    milliseconds = (PythonNumber) positionalArguments.get(3);
                }

                if (positionalArguments.size() >= 5) {
                    minutes = (PythonNumber) positionalArguments.get(4);
                }

                if (positionalArguments.size() >= 6) {
                    hours = (PythonNumber) positionalArguments.get(5);
                }

                if (positionalArguments.size() >= 7) {
                    weeks = (PythonNumber) positionalArguments.get(6);
                }

                PythonString daysKey = PythonString.valueOf("days");
                if (namedArguments.containsKey(daysKey)) {
                    days = ((PythonNumber) namedArguments.get(daysKey));
                }

                PythonString secondsKey = PythonString.valueOf("seconds");
                if (namedArguments.containsKey(secondsKey)) {
                    seconds = ((PythonNumber) namedArguments.get(secondsKey));
                }

                PythonString microsecondsKey = PythonString.valueOf("microseconds");
                if (namedArguments.containsKey(microsecondsKey)) {
                    microseconds = ((PythonNumber) namedArguments.get(microsecondsKey));
                }

                PythonString millisecondsKey = PythonString.valueOf("milliseconds");
                if (namedArguments.containsKey(millisecondsKey)) {
                    milliseconds = ((PythonNumber) namedArguments.get(millisecondsKey));
                }

                PythonString minutesKey = PythonString.valueOf("minutes");
                if (namedArguments.containsKey(minutesKey)) {
                    minutes = ((PythonNumber) namedArguments.get(minutesKey));
                }

                PythonString hoursKey = PythonString.valueOf("hours");
                if (namedArguments.containsKey(hoursKey)) {
                    hours = ((PythonNumber) namedArguments.get(hoursKey));
                }

                PythonString weeksKey = PythonString.valueOf("weeks");
                if (namedArguments.containsKey(weeksKey)) {
                    weeks = ((PythonNumber) namedArguments.get(weeksKey));
                }

                return of(days, seconds, microseconds, milliseconds, minutes, hours, weeks);
            }));

            PythonOverloadImplementor.createDispatchesFor(TIME_DELTA_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        TIME_DELTA_TYPE.addConstructor(new PythonFunctionSignature(
                new MethodDescriptor(PythonTimeDelta.class.getMethod("of", PythonNumber.class, PythonNumber.class,
                        PythonNumber.class, PythonNumber.class, PythonNumber.class,
                        PythonNumber.class, PythonNumber.class)),
                List.of(PythonInteger.ZERO, PythonInteger.ZERO, PythonInteger.ZERO, PythonInteger.ZERO, PythonInteger.ZERO,
                        PythonInteger.ZERO, PythonInteger.ZERO),
                Map.of("days", 0, "seconds", 1, "microseconds", 2,
                        "milliseconds", 3, "minutes", 4, "hours", 5,
                        "weeks", 6),
                TIME_DELTA_TYPE, PythonNumber.NUMBER_TYPE, PythonNumber.NUMBER_TYPE, PythonNumber.NUMBER_TYPE,
                PythonNumber.NUMBER_TYPE, PythonNumber.NUMBER_TYPE,
                PythonNumber.NUMBER_TYPE, PythonNumber.NUMBER_TYPE));

        // Unary
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.POSITIVE,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("pos")),
                        TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.NEGATIVE,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("negate")),
                        TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.ABS,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("abs")),
                        TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.AS_BOOLEAN,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("isZero")),
                        BuiltinTypes.BOOLEAN_TYPE));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("toPythonString")),
                        BuiltinTypes.STRING_TYPE));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                new PythonFunctionSignature(new MethodDescriptor(PythonTimeDelta.class.getMethod("toPythonRepr")),
                        BuiltinTypes.STRING_TYPE));

        // Binary
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.ADD,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("add_time_delta", PythonTimeDelta.class)),
                        TIME_DELTA_TYPE, TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("subtract_time_delta", PythonTimeDelta.class)),
                        TIME_DELTA_TYPE, TIME_DELTA_TYPE));

        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.MULTIPLY,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("get_integer_multiple", PythonInteger.class)),
                        TIME_DELTA_TYPE, BuiltinTypes.INT_TYPE));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.MULTIPLY,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("get_float_multiple", PythonFloat.class)),
                        TIME_DELTA_TYPE, BuiltinTypes.FLOAT_TYPE));

        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("divide_time_delta", PythonTimeDelta.class)),
                        BuiltinTypes.FLOAT_TYPE, TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("divide_integer", PythonInteger.class)),
                        TIME_DELTA_TYPE, BuiltinTypes.INT_TYPE));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.TRUE_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("divide_float", PythonFloat.class)),
                        TIME_DELTA_TYPE, BuiltinTypes.FLOAT_TYPE));

        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("floor_divide_time_delta", PythonTimeDelta.class)),
                        BuiltinTypes.INT_TYPE, TIME_DELTA_TYPE));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.FLOOR_DIVIDE,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("floor_divide_integer", PythonInteger.class)),
                        TIME_DELTA_TYPE, BuiltinTypes.INT_TYPE));

        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperators.MODULO,
                new PythonFunctionSignature(
                        new MethodDescriptor(PythonTimeDelta.class.getMethod("remainder_time_delta", PythonTimeDelta.class)),
                        TIME_DELTA_TYPE, TIME_DELTA_TYPE));

        // Methods
        TIME_DELTA_TYPE.addMethod("total_seconds", new PythonFunctionSignature(
                new MethodDescriptor(PythonTimeDelta.class.getMethod("total_seconds")),
                BuiltinTypes.FLOAT_TYPE));
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

    public static PythonTimeDelta of(PythonNumber days, PythonNumber seconds, PythonNumber microseconds,
            PythonNumber milliseconds, PythonNumber minutes, PythonNumber hours,
            PythonNumber weeks) {
        Duration out = Duration.ZERO;
        out = addToDuration(out, days, ChronoUnit.DAYS);
        out = addToDuration(out, seconds, ChronoUnit.SECONDS);
        out = addToDuration(out, microseconds, ChronoUnit.MICROS);
        out = addToDuration(out, milliseconds, ChronoUnit.MILLIS);
        out = addToDuration(out, minutes, ChronoUnit.MINUTES);
        out = addToDuration(out, hours, ChronoUnit.HOURS);
        if (weeks instanceof PythonInteger) { // weeks is an estimated duration; cannot use addToDuration
            out = out.plusDays(weeks.getValue().longValue() * 7);
        } else if (weeks instanceof PythonFloat) {
            out = out.plusNanos(Math.round(Duration.ofDays(7L).toNanos() * weeks.getValue().doubleValue()));
        } else {
            throw new IllegalArgumentException("Amount for weeks is not a float or integer.");
        }
        return new PythonTimeDelta(out);
    }

    private static Duration addToDuration(Duration duration, PythonNumber amount, TemporalUnit temporalUnit) {
        if (amount instanceof PythonInteger) {
            return duration.plus(amount.getValue().longValue(), temporalUnit);
        } else if (amount instanceof PythonFloat) {
            return duration.plusNanos(Math.round(temporalUnit.getDuration().toNanos() * amount.getValue().doubleValue()));
        } else {
            throw new IllegalArgumentException("Amount for " + temporalUnit.toString() + " is not a float or integer.");
        }
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
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public int compareTo(PythonTimeDelta pythonTimeDelta) {
        return duration.compareTo(pythonTimeDelta.duration);
    }
}
