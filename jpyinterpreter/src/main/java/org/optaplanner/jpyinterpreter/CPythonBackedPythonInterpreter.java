package org.optaplanner.jpyinterpreter;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.optaplanner.jpyinterpreter.builtins.GlobalBuiltins;
import org.optaplanner.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonModule;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;
import org.optaplanner.jpyinterpreter.types.errors.PythonTraceback;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;
import org.optaplanner.jpyinterpreter.types.wrappers.PythonObjectWrapper;
import org.optaplanner.jpyinterpreter.util.function.PentaFunction;
import org.optaplanner.jpyinterpreter.util.function.QuadFunction;
import org.optaplanner.jpyinterpreter.util.function.TriConsumer;
import org.optaplanner.jpyinterpreter.util.function.TriFunction;

public class CPythonBackedPythonInterpreter implements PythonInterpreter {
    InputStream standardInput;
    PrintStream standardOutput;
    Scanner inputScanner;

    Map<ModuleSpec, PythonModule> moduleSpecToModuleMap = new HashMap<>();

    public static Map<Number, Object> pythonObjectIdToConvertedObjectMap = new HashMap<>();

    public static Function<OpaquePythonReference, Number> lookupPythonReferenceIdPythonFunction;

    public static Function<OpaquePythonReference, OpaquePythonReference> lookupPythonReferenceTypePythonFunction;
    public static BiFunction<OpaquePythonReference, String, PythonLikeObject> lookupAttributeOnPythonReferencePythonFunction;
    public static BiFunction<OpaquePythonReference, String, OpaquePythonReference> lookupPointerForAttributeOnPythonReferencePythonFunction;
    public static BiFunction<OpaquePythonReference, String, OpaquePythonReference[]> lookupPointerArrayForAttributeOnPythonReferencePythonFunction;

    public static TriFunction<OpaquePythonReference, String, Map<Number, PythonLikeObject>, PythonLikeObject> lookupAttributeOnPythonReferenceWithMapPythonFunction;
    public static TriConsumer<OpaquePythonReference, String, Object> setAttributeOnPythonReferencePythonFunction;
    public static BiConsumer<OpaquePythonReference, String> deleteAttributeOnPythonReferencePythonFunction;
    public static BiFunction<OpaquePythonReference, Map<Number, PythonLikeObject>, Map<String, PythonLikeObject>> lookupDictOnPythonReferencePythonFunction;
    public static TriFunction<OpaquePythonReference, List<PythonLikeObject>, Map<PythonString, PythonLikeObject>, PythonLikeObject> callPythonFunction;

    public static PentaFunction<String, Map<String, PythonLikeObject>, Map<String, PythonLikeObject>, List<String>, Long, PythonModule> importModuleFunction;
    public static QuadFunction<OpaquePythonReference, Map<String, PythonLikeObject>, PythonLikeTuple, PythonString, PythonObjectWrapper> createFunctionFromCodeFunction;

    public CPythonBackedPythonInterpreter() {
        this(System.in, System.out);
    }

    public CPythonBackedPythonInterpreter(InputStream standardInput, PrintStream standardOutput) {
        this.standardInput = standardInput;
        this.standardOutput = standardOutput;
        this.inputScanner = new Scanner(standardInput);
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

    public static OpaquePythonReference lookupPointerForAttributeOnPythonReference(OpaquePythonReference object,
            String attribute) {
        return lookupPointerForAttributeOnPythonReferencePythonFunction.apply(object, attribute);
    }

    public static OpaquePythonReference[] lookupPointerArrayForAttributeOnPythonReference(OpaquePythonReference object,
            String attribute) {
        return lookupPointerArrayForAttributeOnPythonReferencePythonFunction.apply(object, attribute);
    }

    public static void setAttributeOnPythonReference(OpaquePythonReference object, String attribute, Object value) {
        setAttributeOnPythonReferencePythonFunction.accept(object, attribute, value);
    }

    public static void deleteAttributeOnPythonReference(OpaquePythonReference object, String attribute) {
        deleteAttributeOnPythonReferencePythonFunction.accept(object, attribute);
    }

    public static Map<String, PythonLikeObject> getPythonReferenceDict(OpaquePythonReference object,
            Map<Number, PythonLikeObject> referenceMap) {
        return lookupDictOnPythonReferencePythonFunction.apply(object, referenceMap);
    }

    public static void updateJavaObjectFromPythonObject(CPythonBackedPythonLikeObject javaObject,
            OpaquePythonReference pythonObject,
            Map<Number, PythonLikeObject> instanceMap) {
        javaObject.$setInstanceMap(instanceMap);
        javaObject.$setCPythonReference(pythonObject);
        javaObject.$setCPythonId(PythonInteger.valueOf(getPythonReferenceId(pythonObject).longValue()));
        javaObject.$readFieldsFromCPythonReference();
    }

    public static void updateJavaObjectFromPythonObject(PythonLikeObject javaObject,
            OpaquePythonReference pythonObject,
            Map<Number, PythonLikeObject> instanceMap) {
        Map<String, PythonLikeObject> dict = getPythonReferenceDict(pythonObject, instanceMap);
        dict.forEach(javaObject::__setAttribute);
    }

    public static PythonLikeObject callPythonReference(OpaquePythonReference object, List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> keywordArguments) {
        return callPythonFunction.apply(object, positionalArguments, keywordArguments);
    }

    public static PythonObjectWrapper createPythonFunctionWrapper(
            OpaquePythonReference codeObject,
            Map<String, PythonLikeObject> globals,
            PythonLikeTuple closure,
            PythonString name) {
        return createFunctionFromCodeFunction.apply(codeObject, globals, closure, name);
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
    public void write(String output) {
        standardOutput.println(output);
    }

    @Override
    public String readLine() {
        // TODO: Raise EOFError on end of file
        return inputScanner.nextLine();
    }

    @Override
    public PythonTraceback getTraceback() {
        // TODO: Implement this with an actually traceback
        return new PythonTraceback();
    }
}
