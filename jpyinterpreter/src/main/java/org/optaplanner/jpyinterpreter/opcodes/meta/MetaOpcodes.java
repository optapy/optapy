package org.optaplanner.jpyinterpreter.opcodes.meta;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

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
