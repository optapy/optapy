package org.optaplanner.python.translator.types;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.errors.ValueError;

public class PythonLikeSet extends AbstractPythonLikeObject implements Set<PythonLikeObject> {
    final Set<PythonLikeObject> delegate;

    public final static PythonLikeType SET_TYPE = new PythonLikeType("set", PythonLikeSet.class);

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(SET_TYPE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        SET_TYPE.setConstructor((positionalArguments, namedArguments) -> {
            if (positionalArguments.size() == 0) {
                return new PythonLikeSet();
            } else if (positionalArguments.size() == 1) {
                PythonLikeSet out = new PythonLikeSet();
                out.update(positionalArguments.get(0));
                return out;
            } else {
                throw new ValueError("set expects 0 or 1 arguments, got " + positionalArguments.size());
            }
        });

        // Unary
        SET_TYPE.addMethod(PythonUnaryOperator.LENGTH, PythonLikeSet.class.getMethod("getLength"));
        SET_TYPE.addMethod(PythonUnaryOperator.ITERATOR, PythonLikeSet.class.getMethod("getIterator"));

        // Binary
        SET_TYPE.addMethod(PythonBinaryOperators.CONTAINS,
                PythonLikeSet.class.getMethod("containsItem", PythonLikeObject.class));

        // Other
        SET_TYPE.addMethod("add", PythonLikeSet.class.getMethod("addItem", PythonLikeObject.class));
        SET_TYPE.addMethod("discard", PythonLikeSet.class.getMethod("discard", PythonLikeObject.class));
        SET_TYPE.addMethod("update", PythonLikeSet.class.getMethod("update", PythonLikeObject.class));
        SET_TYPE.addMethod("clear", PythonLikeSet.class.getMethod("clearSet"));
        SET_TYPE.addMethod("copy", PythonLikeSet.class.getMethod("copy"));
        // TODO: Add remaining operations from https://docs.python.org/3/library/stdtypes.html#set-types-set-frozenset
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

    public PythonInteger getLength() {
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

    public PythonBoolean containsItem(PythonLikeObject query) {
        return PythonBoolean.valueOf(delegate.contains(query));
    }

    @Override
    public Iterator<PythonLikeObject> iterator() {
        return delegate.iterator();
    }

    public PythonIterator getIterator() {
        return new PythonIterator(delegate.iterator());
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

    public PythonNone addItem(PythonLikeObject pythonLikeObject) {
        delegate.add(pythonLikeObject);
        return PythonNone.INSTANCE;
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    public PythonNone discard(PythonLikeObject object) {
        delegate.remove(object);
        return PythonNone.INSTANCE;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return delegate.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends PythonLikeObject> collection) {
        return delegate.addAll(collection);
    }

    public PythonNone update(PythonLikeObject collection) {
        if (collection instanceof Collection) {
            delegate.addAll((Collection<? extends PythonLikeObject>) collection);
        } else {
            Iterator<PythonLikeObject> iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(collection);
            iterator.forEachRemaining(delegate::add);
        }
        return PythonNone.INSTANCE;
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

    public PythonNone clearSet() {
        delegate.clear();
        return PythonNone.INSTANCE;
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
