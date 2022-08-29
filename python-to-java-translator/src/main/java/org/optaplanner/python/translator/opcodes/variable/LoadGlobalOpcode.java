package org.optaplanner.python.translator.opcodes.variable;

import org.optaplanner.python.translator.FunctionMetadata;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.StackMetadata;
import org.optaplanner.python.translator.ValueSourceInfo;
import org.optaplanner.python.translator.implementors.VariableImplementor;
import org.optaplanner.python.translator.opcodes.AbstractOpcode;
import org.optaplanner.python.translator.types.PythonLikeType;
import org.optaplanner.python.translator.types.wrappers.CPythonType;
import org.optaplanner.python.translator.types.wrappers.PythonObjectWrapper;

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
            return stackMetadata.push(ValueSourceInfo.of(this, PythonLikeType.getBaseType()));
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject global = functionMetadata.pythonCompiledFunction.globalsMap.get(
                functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg));
        if (global instanceof CPythonType || global instanceof PythonObjectWrapper) {
            throw new UnsupportedOperationException("Detected untranslated object " + global + " in the bytecode; "
                    + "native Python will likely be faster");
        }
        VariableImplementor.loadGlobalVariable(functionMetadata.methodVisitor, functionMetadata.className,
                functionMetadata.pythonCompiledFunction, instruction,
                (global != null) ? global.__getType() : PythonLikeType.getBaseType());
    }
}
