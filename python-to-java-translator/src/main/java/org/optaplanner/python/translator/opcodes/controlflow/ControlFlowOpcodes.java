package org.optaplanner.python.translator.opcodes.controlflow;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.opcodes.Opcode;

public class ControlFlowOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction, int pythonVersion) {
        switch (instruction.opcode) {
            case RETURN_VALUE: {
                return Optional.of(new ReturnValueOpcode(instruction));
            }
            case JUMP_FORWARD: {
                return Optional.of(new JumpForwardOpcode(instruction));
            }
            case POP_JUMP_IF_TRUE: {
                return Optional.of(new PopJumpIfTrueOpcode(instruction));
            }
            case POP_JUMP_IF_FALSE: {
                return Optional.of(new PopJumpIfFalseOpcode(instruction));
            }
            case JUMP_IF_NOT_EXC_MATCH: {
                return Optional.of(new JumpIfNotExcMatchOpcode(instruction));
            }
            case JUMP_IF_TRUE_OR_POP: {
                return Optional.of(new JumpIfTrueOrPopOpcode(instruction));
            }
            case JUMP_IF_FALSE_OR_POP: {
                return Optional.of(new JumpIfFalseOrPopOpcode(instruction));
            }
            case JUMP_ABSOLUTE: {
                return Optional.of(new JumpAbsoluteOpcode(instruction));
            }
            case FOR_ITER: {
                return Optional.of(new ForIterOpcode(instruction));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
