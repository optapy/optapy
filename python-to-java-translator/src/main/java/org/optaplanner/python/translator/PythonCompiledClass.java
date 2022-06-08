package org.optaplanner.python.translator;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.types.CPythonType;
import org.optaplanner.python.translator.types.PythonLikeType;

public class PythonCompiledClass {
    public String className;

    /**
     * The binary type of this PythonCompiledClass;
     * typically {@link CPythonType}. Used when methods
     * cannot be generated.
     */
    public PythonLikeType binaryType;
    public List<PythonLikeType> superclassList;
    public Map<String, PythonCompiledFunction> instanceFunctionNameToPythonBytecode;
    public Map<String, PythonCompiledFunction> staticFunctionNameToPythonBytecode;
    public Map<String, PythonCompiledFunction> classFunctionNameToPythonBytecode;
    public Map<String, PythonLikeObject> staticAttributeNameToObject;
}
