package org.optaplanner.jpyinterpreter;

public class PythonBytecodeInstruction {
    /**
     * The {@link OpcodeIdentifier} for this operation
     */
    public OpcodeIdentifier opcode;

    /**
     * Human readable name for operation
     */
    public String opname;

    /**
     * Numeric argument to operation (if any), otherwise null
     */
    public Integer arg;

    /**
     * Start index of operation within bytecode sequence
     */
    public int offset;

    /**
     * Line started by this opcode (if any), otherwise None
     */
    public Integer startsLine;

    /**
     * True if other code jumps to here, otherwise False
     */
    public boolean isJumpTarget;

    public PythonBytecodeInstruction copy() {
        PythonBytecodeInstruction out = new PythonBytecodeInstruction();

        out.opcode = opcode;
        out.opname = opname;
        out.arg = arg;
        out.offset = offset;
        out.startsLine = startsLine;
        out.isJumpTarget = isJumpTarget;

        return out;
    }

    @Override
    public String toString() {
        return "[" + offset + "] " + opcode.name() + " (" + arg + ")" + (isJumpTarget ? " {JUMP TARGET}" : "");
    }
}
