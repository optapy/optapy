package org.optaplanner.python.translator.types;

public class PythonLikeGenericType extends PythonLikeType {
    final PythonLikeType origin;

    public PythonLikeGenericType(PythonLikeType origin) {
        super(TYPE_TYPE.getTypeName(), PythonLikeType.class);
        this.origin = origin;
    }

    public PythonLikeType getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        return "<class type[" + origin.getTypeName() + "]>";
    }
}
