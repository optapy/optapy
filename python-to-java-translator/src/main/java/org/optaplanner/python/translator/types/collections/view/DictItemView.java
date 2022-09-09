package org.optaplanner.python.translator.types.collections.view;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;
import org.optaplanner.python.translator.util.IteratorUtils;

public class DictItemView extends AbstractPythonLikeObject {
    public final static PythonLikeType DICT_ITEM_VIEW_TYPE = new PythonLikeType("dict_items", DictItemView.class);
    public final static PythonLikeType $TYPE = DICT_ITEM_VIEW_TYPE;

    final PythonLikeDict mapping;
    final Set<Map.Entry<PythonLikeObject, PythonLikeObject>> entrySet;

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(DICT_ITEM_VIEW_TYPE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Unary
        DICT_ITEM_VIEW_TYPE.addMethod(PythonUnaryOperator.LENGTH, DictItemView.class.getMethod("getItemsSize"));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonUnaryOperator.ITERATOR, DictItemView.class.getMethod("getItemsIterator"));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonUnaryOperator.REVERSED, DictItemView.class.getMethod("getReversedItemIterator"));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonUnaryOperator.AS_STRING, DictItemView.class.getMethod("toRepresentation"));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonUnaryOperator.REPRESENTATION, DictItemView.class.getMethod("toRepresentation"));

        // Binary
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.CONTAINS,
                DictItemView.class.getMethod("containsItem", PythonLikeObject.class));

        // Set methods
        DICT_ITEM_VIEW_TYPE.addMethod("isdisjoint", DictItemView.class.getMethod("isDisjoint", DictItemView.class));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.LESS_THAN_OR_EQUAL,
                DictItemView.class.getMethod("isSubset", DictItemView.class));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.LESS_THAN,
                DictItemView.class.getMethod("isStrictSubset", DictItemView.class));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN_OR_EQUAL,
                DictItemView.class.getMethod("isSuperset", DictItemView.class));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.GREATER_THAN,
                DictItemView.class.getMethod("isStrictSuperset", DictItemView.class));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.OR, DictItemView.class.getMethod("union", DictItemView.class));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.AND,
                DictItemView.class.getMethod("intersection", DictItemView.class));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.SUBTRACT,
                DictItemView.class.getMethod("difference", DictItemView.class));
        DICT_ITEM_VIEW_TYPE.addMethod(PythonBinaryOperators.XOR,
                DictItemView.class.getMethod("symmetricDifference", DictItemView.class));
    }

    public DictItemView(PythonLikeDict mapping) {
        super(DICT_ITEM_VIEW_TYPE);
        this.mapping = mapping;
        this.entrySet = mapping.delegate.entrySet();
        __setAttribute("mapping", mapping);
    }

    private List<PythonLikeObject> getEntriesAsTuples() {
        List<PythonLikeObject> out = new ArrayList<>(entrySet.size());
        for (Map.Entry<PythonLikeObject, PythonLikeObject> entry : entrySet) {
            out.add(PythonLikeTuple.fromList(List.of(entry.getKey(), entry.getValue())));
        }
        return out;
    }

    public PythonInteger getItemsSize() {
        return PythonInteger.valueOf(entrySet.size());
    }

    public PythonIterator<PythonLikeObject> getItemsIterator() {
        return new PythonIterator<>(
                IteratorUtils.iteratorMap(entrySet.iterator(),
                        entry -> PythonLikeTuple.fromList(List.of(entry.getKey(), entry.getValue()))));
    }

    public PythonBoolean containsItem(PythonLikeObject o) {
        if (o instanceof PythonLikeTuple) {
            PythonLikeTuple item = (PythonLikeTuple) o;
            if (item.size() != 2) {
                return PythonBoolean.FALSE;
            }
            Map.Entry<PythonLikeObject, PythonLikeObject> entry = new AbstractMap.SimpleEntry<>(item.get(0), item.get(1));
            return PythonBoolean.valueOf(entrySet.contains(entry));
        } else {
            return PythonBoolean.FALSE;
        }
    }

    public PythonIterator<PythonLikeObject> getReversedItemIterator() {
        return new PythonIterator<>(IteratorUtils.iteratorMap(mapping.reversed(),
                key -> PythonLikeTuple.fromList(List.of(key, mapping.delegate.get(key)))));
    }

    public PythonBoolean isDisjoint(DictItemView other) {
        return PythonBoolean.valueOf(Collections.disjoint(entrySet, other.entrySet));
    }

    public PythonBoolean isSubset(DictItemView other) {
        return PythonBoolean.valueOf(other.entrySet.containsAll(entrySet));
    }

    public PythonBoolean isStrictSubset(DictItemView other) {
        return PythonBoolean.valueOf(other.entrySet.containsAll(entrySet) && !entrySet.containsAll(other.entrySet));
    }

    public PythonBoolean isSuperset(DictItemView other) {
        return PythonBoolean.valueOf(entrySet.containsAll(other.entrySet));
    }

    public PythonBoolean isStrictSuperset(DictItemView other) {
        return PythonBoolean.valueOf(entrySet.containsAll(other.entrySet) && !other.entrySet.containsAll(entrySet));
    }

    public PythonLikeSet union(DictItemView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(getEntriesAsTuples());
        out.delegate.addAll(other.getEntriesAsTuples());
        return out;
    }

    public PythonLikeSet intersection(DictItemView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(getEntriesAsTuples());
        out.delegate.retainAll(other.getEntriesAsTuples());
        return out;
    }

    public PythonLikeSet difference(DictItemView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(getEntriesAsTuples());
        other.getEntriesAsTuples().forEach(out.delegate::remove);
        return out;
    }

    public PythonLikeSet symmetricDifference(DictItemView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(getEntriesAsTuples());
        other.getEntriesAsTuples().stream() // for each item in other
                .filter(Predicate.not(out.delegate::add)) // add each item
                .forEach(out.delegate::remove); // add return false iff item already in set, so this remove
        // all items in both this and other
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DictItemView) {
            DictItemView other = (DictItemView) o;
            return entrySet.equals(other.entrySet);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return entrySet.hashCode();
    }

    public PythonString toRepresentation() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("dict_items([");

        for (Map.Entry<PythonLikeObject, PythonLikeObject> entry : entrySet) {
            out.append("(");
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke(entry.getKey()));
            out.append(", ");
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke(entry.getValue()));
            out.append("), ");
        }
        out.delete(out.length() - 2, out.length());
        out.append("])");

        return out.toString();
    }
}
