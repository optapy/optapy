package org.optaplanner.jpyinterpreter.opcodes.generator;

import org.objectweb.asm.Opcodes;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;

public class ResumeOpcode extends AbstractOpcode {

    public ResumeOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    public static ResumeType getResumeType(int arg) {
        switch (arg) {
            case 0:
                return ResumeType.START;

            case 1:
                return ResumeType.YIELD;

            case 2:
                return ResumeType.YIELD_FROM;

            case 3:
                return ResumeType.AWAIT;

            default:
                throw new IllegalArgumentException("Invalid RESUME opcode argument: " + arg);
        }
    }

    public ResumeType getResumeType() {
        return getResumeType(instruction.arg);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.copy();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        functionMetadata.methodVisitor.visitInsn(Opcodes.NOP);
    }

    public enum ResumeType {
        START,
        YIELD,
        YIELD_FROM,
        AWAIT
    }
}
