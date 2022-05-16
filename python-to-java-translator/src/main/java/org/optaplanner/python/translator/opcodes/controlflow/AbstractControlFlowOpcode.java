package org.optaplanner.python.translator.opcodes.controlflow;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.opcodes.Opcode;

public abstract class AbstractControlFlowOpcode implements Opcode {
    protected final PythonBytecodeInstruction instruction;

    public AbstractControlFlowOpcode(PythonBytecodeInstruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public boolean isJumpTarget() {
        return instruction.isJumpTarget;
    }

    @Override
    public int getBytecodeIndex() {
        return instruction.offset;
    }

    @Override
    public String toString() {
        return instruction.toString();
    }
}
