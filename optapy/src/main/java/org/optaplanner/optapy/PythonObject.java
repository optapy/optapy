package org.optaplanner.optapy;

import java.util.Map;

public interface PythonObject {
    Number get__optapy_Id();
    Map<Number, PythonObject> get__optapy_ObjectMap();
}
