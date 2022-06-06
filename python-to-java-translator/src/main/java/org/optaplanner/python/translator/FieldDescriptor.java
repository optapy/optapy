package org.optaplanner.python.translator;

import java.util.Objects;

import org.optaplanner.python.translator.types.PythonLikeType;

public class FieldDescriptor {

    final String fieldName;

    final String declaringClassInternalName;
    final String javaFieldTypeDescriptor;
    final PythonLikeType fieldPythonLikeType;

    public FieldDescriptor(String fieldName, String declaringClassInternalName, String javaFieldTypeDescriptor,
            PythonLikeType fieldPythonLikeType) {
        this.fieldName = fieldName;
        this.declaringClassInternalName = declaringClassInternalName;
        this.javaFieldTypeDescriptor = javaFieldTypeDescriptor;
        this.fieldPythonLikeType = fieldPythonLikeType;
    }

    public String getFieldName() {
        return fieldName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldDescriptor that = (FieldDescriptor) o;
        return fieldName.equals(that.fieldName) && declaringClassInternalName.equals(that.declaringClassInternalName)
                && javaFieldTypeDescriptor.equals(that.javaFieldTypeDescriptor)
                && fieldPythonLikeType.equals(that.fieldPythonLikeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, declaringClassInternalName, javaFieldTypeDescriptor, fieldPythonLikeType);
    }

    @Override
    public String toString() {
        return "FieldDescriptor{" +
                "fieldName='" + fieldName + '\'' +
                ", declaringClassInternalName='" + declaringClassInternalName + '\'' +
                ", javaFieldTypeDescriptor='" + javaFieldTypeDescriptor + '\'' +
                ", fieldPythonLikeType=" + fieldPythonLikeType +
                '}';
    }
}
