package org.optaplanner.optapy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.collection.CompositeCollection;
import org.apache.commons.collections4.set.CompositeSet;

/**
 * A Map that mirrors another Map, but allows new entries to be added without affecting
 * the mirrored map.
 *
 * @param <Key_>
 * @param <Value_>
 */
public class MirrorWithExtrasMap<Key_, Value_> implements Map<Key_, Value_> {
    private final Map<Key_, Value_> delegateMap;
    private final Map<Key_, Value_> extraEntriesMap = new HashMap<>();

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
        if (delegateMap.containsKey(key)) {
            throw new IllegalArgumentException("Cannot set value for key (" + key + ") because it is a mirrored from "
                    + " map (" + delegateMap + ").");
        }
        return extraEntriesMap.put(key, value);
    }

    @Override
    public Value_ remove(Object o) {
        return extraEntriesMap.remove(o);
    }

    @Override
    public void putAll(Map<? extends Key_, ? extends Value_> map) {
        if (!Collections.disjoint(delegateMap.keySet(), map.keySet())) {
            throw new IllegalArgumentException("Cannot put all entries for map (" + map + ") because it shares "
                    + " some keys with the mirrored map (" + delegateMap + ").");
        }
        extraEntriesMap.putAll(map);
    }

    @Override
    public void clear() {
        extraEntriesMap.clear();
    }

    @Override
    public Set<Key_> keySet() {
        return new CompositeSet<>(delegateMap.keySet(), extraEntriesMap.keySet());
    }

    @Override
    public Collection<Value_> values() {
        return new CompositeCollection<>(delegateMap.values(), extraEntriesMap.values());
    }

    @Override
    public Set<Entry<Key_, Value_>> entrySet() {
        return new CompositeSet<>(delegateMap.entrySet(), extraEntriesMap.entrySet());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Map)) {
            return false;
        }
        Map<?, ?> other = (Map) o;
        return entrySet().equals(other.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegateMap, extraEntriesMap);
    }
}
