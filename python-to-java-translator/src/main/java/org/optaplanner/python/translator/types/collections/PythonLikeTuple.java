package org.optaplanner.python.translator.types.collections;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonSlice;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class PythonLikeTuple extends AbstractPythonLikeObject implements List<PythonLikeObject> {
    final List<PythonLikeObject> delegate;
    private int remainderToAdd;

    public final static PythonLikeType TUPLE_TYPE = new PythonLikeType("tuple", PythonLikeTuple.class);

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(TUPLE_TYPE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
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
        TUPLE_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonLikeTuple.class.getMethod("getItem", PythonInteger.class));
        TUPLE_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonLikeTuple.class.getMethod("getSlice", PythonSlice.class));
        TUPLE_TYPE.addMethod(PythonBinaryOperators.CONTAINS,
                PythonLikeTuple.class.getMethod("containsItem", PythonLikeObject.class));

        // Other
        TUPLE_TYPE.addMethod("index", PythonLikeTuple.class.getMethod("index", PythonLikeObject.class));
        TUPLE_TYPE.addMethod("count", PythonLikeTuple.class.getMethod("count", PythonLikeObject.class));
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
        PythonLikeTuple result = new PythonLikeTuple();
        result.addAll(delegate);
        result.addAll(other);
        return result;
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

    public void reverseAdd(PythonLikeObject object) {
        delegate.set(remainderToAdd - 1, object);
        remainderToAdd--;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(delegate.size());
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    public PythonBoolean containsItem(PythonLikeObject item) {
        return PythonBoolean.valueOf(delegate.contains(item));
    }

    @Override
    public Iterator<PythonLikeObject> iterator() {
        return delegate.iterator();
    }

    public PythonIterator getIterator() {
        return new PythonIterator(delegate.iterator());
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

    public PythonLikeObject getItem(PythonInteger index) {
        if (index.value.compareTo(BigInteger.ZERO) < 0) {
            return delegate.get(delegate.size() + index.value.intValueExact());
        } else {
            return delegate.get(index.value.intValueExact());
        }
    }

    public PythonLikeTuple getSlice(PythonSlice slice) {
        int length = size();
        int start = slice.getStartIndex(length);
        int stop = slice.getStopIndex(length);
        int step = slice.getStrideLength();

        PythonLikeTuple out = new PythonLikeTuple();

        if (step > 0) {
            for (int i = start; i < stop; i += step) {
                out.add(delegate.get(i));
            }
        } else {
            for (int i = start; i > stop; i += step) {
                out.add(delegate.get(i));
            }
        }

        return out;
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

    public PythonInteger index(PythonLikeObject item) {
        return PythonInteger.valueOf(delegate.indexOf(item));
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
