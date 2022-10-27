package org.optaplanner.jpyinterpreter.types.numeric;

import java.math.BigInteger;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeComparable;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public interface PythonNumber extends PythonLikeComparable<PythonNumber>,
        PythonLikeObject {

    PythonLikeType NUMBER_TYPE = new PythonLikeType("number", PythonNumber.class);
    PythonInteger MODULUS = PythonInteger.valueOf((1L << 61) - 1);
    PythonInteger INFINITY_HASH_VALUE = PythonInteger.valueOf(314159);

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

    static PythonInteger computeHash(PythonInteger numerator, PythonInteger denominator) {
        PythonInteger P = MODULUS;
        // Remove common factors of P.  (Unnecessary if m and n already coprime.)

        while (numerator.modulo(P) == PythonInteger.ZERO && denominator.modulo(P) == PythonInteger.ZERO) {
            numerator = numerator.floorDivide(P);
            denominator = denominator.floorDivide(P);
        }

        PythonInteger hash_value;
        if (denominator.modulo(P).equals(PythonInteger.ZERO)) {
            hash_value = INFINITY_HASH_VALUE;
        } else {
            // Fermat's Little Theorem: pow(n, P-1, P) is 1, so
            // pow(n, P-2, P) gives the inverse of n modulo P.
            hash_value = (numerator.abs().modulo(P)).multiply(denominator.power(P.subtract(PythonInteger.TWO), P)).modulo(P);
        }

        if (numerator.lessThan(PythonInteger.ZERO).getBooleanValue()) {
            hash_value = hash_value.negative();
        }

        if (hash_value.equals(PythonInteger.valueOf(-1))) {
            hash_value = PythonInteger.valueOf(-2);
        }

        return hash_value;
    }
}
