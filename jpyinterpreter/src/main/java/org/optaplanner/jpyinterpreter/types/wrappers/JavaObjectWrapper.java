package org.optaplanner.jpyinterpreter.types.wrappers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;

public class JavaObjectWrapper implements PythonLikeObject, Comparable<JavaObjectWrapper> {

    final static Map<Class<?>, PythonLikeType> classToPythonTypeMap = new HashMap<>();
    final static Map<Class<?>, Map<String, List<Member>>> classToAttributeNameToMemberListMap = new HashMap<>();

    private final PythonLikeType type;

    private final Object wrappedObject;
    private final Class<?> objectClass;
    private final Map<String, List<Member>> attributeNameToMemberListMap;

    private static Stream<Member> getDeclaredMembersStream(Class<?> baseClass) {
        Stream<Field> fieldStream = Stream.of(baseClass.getDeclaredFields()).filter((field) -> !field.isSynthetic());
        Stream<Method> methodStream = Stream.of(baseClass.getDeclaredMethods()).filter((method) -> !method.isSynthetic());
        return Stream.concat(fieldStream, methodStream);
    }

    public static Map<String, List<Member>> getAllFields(Class<?> baseClass) {
        Class<?> clazz = baseClass;

        Stream<Member> memberStream;
        for (memberStream = Stream.empty(); clazz != null; clazz = clazz.getSuperclass()) {
            memberStream = Stream.concat(memberStream, getDeclaredMembersStream(clazz));
        }

        return memberStream
                .filter(member -> member instanceof Field)
                .collect(Collectors.groupingBy(Member::getName,
                        Collectors.mapping(
                                member -> member,
                                Collectors.toList())));
    }

    private Method getGetterMethod(Field field) {
        String propertyName = field.getName();
        String capitalizedName =
                propertyName.isEmpty() ? "" : propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String getterName = "get" + capitalizedName;

        if (attributeNameToMemberListMap.containsKey(getterName)) {
            List<Member> candidates = attributeNameToMemberListMap.get(getterName);
            for (Member candidate : candidates) {
                if (candidate instanceof Method) {
                    Method method = (Method) candidate;
                    if (method.getParameterCount() == 0) {
                        return method;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Cannot get attribute '" + field.getName() + "' on type '" + objectClass + "'");
    }

    private Method getSetterMethod(Field field) {
        String propertyName = field.getName();
        String capitalizedName =
                propertyName.isEmpty() ? "" : propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String setterName = "set" + capitalizedName;

        if (attributeNameToMemberListMap.containsKey(setterName)) {
            List<Member> candidates = attributeNameToMemberListMap.get(setterName);
            for (Member candidate : candidates) {
                if (candidate instanceof Method) {
                    Method method = (Method) candidate;
                    if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(field.getType())) {
                        return method;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Cannot get attribute '" + field.getName() + "' on type '" + objectClass + "'");
    }

    public JavaObjectWrapper(Object wrappedObject) {
        this.wrappedObject = wrappedObject;
        this.objectClass = wrappedObject.getClass();
        this.attributeNameToMemberListMap =
                classToAttributeNameToMemberListMap.computeIfAbsent(objectClass, JavaObjectWrapper::getAllFields);
        this.type = getPythonTypeForClass(objectClass);
    }

    public static PythonLikeType getPythonTypeForClass(Class<?> objectClass) {
        return classToPythonTypeMap.computeIfAbsent(objectClass, JavaObjectWrapper::generatePythonTypeForClass);
    }

    private static PythonLikeType generatePythonTypeForClass(Class<?> objectClass) {
        PythonLikeType out = new PythonLikeType(objectClass.getName(), JavaObjectWrapper.class);
        getDeclaredMembersStream(objectClass)
                .filter(member -> member instanceof Method)
                .forEach(member -> {
                    out.__dir__.put(member.getName(), new JavaMethodReference((Method) member, Map.of()));
                });
        return out;
    }

    public Object getWrappedObject() {
        return wrappedObject;
    }

    @Override
    public PythonLikeObject __getAttributeOrNull(String attributeName) {
        List<Member> candidates = attributeNameToMemberListMap.get(attributeName);
        if (candidates == null) {
            return null;
        }
        if (candidates.size() == 1) {
            Member candidate = candidates.get(0);
            if (candidate instanceof Field) {
                Field field = (Field) candidate;
                try {
                    Object result;
                    if (Modifier.isPublic(field.getModifiers())) {
                        result = field.get(wrappedObject);
                    } else {
                        Method getterMethod = getGetterMethod(field);
                        result = getterMethod.invoke(wrappedObject);
                    }
                    return JavaPythonTypeConversionImplementor.wrapJavaObject(result);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException(
                            "Cannot modify attribute '" + attributeName + "' on type '" + objectClass + "'");
                }
            } else if (candidate instanceof Method) {
                Method method = (Method) candidate;
                return new JavaMethodReference(method, Map.of());
            } else {
                throw new IllegalStateException("Unknown member type (" + candidate.getClass() + ")");
            }
        } else {
            // TODO
            throw new IllegalStateException("Ambiguous attribute for type '" + objectClass + "': multiple candidates match '"
                    + attributeName + "': (" + candidates + ").");
        }
    }

    @Override
    public void __setAttribute(String attributeName, PythonLikeObject value) {
        List<Member> candidates = attributeNameToMemberListMap.get(attributeName);
        if (candidates == null) {
            throw new IllegalArgumentException("type '" + objectClass + "' does not have attribute '" + attributeName + "'");
        }
        if (candidates.size() == 1) {
            Member candidate = candidates.get(0);
            if (candidate instanceof Field) {
                Field field = (Field) candidate;
                Object javaValue = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(field.getType(), value);
                try {
                    if (Modifier.isPublic(field.getModifiers())) {
                        field.set(wrappedObject, javaValue);
                    } else {
                        Method setterMethod = getSetterMethod(field);
                        setterMethod.invoke(wrappedObject, javaValue);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException(
                            "Cannot modify attribute '" + attributeName + "' on type '" + objectClass + "'");
                }
            } else if (candidate instanceof Method) {
                throw new IllegalArgumentException(
                        "Cannot modify attribute '" + attributeName + "' on type '" + objectClass + "'");
            } else {
                throw new IllegalStateException("Unknown member type (" + candidate.getClass() + ")");
            }
        } else {
            throw new IllegalArgumentException("Cannot modify attribute '" + attributeName + "' on type '" + objectClass + "'");
        }
    }

    @Override
    public void __deleteAttribute(String attributeName) {
        throw new IllegalArgumentException("Cannot delete attributes on type '" + objectClass + "'");
    }

    @Override
    public PythonLikeType __getType() {
        return type;
    }

    @Override
    public int compareTo(JavaObjectWrapper javaObjectWrapper) {
        List<Member> compareToMembers = attributeNameToMemberListMap.get("compareTo");
        for (Member member : compareToMembers) {
            if (member instanceof Method) {
                Method method = (Method) member;
                if (method.getDeclaringClass().equals(Comparable.class)) {
                    try {
                        return (int) method.invoke(wrappedObject, javaObjectWrapper.wrappedObject);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        throw new IllegalStateException("Class " + objectClass + " does not implement Comparable");
    }

    @Override
    public String toString() {
        return wrappedObject.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof JavaObjectWrapper) {
            return wrappedObject.equals(((JavaObjectWrapper) other).wrappedObject);
        }
        return wrappedObject.equals(other);
    }

    @Override
    public int hashCode() {
        return wrappedObject.hashCode();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
