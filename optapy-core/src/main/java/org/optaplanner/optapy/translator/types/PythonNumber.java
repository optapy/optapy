package org.optaplanner.optapy.translator.types;

public interface PythonNumber extends Comparable<PythonNumber> {
    Number getValue();

    @Override
    default int compareTo(PythonNumber pythonNumber) {
        Number value = getValue();
        Number otherValue = pythonNumber.getValue();

        if (value instanceof Long) {
            if (otherValue instanceof Long) {
                return Long.compare(value.longValue(), otherValue.longValue());
            } else {
                return Double.compare(value.longValue(), otherValue.doubleValue());
            }
        } else {
            if (otherValue instanceof Long) {
                return Double.compare(value.doubleValue(), otherValue.longValue());
            } else {
                return Double.compare(value.doubleValue(), otherValue.doubleValue());
            }
        }
    }
}
