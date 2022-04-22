package org.optaplanner.optapy.translator.types;

import java.math.BigInteger;

public interface PythonNumber extends Comparable<PythonNumber> {
    Number getValue();

    @Override
    default int compareTo(PythonNumber pythonNumber) {
        Number value = getValue();
        Number otherValue = pythonNumber.getValue();

        if (value instanceof BigInteger) {
            if (otherValue instanceof BigInteger) {
                return ((BigInteger) value).compareTo((BigInteger) otherValue);
            } else {
                return Double.compare(value.longValue(), otherValue.doubleValue());
            }
        } else {
            return Double.compare(value.doubleValue(), otherValue.doubleValue());
        }
    }
}
