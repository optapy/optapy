package org.optaplanner.python.translator.types.datetime;

import static org.optaplanner.python.translator.types.PythonString.STRING_TYPE;

import java.time.ZoneId;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;

public class PythonTzinfo extends AbstractPythonLikeObject {
    public static PythonLikeType TZ_INFO_TYPE = new PythonLikeType("tzinfo",
            PythonTzinfo.class);

    public static PythonLikeType $TYPE = TZ_INFO_TYPE;

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(TZ_INFO_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        TZ_INFO_TYPE.addMethod("utcoffset", new PythonFunctionSignature(new MethodDescriptor(
                PythonTzinfo.class.getMethod("utcoffset", PythonLikeObject.class)),
                PythonTimeDelta.TIME_DELTA_TYPE, PythonLikeType.getBaseType()));
        TZ_INFO_TYPE.addMethod("dst", new PythonFunctionSignature(new MethodDescriptor(
                PythonTzinfo.class.getMethod("dst", PythonLikeObject.class)),
                PythonTimeDelta.TIME_DELTA_TYPE, PythonLikeType.getBaseType()));
        TZ_INFO_TYPE.addMethod("tzname", new PythonFunctionSignature(new MethodDescriptor(
                PythonTzinfo.class.getMethod("tzname", PythonLikeObject.class)),
                STRING_TYPE, PythonLikeType.getBaseType()));
    }

    final ZoneId zoneId;

    public PythonTzinfo(ZoneId zoneId) {
        super(TZ_INFO_TYPE);
        this.zoneId = zoneId;
    }

    public PythonTimeDelta utcoffset(PythonLikeObject dateTime) {
        throw new UnsupportedOperationException(); // TODO
    }

    public PythonTimeDelta dst(PythonLikeObject dateTime) {
        throw new UnsupportedOperationException(); // TODO
    }

    public PythonString tzname(PythonLikeObject dateTime) {
        throw new UnsupportedOperationException(); // TODO
    }
}
