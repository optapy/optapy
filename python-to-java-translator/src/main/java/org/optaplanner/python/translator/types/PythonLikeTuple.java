package org.optaplanner.python.translator.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.optaplanner.python.translator.PythonLikeObject;

public class PythonLikeTuple extends AbstractPythonLikeObject implements List<PythonLikeObject> {
    final List<PythonLikeObject> delegate;
    private int remainderToAdd;

    private final static PythonLikeType TUPLE_TYPE = new PythonLikeType("tuple");

    static {
        try {
            TUPLE_TYPE.__dir__.put("index", new JavaMethodReference(List.class.getMethod("indexOf", Object.class),
                    Map.of("x", 1)));
            TUPLE_TYPE.__dir__.put("count",
                    new JavaMethodReference(PythonLikeTuple.class.getMethod("count", PythonLikeObject.class),
                            Map.of("x", 1)));
            TUPLE_TYPE.__dir__.put("__len__", new JavaMethodReference(PythonLikeTuple.class.getMethod("size"),
                                                                        Map.of()));
            TUPLE_TYPE.__dir__.put("__add__",
                    new JavaMethodReference(PythonLikeTuple.class.getMethod("concatToNew", PythonLikeTuple.class),
                            Map.of()));
            TUPLE_TYPE.__dir__.put("__iter__", new JavaMethodReference(PythonLikeTuple.class.getMethod("iterator"),
                    Map.of()));
            TUPLE_TYPE.__dir__.put("__contains__",
                    new JavaMethodReference(PythonLikeTuple.class.getMethod("contains", Object.class),
                            Map.of()));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
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
