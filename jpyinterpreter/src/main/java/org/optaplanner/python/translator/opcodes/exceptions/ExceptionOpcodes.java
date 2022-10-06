package org.optaplanner.python.translator.opcodes.exceptions;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonVersion;
import org.optaplanner.python.translator.opcodes.Opcode;
import org.optaplanner.python.translator.util.JumpUtils;

public class ExceptionOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
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
            case RERAISE: {
                return Optional.of(new ReraiseOpcode(instruction));
            }
            case SETUP_FINALLY: {
                return Optional
                        .of(new SetupFinallyOpcode(instruction, JumpUtils.getRelativeTarget(instruction, pythonVersion)));
            }
            case RAISE_VARARGS: {
                return Optional.of(new RaiseVarargsOpcode(instruction));
            }
            case SETUP_WITH: {
                return Optional.of(new SetupWithOpcode(instruction, JumpUtils.getRelativeTarget(instruction, pythonVersion)));
            }
            case WITH_EXCEPT_START: {
                return Optional.of(new WithExceptStartOpcode(instruction));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
