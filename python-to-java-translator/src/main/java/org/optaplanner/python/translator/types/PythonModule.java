package org.optaplanner.python.translator.types;

import org.optaplanner.python.translator.PythonLikeObject;

public class PythonModule extends AbstractPythonLikeObject {
    public static PythonLikeType MODULE_TYPE = new PythonLikeType("module", PythonModule.class);
    public static PythonLikeType $TYPE = MODULE_TYPE;

    private OpaquePythonReference pythonReference;

    public PythonModule() {
        super(MODULE_TYPE);
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
}
