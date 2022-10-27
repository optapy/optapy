package org.optaplanner.optapy;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Map that mirrors another Map, but allows new entries to be added without affecting
 * the mirrored map.
 *
 * @param <Key_>
 * @param <Value_>
 */
public class MirrorWithExtrasMap<Key_, Value_> implements Map<Key_, Value_> {
    final Map<Key_, Value_> delegateMap;
    final Map<Key_, Value_> extraEntriesMap = new HashMap<>();

    public MirrorWithExtrasMap(Map<Key_, Value_> delegateMap) {
        this.delegateMap = delegateMap;
    }

    @Override
    public int size() {
        return delegateMap.size() + extraEntriesMap.size();
    }

    @Override
    public boolean isEmpty() {
        return delegateMap.isEmpty() && extraEntriesMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return delegateMap.containsKey(o) || extraEntriesMap.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return delegateMap.containsValue(o) || extraEntriesMap.containsValue(o);
    }

    @Override
    public Value_ get(Object o) {
        Value_ out = extraEntriesMap.get(o);
        if (out == null) {
            return delegateMap.get(o);
        }
        return out;
    }

    @Override
    public Value_ put(Key_ key, Value_ value) {
        return extraEntriesMap.put(key, value);
    }

    @Override
    public Value_ remove(Object o) {
        return extraEntriesMap.remove(o);
    }

    @Override
    public void putAll(Map<? extends Key_, ? extends Value_> map) {
        extraEntriesMap.putAll(map);
    }

    @Override
    public void clear() {
        extraEntriesMap.clear();
    }

    @Override
    public Set<Key_> keySet() {
        Set<Key_> out = new HashSet<>();
        out.addAll(delegateMap.keySet());
        out.addAll(extraEntriesMap.keySet());
        return out;
    }

    @Override
    public Collection<Value_> values() {
        Set<Value_> out = new HashSet<>();
        out.addAll(delegateMap.values());
        out.addAll(extraEntriesMap.values());
        return out;
    }

    @Override
    public Set<Entry<Key_, Value_>> entrySet() {
        Set<Entry<Key_, Value_>> out = new HashSet<>();
        out.addAll(delegateMap.entrySet());
        out.addAll(extraEntriesMap.entrySet());
        return out;
    }
}
