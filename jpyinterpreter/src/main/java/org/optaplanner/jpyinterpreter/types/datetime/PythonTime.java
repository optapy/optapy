package org.optaplanner.jpyinterpreter.types.datetime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.optaplanner.jpyinterpreter.MethodDescriptor;
import org.optaplanner.jpyinterpreter.PythonFunctionSignature;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;

public class PythonTime extends AbstractPythonLikeObject {
    // Taken from https://docs.python.org/3/library/datetime.html#datetime.time.fromisoformat
    private static final Pattern ISO_FORMAT_PATTERN = Pattern.compile("^(:(?<hour>\\d\\d)" +
            "(::(?<minute>\\d\\d)" +
            "(::(?<second>\\d\\d)" +
            "(:\\.(?<microHigh>\\d\\d\\d)" +
            "(?<microLow>\\d\\d\\d)?" +
            ")?)?)?)?" +
            "(:(?<timezoneHour>\\d\\d):(?<timezoneMinute>\\d\\d)" +
            "(::(?<timezoneSecond>\\d\\d)" +
            "(:\\.(?<timezoneMicro>\\d\\d\\d\\d\\d\\d)" +
            ")?)?)?$");
    public static PythonLikeType TIME_TYPE = new PythonLikeType("time",
            PythonTime.class);

    public static PythonLikeType $TYPE = TIME_TYPE;

    static {
        try {
            registerMethods();

            TIME_TYPE.__setAttribute("min", new PythonTime(LocalTime.MAX));
            TIME_TYPE.__setAttribute("max", new PythonTime(LocalTime.MIN));
            TIME_TYPE.__setAttribute("resolution", new PythonTimeDelta(Duration.ofNanos(1000L)));

            PythonOverloadImplementor.createDispatchesFor(TIME_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        TIME_TYPE.addMethod("replace",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonTime.class.getMethod("replace", PythonInteger.class, PythonInteger.class, PythonInteger.class,
                                PythonInteger.class, PythonTzinfo.class, PythonInteger.class)),
                        TIME_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE,
                        PythonTzinfo.TZ_INFO_TYPE, BuiltinTypes.INT_TYPE));

        TIME_TYPE.addMethod("tzname",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonTime.class.getMethod("tzname")),
                        OBJECT_TYPE));

        TIME_TYPE.addMethod("utcoffset",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonTime.class.getMethod("utcoffset")),
                        OBJECT_TYPE));

        TIME_TYPE.addMethod("dst",
                new PythonFunctionSignature(new MethodDescriptor(
                        PythonTime.class.getMethod("dst")),
                        OBJECT_TYPE));

    }

    final LocalTime localTime;
    final ZoneId zoneId;

    public final PythonInteger hour;
    public final PythonInteger minute;
    public final PythonInteger second;
    public final PythonInteger microsecond;
    public final PythonInteger fold;

    public PythonTime(LocalTime localTime) {
        this(localTime, null, 0);
    }

    public PythonTime(LocalTime localTime, ZoneId zoneId) {
        this(localTime, zoneId, 0);
    }

    public PythonTime(LocalTime localTime, ZoneId zoneId, int fold) {
        super(TIME_TYPE);

        this.localTime = localTime;
        this.zoneId = zoneId;

        hour = PythonInteger.valueOf(localTime.getHour());
        minute = PythonInteger.valueOf(localTime.getMinute());
        second = PythonInteger.valueOf(localTime.getSecond());
        microsecond = PythonInteger.valueOf(localTime.getNano() / 1000); // Micro = Nano // 1000
        this.fold = PythonInteger.valueOf(fold);
    }

    @Override
    public PythonLikeObject __getAttributeOrNull(String name) {
        switch (name) {
            case "hour":
                return hour;
            case "minute":
                return minute;
            case "second":
                return second;
            case "microsecond":
                return microsecond;
            case "fold":
                return fold;
            default:
                return super.__getAttributeOrNull(name);
        }
    }

    public static PythonTime of(int hour, int minute, int second, int microsecond, String tzname, int fold) {
        return new PythonTime(LocalTime.of(hour, minute, second, microsecond * 1000),
                (tzname != null) ? ZoneId.of(tzname) : null, fold);
    }

    public static PythonTime from_iso_format(PythonString dateString) {
        Matcher matcher = ISO_FORMAT_PATTERN.matcher(dateString.getValue());
        if (!matcher.find()) {
            throw new IllegalArgumentException("String \"" + dateString.getValue() + "\" is not an isoformat string");
        }

        String hour = matcher.group("hour");
        String minute = matcher.group("minute");
        String second = matcher.group("second");
        String microHigh = matcher.group("microHigh");
        String microLow = matcher.group("microLow");

        String timezoneHour = matcher.group("timezoneHour");
        String timezoneMinute = matcher.group("timezoneMinute");
        String timezoneSecond = matcher.group("timezoneSecond");
        String timezoneMicro = matcher.group("timezoneMicro");

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
            return new PythonTime(time);
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
        return new PythonTime(time, timezone);
    }

    public PythonTime replace(PythonInteger hour, PythonInteger minute, PythonInteger second,
            PythonInteger microsecond, PythonTzinfo tzinfo, PythonInteger fold) {
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

        return new PythonTime(LocalTime.of(hour.getValue().intValue(),
                minute.getValue().intValue(),
                second.getValue().intValue(),
                microsecond.getValue().intValue() * 1000),
                tzinfo.zoneId,
                fold.getValue().intValue());
    }

    public PythonLikeObject utcoffset() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return new PythonTimeDelta(Duration.ofSeconds(zoneId.getRules().getOffset(Instant.ofEpochMilli(0L)).getTotalSeconds()));
    }

    public PythonLikeObject dst() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return new PythonTimeDelta(zoneId.getRules().getDaylightSavings(Instant.ofEpochMilli(0L)));
    }

    public PythonLikeObject tzname() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return PythonString.valueOf(zoneId.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()));
    }

    public PythonString isoformat() {
        return PythonString.valueOf(localTime.toString());
    }

    public PythonString toPythonString() {
        return new PythonString(localTime.toString());
    }

    @Override
    public String toString() {
        return localTime.toString();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
