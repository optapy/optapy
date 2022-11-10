package org.optaplanner.jpyinterpreter.opcodes.meta;

import org.objectweb.asm.Opcodes;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;

public class ReturnGeneratorOpcode extends AbstractOpcode {

    public ReturnGeneratorOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        // Although this opcode does nothing, it is followed by a POP_TOP, which need something to pop
        return stackMetadata.push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        functionMetadata.methodVisitor.visitInsn(Opcodes.ACONST_NULL);
    }
}
