package org.optaplanner.jpyinterpreter.opcodes.variable;

import org.optaplanner.jpyinterpreter.FunctionMetadata;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
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

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject global = functionMetadata.pythonCompiledFunction.globalsMap
                .get(functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg));
        if (global != null) {
            return stackMetadata.push(ValueSourceInfo.of(this, global.__getType()));
        } else {
            return stackMetadata.push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE));
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject global = functionMetadata.pythonCompiledFunction.globalsMap.get(
                functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg));
        if (global instanceof CPythonType || global instanceof PythonObjectWrapper) {
            // TODO: note native objects are used somewhere
        }
        VariableImplementor.loadGlobalVariable(functionMetadata.methodVisitor, functionMetadata.className,
                functionMetadata.pythonCompiledFunction, instruction,
                (global != null) ? global.__getType() : BuiltinTypes.BASE_TYPE);
    }
}
