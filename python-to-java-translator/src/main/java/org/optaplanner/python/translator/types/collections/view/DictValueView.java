package org.optaplanner.python.translator.types.collections.view;

import java.util.Collection;

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
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonInteger;
import org.optaplanner.python.translator.util.IteratorUtils;

public class DictValueView extends AbstractPythonLikeObject {
    public final static PythonLikeType DICT_VALUE_VIEW_TYPE = new PythonLikeType("dict_values", DictValueView.class);
    public final static PythonLikeType $TYPE = DICT_VALUE_VIEW_TYPE;

    final PythonLikeDict mapping;
    final Collection<PythonLikeObject> valueCollection;

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(DICT_VALUE_VIEW_TYPE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method.", e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Unary
        DICT_VALUE_VIEW_TYPE.addMethod(PythonUnaryOperator.LENGTH, DictValueView.class.getMethod("getValuesSize"));
        DICT_VALUE_VIEW_TYPE.addMethod(PythonUnaryOperator.ITERATOR, DictValueView.class.getMethod("getValueIterator"));
        DICT_VALUE_VIEW_TYPE.addMethod(PythonUnaryOperator.REVERSED, DictValueView.class.getMethod("getReversedValueIterator"));
        DICT_VALUE_VIEW_TYPE.addMethod(PythonUnaryOperator.AS_STRING, DictValueView.class.getMethod("toRepresentation"));
        DICT_VALUE_VIEW_TYPE.addMethod(PythonUnaryOperator.REPRESENTATION, DictValueView.class.getMethod("toRepresentation"));

        // Binary
        DICT_VALUE_VIEW_TYPE.addMethod(PythonBinaryOperators.CONTAINS,
                DictValueView.class.getMethod("containsValue", PythonLikeObject.class));
    }

    public DictValueView(PythonLikeDict mapping) {
        super(DICT_VALUE_VIEW_TYPE);
        this.mapping = mapping;
        this.valueCollection = mapping.delegate.values();
        __setAttribute("mapping", mapping);
    }

    public PythonInteger getValuesSize() {
        return PythonInteger.valueOf(valueCollection.size());
    }

    public PythonIterator<PythonLikeObject> getValueIterator() {
        return new PythonIterator<>(valueCollection.iterator());
    }

    public PythonBoolean containsValue(PythonLikeObject value) {
        return PythonBoolean.valueOf(valueCollection.contains(value));
    }

    public PythonIterator<PythonLikeObject> getReversedValueIterator() {
        return new PythonIterator<>(IteratorUtils.iteratorMap(mapping.reversed(), mapping::get));
    }

    public PythonString toRepresentation() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("dict_values([");

        for (PythonLikeObject value : valueCollection) {
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke(value));
            out.append(", ");
        }
        out.delete(out.length() - 2, out.length());
        out.append("])");

        return out.toString();
    }
}
