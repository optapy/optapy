package org.optaplanner.jpyinterpreter.types.datetime;

import java.time.ZoneId;

import org.optaplanner.jpyinterpreter.MethodDescriptor;
import org.optaplanner.jpyinterpreter.PythonFunctionSignature;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;

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
                PythonTimeDelta.TIME_DELTA_TYPE, BuiltinTypes.BASE_TYPE));
        TZ_INFO_TYPE.addMethod("dst", new PythonFunctionSignature(new MethodDescriptor(
                PythonTzinfo.class.getMethod("dst", PythonLikeObject.class)),
                PythonTimeDelta.TIME_DELTA_TYPE, BuiltinTypes.BASE_TYPE));
        TZ_INFO_TYPE.addMethod("tzname", new PythonFunctionSignature(new MethodDescriptor(
                PythonTzinfo.class.getMethod("tzname", PythonLikeObject.class)),
                BuiltinTypes.STRING_TYPE, BuiltinTypes.BASE_TYPE));
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
