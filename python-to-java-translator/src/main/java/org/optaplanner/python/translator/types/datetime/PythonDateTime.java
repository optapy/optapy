package org.optaplanner.python.translator.types.datetime;

import static org.optaplanner.python.translator.types.PythonFloat.FLOAT_TYPE;
import static org.optaplanner.python.translator.types.PythonInteger.INT_TYPE;
import static org.optaplanner.python.translator.types.PythonLikeTuple.TUPLE_TYPE;
import static org.optaplanner.python.translator.types.PythonString.STRING_TYPE;
import static org.optaplanner.python.translator.types.datetime.PythonTimeDelta.TIME_DELTA_TYPE;
import static org.optaplanner.python.translator.types.datetime.PythonTzinfo.TZ_INFO_TYPE;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.JavaMethodReference;
import org.optaplanner.python.translator.types.PythonFloat;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonLikeComparable;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.PythonString;

/**
 * Python docs: <a href="https://docs.python.org/3/library/datetime.html#datetime.datetime">datetime objects</a>
 */
public class PythonDateTime extends PythonDate<PythonDateTime> {
    // Taken from https://docs.python.org/3/library/datetime.html#datetime.datetime.fromisoformat
    private static final Pattern ISO_FORMAT_PATTERN = Pattern.compile("^(?<year>\\d\\d\\d\\d)-(?<month>\\d\\d)-(?<day>\\d\\d)" +
            "(:.(?<hour>\\d\\d)" +
            "(::(?<minute>\\d\\d)" +
            "(::(?<second>\\d\\d)" +
            "(:\\.(?<microHigh>\\d\\d\\d)" +
            "(?<microLow>\\d\\d\\d)?" +
            ")?)?)?)?" +
            "(:(?<timezoneHour>\\d\\d):(?<timezoneMinute>\\d\\d)" +
            "(::(?<timezoneSecond>\\d\\d)" +
            "(:\\.(?<timezoneMicro>\\d\\d\\d\\d\\d\\d)" +
            ")?)?)?$");

    private static int NANOS_PER_SECOND = 1_000_000_000;
    public static PythonLikeType DATE_TIME_TYPE = new PythonLikeType("datetime",
            PythonDateTime.class,
            List.of(DATE_TYPE));

    static {
        try {
            PythonLikeComparable.setup(DATE_TIME_TYPE.__dir__);
            registerMethods();

            DATE_TIME_TYPE.__setAttribute("min", new PythonDateTime(LocalDate.of(1, 1, 1),
                    LocalTime.MAX));
            DATE_TIME_TYPE.__setAttribute("max", new PythonDateTime(LocalDate.of(9999, 12, 31),
                    LocalTime.MIN));
            DATE_TIME_TYPE.__setAttribute("resolution", new PythonTimeDelta(Duration.ofNanos(1000L)));

            PythonOverloadImplementor.createDispatchesFor(DATE_TIME_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Static methods
        DATE_TIME_TYPE.__setAttribute("combine",
                new JavaMethodReference(PythonDateTime.class.getMethod("combine", PythonDate.class, PythonTime.class),
                        Map.of("date", 0, "time", 1)));

        // Unary Operators
        DATE_TIME_TYPE.addMethod(PythonUnaryOperator.AS_STRING,
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("toPythonString")),
                        STRING_TYPE));

        // Binary Operators
        DATE_TIME_TYPE.addMethod(PythonBinaryOperators.ADD,
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("add_time_delta", PythonTimeDelta.class)),
                        DATE_TIME_TYPE, TIME_DELTA_TYPE));
        DATE_TIME_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("subtract_time_delta", PythonTimeDelta.class)),
                        DATE_TIME_TYPE, TIME_DELTA_TYPE));
        DATE_TIME_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("subtract_date_time", PythonDateTime.class)),
                        TIME_DELTA_TYPE, DATE_TIME_TYPE));

        // Instance methods
        DATE_TIME_TYPE.addMethod("replace",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("replace", PythonInteger.class, PythonInteger.class,
                                PythonInteger.class, PythonInteger.class, PythonInteger.class,
                                PythonInteger.class, PythonInteger.class, PythonTzinfo.class,
                                PythonInteger.class)),
                        DATE_TIME_TYPE, INT_TYPE, INT_TYPE, INT_TYPE,
                        INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, TZ_INFO_TYPE, INT_TYPE));
        DATE_TIME_TYPE.addMethod("timetuple",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("timetuple")),
                        TUPLE_TYPE)); // TODO: use time.struct_time type

        DATE_TIME_TYPE.addMethod("utctimetuple",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("utctimetuple")),
                        TUPLE_TYPE)); // TODO: use time.struct_time type

        DATE_TIME_TYPE.addMethod("date",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("date")),
                        DATE_TYPE));
        DATE_TIME_TYPE.addMethod("time",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("time")),
                        PythonTime.TIME_TYPE));

        DATE_TIME_TYPE.addMethod("timetz",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("timetz")),
                        PythonTime.TIME_TYPE));

        DATE_TIME_TYPE.addMethod("astimezone",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("astimezone", PythonTzinfo.class)),
                        DATE_TIME_TYPE, TZ_INFO_TYPE));

        DATE_TIME_TYPE.addMethod("timestamp",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("timestamp")),
                        FLOAT_TYPE));

        DATE_TIME_TYPE.addMethod("tzname",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("tzname")),
                        OBJECT_TYPE));

        DATE_TIME_TYPE.addMethod("utcoffset",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("utcoffset")),
                        OBJECT_TYPE));

        DATE_TIME_TYPE.addMethod("dst",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("dst")),
                        OBJECT_TYPE));

        DATE_TIME_TYPE.addMethod("toordinal",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("to_ordinal")),
                        INT_TYPE));
        DATE_TIME_TYPE.addMethod("weekday",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("weekday")),
                        INT_TYPE));

        DATE_TIME_TYPE.addMethod("isoweekday",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("iso_weekday")),
                        INT_TYPE));

        DATE_TIME_TYPE.addMethod("isoweekday",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("iso_weekday")),
                        INT_TYPE));

        // TODO: isocalendar
        DATE_TIME_TYPE.addMethod("isoformat",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("iso_format")),
                        STRING_TYPE));

        DATE_TIME_TYPE.addMethod("ctime",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonDateTime.class.getMethod("ctime")),
                        STRING_TYPE));

    }

    final Temporal dateTime;
    final ZoneId zoneId;

    public final PythonInteger hour;
    public final PythonInteger minute;
    public final PythonInteger second;
    public final PythonInteger microsecond;
    public final PythonInteger fold;

    public PythonDateTime(ZonedDateTime zonedDateTime) {
        this(zonedDateTime.toLocalDate(), zonedDateTime.toLocalTime(), zonedDateTime.getZone(),
                zonedDateTime.equals(zonedDateTime.withEarlierOffsetAtOverlap()) ? 0 : 1);
    }

    public PythonDateTime(LocalDateTime localDateTime) {
        this(localDateTime.toLocalDate(), localDateTime.toLocalTime(), null, 0);
    }

    public PythonDateTime(LocalDate localDate, LocalTime localTime) {
        this(localDate, localTime, null, 0);
    }

    public PythonDateTime(LocalDate localDate, LocalTime localTime, ZoneId zoneId) {
        this(localDate, localTime, zoneId, 0);
    }

    public PythonDateTime(LocalDate localDate, LocalTime localTime, ZoneId zoneId, int fold) {
        super(DATE_TIME_TYPE, localDate);

        this.zoneId = zoneId;
        if (zoneId == null) {
            dateTime = LocalDateTime.of(localDate, localTime);
        } else {
            dateTime = ZonedDateTime.of(localDate, localTime, zoneId);
        }

        hour = PythonInteger.valueOf(localTime.getHour());
        minute = PythonInteger.valueOf(localTime.getMinute());
        second = PythonInteger.valueOf(localTime.getSecond());
        microsecond = PythonInteger.valueOf(localTime.getNano() / 1000); // Micro = Nano // 1000
        this.fold = PythonInteger.valueOf(fold);
    }

    public static PythonDateTime of(int year, int month, int day, int hour, int minute, int second,
            int microsecond, String tzname, int fold) {
        return new PythonDateTime(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second, microsecond * 1000),
                (tzname != null) ? ZoneId.of(tzname) : null, fold);
    }

    public static PythonDateTime now() {
        return new PythonDateTime(LocalDateTime.now());
    }

    public static PythonDateTime utc_now() {
        return new PythonDateTime(LocalDateTime.now(ZoneOffset.UTC));
    }

    public static PythonDateTime from_timestamp(PythonInteger timestamp) {
        return new PythonDateTime(LocalDateTime.ofEpochSecond(timestamp.getValue().longValue(), 0,
                ZoneOffset.UTC));
    }

    public static PythonDateTime from_timestamp(PythonFloat timestamp) {
        long epochSeconds = (long) Math.floor(timestamp.getValue().doubleValue());
        double remainder = timestamp.getValue().doubleValue() - epochSeconds;
        int nanos = (int) Math.round(remainder * NANOS_PER_SECOND);
        return new PythonDateTime(LocalDateTime.ofEpochSecond(timestamp.getValue().longValue(), nanos,
                ZoneOffset.UTC));
    }

    public static PythonDateTime utc_from_timestamp(PythonInteger timestamp) {
        return new PythonDateTime(LocalDateTime.ofEpochSecond(timestamp.getValue().longValue(), 0,
                ZoneOffset.UTC));
    }

    public static PythonDateTime utc_from_timestamp(PythonFloat timestamp) {
        long epochSeconds = (long) Math.floor(timestamp.getValue().doubleValue());
        double remainder = timestamp.getValue().doubleValue() - epochSeconds;
        int nanos = (int) Math.round(remainder * 1_000_000_000);
        return new PythonDateTime(LocalDateTime.ofEpochSecond(timestamp.getValue().longValue(), nanos,
                ZoneOffset.UTC));
    }

    public static PythonDateTime from_ordinal(PythonInteger ordinal) {
        return new PythonDateTime(LocalDate.ofEpochDay(ordinal.getValue().longValue() + EPOCH_ORDINAL_OFFSET),
                LocalTime.MIN);
    }

    public static PythonDateTime combine(PythonDate pythonDate, PythonTime pythonTime) {
        return new PythonDateTime(pythonDate.localDate, pythonTime.localTime, pythonTime.zoneId,
                pythonTime.fold.getValue().intValue());
    }

    public static PythonDateTime from_iso_format(PythonString dateString) {
        Matcher matcher = ISO_FORMAT_PATTERN.matcher(dateString.getValue());
        if (!matcher.find()) {
            throw new IllegalArgumentException("String \"" + dateString.getValue() + "\" is not an isoformat string");
        }

        String year = matcher.group("year");
        String month = matcher.group("month");
        String day = matcher.group("day");

        String hour = matcher.group("hour");
        String minute = matcher.group("minute");
        String second = matcher.group("second");
        String microHigh = matcher.group("microHigh");
        String microLow = matcher.group("microLow");

        String timezoneHour = matcher.group("timezoneHour");
        String timezoneMinute = matcher.group("timezoneMinute");
        String timezoneSecond = matcher.group("timezoneSecond");
        String timezoneMicro = matcher.group("timezoneMicro");

        LocalDate date = LocalDate.of(Integer.parseInt(year),
                Integer.parseInt(month),
                Integer.parseInt(day));

        int hoursPart = 0;
        int minutePart = 0;
        int secondPart = 0;
        int microPart = 0;

        if (hour != null) {
            hoursPart = Integer.parseInt(hour);
        }
        if (minute != null) {
            minutePart = Integer.parseInt(minute);
        }
        if (second != null) {
            secondPart = Integer.parseInt(second);
        }
        if (microHigh != null) {
            if (microLow != null) {
                microPart = Integer.parseInt(microHigh + microLow);
            } else {
                microPart = 1000 * Integer.parseInt(microHigh);
            }
        }

        LocalTime time = LocalTime.of(hoursPart, minutePart, secondPart, microPart * 1000);

        if (timezoneHour == null) {
            return new PythonDateTime(date, time);
        }

        int timezoneHourPart = Integer.parseInt(timezoneHour);
        int timezoneMinutePart = Integer.parseInt(timezoneMinute);
        int timezoneSecondPart = 0;
        int timezoneMicroPart = 0;

        if (timezoneSecond != null) {
            timezoneSecondPart = Integer.parseInt(timezoneSecond);
        }

        if (timezoneMicro != null) {
            timezoneMicroPart = Integer.parseInt(timezoneMicro);
        }

        // TODO: ZoneOffset does not support nanos
        ZoneOffset timezone = ZoneOffset.ofHoursMinutesSeconds(timezoneHourPart, timezoneMinutePart, timezoneSecondPart);
        return new PythonDateTime(date, time, timezone);
    }

    public static PythonDate from_iso_calendar(PythonInteger year, PythonInteger week, PythonInteger day) {
        int isoYear = year.getValue().intValue();
        int dayInIsoYear = (week.getValue().intValue() * 7) + day.getValue().intValue();
        int correction = LocalDate.of(isoYear, 1, 4).getDayOfWeek().getValue() + 3;
        int ordinalDate = dayInIsoYear - correction;
        if (ordinalDate <= 0) {
            int daysInYear = LocalDate.ofYearDay(isoYear - 1, 1).lengthOfYear();
            return new PythonDateTime(LocalDate.ofYearDay(isoYear - 1, ordinalDate + daysInYear), LocalTime.MIN);
        } else if (ordinalDate > LocalDate.ofYearDay(isoYear, 1).lengthOfYear()) {
            int daysInYear = LocalDate.ofYearDay(isoYear, 1).lengthOfYear();
            return new PythonDateTime(LocalDate.ofYearDay(isoYear + 1, ordinalDate - daysInYear), LocalTime.MIN);
        } else {
            return new PythonDateTime(LocalDate.ofYearDay(isoYear, ordinalDate), LocalTime.MIN);
        }
    }

    public PythonDateTime add_time_delta(PythonTimeDelta summand) {
        if (dateTime instanceof LocalDateTime) {
            return new PythonDateTime(((LocalDateTime) dateTime).plus(summand.duration));
        } else {
            return new PythonDateTime(((ZonedDateTime) dateTime).plus(summand.duration));
        }
    }

    public PythonDateTime subtract_time_delta(PythonTimeDelta subtrahend) {
        if (dateTime instanceof LocalDateTime) {
            return new PythonDateTime(((LocalDateTime) dateTime).minus(subtrahend.duration));
        } else {
            return new PythonDateTime(((ZonedDateTime) dateTime).minus(subtrahend.duration));
        }
    }

    public PythonTimeDelta subtract_date_time(PythonDateTime subtrahend) {
        return new PythonTimeDelta(Duration.between(subtrahend.dateTime, dateTime));
    }

    @Override
    public int compareTo(PythonDateTime other) {
        if (dateTime instanceof LocalDateTime) {
            return ((LocalDateTime) dateTime).compareTo((LocalDateTime) other.dateTime);
        } else {
            return ((ZonedDateTime) dateTime).compareTo((ZonedDateTime) other.dateTime);
        }
    }

    public PythonDate<PythonDate<?>> date() {
        if (dateTime instanceof LocalDateTime) {
            return new PythonDate<>(((LocalDateTime) dateTime).toLocalDate());
        } else {
            return new PythonDate<>(((ZonedDateTime) dateTime).toLocalDate());
        }
    }

    public PythonTime time() {
        if (dateTime instanceof LocalDateTime) {
            return new PythonTime(((LocalDateTime) dateTime).toLocalTime(), null, fold.getValue().intValue());
        } else {
            return new PythonTime(((ZonedDateTime) dateTime).toLocalTime(), null, fold.getValue().intValue());
        }
    }

    public PythonTime timetz() {
        if (dateTime instanceof LocalDateTime) {
            return new PythonTime(((LocalDateTime) dateTime).toLocalTime(), null, fold.getValue().intValue());
        } else {
            ZonedDateTime zonedDateTime = (ZonedDateTime) dateTime;
            return new PythonTime(zonedDateTime.toLocalTime(), zonedDateTime.getZone(), fold.getValue().intValue());
        }
    }

    public PythonDateTime replace(PythonInteger year, PythonInteger month, PythonInteger day,
            PythonInteger hour, PythonInteger minute, PythonInteger second,
            PythonInteger microsecond, PythonTzinfo tzinfo, PythonInteger fold) {
        if (year == null) {
            year = this.year;
        }

        if (month == null) {
            month = this.month;
        }

        if (day == null) {
            day = this.day;
        }

        if (hour == null) {
            hour = this.hour;
        }

        if (minute == null) {
            minute = this.minute;
        }

        if (second == null) {
            second = this.second;
        }

        if (microsecond == null) {
            microsecond = this.microsecond;
        }

        if (tzinfo == null) {
            tzinfo = new PythonTzinfo(this.zoneId);
        }

        if (fold == null) {
            fold = this.fold;
        }

        return new PythonDateTime(LocalDate.of(year.getValue().intValue(),
                month.getValue().intValue(),
                day.getValue().intValue()),
                LocalTime.of(hour.getValue().intValue(),
                        minute.getValue().intValue(),
                        second.getValue().intValue(),
                        microsecond.getValue().intValue() * 1000),
                tzinfo.zoneId,
                fold.getValue().intValue());
    }

    public PythonDateTime astimezone(PythonTzinfo zoneId) {
        throw new UnsupportedOperationException(); // TODO
    }

    public PythonLikeObject utcoffset() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return new PythonTimeDelta(Duration.ofSeconds(
                zoneId.getRules().getOffset(((ZonedDateTime) dateTime).toInstant()).getTotalSeconds()));
    }

    public PythonLikeObject dst() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return new PythonTimeDelta(zoneId.getRules().getDaylightSavings(((ZonedDateTime) dateTime).toInstant()));
    }

    public PythonLikeObject tzname() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return PythonString.valueOf(zoneId.getRules().getOffset(((ZonedDateTime) dateTime).toInstant())
                .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()));
    }

    @Override
    public Object timetuple() {
        throw new UnsupportedOperationException();
    }

    public Object utctimetuple() {
        throw new UnsupportedOperationException();
    }

    public PythonFloat timestamp() {
        if (dateTime instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) dateTime;
            return PythonFloat.valueOf(localDateTime.toInstant(ZoneId.systemDefault()
                    .getRules()
                    .getOffset(localDateTime))
                    .toEpochMilli() / 1000.0);
        } else {
            return PythonFloat.valueOf(((ZonedDateTime) dateTime).toInstant().toEpochMilli() / 1000.0);
        }
    }

    public PythonString iso_format() {
        return new PythonString(dateTime.toString());
    }

    @Override
    public PythonString toPythonString() {
        return new PythonString(dateTime.toString());
    }

    @Override
    public PythonString ctime() {
        if (dateTime instanceof LocalDateTime) {
            return new PythonString(((LocalDateTime) dateTime).format(C_TIME_FORMATTER).replaceAll("(\\D)\\.", "$1"));
        } else {
            return new PythonString(((ZonedDateTime) dateTime).format(C_TIME_FORMATTER).replaceAll("(\\D)\\.", "$1"));
        }
    }

    @Override
    public PythonString strftime(PythonString format) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonDateTime that = (PythonDateTime) o;
        return dateTime.equals(that.dateTime);
    }

    @Override
    public int hashCode() {
        return dateTime.hashCode();
    }
}
