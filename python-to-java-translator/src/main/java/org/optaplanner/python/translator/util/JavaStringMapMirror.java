package org.optaplanner.python.translator.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonString;

public class JavaStringMapMirror implements Map<PythonLikeObject, PythonLikeObject> {
    final Map<String, PythonLikeObject> delegate;

    public JavaStringMapMirror(Map<String, PythonLikeObject> delegate) {
        this.delegate = delegate;
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
    public boolean containsKey(Object o) {
        return o instanceof PythonString && delegate.containsKey(((PythonString) o).value);
    }

    @Override
    public boolean containsValue(Object o) {
        return delegate.containsValue(o);
    }

    @Override
    public PythonLikeObject get(Object o) {
        if (o instanceof PythonString) {
            return delegate.get(((PythonString) o).value);
        }
        return null;
    }

    @Override
    public PythonLikeObject put(PythonLikeObject key, PythonLikeObject value) {
        if (key instanceof PythonString) {
            return delegate.put(((PythonString) key).value, value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public PythonLikeObject remove(Object o) {
        if (o instanceof PythonString) {
            return delegate.remove(((PythonString) o).value);
        }
        return delegate.remove(o);
    }

    @Override
    public void putAll(Map<? extends PythonLikeObject, ? extends PythonLikeObject> map) {
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<PythonLikeObject> keySet() {
        return delegate.keySet().stream().map(PythonString::valueOf).collect(Collectors.toSet());
    }

    @Override
    public Collection<PythonLikeObject> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<PythonLikeObject, PythonLikeObject>> entrySet() {
        return delegate.entrySet().stream().map(entry -> new Entry<PythonLikeObject, PythonLikeObject>() {
            @Override
            public PythonLikeObject getKey() {
                return PythonString.valueOf(entry.getKey());
            }

            @Override
            public PythonLikeObject getValue() {
                return entry.getValue();
            }

            @Override
            public PythonLikeObject setValue(PythonLikeObject o) {
                return entry.setValue(o);
            }
        }).collect(Collectors.toSet());
    }
}
