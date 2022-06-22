package org.optaplanner.python.translator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.objectweb.asm.Type;
import org.optaplanner.python.translator.types.PythonLikeTuple;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.util.JavaIdentifierUtils;

public class PythonCompiledFunction {
    /**
     * The module where the function was defined.
     */
    public String module;

    /**
     * The qualified name of the function. Does not include module.
     */
    public String qualifiedName;

    /**
     * List of bytecode instructions in the function
     */
    public List<PythonBytecodeInstruction> instructionList;

    /**
     * The closure of the function
     */
    public PythonLikeTuple closure;

    /**
     * The globals of the function
     */
    public Map<String, PythonLikeObject> globalsMap;

    /**
     * Type annotations for the parameters and return.
     * (return is stored under the "return" key).
     */
    public Map<String, PythonLikeType> typeAnnotations;

    /**
     * List of all names used in the function
     */
    public List<String> co_names;

    /**
     * List of names used by local variables in the function
     */
    public List<String> co_varnames;

    /**
     * List of names used by cell variables
     */
    public List<String> co_cellvars;

    /**
     * List of names used by free variables
     */
    public List<String> co_freevars;

    /**
     * List of constants used in bytecode
     */
    public List<PythonLikeObject> co_constants;

    /**
     * The number of arguments the function takes
     */
    public int co_argcount;

    /**
     * The number of keyword only arguments the function takes
     */
    public int co_kwonlyargcount;

    public List<PythonLikeType> getParameterTypes() {
        List<PythonLikeType> out = new ArrayList<>(co_argcount);
        PythonLikeType defaultType = PythonLikeType.getBaseType();

        for (int i = 0; i < co_argcount; i++) {
            String parameterName = co_varnames.get(i);
            out.add(typeAnnotations.getOrDefault(parameterName, defaultType));
        }
        return out;
    }

    public Optional<PythonLikeType> getReturnType() {
        return Optional.ofNullable(typeAnnotations.get("return"));
    }

    public String getAsmMethodDescriptorString() {
        Type returnType = Type.getType('L' + getReturnType().map(PythonLikeType::getJavaTypeInternalName)
                .orElseGet(() -> PythonLikeType.getBaseType().getJavaTypeInternalName()) + ';');
        List<PythonLikeType> parameterPythonTypeList = getParameterTypes();
        Type[] parameterTypes = new Type[co_argcount];

        for (int i = 0; i < co_argcount; i++) {
            parameterTypes[i] = Type.getType('L' + parameterPythonTypeList.get(i).getJavaTypeInternalName() + ';');
        }
        return Type.getMethodDescriptor(returnType, parameterTypes);
    }

    public String getGeneratedClassBaseName() {
        if (module == null || module.isEmpty()) {
            return JavaIdentifierUtils.sanitizeClassName((qualifiedName != null) ? qualifiedName : "PythonFunction");
        }
        return JavaIdentifierUtils
                .sanitizeClassName((qualifiedName != null) ? module + "." + qualifiedName : module + "." + "PythonFunction");
    }
}
