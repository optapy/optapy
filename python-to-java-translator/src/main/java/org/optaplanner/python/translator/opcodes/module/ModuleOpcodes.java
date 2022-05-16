package org.optaplanner.python.translator.opcodes.module;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.opcodes.Opcode;

public class ModuleOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction, int pythonVersion) {
        switch (instruction.opcode) {
            // TODO
            case IMPORT_STAR:
            case IMPORT_NAME:
            case IMPORT_FROM:
            default: {
                return Optional.empty();
            }
        }
    }
}
