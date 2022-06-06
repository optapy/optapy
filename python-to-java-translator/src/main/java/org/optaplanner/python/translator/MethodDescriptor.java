package org.optaplanner.python.translator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MethodDescriptor {
    final String declaringClassInternalName;
    final String methodName;
    final String methodDescriptor;

    final MethodType methodType;

    public MethodDescriptor(Method method) {
        this.declaringClassInternalName = Type.getInternalName(method.getDeclaringClass());
        this.methodName = method.getName();
        this.methodDescriptor = Type.getMethodDescriptor(method);
        if (method.getDeclaringClass().isInterface()) {
            this.methodType = MethodType.INTERFACE;
        } else if (Modifier.isStatic(method.getModifiers())) {
            this.methodType = MethodType.STATIC;
        } else {
            this.methodType = MethodType.VIRTUAL;
        }
    }

    public MethodDescriptor(Constructor<?> constructor) {
        this.declaringClassInternalName = Type.getInternalName(constructor.getDeclaringClass());
        this.methodName = constructor.getName();
        this.methodDescriptor = Type.getConstructorDescriptor(constructor);
        this.methodType = MethodType.CONSTRUCTOR;
    }

    public MethodDescriptor(String declaringClassInternalName, MethodType methodType, String methodName,
            String methodDescriptor) {
        this.declaringClassInternalName = declaringClassInternalName;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.methodType = methodType;
    }

    public MethodDescriptor(Class<?> declaringClass, boolean isStatic,
            String methodName, Class<?> returnType, Class<?>... parameterTypes) {
        this.declaringClassInternalName = Type.getInternalName(declaringClass);
        this.methodName = methodName;
        Type[] parameterAsmTypes = new Type[parameterTypes.length];
        for (int i = 0; i < parameterAsmTypes.length; i++) {
            parameterAsmTypes[i] = Type.getType(parameterTypes[i]);
        }
        this.methodDescriptor = Type.getMethodDescriptor(Type.getType(returnType), parameterAsmTypes);
        if (declaringClass.isInterface()) {
            this.methodType = MethodType.INTERFACE;
        } else if (methodName.equals("<init>")) {
            this.methodType = MethodType.CONSTRUCTOR;
        } else if (isStatic) {
            this.methodType = MethodType.STATIC;
        } else {
            this.methodType = MethodType.VIRTUAL;
        }
    }

    public String getDeclaringClassInternalName() {
        return declaringClassInternalName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public void callMethod(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(methodType.getOpcode(), declaringClassInternalName, methodName, methodDescriptor,
                methodType == MethodType.INTERFACE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodDescriptor that = (MethodDescriptor) o;
        return declaringClassInternalName.equals(that.declaringClassInternalName) && methodName.equals(that.methodName)
                && methodDescriptor.equals(that.methodDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringClassInternalName, methodName, methodDescriptor);
    }

    public Type getReturnType() {
        return Type.getReturnType(methodDescriptor);
    }

    public Type[] getParameterTypes() {
        return Type.getArgumentTypes(methodDescriptor);
    }

    public enum MethodType {
        VIRTUAL(Opcodes.INVOKEVIRTUAL),
        STATIC(Opcodes.INVOKESTATIC),
        INTERFACE(Opcodes.INVOKEINTERFACE),
        CONSTRUCTOR(Opcodes.INVOKESPECIAL);

        private final int opcode;

        MethodType(int opcode) {
            this.opcode = opcode;
        }

        public int getOpcode() {
            return opcode;
        }
    }
}
