package org.optaplanner.python.translator.opcodes.generator;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.opcodes.Opcode;

public class GeneratorOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction, int pythonVersion) {
        switch (instruction.opcode) {
            // TODO
            case YIELD_VALUE: {
                return Optional.of(new YieldValueOpcode(instruction));
            }
            case GEN_START:
            case YIELD_FROM:
            case GET_YIELD_FROM_ITER:
            default: {
                return Optional.empty();
            }
        }
    }
}
