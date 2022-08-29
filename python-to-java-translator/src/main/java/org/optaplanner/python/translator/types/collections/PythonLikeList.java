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
import org.optaplanner.python.translator.PythonTernaryOperators;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.PythonSlice;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class PythonLikeList<T> extends AbstractPythonLikeObject implements List<T> {
    final List delegate;
    private int remainderToAdd;

    public final static PythonLikeType LIST_TYPE = new PythonLikeType("list", PythonLikeList.class);

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(LIST_TYPE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        LIST_TYPE.setConstructor((positionalArguments, namedArguments) -> {
            if (positionalArguments.size() == 0) {
                return new PythonLikeList();
            } else if (positionalArguments.size() == 1) {
                PythonLikeList out = new PythonLikeList();
                out.extend(positionalArguments.get(0));
                return out;
            } else {
                throw new ValueError("list expects 0 or 1 arguments, got " + positionalArguments.size());
            }
        });

        // Unary methods
        LIST_TYPE.addMethod(PythonUnaryOperator.LENGTH, PythonLikeList.class.getMethod("length"));
        LIST_TYPE.addMethod(PythonUnaryOperator.ITERATOR, PythonLikeList.class.getMethod("getIterator"));
        LIST_TYPE.addMethod(PythonUnaryOperator.REPRESENTATION, PythonLikeList.class.getMethod("getRepresentation"));

        // Binary methods
        LIST_TYPE.addMethod(PythonBinaryOperators.ADD, PythonLikeList.class.getMethod("concatToNew", PythonLikeList.class));
        LIST_TYPE.addMethod(PythonBinaryOperators.INPLACE_ADD,
                PythonLikeList.class.getMethod("concatToSelf", PythonLikeList.class));
        LIST_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonLikeList.class.getMethod("getItem", PythonInteger.class));
        LIST_TYPE.addMethod(PythonBinaryOperators.GET_ITEM, PythonLikeList.class.getMethod("getSlice", PythonSlice.class));
        LIST_TYPE.addMethod(PythonBinaryOperators.DELETE_ITEM,
                PythonLikeList.class.getMethod("deleteItem", PythonInteger.class));
        LIST_TYPE.addMethod(PythonBinaryOperators.CONTAINS,
                PythonLikeList.class.getMethod("containsItem", PythonLikeObject.class));

        // Ternary methods
        LIST_TYPE.addMethod(PythonTernaryOperators.SET_ITEM,
                PythonLikeList.class.getMethod("setItem", PythonInteger.class, PythonLikeObject.class));

        // Other
        LIST_TYPE.addMethod("append", PythonLikeList.class.getMethod("append", PythonLikeObject.class));
        LIST_TYPE.addMethod("extend", PythonLikeList.class.getMethod("extend", PythonLikeObject.class));
        LIST_TYPE.addMethod("insert", PythonLikeList.class.getMethod("insert", PythonInteger.class, PythonLikeObject.class));
        LIST_TYPE.addMethod("remove", PythonLikeList.class.getMethod("remove", PythonLikeObject.class));
        LIST_TYPE.addMethod("clear", PythonLikeList.class.getMethod("clearList"));
        LIST_TYPE.addMethod("copy", PythonLikeList.class.getMethod("copy"));
        LIST_TYPE.addMethod("count", PythonLikeList.class.getMethod("count", PythonLikeObject.class));
        LIST_TYPE.addMethod("index", PythonLikeList.class.getMethod("index", PythonLikeObject.class));
    }

    public PythonLikeList() {
        super(LIST_TYPE);
        delegate = new ArrayList<>();
        remainderToAdd = 0;
    }

    public PythonLikeList(int size) {
        super(LIST_TYPE);
        delegate = new ArrayList<>(size);
        remainderToAdd = size;
        for (int i = 0; i < size; i++) {
            delegate.add(null);
        }
    }

    public PythonLikeList(List delegate) {
        super(LIST_TYPE);
        this.delegate = delegate;
        remainderToAdd = 0;
    }

    public PythonLikeList copy() {
        PythonLikeList copy = new PythonLikeList();
        copy.addAll(delegate);
        return copy;
    }

    public PythonLikeList concatToNew(PythonLikeList other) {
        PythonLikeList result = new PythonLikeList();
        result.addAll(delegate);
        result.addAll(other);
        return result;
    }

    public PythonLikeList concatToSelf(PythonLikeList other) {
        this.addAll(other);
        return this;
    }

    public PythonInteger count(PythonLikeObject search) {
        long count = 0;
        for (Object x : delegate) {
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

    public PythonInteger length() {
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
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    public PythonIterator getIterator() {
        return new PythonIterator(delegate.iterator());
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    public Object[] toArray(Object[] ts) {
        return delegate.toArray(ts);
    }

    @Override
    public boolean add(Object pythonLikeObject) {
        return delegate.add(pythonLikeObject);
    }

    public PythonNone append(PythonLikeObject item) {
        delegate.add(item);
        return PythonNone.INSTANCE;
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection collection) {
        return delegate.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection collection) {
        return delegate.addAll(collection);
    }

    public PythonNone extend(PythonLikeObject item) {
        if (item instanceof Collection) {
            delegate.addAll((List) item);
        } else {
            Iterator iterator = (Iterator) UnaryDunderBuiltin.ITERATOR.invoke(item);
            iterator.forEachRemaining(this::add);
        }
        return PythonNone.INSTANCE;
    }

    @Override
    public boolean addAll(int i, Collection collection) {
        return delegate.addAll(i, collection);
    }

    @Override
    public boolean removeAll(Collection collection) {
        return delegate.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection collection) {
        return delegate.retainAll(collection);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    public PythonNone clearList() {
        delegate.clear();
        return PythonNone.INSTANCE;
    }

    @Override
    public T get(int i) {
        return (T) delegate.get(i);
    }

    public PythonLikeObject getItem(PythonInteger index) {
        if (index.value.compareTo(BigInteger.ZERO) < 0) {
            return (PythonLikeObject) delegate.get(delegate.size() + index.value.intValueExact());
        } else {
            return (PythonLikeObject) delegate.get(index.value.intValueExact());
        }
    }

    public PythonLikeList getSlice(PythonSlice slice) {
        int length = size();
        int start = slice.getStartIndex(length);
        int stop = slice.getStopIndex(length);
        int step = slice.getStrideLength();

        PythonLikeList out = new PythonLikeList();

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
    public Object set(int i, Object pythonLikeObject) {
        return delegate.set(i, pythonLikeObject);
    }

    public PythonLikeObject setItem(PythonInteger index, PythonLikeObject value) {
        if (index.value.compareTo(BigInteger.ZERO) < 0) {
            delegate.set(delegate.size() + index.value.intValueExact(), value);
        } else {
            delegate.set(index.value.intValueExact(), value);
        }
        return PythonNone.INSTANCE;
    }

    @Override
    public void add(int i, Object pythonLikeObject) {
        delegate.add(i, pythonLikeObject);
    }

    public PythonNone insert(PythonInteger index, PythonLikeObject item) {
        delegate.add(index.value.intValueExact(), item);
        return PythonNone.INSTANCE;
    }

    @Override
    public T remove(int i) {
        return (T) delegate.remove(i);
    }

    public PythonNone deleteItem(PythonInteger index) {
        if (index.value.compareTo(BigInteger.ZERO) < 0) {
            delegate.remove(delegate.size() + index.value.intValueExact());
        } else {
            delegate.remove(index.value.intValueExact());
        }
        return PythonNone.INSTANCE;
    }

    public PythonNone remove(PythonLikeObject item) {
        delegate.remove(item);
        return PythonNone.INSTANCE;
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
    public ListIterator<T> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        return delegate.listIterator(i);
    }

    @Override
    public List<T> subList(int i, int i1) {
        return delegate.subList(i, i1);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append('[');
        for (int i = 0; i < delegate.size() - 1; i++) {
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke((PythonLikeObject) delegate.get(i)));
            out.append(", ");
        }
        out.append(UnaryDunderBuiltin.REPRESENTATION.invoke((PythonLikeObject) delegate.get(delegate.size() - 1)));
        out.append(']');

        return out.toString();
    }

    public PythonString getRepresentation() {
        return PythonString.valueOf(toString());
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

    public List getDelegate() {
        return delegate;
    }
}
