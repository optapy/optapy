package org.optaplanner.optapy.translator.types;

import java.util.Iterator;
import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.types.errors.StopIteration;

public class PythonIterator extends AbstractPythonLikeObject implements Iterator {
    private static final PythonLikeType ITERATOR_TYPE = new PythonLikeType("iterator");
    private final Iterator delegate;

    static {
        ITERATOR_TYPE.__dir__.put("__next__",
                                  new UnaryLambdaReference((self) -> (PythonLikeObject) ((PythonIterator) self).next(), Map.of()));
    }

    public PythonIterator(Iterator delegate) {
        super(ITERATOR_TYPE);
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public Object next() {
        if (!delegate.hasNext()) {
            throw new StopIteration();
        }
        return delegate.next();
    }
}
