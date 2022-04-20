package org.optaplanner.optapy.translator.types;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.implementors.JavaPythonTypeConversionImplementor;

public class JavaObjectWrapper implements PythonLikeObject {
    final static PythonLikeType CLASS_TYPE = new PythonLikeType("object");

    private final Object wrappedObject;
    private final Class<?> objectClass;
    private final Map<String, List<Member>> attributeNameToMemberListMap;

    private static Stream<Member> getDeclaredMembersStream(Class<?> baseClass) {
        Stream<Field> fieldStream = Stream.of(baseClass.getDeclaredFields()).filter((field) -> !field.isSynthetic());
        Stream<Method> methodStream = Stream.of(baseClass.getDeclaredMethods()).filter((method) -> !method.isSynthetic());
        return Stream.concat(fieldStream, methodStream);
    }

    public static Map<String, List<Member>> getAllMembers(Class<?> baseClass) {
        Class<?> clazz = baseClass;

        Stream<Member> memberStream;
        for(memberStream = Stream.empty(); clazz != null; clazz = clazz.getSuperclass()) {
            memberStream = Stream.concat(memberStream, getDeclaredMembersStream(clazz));
        }

        return memberStream.collect(Collectors.groupingBy(Member::getName,
                                                          Collectors.mapping(
                                                                  member -> member,
                                                                  Collectors.toList())));
    }

    private Method getGetterMethod(Field field) {
        String propertyName = field.getName();
        String capitalizedName = propertyName.isEmpty() ? "" : propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
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
        String capitalizedName = propertyName.isEmpty() ? "" : propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
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
        this.attributeNameToMemberListMap = getAllMembers(objectClass);
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
                    throw new IllegalArgumentException("Cannot modify attribute '" + attributeName + "' on type '" + objectClass + "'");
                }
            } else if (candidate instanceof Method) {
                Method method = (Method) candidate;
                return new JavaMethodReference(method, Map.of());
            } else {
                throw new IllegalStateException("Unknown member type (" + candidate.getClass() + ")");
            }
        } else {
            // TODO
            throw new IllegalStateException("Ambiguous attribute for type '" + objectClass + "': multiple candidates match '" + attributeName + "': (" + candidates + ").");
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
                    throw new IllegalArgumentException("Cannot modify attribute '" + attributeName + "' on type '" + objectClass + "'");
                }
            } else if (candidate instanceof Method) {
                throw new IllegalArgumentException("Cannot modify attribute '" + attributeName + "' on type '" + objectClass + "'");
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
        return CLASS_TYPE;
    }
}
