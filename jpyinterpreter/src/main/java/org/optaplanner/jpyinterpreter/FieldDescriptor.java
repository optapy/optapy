package org.optaplanner.jpyinterpreter;

import java.util.Objects;

import org.optaplanner.jpyinterpreter.types.PythonLikeType;

public class FieldDescriptor {

    final String pythonFieldName;
    final String javaFieldName;
    final String declaringClassInternalName;
    final String javaFieldTypeDescriptor;
    final PythonLikeType fieldPythonLikeType;
    final boolean isTrueFieldDescriptor;

    public FieldDescriptor(String pythonFieldName, String javaFieldName,
            String declaringClassInternalName, String javaFieldTypeDescriptor,
            PythonLikeType fieldPythonLikeType, boolean isTrueFieldDescriptor) {
        this.pythonFieldName = pythonFieldName;
        this.javaFieldName = javaFieldName;
        this.declaringClassInternalName = declaringClassInternalName;
        this.javaFieldTypeDescriptor = javaFieldTypeDescriptor;
        this.fieldPythonLikeType = fieldPythonLikeType;
        this.isTrueFieldDescriptor = isTrueFieldDescriptor;
    }

    public String getPythonFieldName() {
        return pythonFieldName;
    }

    public String getJavaFieldName() {
        return javaFieldName;
    }

    public String getDeclaringClassInternalName() {
        return declaringClassInternalName;
    }

    public String getJavaFieldTypeDescriptor() {
        return javaFieldTypeDescriptor;
    }

    public PythonLikeType getFieldPythonLikeType() {
        return fieldPythonLikeType;
    }

    public boolean isTrueFieldDescriptor() {
        return isTrueFieldDescriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldDescriptor that = (FieldDescriptor) o;
        return pythonFieldName.equals(that.pythonFieldName) && javaFieldName.equals(that.javaFieldName)
                && declaringClassInternalName.equals(that.declaringClassInternalName)
                && javaFieldTypeDescriptor.equals(that.javaFieldTypeDescriptor)
                && fieldPythonLikeType.equals(that.fieldPythonLikeType)
                && isTrueFieldDescriptor == that.isTrueFieldDescriptor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pythonFieldName, javaFieldName, declaringClassInternalName, javaFieldTypeDescriptor,
                fieldPythonLikeType, isTrueFieldDescriptor);
    }

    @Override
    public String toString() {
        return "FieldDescriptor{" +
                "pythonFieldName='" + pythonFieldName + '\'' +
                ", javaFieldName='" + javaFieldName + '\'' +
                ", declaringClassInternalName='" + declaringClassInternalName + '\'' +
                ", javaFieldTypeDescriptor='" + javaFieldTypeDescriptor + '\'' +
                ", fieldPythonLikeType=" + fieldPythonLikeType +
                ", isTrueFieldDescriptor=" + isTrueFieldDescriptor +
                '}';
    }
}
