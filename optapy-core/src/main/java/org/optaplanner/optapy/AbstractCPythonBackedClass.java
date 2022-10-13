package org.optaplanner.optapy;

import org.optaplanner.python.translator.types.CPythonBackedPythonLikeObject;
import org.optaplanner.python.translator.types.PythonLikeType;

public class AbstractCPythonBackedClass extends CPythonBackedPythonLikeObject {
    public static final PythonLikeType $TYPE = CPythonBackedPythonLikeObject.OBJECT_TYPE;

    public AbstractCPythonBackedClass() {
        super($TYPE);
    }

    public AbstractCPythonBackedClass(PythonLikeType __type__) {
        super(__type__);
    }
}
