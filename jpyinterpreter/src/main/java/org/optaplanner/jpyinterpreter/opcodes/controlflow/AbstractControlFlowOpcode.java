package org.optaplanner.jpyinterpreter.opcodes.controlflow;

import java.util.Map;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

public abstract class AbstractControlFlowOpcode implements Opcode {
    protected final PythonBytecodeInstruction instruction;

    public AbstractControlFlowOpcode(PythonBytecodeInstruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex) {
        instruction.offset = originalBytecodeIndexToNewBytecodeIndex.get(instruction.offset);
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
