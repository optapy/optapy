package org.optaplanner.python.translator.opcodes.meta;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonVersion;
import org.optaplanner.python.translator.opcodes.Opcode;

public class MetaOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        switch (instruction.opcode) {
            // TODO
            case EXTENDED_ARG:
            case LOAD_BUILD_CLASS:
            case SETUP_ANNOTATIONS:
            default: {
                return Optional.empty();
            }
        }
    }
}
