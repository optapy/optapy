package org.optaplanner.python.translator.types;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.optaplanner.python.translator.PythonLikeObject;

public class PythonLikeSet extends AbstractPythonLikeObject implements Set<PythonLikeObject> {
    final Set<PythonLikeObject> delegate;

    private final static PythonLikeType SET_TYPE = new PythonLikeType("set");

    static {
        try {
            // TODO: Add remaining operations from https://docs.python.org/3/library/stdtypes.html#set-types-set-frozenset
            SET_TYPE.__dir__.put("add", new JavaMethodReference(List.class.getMethod("add", Object.class), Map.of("x", 1)));
            SET_TYPE.__dir__.put("update", new JavaMethodReference(List.class.getMethod("addAll", Collection.class),
                    Map.of("iterable", 1)));
            SET_TYPE.__dir__.put("discard", new JavaMethodReference(List.class.getMethod("remove", Object.class),
                    Map.of("x", 1)));
            SET_TYPE.__dir__.put("clear", new JavaMethodReference(List.class.getMethod("clear"), Map.of()));
            SET_TYPE.__dir__.put("copy", new JavaMethodReference(PythonLikeSet.class.getMethod("copy"), Map.of()));
            SET_TYPE.__dir__.put("__len__", new JavaMethodReference(PythonLikeSet.class.getMethod("size"),
                                                                     Map.of()));
            SET_TYPE.__dir__.put("__iter__", new JavaMethodReference(PythonLikeSet.class.getMethod("iterator"),
                    Map.of()));
            SET_TYPE.__dir__.put("__contains__",
                    new JavaMethodReference(PythonLikeSet.class.getMethod("contains", Object.class),
                            Map.of()));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    public PythonLikeSet() {
        super(SET_TYPE);
        delegate = new HashSet<>();
    }

    public PythonLikeSet(int size) {
        super(SET_TYPE);
        delegate = new HashSet<>(size);
    }

    public void reverseAdd(PythonLikeObject object) {
        delegate.add(object);
    }

    public PythonLikeSet copy() {
        PythonLikeSet copy = new PythonLikeSet();
        copy.addAll(delegate);
        return copy;
    }

    public PythonLikeSet concatToNew(PythonLikeSet other) {
        PythonLikeSet result = new PythonLikeSet();
        result.addAll(delegate);
        result.addAll(other);
        return result;
    }

    public PythonLikeSet concatToSelf(PythonLikeSet other) {
        this.addAll(other);
        return this;
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
    public boolean equals(Object o) {
        if (o instanceof Set) {
            Set other = (Set) o;
            if (other.size() != this.size()) {
                return false;
            }
            return containsAll(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}
