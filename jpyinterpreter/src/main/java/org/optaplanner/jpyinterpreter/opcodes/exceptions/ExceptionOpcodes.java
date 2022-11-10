package org.optaplanner.jpyinterpreter.opcodes.exceptions;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;
import org.optaplanner.jpyinterpreter.util.JumpUtils;

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
            case CHECK_EXC_MATCH: {
                return Optional.of(new CheckExcMatchOpcode(instruction));
            }
            case PUSH_EXC_INFO: {
                return Optional.of(new PushExcInfoOpcode(instruction));
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
