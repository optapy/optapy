package org.optaplanner.jpyinterpreter.opcodes.function;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

public class FunctionOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        switch (instruction.opcode) {
            case CALL_FUNCTION: {
                return Optional.of(new CallFunctionOpcode(instruction));
            }
            case CALL_FUNCTION_KW: {
                return Optional.of(new CallFunctionKeywordOpcode(instruction));
            }
            case CALL_FUNCTION_EX: {
                return Optional.of(new CallFunctionUnpackOpcode(instruction));
            }
            case LOAD_METHOD: {
                return Optional.of(new LoadMethodOpcode(instruction));
            }
            case CALL_METHOD: {
                return Optional.of(new CallMethodOpcode(instruction));
            }
            case MAKE_FUNCTION: {
                return Optional.of(new MakeFunctionOpcode(instruction));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
