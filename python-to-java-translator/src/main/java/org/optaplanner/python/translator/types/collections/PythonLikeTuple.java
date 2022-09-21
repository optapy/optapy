package org.optaplanner.python.translator.types.collections;

import static org.optaplanner.python.translator.types.BuiltinTypes.TUPLE_TYPE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonSlice;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.errors.lookup.IndexError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class PythonLikeTuple extends AbstractPythonLikeObject implements List<PythonLikeObject>, RandomAccess {
    final List<PythonLikeObject> delegate;
    private int remainderToAdd;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeTuple::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        TUPLE_TYPE.setConstructor((positionalArguments, namedArguments) -> {
            if (positionalArguments.isEmpty()) {
                return new PythonLikeTuple();
            } else if (positionalArguments.size() == 1) {
                PythonLikeTuple out = new PythonLikeTuple();
                PythonLikeObject iterable = positionalArguments.get(0);
                if (iterable instanceof Collection) {
                    out.delegate.addAll((Collection<? extends PythonLikeObject>) iterable);
                } else {
                    Iterator<PythonLikeObject> iterator =
                            (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(iterable);
                    iterator.forEachRemaining(out.delegate::add);
                }
                return out;
            } else {
                throw new ValueError("tuple takes 0 or 1 arguments, got " + positionalArguments.size());
            }
        });
        // Unary
        TUPLE_TYPE.addMethod(PythonUnaryOperator.LENGTH, PythonLikeTuple.class.getMethod("getLength"));
        TUPLE_TYPE.addMethod(PythonUnaryOperator.ITERATOR, PythonLikeTuple.class.getMethod("getIterator"));

        // Binary
        TUPLE_TYPE.addMethod(PythonBinaryOperators.ADD, PythonLikeTuple.class.getMethod("concatToNew", PythonLikeTuple.class));
        TUPLE_TYPE.addMethod(PythonBinaryOperators.MULTIPLY,
                PythonLikeTuple.class.getMethod("multiplyToNew", PythonInteger.class));
        TUPLE_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonLikeTuple.class.getMethod("getItem", PythonInteger.class));
        TUPLE_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonLikeTuple.class.getMethod("getSlice", PythonSlice.class));
        TUPLE_TYPE.addMethod(PythonBinaryOperators.CONTAINS,
                PythonLikeTuple.class.getMethod("containsItem", PythonLikeObject.class));

        // Other
        TUPLE_TYPE.addMethod("index", PythonLikeTuple.class.getMethod("index", PythonLikeObject.class));
        TUPLE_TYPE.addMethod("index", PythonLikeTuple.class.getMethod("index", PythonLikeObject.class, PythonInteger.class));
        TUPLE_TYPE.addMethod("index",
                PythonLikeTuple.class.getMethod("index", PythonLikeObject.class, PythonInteger.class, PythonInteger.class));
        TUPLE_TYPE.addMethod("count", PythonLikeTuple.class.getMethod("count", PythonLikeObject.class));

        return TUPLE_TYPE;
    }

    public PythonLikeTuple() {
        super(TUPLE_TYPE);
        delegate = new ArrayList<>();
        remainderToAdd = 0;
    }

    public PythonLikeTuple(int size) {
        super(TUPLE_TYPE);
        delegate = new ArrayList<>(size);
        remainderToAdd = size;
        for (int i = 0; i < size; i++) {
            delegate.add(null);
        }
    }

    public static PythonLikeTuple fromList(List<PythonLikeObject> other) {
        PythonLikeTuple result = new PythonLikeTuple();
        result.addAll(other);
        return result;
    }

    public PythonLikeTuple concatToNew(PythonLikeTuple other) {
        if (delegate.isEmpty()) {
            return other;
        } else if (other.delegate.isEmpty()) {
            return this;
        }

        PythonLikeTuple result = new PythonLikeTuple();
        result.addAll(delegate);
        result.addAll(other);
        return result;
    }

    public PythonLikeTuple multiplyToNew(PythonInteger times) {
        if (times.value.compareTo(BigInteger.ZERO) <= 0) {
            if (delegate.isEmpty()) {
                return this;
            }
            return new PythonLikeTuple();
        }

        if (times.value.equals(BigInteger.ONE)) {
            return this;
        }

        PythonLikeTuple result = new PythonLikeTuple();
        int timesAsInt = times.value.intValueExact();

        for (int i = 0; i < timesAsInt; i++) {
            result.addAll(delegate);
        }

        return result;
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(delegate.size());
    }

    public PythonBoolean containsItem(PythonLikeObject item) {
        return PythonBoolean.valueOf(delegate.contains(item));
    }

    public PythonIterator getIterator() {
        return new PythonIterator(delegate.iterator());
    }

    public PythonLikeObject getItem(PythonInteger index) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            indexAsInt = delegate.size() + index.value.intValueExact();
        }

        if (indexAsInt < 0 || indexAsInt >= delegate.size()) {
            throw new IndexError("list index out of range");
        }

        return delegate.get(indexAsInt);
    }

    public PythonLikeTuple getSlice(PythonSlice slice) {
        int length = delegate.size();

        PythonLikeTuple out = new PythonLikeTuple();

        slice.iterate(length, (i, processed) -> {
            out.add(delegate.get(i));
        });

        return out;
    }

    public PythonInteger count(PythonLikeObject search) {
        long count = 0;
        for (PythonLikeObject x : delegate) {
            if (Objects.equals(search, x)) {
                count++;
            }
        }
        return new PythonInteger(count);
    }

    public PythonInteger index(PythonLikeObject item) {
        int result = delegate.indexOf(item);

        if (result != -1) {
            return PythonInteger.valueOf(result);
        } else {
            throw new ValueError(item + " is not in list");
        }
    }

    public PythonInteger index(PythonLikeObject item, PythonInteger start) {
        int startAsInt = start.value.intValueExact();
        if (startAsInt < 0) {
            startAsInt = delegate.size() + startAsInt;
        }

        List<PythonLikeObject> searchList = delegate.subList(startAsInt, delegate.size());
        int result = searchList.indexOf(item);
        if (result != -1) {
            return PythonInteger.valueOf(startAsInt + result);
        } else {
            throw new ValueError(item + " is not in list");
        }
    }

    public PythonInteger index(PythonLikeObject item, PythonInteger start, PythonInteger end) {
        int startAsInt = start.value.intValueExact();
        int endAsInt = end.value.intValueExact();

        if (startAsInt < 0) {
            startAsInt = delegate.size() + startAsInt;
        }

        if (endAsInt < 0) {
            endAsInt = delegate.size() + endAsInt;
        }

        List<PythonLikeObject> searchList = delegate.subList(startAsInt, endAsInt);
        int result = searchList.indexOf(item);
        if (result != -1) {
            return PythonInteger.valueOf(startAsInt + result);
        } else {
            throw new ValueError(item + " is not in list");
        }
    }

    public void reverseAdd(PythonLikeObject object) {
        delegate.set(remainderToAdd - 1, object);
        remainderToAdd--;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<PythonLikeObject> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return delegate.toArray(ts);
    }

    @Override
    public boolean add(PythonLikeObject pythonLikeObject) {
        return delegate.add(pythonLikeObject);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return delegate.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends PythonLikeObject> collection) {
        return delegate.addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends PythonLikeObject> collection) {
        return delegate.addAll(i, collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return delegate.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return delegate.retainAll(collection);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public PythonLikeObject get(int i) {
        return delegate.get(i);
    }

    @Override
    public PythonLikeObject set(int i, PythonLikeObject pythonLikeObject) {
        return delegate.set(i, pythonLikeObject);
    }

    @Override
    public void add(int i, PythonLikeObject pythonLikeObject) {
        delegate.add(i, pythonLikeObject);
    }

    @Override
    public PythonLikeObject remove(int i) {
        return delegate.remove(i);
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<PythonLikeObject> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<PythonLikeObject> listIterator(int i) {
        return delegate.listIterator(i);
    }

    @Override
    public List<PythonLikeObject> subList(int i, int i1) {
        return delegate.subList(i, i1);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof List) {
            List other = (List) o;
            if (other.size() != this.size()) {
                return false;
            }
            int itemCount = size();
            for (int i = 0; i < itemCount; i++) {
                if (!Objects.equals(get(i), other.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}