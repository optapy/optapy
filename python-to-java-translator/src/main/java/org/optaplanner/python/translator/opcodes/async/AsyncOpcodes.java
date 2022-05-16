package org.optaplanner.python.translator.opcodes.async;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.opcodes.Opcode;

public class AsyncOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction, int pythonVersion) {
        switch (instruction.opcode) {
            // TODO
            case GET_AWAITABLE:
            case GET_AITER:
            case GET_ANEXT:
            case END_ASYNC_FOR:
            case BEFORE_ASYNC_WITH:
            case SETUP_ASYNC_WITH:
            default: {
                return Optional.empty();
            }
        }
    }
}
