package org.optaplanner.python.translator;

import java.util.List;

public class PythonCompiledFunction {
    /**
     * List of bytecode instructions in the function
     */
    public List<PythonBytecodeInstruction> instructionList;

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
    public List<Object> co_constants;

    /**
     * The number of arguments the function takes
     */
    public int co_argcount;

    /**
     * The number of keyword only arguments the function takes
     */
    public int co_kwonlyargcount;
}
