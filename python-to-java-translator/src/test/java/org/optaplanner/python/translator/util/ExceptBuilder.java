package org.optaplanner.python.translator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.types.PythonLikeType;

/**
 * Builds except and finally blocks
 */
public class ExceptBuilder {

    /**
     * The {@link PythonFunctionBuilder} that created this {@link ExceptBuilder}.
     */
    final PythonFunctionBuilder delegate;

    /**
     * The {@link PythonBytecodeInstruction.OpcodeIdentifier#JUMP_ABSOLUTE} instruction at the end
     * of the try block, which is where the try should go if it completed without error
     * (finally block if it was specified, else catch block).
     */
    final PythonBytecodeInstruction tryEndGoto;

    final List<PythonBytecodeInstruction> exceptEndJumpList = new ArrayList<>();

    boolean hasFinally = false;

    boolean allExceptsExitEarly = true;

    public ExceptBuilder(PythonFunctionBuilder delegate, PythonBytecodeInstruction tryEndGoto) {
        this.delegate = delegate;
        this.tryEndGoto = tryEndGoto;
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
        exceptBlockStartInstruction.opcode = PythonBytecodeInstruction.OpcodeIdentifier.DUP_TOP;
        exceptBlockStartInstruction.offset = delegate.instructionList.size();
        exceptBlockStartInstruction.isJumpTarget = true;
        delegate.instructionList.add(exceptBlockStartInstruction);

        delegate.loadConstant(type);

        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = PythonBytecodeInstruction.OpcodeIdentifier.JUMP_IF_NOT_EXC_MATCH; // Skip block if False (i.e. enter block if True)
        instruction.offset = delegate.instructionList.size();
        delegate.instructionList.add(instruction);

        delegate.op(PythonBytecodeInstruction.OpcodeIdentifier.POP_TOP);
        delegate.op(PythonBytecodeInstruction.OpcodeIdentifier.POP_TOP);
        delegate.op(PythonBytecodeInstruction.OpcodeIdentifier.POP_TOP);
        delegate.op(PythonBytecodeInstruction.OpcodeIdentifier.POP_EXCEPT);
        exceptBuilder.accept(delegate);

        if (!exitEarly) {
            allExceptsExitEarly = false;
            PythonBytecodeInstruction exceptEndJumpInstruction = new PythonBytecodeInstruction();
            exceptEndJumpInstruction.opcode = PythonBytecodeInstruction.OpcodeIdentifier.JUMP_ABSOLUTE;
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

        PythonBytecodeInstruction finallyFromExceptStartInstruction = new PythonBytecodeInstruction();
        finallyFromExceptStartInstruction.opcode = PythonBytecodeInstruction.OpcodeIdentifier.POP_TOP;
        finallyFromExceptStartInstruction.offset = delegate.instructionList.size();
        finallyFromExceptStartInstruction.isJumpTarget = true;
        delegate.instructionList.add(finallyFromExceptStartInstruction);

        delegate.op(PythonBytecodeInstruction.OpcodeIdentifier.POP_TOP);
        delegate.op(PythonBytecodeInstruction.OpcodeIdentifier.POP_TOP);
        delegate.op(PythonBytecodeInstruction.OpcodeIdentifier.POP_EXCEPT);

        exceptEndJumpList.forEach(instruction -> {
            instruction.arg = delegate.instructionList.size();
        });

        PythonBytecodeInstruction exceptGotoTarget = new PythonBytecodeInstruction();
        exceptGotoTarget.opcode = PythonBytecodeInstruction.OpcodeIdentifier.NOP;
        exceptGotoTarget.offset = delegate.instructionList.size();
        exceptGotoTarget.isJumpTarget = true;
        delegate.instructionList.add(exceptGotoTarget);

        finallyBuilder.accept(delegate); // finally to handle if no except handler catches

        tryEndGoto.arg = delegate.instructionList.size();

        PythonBytecodeInstruction finallyFromTryStartInstruction = new PythonBytecodeInstruction();
        finallyFromTryStartInstruction.opcode = PythonBytecodeInstruction.OpcodeIdentifier.NOP;
        finallyFromTryStartInstruction.offset = delegate.instructionList.size();
        finallyFromTryStartInstruction.isJumpTarget = true;
        delegate.instructionList.add(finallyFromTryStartInstruction);

        finallyBuilder.accept(delegate); // finally from try
        return this;
    }

    /**
     * Ends the except/finally blocks, returning the {@link PythonFunctionBuilder} that created this
     * {@link ExceptBuilder}.
     *
     * @return the {@link PythonFunctionBuilder} that created this {@link ExceptBuilder}.
     */
    public PythonFunctionBuilder tryEnd() {
        if (tryEndGoto.arg == 0) {
            if (!hasFinally) {
                PythonBytecodeInstruction reraiseInstruction = new PythonBytecodeInstruction();
                reraiseInstruction.opcode = PythonBytecodeInstruction.OpcodeIdentifier.RERAISE;
                reraiseInstruction.arg = 0;
                reraiseInstruction.offset = delegate.instructionList.size();
                reraiseInstruction.isJumpTarget = true;
                delegate.instructionList.add(reraiseInstruction);
            }

            if (!allExceptsExitEarly) {
                tryEndGoto.arg = delegate.instructionList.size();
                PythonBytecodeInstruction noopInstruction = new PythonBytecodeInstruction();
                noopInstruction.opcode = PythonBytecodeInstruction.OpcodeIdentifier.NOP;
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
