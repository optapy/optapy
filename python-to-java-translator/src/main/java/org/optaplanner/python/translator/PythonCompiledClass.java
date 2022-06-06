package org.optaplanner.python.translator;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.types.PythonLikeType;

public class PythonCompiledClass {
    public String className;
    public List<PythonLikeType> superclassList;
    public Map<String, PythonCompiledFunction> instanceFunctionNameToPythonBytecode;
    public Map<String, PythonCompiledFunction> staticFunctionNameToPythonBytecode;
    public Map<String, PythonCompiledFunction> classFunctionNameToPythonBytecode;
    public Map<String, PythonLikeObject> staticAttributeNameToObject;
}
