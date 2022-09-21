package org.optaplanner.python.translator.util;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonVersion;

public class JumpUtils {
    public static int getAbsoluteTarget(PythonBytecodeInstruction instruction, PythonVersion pythonVersion) {
        if (pythonVersion.isBefore(PythonVersion.PYTHON_3_10)) {
            return instruction.arg >> 1;
        } else {
            return instruction.arg;
        }
    }

    public static int getRelativeTarget(PythonBytecodeInstruction instruction, PythonVersion pythonVersion) {
        if (pythonVersion.isBefore(PythonVersion.PYTHON_3_10)) {
            return instruction.offset + (instruction.arg >> 1) + 1;
        } else {
            return instruction.offset + instruction.arg + 1;
        }
    }
}