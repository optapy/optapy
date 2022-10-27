package org.optaplanner.jpyinterpreter.opcodes.collection;

import java.util.Optional;

import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.opcodes.Opcode;

public class CollectionOpcodes {
    public static Optional<Opcode> lookupOpcodeForInstruction(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        switch (instruction.opcode) {
            case GET_ITER: {
                return Optional.of(new GetIterOpcode(instruction));
            }
            case STORE_SUBSCR: {
                return Optional.of(new SetItemOpcode(instruction));
            }
            case DELETE_SUBSCR: {
                return Optional.of(new DeleteItemOpcode(instruction));
            }
            case CONTAINS_OP: {
                return Optional.of(new ContainsOpcode(instruction));
            }
            case UNPACK_SEQUENCE: {
                return Optional.of(new UnpackSequenceOpcode(instruction));
            }
            case UNPACK_EX: {
                return Optional.of(new UnpackSequenceWithTailOpcode(instruction));
            }

            // **************************************************
            // Collection Construction Operations
            // **************************************************
            case BUILD_SLICE: {
                return Optional.of(new BuildSliceOpcode(instruction));
            }

            case BUILD_TUPLE: {
                return Optional.of(new BuildTupleOpcode(instruction));
            }
            case BUILD_LIST: {
                return Optional.of(new BuildListOpcode(instruction));
            }
            case BUILD_SET: {
                return Optional.of(new BuildSetOpcode(instruction));
            }
            case BUILD_MAP: {
                return Optional.of(new BuildMapOpcode(instruction));
            }
            case BUILD_CONST_KEY_MAP: {
                return Optional.of(new BuildConstantKeyMapOpcode(instruction));
            }

            // **************************************************
            // Collection Edit Operations
            // **************************************************
            case LIST_TO_TUPLE: {
                return Optional.of(new ListToTupleOpcode(instruction));
            }

            case SET_ADD:
            case LIST_APPEND: {
                // Same opcode for SET_ADD and LIST_APPEND
                return Optional.of(new CollectionAddOpcode(instruction));
            }
            case MAP_ADD: {
                return Optional.of(new MapPutOpcode(instruction));
            }

            case LIST_EXTEND:
            case SET_UPDATE: {
                // Same opcode for LIST_EXTEND and SET_UPDATE
                return Optional.of(new CollectionAddAllOpcode(instruction));
            }
            case DICT_UPDATE: {
                return Optional.of(new MapPutAllOpcode(instruction));
            }
            case DICT_MERGE: {
                return Optional.of(new MapMergeOpcode(instruction));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
