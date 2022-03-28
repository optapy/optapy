package org.optaplanner.optapy.logging;

import ch.qos.logback.classic.Level;

public enum PythonLogLevel {
    CRITICAL(50, Level.ERROR),
    ERROR(40, Level.ERROR),
    WARNING(30, Level.WARN),
    INFO(20, Level.INFO),
    DEBUG(10, Level.DEBUG),
    NOTSET(0, Level.INFO);

    final int pythonLevelNumber;
    final Level javaLogLevel;

    PythonLogLevel(int pythonLevelNumber, Level javaLogLevel) {
        this.pythonLevelNumber = pythonLevelNumber;
        this.javaLogLevel = javaLogLevel;
    }

    public int getPythonLevelNumber() {
        return pythonLevelNumber;
    }

    public Level getJavaLogLevel() {
        return javaLogLevel;
    }

    public static PythonLogLevel fromPythonLevelNumber(int levelNumber) {
        PythonLogLevel bestMatch = PythonLogLevel.CRITICAL;
        int bestMatchLevelNumber = 50;
        for (PythonLogLevel pythonLogLevel : PythonLogLevel.values()) {
            if (pythonLogLevel.pythonLevelNumber >= levelNumber && pythonLogLevel.pythonLevelNumber < bestMatchLevelNumber) {
                bestMatch = pythonLogLevel;
            }
        }
        return bestMatch;
    }
}
