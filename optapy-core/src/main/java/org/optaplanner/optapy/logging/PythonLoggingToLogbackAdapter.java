package org.optaplanner.optapy.logging;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class PythonLoggingToLogbackAdapter {

    private static Logger getLogger(String loggerName) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerContext.getLogger(loggerName);
    }

    public static void setLevel(String loggerName, int logLevel) {
        Logger logger = getLogger(loggerName);
        PythonLogLevel pythonLogLevel = PythonLogLevel.fromPythonLevelNumber(logLevel);
        logger.setLevel(pythonLogLevel.getJavaLogLevel());
    }

    public static boolean isEnabledFor(String loggerName, int logLevel) {
        Logger logger = getLogger(loggerName);
        PythonLogLevel pythonLogLevel = PythonLogLevel.fromPythonLevelNumber(logLevel);
        return logger.isEnabledFor(pythonLogLevel.getJavaLogLevel());
    }

    public static int getEffectiveLevel(String loggerName) {
        Logger logger = getLogger(loggerName);
        Level effectiveLogLevel = logger.getEffectiveLevel();
        return effectiveLogLevel.levelInt / 1000;
    }
}
