package org.optaplanner.jpyinterpreter.opcodes.dunder;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBinaryOperators;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonUnaryOperator;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

public class DunderOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        switch (instruction.opcode) {
            case COMPARE_OP: {
                return Optional.of(new CompareOpcode(instruction));
            }
            case UNARY_NOT: {
                return Optional.of(new NotOpcode(instruction));
            }

            case UNARY_POSITIVE: {
                return Optional.of(new UniDunerOpcode(instruction, PythonUnaryOperator.POSITIVE));
            }
            case UNARY_NEGATIVE: {
                return Optional.of(new UniDunerOpcode(instruction, PythonUnaryOperator.NEGATIVE));
            }
            case UNARY_INVERT: {
                return Optional.of(new UniDunerOpcode(instruction, PythonUnaryOperator.INVERT));
            }

            case BINARY_OP: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.lookup(instruction.arg)));
            }
            case BINARY_POWER: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.POWER));
            }
            case BINARY_MULTIPLY: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.MULTIPLY));
            }
            case BINARY_MATRIX_MULTIPLY: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.MATRIX_MULTIPLY));
            }
            case BINARY_FLOOR_DIVIDE: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.FLOOR_DIVIDE));
            }
            case BINARY_TRUE_DIVIDE: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.TRUE_DIVIDE));
            }
            case BINARY_MODULO: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.MODULO));
            }
            case BINARY_ADD: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.ADD));
            }
            case BINARY_SUBTRACT: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.SUBTRACT));
            }
            case BINARY_SUBSCR: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.GET_ITEM));
            }
            case BINARY_LSHIFT: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.LSHIFT));
            }
            case BINARY_RSHIFT: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.RSHIFT));
            }
            case BINARY_AND: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.AND));
            }
            case BINARY_XOR: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.XOR));
            }
            case BINARY_OR: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.OR));
            }

            // **************************************************
            // In-place Dunder Operations
            // **************************************************
            case INPLACE_POWER: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_POWER));
            }
            case INPLACE_MULTIPLY: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_MULTIPLY));
            }
            case INPLACE_MATRIX_MULTIPLY: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_MATRIX_MULTIPLY));
            }
            case INPLACE_FLOOR_DIVIDE: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_FLOOR_DIVIDE));
            }
            case INPLACE_TRUE_DIVIDE: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_TRUE_DIVIDE));
            }
            case INPLACE_MODULO: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_MODULO));
            }
            case INPLACE_ADD: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_ADD));
            }
            case INPLACE_SUBTRACT: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_SUBTRACT));
            }
            case INPLACE_LSHIFT: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_LSHIFT));
            }
            case INPLACE_RSHIFT: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_RSHIFT));
            }
            case INPLACE_AND: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_AND));
            }
            case INPLACE_XOR: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_XOR));
            }
            case INPLACE_OR: {
                return Optional.of(new BinaryDunderOpcode(instruction, PythonBinaryOperators.INPLACE_OR));
            }

            default:
                return Optional.empty();
        }
    }
}
