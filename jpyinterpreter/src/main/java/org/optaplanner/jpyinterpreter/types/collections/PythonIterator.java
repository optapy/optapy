package org.optaplanner.jpyinterpreter.types.collections;

import java.util.Iterator;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonOverloadImplementor;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.types.AbstractPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.errors.StopIteration;

public class PythonIterator<T> extends AbstractPythonLikeObject implements Iterator<T> {
    private final Iterator<T> delegate;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonIterator::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        BuiltinTypes.ITERATOR_TYPE.addUnaryMethod(PythonUnaryOperator.NEXT, PythonIterator.class.getMethod("nextPythonItem"));
        BuiltinTypes.ITERATOR_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR, PythonIterator.class.getMethod("getIterator"));
        return BuiltinTypes.ITERATOR_TYPE;
    }

    public PythonIterator(PythonLikeType type) {
        super(type);
        this.delegate = this;
    }

    public PythonIterator(Iterator<T> delegate) {
        super(BuiltinTypes.ITERATOR_TYPE);
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        if (!delegate.hasNext()) {
            throw new StopIteration();
        }
        return delegate.next();
    }

    public PythonLikeObject nextPythonItem() {
        if (!delegate.hasNext()) {
            throw new StopIteration();
        }
        return (PythonLikeObject) delegate.next();
    }

    public PythonIterator<T> getIterator() {
        return this;
    }
}
