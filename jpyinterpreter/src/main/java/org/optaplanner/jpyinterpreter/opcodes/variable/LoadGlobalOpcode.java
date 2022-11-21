package org.optaplanner.jpyinterpreter.opcodes.variable;

import org.objectweb.asm.Opcodes;
import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.StackMetadata;
import org.optaplanner.jpyinterpreter.ValueSourceInfo;
import org.optaplanner.jpyinterpreter.implementors.VariableImplementor;
import org.optaplanner.jpyinterpreter.opcodes.AbstractOpcode;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.wrappers.CPythonType;
import org.optaplanner.jpyinterpreter.types.wrappers.PythonObjectWrapper;

public class LoadGlobalOpcode extends AbstractOpcode {

    public LoadGlobalOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    private int getGlobalIndex(FunctionMetadata functionMetadata) {
        return (functionMetadata.pythonCompiledFunction.pythonVersion.compareTo(PythonVersion.PYTHON_3_11) >= 0)
                ? instruction.arg >> 1
                : instruction.arg;
    }

    private boolean pushNullBeforeGlobal(FunctionMetadata functionMetadata) {
        return functionMetadata.pythonCompiledFunction.pythonVersion.compareTo(PythonVersion.PYTHON_3_11) >= 0
                && ((instruction.arg & 1) == 1);
    }

    private PythonLikeObject getGlobal(FunctionMetadata functionMetadata) {
        return functionMetadata.pythonCompiledFunction.globalsMap
                .get(functionMetadata.pythonCompiledFunction.co_names.get(getGlobalIndex(functionMetadata)));
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        boolean pushNull = pushNullBeforeGlobal(functionMetadata);
        PythonLikeObject global = getGlobal(functionMetadata);

        if (pushNull) {
            if (global != null) {
                return stackMetadata
                        .push(ValueSourceInfo.of(this, BuiltinTypes.NULL_TYPE))
                        .push(ValueSourceInfo.of(this, global.__getGenericType()));
            } else {
                return stackMetadata
                        .push(ValueSourceInfo.of(this, BuiltinTypes.NULL_TYPE))
                        .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE));
            }
        } else {
            if (global != null) {
                return stackMetadata
                        .push(ValueSourceInfo.of(this, global.__getGenericType()));
            } else {
                return stackMetadata
                        .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE));
            }
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        int globalIndex = getGlobalIndex(functionMetadata);
        boolean pushNull = pushNullBeforeGlobal(functionMetadata);

        PythonLikeObject global = getGlobal(functionMetadata);

        if (global instanceof CPythonType || global instanceof PythonObjectWrapper) {
            // TODO: note native objects are used somewhere
        }

        if (pushNull) {
            functionMetadata.methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        }
        VariableImplementor.loadGlobalVariable(functionMetadata, stackMetadata, globalIndex,
                (global != null) ? global.__getGenericType() : BuiltinTypes.BASE_TYPE);
    }
}
