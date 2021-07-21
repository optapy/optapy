package org.optaplanner.optapy;

import java.util.Map;

public interface PythonObject {
    String get__optapy_Id();
    Map<String, PythonObject> get__optapy_ObjectMap();
}
