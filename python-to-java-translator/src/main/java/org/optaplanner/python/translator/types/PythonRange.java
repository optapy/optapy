package org.optaplanner.python.translator.types;

import static org.optaplanner.python.translator.types.BuiltinTypes.BASE_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.BOOLEAN_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.INT_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.ITERATOR_TYPE;
import static org.optaplanner.python.translator.types.BuiltinTypes.RANGE_TYPE;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.optaplanner.python.translator.MethodDescriptor;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonFunctionSignature;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.collections.PythonIterator;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class PythonRange extends AbstractPythonLikeObject implements List<PythonInteger> {
    public static PythonLikeType $TYPE = RANGE_TYPE;

    public final PythonInteger start;
    public final PythonInteger stop;
    public final PythonInteger step;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonRange::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        RANGE_TYPE.setConstructor(((positionalArguments, namedArguments) -> {
            PythonLikeObject start;
            PythonLikeObject stop;
            PythonLikeObject step;

            namedArguments = (namedArguments != null) ? namedArguments : Map.of();

            if (positionalArguments.size() == 3) {
                start = positionalArguments.get(0);
                stop = positionalArguments.get(1);
                step = positionalArguments.get(2);
            } else if (positionalArguments.size() == 2) {
                start = positionalArguments.get(0);
                stop = positionalArguments.get(1);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonInteger.valueOf(1));
            } else if (positionalArguments.size() == 1 && namedArguments.containsKey(PythonString.valueOf("stop"))) {
                start = positionalArguments.get(0);
                stop = namedArguments.get(PythonString.valueOf("stop"));
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonInteger.valueOf(1));
            } else if (positionalArguments.size() == 1) {
                stop = positionalArguments.get(0);
                start = PythonInteger.valueOf(0);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonInteger.valueOf(1));
            } else if (positionalArguments.isEmpty()) {
                start = namedArguments.getOrDefault(PythonString.valueOf("start"), PythonInteger.valueOf(0));
                stop = namedArguments.get(PythonString.valueOf("stop"));
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonInteger.valueOf(1));
            } else {
                throw new ValueError("range expects 1 to 3 arguments, got " + positionalArguments.size());
            }

            PythonInteger intStart, intStop, intStep;

            if (start instanceof PythonInteger) {
                intStart = (PythonInteger) start;
            } else {
                intStart = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start);
            }

            if (stop instanceof PythonInteger) {
                intStop = (PythonInteger) stop;
            } else {
                intStop = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(stop);
            }

            if (step instanceof PythonInteger) {
                intStep = (PythonInteger) step;
            } else {
                intStep = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(step);
            }

            return new PythonRange(intStart, intStop, intStep);
        }));

        // Unary methods
        RANGE_TYPE.addMethod(PythonUnaryOperator.LENGTH, new PythonFunctionSignature(
                new MethodDescriptor(PythonRange.class.getMethod("getLength")),
                INT_TYPE));
        RANGE_TYPE.addMethod(PythonUnaryOperator.ITERATOR, new PythonFunctionSignature(
                new MethodDescriptor(PythonRange.class.getMethod("getPythonIterator")),
                ITERATOR_TYPE));

        // Binary methods
        RANGE_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, new PythonFunctionSignature(
                new MethodDescriptor(PythonRange.class.getMethod("getItem", PythonInteger.class)),
                INT_TYPE, INT_TYPE));
        RANGE_TYPE.addMethod(PythonBinaryOperators.CONTAINS, new PythonFunctionSignature(
                new MethodDescriptor(PythonRange.class.getMethod("isObjectInRange", PythonLikeObject.class)),
                BOOLEAN_TYPE, BASE_TYPE));

        return RANGE_TYPE;
    }

    public PythonRange(PythonInteger start, PythonInteger stop, PythonInteger step) {
        super(RANGE_TYPE);
        this.start = start;
        this.stop = stop;
        this.step = step;

        __setAttribute("start", start);
        __setAttribute("stop", stop);
        __setAttribute("step", step);
    }

    @Override
    public int size() {
        return stop.value.subtract(start.value).divide(step.value).intValueExact();
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(size());
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof PythonInteger)) {
            return false;
        }
        PythonInteger query = (PythonInteger) o;

        if (step.value.compareTo(BigInteger.ZERO) < 0) {
            if (query.value.compareTo(stop.value) < 0) {
                return false;
            }
        } else {
            if (query.value.compareTo(stop.value) > 0) {
                return false;
            }
        }

        BigInteger relativeToStart = query.value.subtract(start.value);
        BigInteger[] divisionAndRemainder = relativeToStart.divideAndRemainder(step.value);

        if (!divisionAndRemainder[1].equals(BigInteger.ZERO)) {
            return false; // cannot be represented as start + step * i
        }
        return divisionAndRemainder[0].compareTo(BigInteger.ZERO) >= 0; // return true iff i is not negative
    }

    public PythonBoolean isObjectInRange(PythonLikeObject query) {
        return PythonBoolean.valueOf(contains(query));
    }

    @Override
    public Iterator<PythonInteger> iterator() {
        return new RangeIterator(start, stop, step, start, 0);
    }

    public PythonIterator getPythonIterator() {
        return new PythonIterator(iterator());
    }

    @Override
    public Object[] toArray() {
        PythonInteger[] out = new PythonInteger[size()];

        for (int i = 0; i < out.length; i++) {
            out[i] = get(i);
        }

        return out;
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        T[] out = ts;
        if (ts.length < size()) {
            out = (T[]) Array.newInstance(ts.getClass().getComponentType(), size());
        }

        for (int i = 0; i < out.length; i++) {
            out[i] = (T) get(i);
        }

        if (out.length > size()) {
            out[size()] = null;
        }

        return out;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        for (Object o : collection) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public PythonInteger get(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException();
        }

        PythonInteger out = start.add(step.multiply(PythonInteger.valueOf(i)));
        if (!contains(out)) {
            throw new IndexOutOfBoundsException();
        }
        return out;
    }

    public PythonInteger getItem(PythonInteger index) {
        if (index.value.compareTo(BigInteger.ZERO) < 0) {
            throw new IndexOutOfBoundsException();
        }
        PythonInteger out = start.add(step.multiply(index));

        if (!contains(out)) {
            throw new IndexOutOfBoundsException();
        }

        return out;
    }

    @Override
    public int indexOf(Object o) {
        if (!contains(o)) {
            return -1;
        }
        PythonInteger query = (PythonInteger) o;
        BigInteger relativeToStart = query.value.subtract(start.value);
        return relativeToStart.divide(step.value).intValueExact();
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    @Override
    public ListIterator<PythonInteger> listIterator() {
        return new RangeIterator(start, stop, step, start, 0);
    }

    @Override
    public ListIterator<PythonInteger> listIterator(int i) {
        return new RangeIterator(get(i), stop, step, get(i), i);
    }

    @Override
    public List<PythonInteger> subList(int startIndexInclusive, int endIndexExclusive) {
        return new PythonRange(get(startIndexInclusive), get(endIndexExclusive), step);
    }

    @Override
    public boolean addAll(Collection<? extends PythonInteger> collection) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public boolean addAll(int i, Collection<? extends PythonInteger> collection) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public PythonInteger set(int i, PythonInteger pythonInteger) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public void add(int i, PythonInteger pythonInteger) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public PythonInteger remove(int i) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public boolean add(PythonInteger pythonInteger) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Cannot modify range");
    }

    public static class RangeIterator implements ListIterator<PythonInteger> {
        final PythonInteger startValue;
        final PythonInteger stopValue;
        final PythonInteger step;
        final int startOffset;

        PythonInteger currentValue;

        public RangeIterator(PythonInteger startValue, PythonInteger stopValue, PythonInteger step, PythonInteger currentValue,
                int startOffset) {
            this.startValue = startValue;
            this.stopValue = stopValue;
            this.step = step;
            this.currentValue = currentValue;
            this.startOffset = startOffset;
        }

        @Override
        public boolean hasNext() {
            if (step.value.compareTo(BigInteger.ZERO) < 0) {
                return currentValue.compareTo(stopValue) > 0;
            } else {
                return currentValue.compareTo(stopValue) < 0;
            }
        }

        @Override
        public PythonInteger next() {
            PythonInteger out = currentValue;
            currentValue = currentValue.add(step);
            return out;
        }

        @Override
        public boolean hasPrevious() {
            if (step.value.compareTo(BigInteger.ZERO) < 0) {
                return currentValue.compareTo(startValue) < 0;
            } else {
                return currentValue.compareTo(startValue) > 0;
            }
        }

        @Override
        public PythonInteger previous() {
            PythonInteger out = currentValue;
            currentValue = currentValue.subtract(step);
            return out;
        }

        @Override
        public int nextIndex() {
            return currentValue.value.divide(step.value).intValueExact() + startOffset + 1;
        }

        @Override
        public int previousIndex() {
            return currentValue.value.divide(step.value).intValueExact() + startOffset - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot modify range");
        }

        @Override
        public void set(PythonInteger pythonInteger) {
            throw new UnsupportedOperationException("Cannot modify range");
        }

        @Override
        public void add(PythonInteger pythonInteger) {
            throw new UnsupportedOperationException("Cannot modify range");
        }
    }
}