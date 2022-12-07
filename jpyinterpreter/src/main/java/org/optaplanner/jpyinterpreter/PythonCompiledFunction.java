package org.optaplanner.jpyinterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.util.JavaIdentifierUtils;
import org.optaplanner.jpyinterpreter.util.arguments.ArgumentSpec;

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
     * Default positional arguments
     */
    public PythonLikeTuple defaultPositionalArguments = new PythonLikeTuple();

    /**
     * Default keyword arguments
     */
    public PythonLikeDict defaultKeywordArguments = new PythonLikeDict();

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
     * The exception table; only populated in Python 3.11 and above (in Python 3.10 and below,
     * the table will be empty, since those use explict block instructions)
     */
    public PythonExceptionTable co_exceptiontable;

    /**
     * The number of not keyword only arguments the function takes
     */
    public int co_argcount;

    /**
     * The number of keyword only arguments the function takes
     */
    public int co_kwonlyargcount;

    /**
     * The number of positional only arguments the function takes
     */
    public int co_posonlyargcount;

    /**
     * True if the python function can take extra positional arguments that were not specified in its arguments
     */
    public boolean supportExtraPositionalArgs = false;

    /**
     * True if the python function can take extra keyword arguments that were not specified in its arguments
     */
    public boolean supportExtraKeywordsArgs = false;

    /**
     * The python version this function was compiled in (see sys.hexversion)
     */
    public PythonVersion pythonVersion;

    public PythonClassTranslator.PythonMethodKind methodKind = PythonClassTranslator.PythonMethodKind.STATIC_METHOD;

    public PythonCompiledFunction() {
    }

    public PythonCompiledFunction copy() {
        PythonCompiledFunction out = new PythonCompiledFunction();

        out.module = module;
        out.qualifiedName = qualifiedName;
        out.instructionList = instructionList.stream().map(PythonBytecodeInstruction::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        out.closure = closure;
        out.globalsMap = globalsMap;
        out.typeAnnotations = typeAnnotations;
        out.defaultPositionalArguments = defaultPositionalArguments;
        out.defaultKeywordArguments = defaultKeywordArguments;
        out.co_exceptiontable = this.co_exceptiontable;
        out.co_names = new ArrayList<>(co_names);
        out.co_varnames = new ArrayList<>(co_varnames);
        out.co_cellvars = new ArrayList<>(co_cellvars);
        out.co_freevars = new ArrayList<>(co_freevars);
        out.co_constants = new ArrayList<>(co_constants);
        out.co_argcount = co_argcount;
        out.co_kwonlyargcount = co_kwonlyargcount;
        out.pythonVersion = pythonVersion;
        out.methodKind = methodKind;

        return out;
    }

    public List<PythonLikeType> getParameterTypes() {
        List<PythonLikeType> out = new ArrayList<>(totalArgCount());
        PythonLikeType defaultType = BuiltinTypes.BASE_TYPE;

        for (int i = 0; i < totalArgCount(); i++) {
            String parameterName = co_varnames.get(i);
            PythonLikeType parameterType = typeAnnotations.get(parameterName);
            if (parameterType == null) { // map may have nulls
                parameterType = defaultType;
            }
            out.add(parameterType);
        }
        return out;
    }

    public Optional<PythonLikeType> getReturnType() {
        return Optional.ofNullable(typeAnnotations.get("return"));
    }

    public String getAsmMethodDescriptorString() {
        Type returnType = Type.getType('L' + getReturnType().map(PythonLikeType::getJavaTypeInternalName)
                .orElseGet(BuiltinTypes.BASE_TYPE::getJavaTypeInternalName) + ';');
        List<PythonLikeType> parameterPythonTypeList = getParameterTypes();
        Type[] parameterTypes = new Type[totalArgCount()];

        for (int i = 0; i < totalArgCount(); i++) {
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> Class<T> getParameterJavaClass(List<PythonLikeType> parameterTypeList, int variableIndex) {
        return (Class) parameterTypeList.get(variableIndex).getJavaClassOrDefault(PythonLikeObject.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public BiFunction<PythonLikeTuple, PythonLikeDict, ArgumentSpec<PythonLikeObject>> getArgumentSpecMapper() {
        return (defaultPositionalArguments, defaultKeywordArguments) -> {
            ArgumentSpec<PythonLikeObject> out = ArgumentSpec.forFunctionReturning(qualifiedName, getReturnType()
                    .map(type -> (Class) type.getJavaClassOrDefault(PythonLikeObject.class))
                    .orElse(PythonLikeObject.class));

            int variableIndex = 0;
            int defaultPositionalStartIndex = co_argcount - defaultPositionalArguments.size();

            if (methodKind == PythonClassTranslator.PythonMethodKind.VIRTUAL_METHOD) {
                variableIndex = 1;
            }

            List<PythonLikeType> parameterTypeList = getParameterTypes();
            for (; variableIndex < co_posonlyargcount; variableIndex++) {
                if (variableIndex >= defaultPositionalStartIndex) {
                    out = out.addPositionalOnlyArgument(co_varnames.get(variableIndex),
                            getParameterJavaClass(parameterTypeList, variableIndex),
                            defaultPositionalArguments.get(
                                    variableIndex - defaultPositionalStartIndex));
                } else {
                    out = out.addPositionalOnlyArgument(co_varnames.get(variableIndex),
                            getParameterJavaClass(parameterTypeList, variableIndex));
                }
            }

            for (; variableIndex < co_argcount; variableIndex++) {
                if (variableIndex >= defaultPositionalStartIndex) {
                    out = out.addArgument(co_varnames.get(variableIndex),
                            getParameterJavaClass(parameterTypeList, variableIndex),
                            defaultPositionalArguments.get(variableIndex - defaultPositionalStartIndex));
                } else {
                    out = out.addArgument(co_varnames.get(variableIndex),
                            getParameterJavaClass(parameterTypeList, variableIndex));
                }
            }

            for (int i = 0; i < co_kwonlyargcount; i++) {
                PythonLikeObject maybeDefault =
                        defaultKeywordArguments.get(PythonString.valueOf(co_varnames.get(variableIndex)));
                if (maybeDefault != null) {
                    out = out.addKeywordOnlyArgument(co_varnames.get(variableIndex),
                            getParameterJavaClass(parameterTypeList, variableIndex),
                            maybeDefault);
                } else {
                    out = out.addKeywordOnlyArgument(co_varnames.get(variableIndex),
                            getParameterJavaClass(parameterTypeList, variableIndex));
                }
                variableIndex++;
            }

            // vargs and kwargs are always last, despite position in signature
            if (supportExtraPositionalArgs) {
                out = out.addExtraPositionalVarArgument(co_varnames.get(variableIndex));
                variableIndex++;
            }

            if (supportExtraKeywordsArgs) {
                out = out.addExtraKeywordVarArgument(co_varnames.get(variableIndex));
            }

            return out;
        };
    }

    /**
     * The total number of arguments the function takes
     */
    public int totalArgCount() {
        int extraArgs = 0;
        if (supportExtraPositionalArgs) {
            extraArgs++;
        }
        if (supportExtraKeywordsArgs) {
            extraArgs++;
        }

        return co_argcount + co_kwonlyargcount + extraArgs;
    }
}
