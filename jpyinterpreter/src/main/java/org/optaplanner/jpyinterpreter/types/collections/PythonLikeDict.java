package org.optaplanner.jpyinterpreter.types.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.LinkedMap;
import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonTernaryOperators;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.builtins.UnaryDunderBuiltin;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.collections.view.DictItemView;
import org.optaplanner.jpyinterpreter.types.collections.view.DictKeyView;
import org.optaplanner.jpyinterpreter.types.collections.view.DictValueView;
import org.optaplanner.jpyinterpreter.types.errors.ValueError;
import org.optaplanner.jpyinterpreter.types.errors.lookup.KeyError;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.util.JavaStringMapMirror;

public class PythonLikeDict extends AbstractPythonLikeObject
        implements Map<PythonLikeObject, PythonLikeObject>, Iterable<PythonLikeObject> {
    public final OrderedMap<PythonLikeObject, PythonLikeObject> delegate;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeDict::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        BuiltinTypes.DICT_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
            PythonLikeDict out = new PythonLikeDict();
            if (positionalArguments.isEmpty()) {
                out.putAll(namedArguments);
                return out;
            } else if (positionalArguments.size() == 1) {
                PythonLikeObject from = positionalArguments.get(0);
                if (from instanceof Map) {
                    out.putAll((Map) from);
                } else {
                    Iterator iterator = (Iterator) UnaryDunderBuiltin.ITERATOR.invoke(from);
                    while (iterator.hasNext()) {
                        List item = (List) iterator.next();
                        out.put((PythonLikeObject) item.get(0), (PythonLikeObject) item.get(1));
                    }
                }
                out.putAll(namedArguments);
                return out;
            } else {
                throw new ValueError("dict takes 0 or 1 positional arguments, got " + positionalArguments.size());
            }
        }));

        // Unary operators
        BuiltinTypes.DICT_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR, PythonLikeDict.class.getMethod("getKeyIterator"));
        BuiltinTypes.DICT_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, PythonLikeDict.class.getMethod("getSize"));
        BuiltinTypes.DICT_TYPE.addUnaryMethod(PythonUnaryOperator.REVERSED, PythonLikeDict.class.getMethod("reversed"));

        // Binary operators
        BuiltinTypes.DICT_TYPE.addBinaryMethod(PythonBinaryOperators.GET_ITEM,
                PythonLikeDict.class.getMethod("getItemOrError", PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addBinaryMethod(PythonBinaryOperators.DELETE_ITEM,
                PythonLikeDict.class.getMethod("removeItemOrError", PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addBinaryMethod(PythonBinaryOperators.CONTAINS,
                PythonLikeDict.class.getMethod("isKeyInDict", PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addBinaryMethod(PythonBinaryOperators.OR,
                PythonLikeDict.class.getMethod("binaryOr", PythonLikeDict.class));
        BuiltinTypes.DICT_TYPE.addBinaryMethod(PythonBinaryOperators.INPLACE_OR,
                PythonLikeDict.class.getMethod("binaryInplaceOr", PythonLikeDict.class));

        // Ternary operators
        BuiltinTypes.DICT_TYPE.addTernaryMethod(PythonTernaryOperators.SET_ITEM,
                PythonLikeDict.class.getMethod("setItem", PythonLikeObject.class, PythonLikeObject.class));

        // Other
        BuiltinTypes.DICT_TYPE.addMethod("clear", PythonLikeDict.class.getMethod("clearDict"));
        BuiltinTypes.DICT_TYPE.addMethod("copy", PythonLikeDict.class.getMethod("copy"));
        BuiltinTypes.DICT_TYPE.addMethod("get", PythonLikeDict.class.getMethod("getItemOrNone", PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addMethod("get",
                PythonLikeDict.class.getMethod("getItemOrDefault", PythonLikeObject.class, PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addMethod("items", PythonLikeDict.class.getMethod("getItems"));
        BuiltinTypes.DICT_TYPE.addMethod("keys", PythonLikeDict.class.getMethod("getKeyView"));
        BuiltinTypes.DICT_TYPE.addMethod("pop", PythonLikeDict.class.getMethod("popItemOrError", PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addMethod("pop",
                PythonLikeDict.class.getMethod("popItemOrDefault", PythonLikeObject.class, PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addMethod("popitem", PythonLikeDict.class.getMethod("popLast"));
        BuiltinTypes.DICT_TYPE.addMethod("setdefault", PythonLikeDict.class.getMethod("setIfAbsent", PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addMethod("setdefault",
                PythonLikeDict.class.getMethod("setIfAbsent", PythonLikeObject.class, PythonLikeObject.class));
        BuiltinTypes.DICT_TYPE.addMethod("update", PythonLikeDict.class.getMethod("update", PythonLikeDict.class));
        BuiltinTypes.DICT_TYPE.addMethod("update", PythonLikeDict.class.getMethod("update", PythonLikeObject.class));
        // TODO: Keyword update
        BuiltinTypes.DICT_TYPE.addMethod("values", PythonLikeDict.class.getMethod("getValueView"));

        return BuiltinTypes.DICT_TYPE;
    }

    public PythonLikeDict() {
        super(BuiltinTypes.DICT_TYPE);
        delegate = new LinkedMap<>();
    }

    public PythonLikeDict(int size) {
        super(BuiltinTypes.DICT_TYPE);
        delegate = new LinkedMap<>(size);
    }

    public PythonLikeDict(OrderedMap<PythonLikeObject, PythonLikeObject> source) {
        super(BuiltinTypes.DICT_TYPE);
        delegate = source;
    }

    public static PythonLikeDict mirror(Map<String, PythonLikeObject> globals) {
        return new PythonLikeDict(new JavaStringMapMirror(globals));
    }

    public PythonLikeTuple toFlattenKeyValueTuple() {
        return PythonLikeTuple.fromList(delegate.entrySet().stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
    }

    public PythonLikeDict copy() {
        return new PythonLikeDict(new LinkedMap<>(delegate));
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

    public PythonInteger getSize() {
        return PythonInteger.valueOf(delegate.size());
    }

    public PythonIterator<PythonLikeObject> getKeyIterator() {
        return new PythonIterator<>(delegate.keySet().iterator());
    }

    public PythonLikeObject getItemOrError(PythonLikeObject key) {
        PythonLikeObject out = delegate.get(key);
        if (out == null) {
            throw new KeyError(key.toString());
        }
        return out;
    }

    public PythonLikeObject getItemOrNone(PythonLikeObject key) {
        PythonLikeObject out = delegate.get(key);
        if (out == null) {
            return PythonNone.INSTANCE;
        }
        return out;
    }

    public PythonLikeObject getItemOrDefault(PythonLikeObject key, PythonLikeObject defaultValue) {
        PythonLikeObject out = delegate.get(key);
        if (out == null) {
            return defaultValue;
        }
        return out;
    }

    public DictItemView getItems() {
        return new DictItemView(this);
    }

    public DictKeyView getKeyView() {
        return new DictKeyView(this);
    }

    public PythonNone setItem(PythonLikeObject key, PythonLikeObject value) {
        delegate.put(key, value);
        return PythonNone.INSTANCE;
    }

    public PythonNone removeItemOrError(PythonLikeObject key) {
        if (delegate.remove(key) == null) {
            throw new KeyError(key.toString());
        }
        return PythonNone.INSTANCE;
    }

    public PythonBoolean isKeyInDict(PythonLikeObject key) {
        return PythonBoolean.valueOf(delegate.containsKey(key));
    }

    public PythonNone clearDict() {
        delegate.clear();
        return PythonNone.INSTANCE;
    }

    public PythonLikeObject popItemOrError(PythonLikeObject key) {
        PythonLikeObject out = delegate.remove(key);
        if (out == null) {
            throw new KeyError(key.toString());
        }
        return out;
    }

    public PythonLikeObject popItemOrDefault(PythonLikeObject key, PythonLikeObject defaultValue) {
        PythonLikeObject out = delegate.remove(key);
        if (out == null) {
            return defaultValue;
        }
        return out;
    }

    public PythonLikeObject popLast() {
        if (delegate.isEmpty()) {
            throw new KeyError("popitem(): dictionary is empty");
        }

        PythonLikeObject lastKey = delegate.lastKey();
        return PythonLikeTuple.fromList(List.of(lastKey, delegate.remove(lastKey)));
    }

    public PythonIterator<PythonLikeObject> reversed() {
        if (delegate.isEmpty()) {
            return new PythonIterator<>(Collections.emptyIterator());
        }

        final PythonLikeObject lastKey = delegate.lastKey();
        return new PythonIterator<>(new Iterator<>() {
            PythonLikeObject current = lastKey;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public PythonLikeObject next() {
                PythonLikeObject out = current;
                current = delegate.previousKey(current);
                return out;
            }
        });
    }

    public PythonLikeObject setIfAbsent(PythonLikeObject key) {
        PythonLikeObject value = delegate.get(key);
        if (value != null) {
            return value;
        }
        delegate.put(key, PythonNone.INSTANCE);
        return PythonNone.INSTANCE;
    }

    public PythonLikeObject setIfAbsent(PythonLikeObject key, PythonLikeObject defaultValue) {
        PythonLikeObject value = delegate.get(key);
        if (value != null) {
            return value;
        }
        delegate.put(key, defaultValue);
        return defaultValue;
    }

    public PythonNone update(PythonLikeDict other) {
        delegate.putAll(other.delegate);
        return PythonNone.INSTANCE;
    }

    public PythonNone update(PythonLikeObject iterable) {
        Iterator<List<PythonLikeObject>> iterator =
                (Iterator<List<PythonLikeObject>>) UnaryDunderBuiltin.ITERATOR.invoke(iterable);

        while (iterator.hasNext()) {
            List<PythonLikeObject> keyValuePair = iterator.next();
            delegate.put(keyValuePair.get(0), keyValuePair.get(1));
        }

        return PythonNone.INSTANCE;
    }

    public DictValueView getValueView() {
        return new DictValueView(this);
    }

    public PythonLikeDict binaryOr(PythonLikeDict other) {
        PythonLikeDict out = new PythonLikeDict();
        out.delegate.putAll(delegate);
        out.delegate.putAll(other.delegate);
        return out;
    }

    public PythonLikeDict binaryInplaceOr(PythonLikeDict other) {
        delegate.putAll(other.delegate);
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
