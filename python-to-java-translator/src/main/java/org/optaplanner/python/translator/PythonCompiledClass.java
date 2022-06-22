package org.optaplanner.python.translator;

import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.types.CPythonType;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.util.JavaIdentifierUtils;

public class PythonCompiledClass {
    /**
     * The module where the class was defined.
     */
    public String module;

    /**
     * The qualified name of the class. Does not include module.
     */
    public String qualifiedName;

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

    public String getGeneratedClassBaseName() {
        if (module == null || module.isEmpty()) {
            return JavaIdentifierUtils.sanitizeClassName((qualifiedName != null) ? qualifiedName : "PythonClass");
        }
        return JavaIdentifierUtils
                .sanitizeClassName((qualifiedName != null) ? module + "." + qualifiedName : module + "." + "PythonClass");
    }
}
