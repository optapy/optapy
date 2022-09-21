package org.optaplanner.python.translator.types.collections;

import static org.optaplanner.python.translator.types.BuiltinTypes.SET_TYPE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.errors.ValueError;
import org.optaplanner.python.translator.types.errors.lookup.KeyError;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class PythonLikeSet extends AbstractPythonLikeObject implements Set<PythonLikeObject> {
    public final Set<PythonLikeObject> delegate;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeSet::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
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
        SET_TYPE.addMethod("isdisjoint", PythonLikeSet.class.getMethod("isDisjoint", PythonLikeSet.class));
        SET_TYPE.addMethod("isdisjoint", PythonLikeSet.class.getMethod("isDisjoint", PythonLikeFrozenSet.class));

        SET_TYPE.addMethod("issubset", PythonLikeSet.class.getMethod("isSubset", PythonLikeSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonLikeSet.class.getMethod("isSubset", PythonLikeSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.LESS_THAN,
                PythonLikeSet.class.getMethod("isStrictSubset", PythonLikeSet.class));
        SET_TYPE.addMethod("issubset", PythonLikeSet.class.getMethod("isSubset", PythonLikeFrozenSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                PythonLikeSet.class.getMethod("isSubset", PythonLikeFrozenSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.LESS_THAN,
                PythonLikeSet.class.getMethod("isStrictSubset", PythonLikeFrozenSet.class));

        SET_TYPE.addMethod("issuperset", PythonLikeSet.class.getMethod("isSuperset", PythonLikeSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonLikeSet.class.getMethod("isSuperset", PythonLikeSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                PythonLikeSet.class.getMethod("isStrictSuperset", PythonLikeSet.class));
        SET_TYPE.addMethod("issuperset", PythonLikeSet.class.getMethod("isSuperset", PythonLikeFrozenSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                PythonLikeSet.class.getMethod("isSuperset", PythonLikeFrozenSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                PythonLikeSet.class.getMethod("isStrictSuperset", PythonLikeFrozenSet.class));

        SET_TYPE.addMethod("union", PythonLikeSet.class.getMethod("union", PythonLikeSet.class));
        SET_TYPE.addMethod("union", PythonLikeSet.class.getMethod("union", PythonLikeFrozenSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.OR, PythonLikeSet.class.getMethod("union", PythonLikeSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.OR, PythonLikeSet.class.getMethod("union", PythonLikeFrozenSet.class));

        SET_TYPE.addMethod("intersection", PythonLikeSet.class.getMethod("intersection", PythonLikeSet.class));
        SET_TYPE.addMethod("intersection", PythonLikeSet.class.getMethod("intersection", PythonLikeFrozenSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.AND, PythonLikeSet.class.getMethod("intersection", PythonLikeSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.AND, PythonLikeSet.class.getMethod("intersection", PythonLikeFrozenSet.class));

        SET_TYPE.addMethod("difference", PythonLikeSet.class.getMethod("difference", PythonLikeSet.class));
        SET_TYPE.addMethod("difference", PythonLikeSet.class.getMethod("difference", PythonLikeFrozenSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.SUBTRACT, PythonLikeSet.class.getMethod("difference", PythonLikeSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                PythonLikeSet.class.getMethod("difference", PythonLikeFrozenSet.class));

        SET_TYPE.addMethod("symmetric_difference", PythonLikeSet.class.getMethod("symmetricDifference", PythonLikeSet.class));
        SET_TYPE.addMethod("symmetric_difference",
                PythonLikeSet.class.getMethod("symmetricDifference", PythonLikeFrozenSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.XOR,
                PythonLikeSet.class.getMethod("symmetricDifference", PythonLikeSet.class));
        SET_TYPE.addMethod(PythonBinaryOperators.XOR,
                PythonLikeSet.class.getMethod("symmetricDifference", PythonLikeFrozenSet.class));

        SET_TYPE.addMethod("update", PythonLikeSet.class.getMethod("update", PythonLikeObject.class));
        SET_TYPE.addMethod(PythonBinaryOperators.INPLACE_OR,
                PythonLikeSet.class.getMethod("updateWithResult", PythonLikeObject.class));

        SET_TYPE.addMethod("intersection_update", PythonLikeSet.class.getMethod("intersectionUpdate", PythonLikeObject.class));
        SET_TYPE.addMethod(PythonBinaryOperators.INPLACE_AND,
                PythonLikeSet.class.getMethod("intersectionUpdateWithResult", PythonLikeObject.class));

        SET_TYPE.addMethod("difference_update", PythonLikeSet.class.getMethod("differenceUpdate", PythonLikeObject.class));
        SET_TYPE.addMethod(PythonBinaryOperators.INPLACE_SUBTRACT,
                PythonLikeSet.class.getMethod("differenceUpdateWithResult", PythonLikeObject.class));

        SET_TYPE.addMethod("symmetric_difference_update",
                PythonLikeSet.class.getMethod("symmetricDifferenceUpdate", PythonLikeObject.class));
        SET_TYPE.addMethod(PythonBinaryOperators.INPLACE_XOR,
                PythonLikeSet.class.getMethod("symmetricDifferenceUpdateWithResult", PythonLikeObject.class));

        SET_TYPE.addMethod("add", PythonLikeSet.class.getMethod("addItem", PythonLikeObject.class));
        SET_TYPE.addMethod("remove", PythonLikeSet.class.getMethod("removeOrError", PythonLikeObject.class));
        SET_TYPE.addMethod("discard", PythonLikeSet.class.getMethod("discard", PythonLikeObject.class));
        SET_TYPE.addMethod("pop", PythonLikeSet.class.getMethod("pop"));
        SET_TYPE.addMethod("clear", PythonLikeSet.class.getMethod("clearSet"));
        SET_TYPE.addMethod("copy", PythonLikeSet.class.getMethod("copy"));

        return SET_TYPE;
    }

    public PythonLikeSet() {
        super(SET_TYPE);
        delegate = new HashSet<>();
    }

    public PythonLikeSet(int size) {
        super(SET_TYPE);
        delegate = new HashSet<>(size);
    }

    // Required for bytecode generation
    @SuppressWarnings("unused")
    public void reverseAdd(PythonLikeObject item) {
        delegate.add(item);
    }

    public PythonBoolean isDisjoint(PythonLikeSet other) {
        return PythonBoolean.valueOf(Collections.disjoint(delegate, other.delegate));
    }

    public PythonBoolean isDisjoint(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(Collections.disjoint(delegate, other.delegate));
    }

    public PythonBoolean isSubset(PythonLikeSet other) {
        return PythonBoolean.valueOf(other.delegate.containsAll(delegate));
    }

    public PythonBoolean isSubset(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(other.delegate.containsAll(delegate));
    }

    public PythonBoolean isStrictSubset(PythonLikeSet other) {
        return PythonBoolean.valueOf(other.delegate.containsAll(delegate) && !delegate.containsAll(other.delegate));
    }

    public PythonBoolean isStrictSubset(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(other.delegate.containsAll(delegate) && !delegate.containsAll(other.delegate));
    }

    public PythonBoolean isSuperset(PythonLikeSet other) {
        return PythonBoolean.valueOf(delegate.containsAll(other.delegate));
    }

    public PythonBoolean isSuperset(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(delegate.containsAll(other.delegate));
    }

    public PythonBoolean isStrictSuperset(PythonLikeSet other) {
        return PythonBoolean.valueOf(delegate.containsAll(other.delegate) && !other.delegate.containsAll(delegate));
    }

    public PythonBoolean isStrictSuperset(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(delegate.containsAll(other.delegate) && !other.delegate.containsAll(delegate));
    }

    public PythonLikeSet union(PythonLikeSet other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(delegate);
        out.delegate.addAll(other.delegate);
        return out;
    }

    public PythonLikeSet union(PythonLikeFrozenSet other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(delegate);
        out.delegate.addAll(other.delegate);
        return out;
    }

    public PythonLikeSet intersection(PythonLikeSet other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(delegate);
        out.delegate.retainAll(other.delegate);
        return out;
    }

    public PythonLikeSet intersection(PythonLikeFrozenSet other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(delegate);
        out.delegate.retainAll(other.delegate);
        return out;
    }

    public PythonLikeSet difference(PythonLikeSet other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(delegate);
        out.delegate.removeAll(other.delegate);
        return out;
    }

    public PythonLikeSet difference(PythonLikeFrozenSet other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(delegate);
        out.delegate.removeAll(other.delegate);
        return out;
    }

    public PythonLikeSet symmetricDifference(PythonLikeSet other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(delegate);
        other.delegate.stream() // for each item in other
                .filter(Predicate.not(out.delegate::add)) // add each item
                .forEach(out.delegate::remove); // add return false iff item already in set, so this remove
        // all items in both this and other
        return out;
    }

    public PythonLikeSet symmetricDifference(PythonLikeFrozenSet other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(delegate);
        other.delegate.stream() // for each item in other
                .filter(Predicate.not(out.delegate::add)) // add each item
                .forEach(out.delegate::remove); // add return false iff item already in set, so this remove
        // all items in both this and other
        return out;
    }

    public PythonLikeSet updateWithResult(PythonLikeObject collection) {
        if (collection instanceof Collection) {
            delegate.addAll((Collection<? extends PythonLikeObject>) collection);
        } else {
            Iterator<PythonLikeObject> iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(collection);
            iterator.forEachRemaining(delegate::add);
        }
        return this;
    }

    public PythonNone update(PythonLikeObject collection) {
        updateWithResult(collection);
        return PythonNone.INSTANCE;
    }

    public PythonLikeSet intersectionUpdateWithResult(PythonLikeObject collection) {
        if (collection instanceof Collection) {
            delegate.retainAll((Collection<? extends PythonLikeObject>) collection);
        } else {
            Iterator<PythonLikeObject> iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(collection);
            Set<PythonLikeObject> temp = new HashSet<>();
            iterator.forEachRemaining(temp::add);
            delegate.retainAll(temp);
        }
        return this;
    }

    public PythonNone intersectionUpdate(PythonLikeObject collection) {
        intersectionUpdateWithResult(collection);
        return PythonNone.INSTANCE;
    }

    public PythonLikeSet differenceUpdateWithResult(PythonLikeObject collection) {
        if (collection instanceof Collection) {
            delegate.removeAll((Collection<? extends PythonLikeObject>) collection);
        } else {
            Iterator<PythonLikeObject> iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(collection);
            iterator.forEachRemaining(delegate::remove);
        }
        return this;
    }

    public PythonNone differenceUpdate(PythonLikeObject collection) {
        differenceUpdateWithResult(collection);
        return PythonNone.INSTANCE;
    }

    public PythonLikeSet symmetricDifferenceUpdateWithResult(PythonLikeObject collection) {
        if (collection instanceof Collection) {
            Collection<PythonLikeObject> otherSet = (Collection<PythonLikeObject>) collection;
            Set<PythonLikeObject> temp = new HashSet<>(delegate);
            temp.retainAll(otherSet);
            delegate.addAll(otherSet);
            delegate.removeAll(temp);
        } else {
            Iterator<PythonLikeObject> iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(collection);
            Set<PythonLikeObject> encountered = new HashSet<>(delegate);
            while (iterator.hasNext()) {
                PythonLikeObject item = iterator.next();
                if (encountered.contains(item)) {
                    continue;
                }

                if (delegate.contains(item)) {
                    delegate.remove(item);
                } else {
                    delegate.add(item);
                }

                encountered.add(item);
            }
        }
        return this;
    }

    public PythonNone symmetricDifferenceUpdate(PythonLikeObject collection) {
        symmetricDifferenceUpdateWithResult(collection);
        return PythonNone.INSTANCE;
    }

    public PythonNone addItem(PythonLikeObject pythonLikeObject) {
        delegate.add(pythonLikeObject);
        return PythonNone.INSTANCE;
    }

    public PythonNone discard(PythonLikeObject object) {
        delegate.remove(object);
        return PythonNone.INSTANCE;
    }

    public PythonNone removeOrError(PythonLikeObject object) {
        if (!delegate.remove(object)) {
            throw new KeyError("set (" + this + ") does not contain the specified element (" + object + ").");
        }
        return PythonNone.INSTANCE;
    }

    public PythonLikeObject pop() {
        if (delegate.isEmpty()) {
            throw new KeyError("set (" + this + ") is empty.");
        }
        PythonLikeObject out = delegate.iterator().next();
        delegate.remove(out);
        return out;
    }

    public PythonLikeSet copy() {
        PythonLikeSet copy = new PythonLikeSet();
        copy.addAll(delegate);
        return copy;
    }

    public PythonNone clearSet() {
        delegate.clear();
        return PythonNone.INSTANCE;
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(delegate.size());
    }

    public PythonBoolean containsItem(PythonLikeObject query) {
        return PythonBoolean.valueOf(delegate.contains(query));
    }

    // Java Set methods

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
            return containsAll(other) && other.containsAll(this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}