package org.optaplanner.jpyinterpreter.types.datetime;

import static org.optaplanner.jpyinterpreter.types.datetime.PythonTimeDelta.TIME_DELTA_TYPE;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.optaplanner.jpyinterpreter.MethodDescriptor;
import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonFunctionSignature;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeComparable;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.errors.ValueError;
import org.optaplanner.jpyinterpreter.types.numeric.PythonFloat;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.util.arguments.ArgumentSpec;

/**
 * Python docs: <a href="https://docs.python.org/3/library/datetime.html#datetime.date">date objects</a>
 */
public class PythonDate<T extends PythonDate<?>> extends AbstractPythonLikeObject implements PythonLikeComparable<T> {
    static final long EPOCH_ORDINAL_OFFSET = Duration.between(LocalDateTime.of(LocalDate.of(0, 12, 31), LocalTime.MIDNIGHT),
            LocalDateTime.of(LocalDate.ofEpochDay(0), LocalTime.MIDNIGHT)).toDays();

    // Ex: Wed Jun  9 04:26:40 1993
    static final DateTimeFormatter C_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM ppd HH:mm:ss yyyy");
    public static PythonLikeType DATE_TYPE = new PythonLikeType("date",
            PythonDate.class);

    public static PythonLikeType $TYPE = DATE_TYPE;

    static {
        try {
            PythonLikeComparable.setup(DATE_TYPE);
            registerMethods();

            DATE_TYPE.__setAttribute("min", new PythonDate(LocalDate.of(1, 1, 1)));
            DATE_TYPE.__setAttribute("max", new PythonDate(LocalDate.of(9999, 12, 31)));
            DATE_TYPE.__setAttribute("resolution", new PythonTimeDelta(Duration.ofDays(1)));

            PythonOverloadImplementor.createDispatchesFor(DATE_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        DATE_TYPE.setConstructor(ArgumentSpec.forFunctionReturning("date", PythonDate.class)
                .addArgument("year", PythonInteger.class)
                .addArgument("month", PythonInteger.class)
                .addArgument("day", PythonInteger.class)
                .asStaticPythonLikeFunction((year, month, day) -> of(year.value.intValueExact(),
                        month.value.intValueExact(),
                        day.value.intValueExact())));
        // Unary Operators
        DATE_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("toPythonString")),
                        BuiltinTypes.STRING_TYPE));

        // Binary Operators
        DATE_TYPE.addBinaryMethod(PythonBinaryOperators.ADD,
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("add_time_delta", PythonTimeDelta.class)),
                        DATE_TYPE, TIME_DELTA_TYPE));
        DATE_TYPE.addBinaryMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("subtract_time_delta", PythonTimeDelta.class)),
                        DATE_TYPE, TIME_DELTA_TYPE));
        DATE_TYPE.addBinaryMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("subtract_date", PythonDate.class)),
                        TIME_DELTA_TYPE, DATE_TYPE));
        DATE_TYPE.addMethod("replace",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("replace", PythonInteger.class, PythonInteger.class, PythonInteger.class)),
                        DATE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE));
        DATE_TYPE.addMethod("timetuple",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("timetuple")),
                        BuiltinTypes.TUPLE_TYPE)); // TODO: use time.struct_time type

        DATE_TYPE.addMethod("toordinal",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("to_ordinal")),
                        BuiltinTypes.INT_TYPE));
        DATE_TYPE.addMethod("weekday",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("weekday")),
                        BuiltinTypes.INT_TYPE));

        DATE_TYPE.addMethod("isoweekday",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("iso_weekday")),
                        BuiltinTypes.INT_TYPE));

        DATE_TYPE.addMethod("isoweekday",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("iso_weekday")),
                        BuiltinTypes.INT_TYPE));

        // TODO: isocalendar
        DATE_TYPE.addMethod("isoformat",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("iso_format")),
                        BuiltinTypes.STRING_TYPE));

        DATE_TYPE.addMethod("ctime",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDate.class.getMethod("ctime")),
                        BuiltinTypes.STRING_TYPE));

        // Static methods
        DATE_TYPE.__setAttribute("fromordinal",
                ArgumentSpec.forFunctionReturning("fromordinal", PythonDate.class)
                        .addArgument("ordinal", PythonInteger.class)
                        .asStaticPythonLikeFunction(PythonDate::from_ordinal));

    }

    final LocalDate localDate;

    public final PythonInteger year;
    public final PythonInteger month;
    public final PythonInteger day;

    public PythonDate(LocalDate localDate) {
        this(DATE_TYPE, localDate);
    }

    public PythonDate(PythonLikeType type, LocalDate localDate) {
        super(type);
        this.localDate = localDate;

        this.year = PythonInteger.valueOf(localDate.getYear());
        this.month = PythonInteger.valueOf(localDate.getMonthValue());
        this.day = PythonInteger.valueOf(localDate.getDayOfMonth());
    }

    public static PythonDate of(int year, int month, int day) {
        if (month < 1 || month > 12) {
            throw new ValueError("month must be between 1 and 12");
        }
        if (!YearMonth.of(year, month).isValidDay(day)) {
            throw new ValueError("day must be between 1 and " + YearMonth.of(year, month).lengthOfMonth());
        }
        return new PythonDate(LocalDate.of(year, month, day));
    }

    @Override
    public PythonLikeObject __getAttributeOrNull(String name) {
        switch (name) {
            case "year":
                return year;
            case "month":
                return month;
            case "day":
                return day;
            default:
                return super.__getAttributeOrNull(name);
        }
    }

    public static PythonDate today() {
        return new PythonDate(LocalDate.now());
    }

    public static PythonDate from_timestamp(PythonInteger timestamp) {
        return new PythonDate(LocalDate.ofInstant(Instant.ofEpochSecond(timestamp.getValue().longValue()),
                ZoneOffset.UTC));
    }

    public static PythonDate from_timestamp(PythonFloat timestamp) {
        return new PythonDate(LocalDate.ofInstant(Instant.ofEpochMilli(
                Math.round(timestamp.getValue().doubleValue() * 1000)),
                ZoneOffset.UTC));
    }

    public static PythonDate from_ordinal(PythonInteger ordinal) {
        return new PythonDate(LocalDate.ofEpochDay(ordinal.getValue().longValue() - EPOCH_ORDINAL_OFFSET));
    }

    public static PythonDate from_iso_format(PythonString dateString) {
        return new PythonDate(LocalDate.parse(dateString.getValue()));
    }

    public static PythonDate from_iso_calendar(PythonInteger year, PythonInteger week, PythonInteger day) {
        int isoYear = year.getValue().intValue();
        int dayInIsoYear = (week.getValue().intValue() * 7) + day.getValue().intValue();
        int correction = LocalDate.of(isoYear, 1, 4).getDayOfWeek().getValue() + 3;
        int ordinalDate = dayInIsoYear - correction;
        if (ordinalDate <= 0) {
            int daysInYear = LocalDate.ofYearDay(isoYear - 1, 1).lengthOfYear();
            return new PythonDate(LocalDate.ofYearDay(isoYear - 1, ordinalDate + daysInYear));
        } else if (ordinalDate > LocalDate.ofYearDay(isoYear, 1).lengthOfYear()) {
            int daysInYear = LocalDate.ofYearDay(isoYear, 1).lengthOfYear();
            return new PythonDate(LocalDate.ofYearDay(isoYear + 1, ordinalDate - daysInYear));
        } else {
            return new PythonDate(LocalDate.ofYearDay(isoYear, ordinalDate));
        }
    }

    public PythonDate add_time_delta(PythonTimeDelta summand) {
        return new PythonDate(localDate.plusDays(summand.duration.toDays()));
    }

    public PythonDate subtract_time_delta(PythonTimeDelta subtrahend) {
        return new PythonDate(localDate.minusDays(subtrahend.duration.toDays()));
    }

    public PythonTimeDelta subtract_date(PythonDate subtrahend) {
        return new PythonTimeDelta(Duration.ofDays(localDate.toEpochDay() - subtrahend.localDate.toEpochDay()));
    }

    public PythonDate replace(PythonInteger year, PythonInteger month, PythonInteger day) {
        if (year == null) {
            year = this.year;
        }

        if (month == null) {
            month = this.month;
        }

        if (day == null) {
            day = this.day;
        }

        return new PythonDate(LocalDate.of(year.getValue().intValue(),
                month.getValue().intValue(),
                day.getValue().intValue()));
    }

    public Object timetuple() {
        throw new UnsupportedOperationException(); // TODO https://docs.python.org/3/library/datetime.html#datetime.date.timetuple
    }

    public PythonInteger to_ordinal() {
        return PythonInteger.valueOf(localDate.toEpochDay() + EPOCH_ORDINAL_OFFSET);
    }

    public PythonInteger weekday() {
        return PythonInteger.valueOf(localDate.getDayOfWeek().getValue() - 1);
    }

    public PythonInteger iso_weekday() {
        return PythonInteger.valueOf(localDate.getDayOfWeek().getValue());
    }

    public PythonString iso_format() {
        return new PythonString(localDate.toString());
    }

    public PythonString toPythonString() {
        return new PythonString(toString());
    }

    public PythonString ctime() {
        return new PythonString(localDate.atStartOfDay().format(C_TIME_FORMATTER).replaceAll("(\\D)\\.", "$1"));
    }

    public PythonString strftime(PythonString format) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(T date) {
        return localDate.compareTo(date.localDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonDate<?> that = (PythonDate<?>) o;
        return localDate.equals(that.localDate);
    }

    @Override
    public int hashCode() {
        return localDate.hashCode();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
