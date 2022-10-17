package org.optaplanner.jpyinterpreter.opcodes.variable;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

public class VariableOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        switch (instruction.opcode) {
            case LOAD_CONST: {
                return Optional.of(new LoadConstantOpcode(instruction));
            }

            case LOAD_NAME:
            case STORE_NAME:
            case DELETE_NAME: {
                return Optional.empty(); //TODO
            }

            case LOAD_GLOBAL: {
                return Optional.of(new LoadGlobalOpcode(instruction));
            }
            case STORE_GLOBAL: {
                return Optional.of(new StoreGlobalOpcode(instruction));
            }
            case DELETE_GLOBAL: {
                return Optional.of(new DeleteGlobalOpcode(instruction));
            }

            case LOAD_FAST: {
                return Optional.of(new LoadFastOpcode(instruction));
            }
            case STORE_FAST: {
                return Optional.of(new StoreFastOpcode(instruction));
            }
            case DELETE_FAST: {
                return Optional.of(new DeleteFastOpcode(instruction));
            }

            case LOAD_CLOSURE: {
                return Optional.of(new LoadClosureOpcode(instruction));
            }
            case LOAD_DEREF: {
                return Optional.of(new LoadDerefOpcode(instruction));
            }
            case STORE_DEREF: {
                return Optional.of(new StoreDerefOpcode(instruction));
            }
            case DELETE_DEREF: {
                return Optional.of(new DeleteDerefOpcode(instruction));
            }

            case LOAD_CLASSDEREF: {
                return Optional.empty(); // TODO
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
