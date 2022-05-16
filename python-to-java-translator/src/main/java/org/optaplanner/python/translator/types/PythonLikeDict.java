package org.optaplanner.python.translator.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.optaplanner.python.translator.PythonLikeObject;

public class PythonLikeDict extends AbstractPythonLikeObject
        implements Map<PythonLikeObject, PythonLikeObject>, Iterable<PythonLikeObject> {
    final Map<PythonLikeObject, PythonLikeObject> delegate;

    public final static PythonLikeType DICT_TYPE = new PythonLikeType("dict");

    static {
        try {
            // TODO: Add remaining operations from https://docs.python.org/3/library/stdtypes.html#dict

            DICT_TYPE.__dir__.put("__len__", new JavaMethodReference(Map.class.getMethod("size"),
                    Map.of()));
            DICT_TYPE.__dir__.put("__getitem__",
                    new JavaMethodReference(Map.class.getMethod("get", Object.class), Map.of("key", 1)));
            DICT_TYPE.__dir__.put("__setitem__", new JavaMethodReference(Map.class.getMethod("put", Object.class, Object.class),
                    Map.of("key", 1, "value", 2)));
            DICT_TYPE.__dir__.put("__delitem__", new JavaMethodReference(Map.class.getMethod("remove", Object.class),
                    Map.of("key", 1)));
            DICT_TYPE.__dir__.put("__iter__", new JavaMethodReference(PythonLikeDict.class.getMethod("iterator"),
                    Map.of()));
            DICT_TYPE.__dir__.put("__contains__", new JavaMethodReference(Map.class.getMethod("containsKey", Object.class),
                    Map.of()));
            DICT_TYPE.__dir__.put("clear", new JavaMethodReference(Map.class.getMethod("clear"), Map.of()));
            DICT_TYPE.__dir__.put("copy", new JavaMethodReference(PythonLikeDict.class.getMethod("copy"), Map.of()));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    public PythonLikeDict() {
        super(DICT_TYPE);
        delegate = new HashMap<>();
    }

    public PythonLikeDict(int size) {
        super(DICT_TYPE);
        delegate = new HashMap<>(size);
    }

    public PythonLikeDict copy() {
        PythonLikeDict copy = new PythonLikeDict();
        copy.putAll(delegate);
        return copy;
    }

    public PythonLikeDict concatToNew(PythonLikeDict other) {
        PythonLikeDict result = new PythonLikeDict();
        result.putAll(delegate);
        result.putAll(other);
        return result;
    }

    public PythonLikeDict concatToSelf(PythonLikeDict other) {
        this.putAll(other);
        return this;
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
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public PythonLikeObject get(Object key) {
        return delegate.get(key);
    }

    @Override
    public PythonLikeObject put(PythonLikeObject key, PythonLikeObject value) {
        return delegate.put(key, value);
    }

    @Override
    public PythonLikeObject remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public void putAll(Map<? extends PythonLikeObject, ? extends PythonLikeObject> map) {
        delegate.putAll(map);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<PythonLikeObject> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<PythonLikeObject> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<PythonLikeObject, PythonLikeObject>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public Iterator<PythonLikeObject> iterator() {
        return delegate.keySet().iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Map) {
            Map other = (Map) o;
            if (other.size() != this.size()) {
                return false;
            }
            return this.entrySet().containsAll(other.entrySet());
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
