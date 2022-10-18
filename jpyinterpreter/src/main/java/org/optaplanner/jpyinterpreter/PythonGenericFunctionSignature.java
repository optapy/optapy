package org.optaplanner.jpyinterpreter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.optaplanner.jpyinterpreter.implementors.CollectionImplementor;
import org.optaplanner.jpyinterpreter.implementors.FunctionImplementor;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.types.BuiltinTypes;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeDict;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeTuple;

public class PythonGenericFunctionSignature extends PythonFunctionSignature {
    public static PythonGenericFunctionSignature forGenericMethod(Method method) {
        MethodDescriptor methodDescriptor = new MethodDescriptor(method);
        PythonLikeType returnType = JavaPythonTypeConversionImplementor.getPythonLikeType(method.getReturnType());
        return new PythonGenericFunctionSignature(methodDescriptor, returnType);
    }

    public PythonGenericFunctionSignature(MethodDescriptor methodDescriptor,
            PythonLikeType returnType) {
        super(methodDescriptor, returnType,
                BuiltinTypes.LIST_TYPE, BuiltinTypes.DICT_TYPE);
    }

    @Override
    public boolean matchesParameters(PythonLikeType... callParameters) {
        return true;
    }

    @Override
    public boolean moreSpecificThan(PythonFunctionSignature other) {
        return false;
    }

    public void callMethod(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int argumentCount) {
        callWithoutKeywords(functionMetadata, stackMetadata, argumentCount);
    }

    public void callWithoutKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int argumentCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
        callWithKeywords(functionMetadata, stackMetadata.pushTemp(BuiltinTypes.TUPLE_TYPE), argumentCount);
    }

    public void callWithKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int argumentCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        unwrapBoundMethod(functionMetadata, stackMetadata, argumentCount + 1);

        // stack is callable, arg0, arg1, ..., arg(argc - len(keys)), ..., arg(argc - 1), keys
        // We know the total number of arguments, but not the number of individual positional/keyword arguments
        // Since Java Bytecode require consistent stack frames  (i.e. the body of a loop must start with
        // the same number of elements in the stack), we need to add the tuple/map in the same object
        // which will delegate it to either the tuple or the map depending on position and the first item size
        CollectionImplementor.buildCollection(FunctionImplementor.TupleMapPair.class, methodVisitor, argumentCount);

        // stack is callable, tupleMapPair
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(FunctionImplementor.TupleMapPair.class), "tuple",
                Type.getDescriptor(PythonLikeTuple.class));

        // stack is callable, tupleMapPair, positionalArgs
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(FunctionImplementor.TupleMapPair.class), "map",
                Type.getDescriptor(PythonLikeDict.class));

        // Stack is callable, positionalArgs, keywordArgs
        methodDescriptor.callMethod(methodVisitor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonGenericFunctionSignature that = (PythonGenericFunctionSignature) o;
        return returnType.equals(that.returnType) && Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(returnType);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    @Override
    public String toString() {
        return methodDescriptor.getMethodName() +
                Arrays.stream(parameterTypes).map(PythonLikeType::toString).collect(Collectors.joining(", ", "(", ") -> ")) +
                returnType;
    }
}
