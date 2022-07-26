package org.optaplanner.python.translator.opcodes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.opcodes.async.AsyncOpcodes;
import org.optaplanner.python.translator.opcodes.collection.CollectionOpcodes;
import org.optaplanner.python.translator.opcodes.controlflow.ControlFlowOpcodes;
import org.optaplanner.python.translator.opcodes.dunder.DunderOpcodes;
import org.optaplanner.python.translator.opcodes.exceptions.ExceptionOpcodes;
import org.optaplanner.python.translator.opcodes.function.FunctionOpcodes;
import org.optaplanner.python.translator.opcodes.generator.GeneratorOpcodes;
import org.optaplanner.python.translator.opcodes.meta.MetaOpcodes;
import org.optaplanner.python.translator.opcodes.module.ModuleOpcodes;
import org.optaplanner.python.translator.opcodes.object.ObjectOpcodes;
import org.optaplanner.python.translator.opcodes.stack.StackManipulationOpcodes;
import org.optaplanner.python.translator.opcodes.string.StringOpcodes;
import org.optaplanner.python.translator.opcodes.variable.VariableOpcodes;

public interface Opcode {

    /**
     * Return the bytecode index of the instruction, which can be used
     * to identify the instruction as the target of a jump.
     *
     * @return The bytecode index of the instruction, which is defined
     *         as the number of instructions before it in the instruction
     *         listing.
     */
    int getBytecodeIndex();

    /**
     * Relabel the instruction corresponding to this opcode. Updates bytecode
     * index according to {@code originalBytecodeIndexToNewBytecodeIndex}, and if arg represents a jump target,
     * recompute it using {@code originalBytecodeIndexToNewBytecodeIndex}.
     *
     * @param originalBytecodeIndexToNewBytecodeIndex A map from the original bytecode index to the new bytecode index
     */
    void relabel(Map<Integer, Integer> originalBytecodeIndexToNewBytecodeIndex);

    /**
     * Return the possible next bytecode index after this instruction is executed.
     * The default simply return [getBytecodeIndex() + 1], but is
     * typically overwritten in jump instructions.
     *
     * @return the possible next bytecode index after this instruction is executed
     */
    default List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(getBytecodeIndex() + 1);
    }

    /**
     * Return a list of {@link StackMetadata} corresponding to each branch returned by
     * {@link #getPossibleNextBytecodeIndexList()}.
     *
     * @param functionMetadata Metadata about the function being compiled.
     * @param stackMetadata the StackMetadata just before this instruction is executed.
     * @return a new List, the same size as {@link #getPossibleNextBytecodeIndexList()},
     *         containing the StackMetadata after this instruction is executed for the given branch
     *         in {@link #getPossibleNextBytecodeIndexList()}.
     */
    List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata);

    /**
     * Implements the opcode.
     *
     * @param functionMetadata Metadata about the function being compiled.
     * @param stackMetadata Metadata about the state of the stack when this instruction is executed.
     */
    void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata);

    /**
     * @return true if this opcode the target of a jump
     */
    boolean isJumpTarget();

    /**
     * @return true if this opcode is a forced jump (i.e. goto)
     */
    default boolean isForcedJump() {
        return false;
    }

    static Opcode lookupOpcodeForInstruction(PythonBytecodeInstruction instruction, int pythonVersion) {
        return Optional.<Opcode> empty() // For more readable code, start with empty
                .or(() -> AsyncOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> CollectionOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> ControlFlowOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> DunderOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> ExceptionOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> FunctionOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> GeneratorOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> MetaOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> ModuleOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> ObjectOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> StackManipulationOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> StringOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .or(() -> VariableOpcodes.lookupOpcodeForInstruction(instruction, pythonVersion))
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Could not find implementation for Opcode " + instruction.opcode +
                                " for Python version " + pythonVersion +
                                " (cause by instruction: " + instruction + ")."));
    }
}
