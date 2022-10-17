package org.optaplanner.jpyinterpreter;

import java.util.List;
import java.util.Map;

import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.wrappers.CPythonType;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;
import org.optaplanner.jpyinterpreter.util.JavaIdentifierUtils;

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
     * Type annotations for fields
     */
    public Map<String, PythonLikeType> typeAnnotations;

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

    /**
     * Contains static attributes that are not instances of this class
     */
    public Map<String, PythonLikeObject> staticAttributeNameToObject;

    /**
     * Contains static attributes that are instances of this class
     */
    public Map<String, OpaquePythonReference> staticAttributeNameToClassInstance;

    public String getGeneratedClassBaseName() {
        if (module == null || module.isEmpty()) {
            return JavaIdentifierUtils.sanitizeClassName((qualifiedName != null) ? qualifiedName : "PythonClass");
        }
        return JavaIdentifierUtils
                .sanitizeClassName((qualifiedName != null) ? module + "." + qualifiedName : module + "." + "PythonClass");
    }
}
