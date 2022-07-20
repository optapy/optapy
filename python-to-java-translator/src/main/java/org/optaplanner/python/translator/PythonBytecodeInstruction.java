package org.optaplanner.python.translator;

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

    @Override
    public String toString() {
        return "[" + offset + "] " + opcode.name() + " (" + arg + ")" + (isJumpTarget ? " {JUMP TARGET}" : "");
    }
}
