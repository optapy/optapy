package org.optaplanner.optapy;

import org.optaplanner.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class AbstractCPythonBackedClass extends CPythonBackedPythonLikeObject {
    public static final PythonLikeType $TYPE = CPythonBackedPythonLikeObject.OBJECT_TYPE;

    public AbstractCPythonBackedClass() {
        super($TYPE);
    }

    public AbstractCPythonBackedClass(PythonLikeType __type__) {
        super(__type__);
    }
}
