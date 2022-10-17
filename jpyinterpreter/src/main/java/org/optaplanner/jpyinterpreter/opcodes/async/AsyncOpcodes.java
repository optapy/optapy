package org.optaplanner.jpyinterpreter.opcodes.async;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

public class AsyncOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
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
