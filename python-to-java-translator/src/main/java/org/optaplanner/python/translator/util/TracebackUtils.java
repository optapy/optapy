package org.optaplanner.python.translator.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class TracebackUtils {
    private TracebackUtils() {

    }

    public static String getTraceback(Throwable t) {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(byteOutputStream);
        t.printStackTrace(printWriter);
        return byteOutputStream.toString();
    }
}
