package org.optaplanner.optapy;

import java.util.Map;

public interface PythonObject {
    String __get__optapy_Id();
    Map<String, PythonObject> __get__optapy_ObjectMap();
}
