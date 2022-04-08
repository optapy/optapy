package org.optaplanner.optapy.translator;

import java.lang.reflect.Parameter;
import java.util.Arrays;

public class LocalVariableHelper {

    public final Parameter[] parameters;
    public final int parameterSlotsEnd;
    public final int pythonLocalVariablesSlotEnd;
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
        parameterSlotsEnd = slotsUsedByParameters;
        pythonLocalVariablesSlotEnd = parameterSlotsEnd + compiledFunction.co_varnames.size();
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

    public int newLocal() {
        int slot = pythonLocalVariablesSlotEnd + usedLocals;
        usedLocals++;
        return slot;
    }

    public void freeLocal() {
        usedLocals--;
    }
}
