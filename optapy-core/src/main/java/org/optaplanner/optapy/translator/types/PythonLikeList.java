package org.optaplanner.optapy.translator.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.optaplanner.optapy.PythonLikeObject;

public class PythonLikeList extends AbstractPythonLikeObject implements List<PythonLikeObject> {
    final List<PythonLikeObject> delegate;
    private int remainderToAdd;

    private final static Map<String, PythonLikeObject> DEFAULT_DICT;

    static {
        DEFAULT_DICT = new HashMap<>();
        try {
            DEFAULT_DICT.put("append", new JavaMethodReference(List.class.getMethod("add", Object.class), Map.of("x", 1)));
            DEFAULT_DICT.put("extend", new JavaMethodReference(List.class.getMethod("addAll", Collection.class),
                                             Map.of("iterable", 1)));
            DEFAULT_DICT.put("insert", new JavaMethodReference(List.class.getMethod("add", int.class, Object.class),
                                                               Map.of("i", 1, "x", 2)));
            DEFAULT_DICT.put("remove", new JavaMethodReference(List.class.getMethod("remove", Object.class),
                                                               Map.of("x", 1)));
            DEFAULT_DICT.put("clear", new JavaMethodReference(List.class.getMethod("clear"), Map.of()));
            DEFAULT_DICT.put("copy", new JavaMethodReference(PythonLikeList.class.getMethod("copy"), Map.of()));
            DEFAULT_DICT.put("count", new JavaMethodReference(PythonLikeList.class.getMethod("count", PythonLikeObject.class),
                                                              Map.of("x", 1)));
            DEFAULT_DICT.put("__add__", new JavaMethodReference(PythonLikeList.class.getMethod("concatToNew", PythonLikeList.class),
                                                    Map.of()));
            DEFAULT_DICT.put("__iadd__", new JavaMethodReference(PythonLikeList.class.getMethod("concatToSelf", PythonLikeList.class),
                                                    Map.of()));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    public PythonLikeList() {
        super(new CopyOnWriteMap<>(DEFAULT_DICT));
        delegate = new ArrayList<>();
        remainderToAdd = 0;
    }

    public PythonLikeList(int size) {
        super(new CopyOnWriteMap<>(DEFAULT_DICT));
        delegate = new ArrayList<>(size);
        remainderToAdd = size;
        for (int i = 0; i < size; i++) {
            delegate.add(null);
        }
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
}
