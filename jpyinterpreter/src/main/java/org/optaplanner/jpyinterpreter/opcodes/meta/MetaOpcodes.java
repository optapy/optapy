package org.optaplanner.jpyinterpreter.opcodes.meta;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

public class MetaOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        switch (instruction.opcode) {
            case NOP:
            case CACHE:
            case PRECALL:
            case MAKE_CELL:
            case COPY_FREE_VARS: {
                return Optional.of(new NopOpcode(instruction));
            }

            case RETURN_GENERATOR: {
                return Optional.of(new ReturnGeneratorOpcode(instruction));
            }
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
