package org.optaplanner.optapy;

import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("unused")
public class PythonComparable implements Comparable<PythonComparable> {

    // Compares two OpaquePythonReference
    private static BiFunction<OpaquePythonReference, OpaquePythonReference, Integer> pythonObjectCompareTo;

    // Check if two OpaquePythonReferences are equal
    private static BiFunction<OpaquePythonReference, OpaquePythonReference, Boolean> pythonObjectEquals;

    // Get the hash of an OpaquePythonReference
    private static Function<OpaquePythonReference, Integer> pythonObjectHash;

    @SuppressWarnings("unused")
    public static void setPythonObjectCompareTo(
            BiFunction<OpaquePythonReference, OpaquePythonReference, Integer> compareTo) {
        pythonObjectCompareTo = compareTo;
    }

    @SuppressWarnings("unused")
    public static void setPythonObjectEquals(
            BiFunction<OpaquePythonReference, OpaquePythonReference, Boolean> isEqual) {
        pythonObjectEquals = isEqual;
    }

    public static void setPythonObjectHash(Function<OpaquePythonReference, Integer> pythonObjectHash) {
        PythonComparable.pythonObjectHash = pythonObjectHash;
    }

    public static boolean isPythonObjectEqualToOther(OpaquePythonReference a, OpaquePythonReference b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            // a != b, and one of a/b is null, so they are not the same
            return false;
        }

        return pythonObjectEquals.apply(a, b);
    }

    public static int getPythonObjectHash(OpaquePythonReference pythonObject) {
        return pythonObjectHash.apply(pythonObject);
    }

    private final OpaquePythonReference reference;

    public PythonComparable(OpaquePythonReference reference) {
        this.reference = reference;
    }

    @Override
    public int compareTo(PythonComparable other) {
        return pythonObjectCompareTo.apply(reference, other.reference);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PythonComparable) {
            return pythonObjectEquals.apply(reference, ((PythonComparable) other).reference);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pythonObjectHash.apply(reference);
    }

    @Override
    public String toString() {
        return PythonWrapperGenerator.getPythonObjectString(reference);
    }
}
