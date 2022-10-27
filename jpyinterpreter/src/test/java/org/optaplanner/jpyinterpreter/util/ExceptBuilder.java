package org.optaplanner.jpyinterpreter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.optaplanner.jpyinterpreter.OpcodeIdentifier;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

/**
 * Builds except and finally blocks
 */
public class ExceptBuilder {

    /**
     * The {@link PythonFunctionBuilder} that created this {@link ExceptBuilder}.
     */
    final PythonFunctionBuilder delegate;

    /**
     * The {@link OpcodeIdentifier#JUMP_ABSOLUTE} instruction at the end
     * of the try block, which is where the try should go if it completed without error
     * (finally block if it was specified, else catch block).
     */
    final PythonBytecodeInstruction tryEndGoto;

    /**
     * The {@link OpcodeIdentifier#SETUP_FINALLY} instruction before the try block that
     * handles the case where the exception is not caught
     */
    final PythonBytecodeInstruction exceptFinallyInstruction;

    final List<PythonBytecodeInstruction> exceptEndJumpList = new ArrayList<>();

    boolean hasFinally = false;

    boolean allExceptsExitEarly = true;

    public ExceptBuilder(PythonFunctionBuilder delegate, PythonBytecodeInstruction tryEndGoto,
            PythonBytecodeInstruction exceptFinallyInstruction) {
        this.delegate = delegate;
        this.tryEndGoto = tryEndGoto;
        this.exceptFinallyInstruction = exceptFinallyInstruction;
    }

    /**
     * Add an except block for the {@code type} argument.
     *
     * @param type The exception type handled by the except block
     * @param exceptBuilder The code in the except block
     */
    public ExceptBuilder except(PythonLikeType type, Consumer<PythonFunctionBuilder> exceptBuilder,
            boolean exitEarly) {

        PythonBytecodeInstruction exceptBlockStartInstruction = new PythonBytecodeInstruction();
        exceptBlockStartInstruction.opcode = OpcodeIdentifier.DUP_TOP;
        exceptBlockStartInstruction.offset = delegate.instructionList.size();
        exceptBlockStartInstruction.isJumpTarget = true;
        delegate.instructionList.add(exceptBlockStartInstruction);

        delegate.loadConstant(type);

        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.JUMP_IF_NOT_EXC_MATCH; // Skip block if False (i.e. enter block if True)
        instruction.offset = delegate.instructionList.size();
        delegate.instructionList.add(instruction);

        delegate.op(OpcodeIdentifier.POP_TOP);
        delegate.op(OpcodeIdentifier.POP_TOP);
        delegate.op(OpcodeIdentifier.POP_TOP);
        delegate.op(OpcodeIdentifier.POP_EXCEPT);
        exceptBuilder.accept(delegate);

        if (!exitEarly) {
            allExceptsExitEarly = false;
            PythonBytecodeInstruction exceptEndJumpInstruction = new PythonBytecodeInstruction();
            exceptEndJumpInstruction.opcode = OpcodeIdentifier.JUMP_ABSOLUTE;
            exceptEndJumpInstruction.offset = delegate.instructionList.size();
            delegate.instructionList.add(exceptEndJumpInstruction);
            exceptEndJumpList.add(exceptEndJumpInstruction);
        }

        instruction.arg = delegate.instructionList.size();
        return this;
    }

    /**
     * Creates a finally block.
     *
     * @param finallyBuilder The code in the finally block
     */
    public ExceptBuilder andFinally(Consumer<PythonFunctionBuilder> finallyBuilder, boolean exitEarly) {
        hasFinally = true;

        if (!exitEarly) {
            allExceptsExitEarly = false;
        }

        PythonBytecodeInstruction exceptGotoTarget = new PythonBytecodeInstruction();
        exceptGotoTarget.opcode = OpcodeIdentifier.NOP;
        exceptGotoTarget.offset = delegate.instructionList.size();
        exceptGotoTarget.isJumpTarget = true;
        delegate.instructionList.add(exceptGotoTarget);

        delegate.op(OpcodeIdentifier.POP_TOP);
        delegate.op(OpcodeIdentifier.RERAISE, 0);

        tryEndGoto.arg = delegate.instructionList.size();

        exceptEndJumpList.forEach(instruction -> {
            instruction.arg = delegate.instructionList.size();
        });

        PythonBytecodeInstruction finallyFromTryStartInstruction = new PythonBytecodeInstruction();
        finallyFromTryStartInstruction.opcode = OpcodeIdentifier.NOP;
        finallyFromTryStartInstruction.offset = delegate.instructionList.size();
        finallyFromTryStartInstruction.isJumpTarget = true;
        delegate.instructionList.add(finallyFromTryStartInstruction);

        finallyBuilder.accept(delegate); // finally from try

        PythonBytecodeInstruction finallyEndInstruction = new PythonBytecodeInstruction();
        finallyEndInstruction.opcode = OpcodeIdentifier.JUMP_ABSOLUTE;
        finallyEndInstruction.offset = delegate.instructionList.size();
        delegate.instructionList.add(finallyEndInstruction);

        exceptFinallyInstruction.arg = delegate.instructionList.size() - exceptFinallyInstruction.offset - 1;

        PythonBytecodeInstruction finallyFromUncaughtStartInstruction = new PythonBytecodeInstruction();
        finallyFromUncaughtStartInstruction.opcode = OpcodeIdentifier.NOP;
        finallyFromUncaughtStartInstruction.offset = delegate.instructionList.size();
        finallyFromUncaughtStartInstruction.isJumpTarget = true;
        delegate.instructionList.add(finallyFromUncaughtStartInstruction);

        finallyBuilder.accept(delegate);

        delegate.op(OpcodeIdentifier.POP_TOP);
        delegate.op(OpcodeIdentifier.RERAISE);

        finallyEndInstruction.arg = delegate.instructionList.size();

        PythonBytecodeInstruction tryCatchBlockEnd = new PythonBytecodeInstruction();
        tryCatchBlockEnd.opcode = OpcodeIdentifier.NOP;
        tryCatchBlockEnd.offset = delegate.instructionList.size();
        tryCatchBlockEnd.isJumpTarget = true;
        delegate.instructionList.add(tryCatchBlockEnd);
        return this;
    }

    /**
     * Ends the except/finally blocks, returning the {@link PythonFunctionBuilder} that created this
     * {@link ExceptBuilder}.
     *
     * @return the {@link PythonFunctionBuilder} that created this {@link ExceptBuilder}.
     */
    public PythonFunctionBuilder tryEnd() {
        if (!hasFinally) {
            PythonBytecodeInstruction exceptGotoTarget = new PythonBytecodeInstruction();
            exceptGotoTarget.opcode = OpcodeIdentifier.NOP;
            exceptGotoTarget.offset = delegate.instructionList.size();
            exceptGotoTarget.isJumpTarget = true;
            delegate.instructionList.add(exceptGotoTarget);

            delegate.op(OpcodeIdentifier.POP_TOP);
            delegate.op(OpcodeIdentifier.RERAISE, 0);
        }

        if (tryEndGoto.arg == 0) {
            if (!hasFinally) {
                exceptFinallyInstruction.arg = delegate.instructionList.size() - exceptFinallyInstruction.offset - 1;
                PythonBytecodeInstruction reraiseInstruction = new PythonBytecodeInstruction();
                reraiseInstruction.opcode = OpcodeIdentifier.RERAISE;
                reraiseInstruction.arg = 0;
                reraiseInstruction.offset = delegate.instructionList.size();
                reraiseInstruction.isJumpTarget = true;
                delegate.instructionList.add(reraiseInstruction);
            }

            if (!allExceptsExitEarly) {
                tryEndGoto.arg = delegate.instructionList.size();
                PythonBytecodeInstruction noopInstruction = new PythonBytecodeInstruction();
                noopInstruction.opcode = OpcodeIdentifier.NOP;
                noopInstruction.offset = delegate.instructionList.size();
                noopInstruction.isJumpTarget = true;
                delegate.instructionList.add(noopInstruction);

                if (!hasFinally) {
                    exceptEndJumpList.forEach(instruction -> {
                        instruction.arg = delegate.instructionList.size();
                    });
                }
            }
        }
        return delegate;
    }
}
