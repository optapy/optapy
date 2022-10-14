package org.optaplanner.python.translator.types.collections;

import static org.optaplanner.python.translator.types.BuiltinTypes.DICT_TYPE;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.LinkedMap;
import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonTernaryOperators;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.collections.view.DictItemView;
import org.optaplanner.python.translator.types.collections.view.DictKeyView;
import org.optaplanner.python.translator.types.collections.view.DictValueView;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.errors.lookup.KeyError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;
import org.optaplanner.python.translator.util.JavaStringMapMirror;

public class PythonLikeDict extends AbstractPythonLikeObject
        implements Map<PythonLikeObject, PythonLikeObject>, Iterable<PythonLikeObject> {
    public final OrderedMap<PythonLikeObject, PythonLikeObject> delegate;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeDict::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        DICT_TYPE.setConstructor(((positionalArguments, namedArguments) -> {
            namedArguments = (namedArguments != null) ? namedArguments : Map.of();

            PythonLikeDict out = new PythonLikeDict();
            if (positionalArguments.isEmpty()) {
                out.putAll(namedArguments);
                out.remove(PythonString.CALLER_INSTANCE_KEY);
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
                if (out.containsKey(out.remove(PythonString.CALLER_INSTANCE_KEY))) {
                    out.putAll(namedArguments);
                } else {
                    out.putAll(namedArguments);
                    out.remove(PythonString.CALLER_INSTANCE_KEY);
                }
                return out;
            } else {
                throw new ValueError("dict takes 0 or 1 positional arguments, got " + positionalArguments.size());
            }
        }));

        // Unary operators
        DICT_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR, PythonLikeDict.class.getMethod("getKeyIterator"));
        DICT_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, PythonLikeDict.class.getMethod("getSize"));
        DICT_TYPE.addUnaryMethod(PythonUnaryOperator.REVERSED, PythonLikeDict.class.getMethod("reversed"));

        // Binary operators
        DICT_TYPE.addBinaryMethod(PythonBinaryOperators.GET_ITEM,
                PythonLikeDict.class.getMethod("getItemOrError", PythonLikeObject.class));
        DICT_TYPE.addBinaryMethod(PythonBinaryOperators.DELETE_ITEM,
                PythonLikeDict.class.getMethod("removeItemOrError", PythonLikeObject.class));
        DICT_TYPE.addBinaryMethod(PythonBinaryOperators.CONTAINS,
                PythonLikeDict.class.getMethod("isKeyInDict", PythonLikeObject.class));
        DICT_TYPE.addBinaryMethod(PythonBinaryOperators.OR, PythonLikeDict.class.getMethod("binaryOr", PythonLikeDict.class));
        DICT_TYPE.addBinaryMethod(PythonBinaryOperators.INPLACE_OR,
                PythonLikeDict.class.getMethod("binaryInplaceOr", PythonLikeDict.class));

        // Ternary operators
        DICT_TYPE.addTernaryMethod(PythonTernaryOperators.SET_ITEM,
                PythonLikeDict.class.getMethod("setItem", PythonLikeObject.class, PythonLikeObject.class));

        // Other
        DICT_TYPE.addMethod("clear", PythonLikeDict.class.getMethod("clearDict"));
        DICT_TYPE.addMethod("copy", PythonLikeDict.class.getMethod("copy"));
        DICT_TYPE.addMethod("get", PythonLikeDict.class.getMethod("getItemOrNone", PythonLikeObject.class));
        DICT_TYPE.addMethod("get",
                PythonLikeDict.class.getMethod("getItemOrDefault", PythonLikeObject.class, PythonLikeObject.class));
        DICT_TYPE.addMethod("items", PythonLikeDict.class.getMethod("getItems"));
        DICT_TYPE.addMethod("keys", PythonLikeDict.class.getMethod("getKeyView"));
        DICT_TYPE.addMethod("pop", PythonLikeDict.class.getMethod("popItemOrError", PythonLikeObject.class));
        DICT_TYPE.addMethod("pop",
                PythonLikeDict.class.getMethod("popItemOrDefault", PythonLikeObject.class, PythonLikeObject.class));
        DICT_TYPE.addMethod("popitem", PythonLikeDict.class.getMethod("popLast"));
        DICT_TYPE.addMethod("setdefault", PythonLikeDict.class.getMethod("setIfAbsent", PythonLikeObject.class));
        DICT_TYPE.addMethod("setdefault",
                PythonLikeDict.class.getMethod("setIfAbsent", PythonLikeObject.class, PythonLikeObject.class));
        DICT_TYPE.addMethod("update", PythonLikeDict.class.getMethod("update", PythonLikeDict.class));
        DICT_TYPE.addMethod("update", PythonLikeDict.class.getMethod("update", PythonLikeObject.class));
        // TODO: Keyword update
        DICT_TYPE.addMethod("values", PythonLikeDict.class.getMethod("getValueView"));

        return DICT_TYPE;
    }

    public PythonLikeDict() {
        super(DICT_TYPE);
        delegate = new LinkedMap<>();
    }

    public PythonLikeDict(int size) {
        super(DICT_TYPE);
        delegate = new LinkedMap<>(size);
    }

    public PythonLikeDict(OrderedMap<PythonLikeObject, PythonLikeObject> source) {
        super(DICT_TYPE);
        delegate = source;
    }

    public static PythonLikeDict mirror(Map<String, PythonLikeObject> globals) {
        return new PythonLikeDict(new JavaStringMapMirror(globals));
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
