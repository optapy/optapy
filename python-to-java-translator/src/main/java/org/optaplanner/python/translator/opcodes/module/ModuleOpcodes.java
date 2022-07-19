package org.optaplanner.python.translator.opcodes.module;

import java.util.Optional;

import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.opcodes.Opcode;

public class ModuleOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction, int pythonVersion) {
        switch (instruction.opcode) {
            case IMPORT_NAME: {
                return Optional.of(new ImportNameOpcode(instruction));
            }
            case IMPORT_FROM: {
                return Optional.of(new ImportFromOpcode(instruction));
            }
            case IMPORT_STAR: {
                // From https://docs.python.org/3/reference/simple_stmts.html#the-import-statement ,
                // Import * is only allowed at the module level and as such WILL never appear
                // in functions.
                throw new UnsupportedOperationException(
                        "Impossible state/invalid bytecode: import * only allowed at module level");
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
