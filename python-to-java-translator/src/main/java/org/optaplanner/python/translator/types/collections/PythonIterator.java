package org.optaplanner.python.translator.types.collections;

import java.util.Iterator;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.PythonUnaryOperator;
import org.optaplanner.python.translator.types.AbstractPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.errors.StopIteration;

public class PythonIterator<T> extends AbstractPythonLikeObject implements Iterator<T> {
    public static final PythonLikeType ITERATOR_TYPE = new PythonLikeType("iterator", PythonIterator.class);
    private final Iterator<T> delegate;

    static {
        try {
            registerMethods();
            PythonOverloadImplementor.createDispatchesFor(ITERATOR_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        ITERATOR_TYPE.addMethod(PythonUnaryOperator.NEXT, PythonIterator.class.getMethod("nextPythonItem"));
        ITERATOR_TYPE.addMethod(PythonUnaryOperator.ITERATOR, PythonIterator.class.getMethod("getIterator"));
    }

    public PythonIterator(Iterator<T> delegate) {
        super(ITERATOR_TYPE);
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
