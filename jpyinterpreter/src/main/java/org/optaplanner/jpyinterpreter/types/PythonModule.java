package org.optaplanner.jpyinterpreter.types;

import java.util.Map;

import org.optaplanner.jpyinterpreter.CPythonBackedPythonInterpreter;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;

public class PythonModule extends AbstractPythonLikeObject {
    public static PythonLikeType MODULE_TYPE = new PythonLikeType("module", PythonModule.class);
    public static PythonLikeType $TYPE = MODULE_TYPE;

    private OpaquePythonReference pythonReference;
    private Map<Number, PythonLikeObject> referenceMap;

    public PythonModule(Map<Number, PythonLikeObject> referenceMap) {
        super(MODULE_TYPE);
        this.referenceMap = referenceMap;
    }

    public void addItem(String itemName, PythonLikeObject itemValue) {
        __setAttribute(itemName, itemValue);
    }

    public OpaquePythonReference getPythonReference() {
        return pythonReference;
    }

    public void setPythonReference(OpaquePythonReference pythonReference) {
        this.pythonReference = pythonReference;
    }

    @Override
    public PythonLikeObject __getAttributeOrNull(String attributeName) {
        PythonLikeObject result = super.__getAttributeOrNull(attributeName);
        if (result == null) {
            PythonLikeObject actual = CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference(pythonReference,
                    attributeName, referenceMap);
            __setAttribute(attributeName, actual);
            return actual;
        }
        return result;
    }
}
