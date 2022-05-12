package org.optaplanner.python.translator;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.optaplanner.python.translator.builtins.GlobalBuiltins;
import org.optaplanner.python.translator.types.OpaquePythonReference;
import org.optaplanner.python.translator.types.PythonString;
import org.optaplanner.python.translator.types.errors.PythonTraceback;

public class CPythonBackedPythonInterpreter implements PythonInterpreter {
    Map<String, PythonLikeObject> globalsMap;
    PrintStream standardOutput;
    public static BiFunction<OpaquePythonReference, String, PythonLikeObject> lookupAttributeOnPythonReferencePythonFunction;
    public static TriConsumer<OpaquePythonReference, String, Object> setAttributeOnPythonReferencePythonFunction;
    public static BiConsumer<OpaquePythonReference, String> deleteAttributeOnPythonReferencePythonFunction;
    public static TriFunction<OpaquePythonReference, List<PythonLikeObject>, Map<PythonString, PythonLikeObject>, PythonLikeObject> callPythonFunction;

    public CPythonBackedPythonInterpreter() {
        this(System.out);
    }

    public CPythonBackedPythonInterpreter(PrintStream standardOutput) {
        this.globalsMap = new HashMap<>();
        this.standardOutput = standardOutput;
    }

    public static PythonLikeObject lookupAttributeOnPythonReference(OpaquePythonReference object, String attribute) {
        return lookupAttributeOnPythonReferencePythonFunction.apply(object, attribute);
    }

    public static void setAttributeOnPythonReference(OpaquePythonReference object, String attribute, Object value) {
        setAttributeOnPythonReferencePythonFunction.accept(object, attribute, value);
    }

    public static void deleteAttributeOnPythonReference(OpaquePythonReference object, String attribute) {
        deleteAttributeOnPythonReferencePythonFunction.accept(object, attribute);
    }
    public static PythonLikeObject callPythonReference(OpaquePythonReference object, List<PythonLikeObject> positionalArguments,
                                                            Map<PythonString, PythonLikeObject> keywordArguments) {
        return callPythonFunction.apply(object, positionalArguments, keywordArguments);
    }

    @Override
    public PythonLikeObject getGlobal(String name) {
        PythonLikeObject out = globalsMap.get(name);
        if (out == null) {
            return GlobalBuiltins.lookupOrError(this, name);
        }
        return out;
    }

    @Override
    public void setGlobal(String name, PythonLikeObject value) {
        globalsMap.put(name, value);
    }

    @Override
    public void deleteGlobal(String name) {
        globalsMap.remove(name);
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
