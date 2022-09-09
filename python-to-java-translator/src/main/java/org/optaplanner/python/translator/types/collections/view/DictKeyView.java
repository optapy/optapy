package org.optaplanner.python.translator.types.collections.view;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import org.optaplanner.python.translator.PythonBinaryOperators;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.builtins.UnaryDunderBuiltin;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.collections.PythonIterator;
import org.optaplanner.python.translator.types.collections.PythonLikeDict;
import org.optaplanner.python.translator.types.collections.PythonLikeSet;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;

public class DictKeyView extends AbstractPythonLikeObject {
    public final static PythonLikeType DICT_KEY_VIEW_TYPE = new PythonLikeType("dict_keys", DictKeyView.class);
    public final static PythonLikeType $TYPE = DICT_KEY_VIEW_TYPE;

    final PythonLikeDict mapping;
    final Set<PythonLikeObject> keySet;

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(DICT_KEY_VIEW_TYPE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Unary
        DICT_KEY_VIEW_TYPE.addMethod(PythonUnaryOperator.LENGTH, DictKeyView.class.getMethod("getKeysSize"));
        DICT_KEY_VIEW_TYPE.addMethod(PythonUnaryOperator.ITERATOR, DictKeyView.class.getMethod("getKeysIterator"));
        DICT_KEY_VIEW_TYPE.addMethod(PythonUnaryOperator.REVERSED, DictKeyView.class.getMethod("getReversedKeyIterator"));
        DICT_KEY_VIEW_TYPE.addMethod(PythonUnaryOperator.AS_STRING, DictKeyView.class.getMethod("toRepresentation"));
        DICT_KEY_VIEW_TYPE.addMethod(PythonUnaryOperator.REPRESENTATION, DictKeyView.class.getMethod("toRepresentation"));

        // Binary
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.CONTAINS,
                DictKeyView.class.getMethod("containsKey", PythonLikeObject.class));

        // Set methods
        DICT_KEY_VIEW_TYPE.addMethod("isdisjoint", DictKeyView.class.getMethod("isDisjoint", DictKeyView.class));
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                DictKeyView.class.getMethod("isSubset", DictKeyView.class));
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.LESS_THAN,
                DictKeyView.class.getMethod("isStrictSubset", DictKeyView.class));
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                DictKeyView.class.getMethod("isSuperset", DictKeyView.class));
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                DictKeyView.class.getMethod("isStrictSuperset", DictKeyView.class));
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.OR, DictKeyView.class.getMethod("union", DictKeyView.class));
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.AND,
                DictKeyView.class.getMethod("intersection", DictKeyView.class));
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                DictKeyView.class.getMethod("difference", DictKeyView.class));
        DICT_KEY_VIEW_TYPE.addMethod(PythonBinaryOperators.XOR,
                DictKeyView.class.getMethod("symmetricDifference", DictKeyView.class));
    }

    public DictKeyView(PythonLikeDict mapping) {
        super(DICT_KEY_VIEW_TYPE);
        this.mapping = mapping;
        this.keySet = mapping.delegate.keySet();
        __setAttribute("mapping", mapping);
    }

    public PythonInteger getKeysSize() {
        return PythonInteger.valueOf(keySet.size());
    }

    public PythonIterator<PythonLikeObject> getKeysIterator() {
        return new PythonIterator<>(keySet.iterator());
    }

    public PythonBoolean containsKey(PythonLikeObject key) {
        return PythonBoolean.valueOf(keySet.contains(key));
    }

    public PythonIterator<PythonLikeObject> getReversedKeyIterator() {
        return mapping.reversed();
    }

    public PythonBoolean isDisjoint(DictKeyView other) {
        return PythonBoolean.valueOf(Collections.disjoint(keySet, other.keySet));
    }

    public PythonBoolean isSubset(DictKeyView other) {
        return PythonBoolean.valueOf(other.keySet.containsAll(keySet));
    }

    public PythonBoolean isStrictSubset(DictKeyView other) {
        return PythonBoolean.valueOf(other.keySet.containsAll(keySet) && !keySet.containsAll(other.keySet));
    }

    public PythonBoolean isSuperset(DictKeyView other) {
        return PythonBoolean.valueOf(keySet.containsAll(other.keySet));
    }

    public PythonBoolean isStrictSuperset(DictKeyView other) {
        return PythonBoolean.valueOf(keySet.containsAll(other.keySet) && !other.keySet.containsAll(keySet));
    }

    public PythonLikeSet union(DictKeyView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(keySet);
        out.delegate.addAll(other.keySet);
        return out;
    }

    public PythonLikeSet intersection(DictKeyView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(keySet);
        out.delegate.retainAll(other.keySet);
        return out;
    }

    public PythonLikeSet difference(DictKeyView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(keySet);
        out.delegate.removeAll(other.keySet);
        return out;
    }

    public PythonLikeSet symmetricDifference(DictKeyView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(keySet);
        other.keySet.stream() // for each item in other
                .filter(Predicate.not(out.delegate::add)) // add each item
                .forEach(out.delegate::remove); // add return false iff item already in set, so this remove
        // all items in both this and other
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DictKeyView) {
            DictKeyView other = (DictKeyView) o;
            return keySet.equals(other.keySet);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return keySet.hashCode();
    }

    public PythonString toRepresentation() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("dict_keys([");

        for (PythonLikeObject key : keySet) {
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke(key));
            out.append(", ");
        }
        out.delete(out.length() - 2, out.length());
        out.append("])");

        return out.toString();
    }
}
