package org.optaplanner.python.translator.opcodes.object;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonVersion;
import org.optaplanner.python.translator.opcodes.Opcode;

public class ObjectOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        switch (instruction.opcode) {
            case IS_OP: {
                return Optional.of(new IsOpcode(instruction));
            }
            case LOAD_ATTR: {
                return Optional.of(new LoadAttrOpcode(instruction));
            }
            case STORE_ATTR: {
                return Optional.of(new StoreAttrOpcode(instruction));
            }
            case DELETE_ATTR: {
                return Optional.of(new DeleteAttrOpcode(instruction));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
