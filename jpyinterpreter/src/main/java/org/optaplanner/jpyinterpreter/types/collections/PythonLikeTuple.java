package org.optaplanner.jpyinterpreter.types.collections;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.builtins.UnaryDunderBuiltin;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonSlice;
import org.optaplanner.jpyinterpreter.types.errors.ValueError;
import org.optaplanner.jpyinterpreter.types.errors.lookup.IndexError;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;

public class PythonLikeTuple extends AbstractPythonLikeObject implements List<PythonLikeObject>, RandomAccess {
    public static PythonLikeTuple EMPTY = PythonLikeTuple.fromList(List.of());

    final List<PythonLikeObject> delegate;
    private int remainderToAdd;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeTuple::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        BuiltinTypes.TUPLE_TYPE.setConstructor((positionalArguments, namedArguments, callerInstance) -> {
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
        BuiltinTypes.TUPLE_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, PythonLikeTuple.class.getMethod("getLength"));
        BuiltinTypes.TUPLE_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR, PythonLikeTuple.class.getMethod("getIterator"));

        // Binary
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperators.ADD,
                PythonLikeTuple.class.getMethod("concatToNew", PythonLikeTuple.class));
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperators.MULTIPLY,
                PythonLikeTuple.class.getMethod("multiplyToNew", PythonInteger.class));
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperators.GET_ITEM,
                PythonLikeTuple.class.getMethod("getItem", PythonInteger.class));
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperators.GET_ITEM,
                PythonLikeTuple.class.getMethod("getSlice", PythonSlice.class));
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperators.CONTAINS,
                PythonLikeTuple.class.getMethod("containsItem", PythonLikeObject.class));

        // Other
        BuiltinTypes.TUPLE_TYPE.addMethod("index", PythonLikeTuple.class.getMethod("index", PythonLikeObject.class));
        BuiltinTypes.TUPLE_TYPE.addMethod("index",
                PythonLikeTuple.class.getMethod("index", PythonLikeObject.class, PythonInteger.class));
        BuiltinTypes.TUPLE_TYPE.addMethod("index",
                PythonLikeTuple.class.getMethod("index", PythonLikeObject.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.TUPLE_TYPE.addMethod("count", PythonLikeTuple.class.getMethod("count", PythonLikeObject.class));

        return BuiltinTypes.TUPLE_TYPE;
    }

    public PythonLikeTuple() {
        super(BuiltinTypes.TUPLE_TYPE);
        delegate = new ArrayList<>();
        remainderToAdd = 0;
    }

    public PythonLikeTuple(int size) {
        super(BuiltinTypes.TUPLE_TYPE);
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

    public PythonIterator getReversedIterator() {

        final ListIterator<PythonLikeObject> listIterator = delegate.listIterator(delegate.size());
        return new PythonIterator<>(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return listIterator.hasPrevious();
            }

            @Override
            public Object next() {
                return listIterator.previous();
            }
        });
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
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
