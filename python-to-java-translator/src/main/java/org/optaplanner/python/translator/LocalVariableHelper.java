package org.optaplanner.python.translator;

import java.lang.reflect.Parameter;
import java.util.Arrays;

public class LocalVariableHelper {

    public final Parameter[] parameters;
    public final int parameterSlotsEnd;
    public final int pythonCellVariablesStart;
    public final int pythonFreeVariablesStart;
    public final int pythonLocalVariablesSlotEnd;

    public final int pythonBoundVariables;
    public final int pythonFreeVariables;

    public final int currentExceptionVariableSlot;
    int usedLocals;

    public LocalVariableHelper(Parameter[] parameters, PythonCompiledFunction compiledFunction) {
        this.parameters = parameters;
        int slotsUsedByParameters = 1;
        for (Parameter parameter : parameters) {
            if (parameter.getType().equals(long.class) || parameter.getType().equals(double.class)) {
                slotsUsedByParameters += 2;
            } else {
                slotsUsedByParameters += 1;
            }
        }

        pythonBoundVariables = compiledFunction.co_cellvars.size();
        pythonFreeVariables = compiledFunction.co_freevars.size();

        parameterSlotsEnd = slotsUsedByParameters;
        pythonCellVariablesStart = parameterSlotsEnd + compiledFunction.co_varnames.size();
        pythonFreeVariablesStart = pythonCellVariablesStart + pythonBoundVariables;
        currentExceptionVariableSlot = pythonFreeVariablesStart + pythonFreeVariables;
        pythonLocalVariablesSlotEnd = currentExceptionVariableSlot + 1;
    }

    public int getParameterSlot(int parameterIndex) {
        if (parameterIndex > parameters.length) {
            throw new IndexOutOfBoundsException("Asked for the slot corresponding to the (" + parameterIndex + ") " +
                    "parameter, but there are only (" + parameters.length + ") parameters (" + Arrays.toString(parameters)
                    + ").");
        }
        int slotsUsedByParameters = 1;
        for (int i = 0; i < parameterIndex; i++) {
            if (parameters[i].getType().equals(long.class) || parameters[i].getType().equals(double.class)) {
                slotsUsedByParameters += 2;
            } else {
                slotsUsedByParameters += 1;
            }
        }
        return slotsUsedByParameters;
    }

    public int getPythonLocalVariableSlot(int index) {
        return parameterSlotsEnd + index;
    }

    public int getPythonCellOrFreeVariableSlot(int index) {
        return pythonCellVariablesStart + index;
    }

    public int getCurrentExceptionVariableSlot() {
        return currentExceptionVariableSlot;
    }

    public int getNumberOfFreeCells() {
        return pythonFreeVariables;
    }

    public int getNumberOfBoundCells() {
        return pythonBoundVariables;
    }

    public int getNumberOfCells() {
        return pythonBoundVariables + pythonFreeVariables;
    }

    public int getNumberOfLocalVariables() {
        return pythonCellVariablesStart - parameterSlotsEnd;
    }

    public int newLocal() {
        int slot = pythonLocalVariablesSlotEnd + usedLocals;
        usedLocals++;
        return slot;
    }

    public void freeLocal() {
        usedLocals--;
    }
}
