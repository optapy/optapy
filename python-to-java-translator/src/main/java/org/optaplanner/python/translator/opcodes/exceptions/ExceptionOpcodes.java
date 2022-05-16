package org.optaplanner.python.translator.opcodes.exceptions;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.opcodes.Opcode;

public class ExceptionOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction, int pythonVersion) {
        switch (instruction.opcode) {
            case LOAD_ASSERTION_ERROR: {
                return Optional.of(new LoadAssertionErrorOpcode(instruction));
            }
            case POP_BLOCK: {
                return Optional.of(new PopBlockOpcode(instruction));
            }
            case POP_EXCEPT: {
                return Optional.of(new PopExceptOpcode(instruction));
            }
            case WITH_EXCEPT_START: {
                return Optional.empty(); // TODO
            }
            case RERAISE: {
                return Optional.of(new ReraiseOpcode(instruction));
            }
            case SETUP_FINALLY: {
                return Optional.of(new SetupFinallyOpcode(instruction));
            }
            case RAISE_VARARGS: {
                return Optional.of(new RaiseVarargsOpcode(instruction));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
