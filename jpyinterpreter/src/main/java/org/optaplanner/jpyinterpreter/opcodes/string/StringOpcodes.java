package org.optaplanner.jpyinterpreter.opcodes.string;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

public class StringOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        switch (instruction.opcode) {
            case PRINT_EXPR: {
                return Optional.of(new PrintExprOpcode(instruction));
            }

            case FORMAT_VALUE: {
                return Optional.of(new FormatValueOpcode(instruction));
            }

            case BUILD_STRING: {
                return Optional.of(new BuildStringOpcode(instruction));
            }

            default: {
                return Optional.empty();
            }
        }
    }
}
