package org.optaplanner.jpyinterpreter.types.collections.view;

import java.util.Collection;

import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.builtins.UnaryDunderBuiltin;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonIterator;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.util.IteratorUtils;

public class DictValueView extends AbstractPythonLikeObject {
    public final static PythonLikeType $TYPE = BuiltinTypes.DICT_VALUE_VIEW_TYPE;

    final PythonLikeDict mapping;
    final Collection<PythonLikeObject> valueCollection;

    static {
        PythonOverloadImplementor.deferDispatchesFor(DictValueView::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Unary
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH,
                DictValueView.class.getMethod("getValuesSize"));
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR,
                DictValueView.class.getMethod("getValueIterator"));
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.REVERSED,
                DictValueView.class.getMethod("getReversedValueIterator"));
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                DictValueView.class.getMethod("toRepresentation"));
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                DictValueView.class.getMethod("toRepresentation"));

        // Binary
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addBinaryMethod(PythonBinaryOperators.CONTAINS,
                DictValueView.class.getMethod("containsValue", PythonLikeObject.class));

        return BuiltinTypes.DICT_VALUE_VIEW_TYPE;
    }

    public DictValueView(PythonLikeDict mapping) {
        super(BuiltinTypes.DICT_VALUE_VIEW_TYPE);
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
