package org.optaplanner.python.translator;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.optaplanner.python.translator.builtins.GlobalBuiltins;
import org.optaplanner.python.translator.types.CPythonBackedPythonLikeObject;
import org.optaplanner.python.translator.types.OpaquePythonReference;
import org.optaplanner.python.translator.types.PythonInteger;
import org.optaplanner.python.translator.types.PythonModule;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.PythonTraceback;

public class CPythonBackedPythonInterpreter implements PythonInterpreter {
    PrintStream standardOutput;

    Map<ModuleSpec, PythonModule> moduleSpecToModuleMap = new HashMap<>();

    public static Map<Number, Object> pythonObjectIdToConvertedObjectMap = new HashMap<>();

    public static Function<OpaquePythonReference, Number> lookupPythonReferenceIdPythonFunction;

    public static Function<OpaquePythonReference, OpaquePythonReference> lookupPythonReferenceTypePythonFunction;
    public static BiFunction<OpaquePythonReference, String, PythonLikeObject> lookupAttributeOnPythonReferencePythonFunction;

    public static TriFunction<OpaquePythonReference, String, Map<Number, PythonLikeObject>, PythonLikeObject> lookupAttributeOnPythonReferenceWithMapPythonFunction;
    public static TriConsumer<OpaquePythonReference, String, Object> setAttributeOnPythonReferencePythonFunction;
    public static BiConsumer<OpaquePythonReference, String> deleteAttributeOnPythonReferencePythonFunction;
    public static TriFunction<OpaquePythonReference, List<PythonLikeObject>, Map<PythonString, PythonLikeObject>, PythonLikeObject> callPythonFunction;

    public static PentaFunction<String, Map<String, PythonLikeObject>, Map<String, PythonLikeObject>, List<String>, Long, PythonModule> importModuleFunction;

    public CPythonBackedPythonInterpreter() {
        this(System.out);
    }

    public CPythonBackedPythonInterpreter(PrintStream standardOutput) {
        this.standardOutput = standardOutput;
    }

    public static Number getPythonReferenceId(OpaquePythonReference reference) {
        return lookupPythonReferenceIdPythonFunction.apply(reference);
    }

    public static OpaquePythonReference getPythonReferenceType(OpaquePythonReference reference) {
        return lookupPythonReferenceTypePythonFunction.apply(reference);
    }

    public static PythonLikeObject lookupAttributeOnPythonReference(OpaquePythonReference object, String attribute) {
        return lookupAttributeOnPythonReferencePythonFunction.apply(object, attribute);
    }

    public static PythonLikeObject lookupAttributeOnPythonReference(OpaquePythonReference object, String attribute,
            Map<Number, PythonLikeObject> map) {
        return lookupAttributeOnPythonReferenceWithMapPythonFunction.apply(object, attribute, map);
    }

    public static void setAttributeOnPythonReference(OpaquePythonReference object, String attribute, Object value) {
        setAttributeOnPythonReferencePythonFunction.accept(object, attribute, value);
    }

    public static void deleteAttributeOnPythonReference(OpaquePythonReference object, String attribute) {
        deleteAttributeOnPythonReferencePythonFunction.accept(object, attribute);
    }

    public static void updateJavaObjectFromPythonObject(CPythonBackedPythonLikeObject javaObject,
            OpaquePythonReference pythonObject,
            Map<Number, PythonLikeObject> instanceMap) {
        javaObject.$setInstanceMap(instanceMap);
        javaObject.$setCPythonReference(pythonObject);
        javaObject.$readFieldsFromCPythonReference();
    }

    public static PythonLikeObject callPythonReference(OpaquePythonReference object, List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> keywordArguments) {
        return callPythonFunction.apply(object, positionalArguments, keywordArguments);
    }

    @Override
    public PythonLikeObject getGlobal(Map<String, PythonLikeObject> globalsMap, String name) {
        PythonLikeObject out = globalsMap.get(name);
        if (out == null) {
            return GlobalBuiltins.lookupOrError(this, name);
        }
        return out;
    }

    @Override
    public void setGlobal(Map<String, PythonLikeObject> globalsMap, String name, PythonLikeObject value) {
        globalsMap.put(name, value);
    }

    @Override
    public void deleteGlobal(Map<String, PythonLikeObject> globalsMap, String name) {
        globalsMap.remove(name);
    }

    @Override
    public PythonModule importModule(PythonInteger level, List<PythonString> fromList, Map<String, PythonLikeObject> globalsMap,
            Map<String, PythonLikeObject> localsMap, String moduleName) {
        // See https://docs.python.org/3/library/functions.html#import__ for semantics
        ModuleSpec moduleSpec = new ModuleSpec(level, fromList, globalsMap, localsMap, moduleName);

        return moduleSpecToModuleMap.computeIfAbsent(moduleSpec, spec -> {
            Long theLevel = level.getValue().longValue();
            List<String> importNameList = new ArrayList<>(fromList.size());
            for (PythonString name : fromList) {
                importNameList.add(name.getValue());
            }

            return importModuleFunction.apply(moduleName, globalsMap, localsMap, importNameList, theLevel);
        });
    }

    @Override
    public void print(PythonLikeObject object) {
        standardOutput.println(object);
    }

    @Override
    public PythonTraceback getTraceback() {
        // TODO: Implement this with an actually traceback
        return new PythonTraceback();
    }
}
