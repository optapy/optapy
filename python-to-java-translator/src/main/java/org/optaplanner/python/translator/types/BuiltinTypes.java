package org.optaplanner.python.translator.types;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.PythonOverloadImplementor;
import org.optaplanner.python.translator.builtins.GlobalBuiltins;
import org.optaplanner.python.translator.types.collections.PythonIterator;
import org.optaplanner.python.translator.types.collections.PythonLikeDict;
import org.optaplanner.python.translator.types.collections.PythonLikeFrozenSet;
import org.optaplanner.python.translator.types.collections.PythonLikeList;
import org.optaplanner.python.translator.types.collections.PythonLikeSet;
import org.optaplanner.python.translator.types.collections.PythonLikeTuple;
import org.optaplanner.python.translator.types.collections.view.DictItemView;
import org.optaplanner.python.translator.types.collections.view.DictKeyView;
import org.optaplanner.python.translator.types.collections.view.DictValueView;
import org.optaplanner.python.translator.types.numeric.PythonBoolean;
import org.optaplanner.python.translator.types.numeric.PythonComplex;
import org.optaplanner.python.translator.types.numeric.PythonFloat;
import org.optaplanner.python.translator.types.numeric.PythonInteger;
import org.optaplanner.python.translator.types.numeric.PythonNumber;

public class BuiltinTypes {
    public static final PythonLikeType BASE_TYPE =
            new PythonLikeType("base-object", PythonLikeObject.class, Collections.emptyList());
    public static final PythonLikeType TYPE_TYPE = new PythonLikeType("type", PythonLikeType.class, List.of(BASE_TYPE));
    public static final PythonLikeType MODULE_TYPE = new PythonLikeType("module", PythonModule.class, List.of(BASE_TYPE));
    public static final PythonLikeType FUNCTION_TYPE =
            new PythonLikeType("function", PythonLikeFunction.class, List.of(BASE_TYPE));
    public static final PythonLikeType GENERATOR_TYPE =
            new PythonLikeType("generator", PythonGenerator.class, List.of(BASE_TYPE));
    public static final PythonLikeType CODE_TYPE = new PythonLikeType("code", PythonCode.class, List.of(BASE_TYPE));
    public static final PythonLikeType CELL_TYPE = new PythonLikeType("cell", PythonCell.class, List.of(BASE_TYPE));

    public static final PythonLikeType NONE_TYPE = new PythonLikeType("NoneType", PythonNone.class, List.of(BASE_TYPE));
    public static final PythonLikeType NOT_IMPLEMENTED_TYPE =
            new PythonLikeType("NotImplementedType", NotImplemented.class, List.of(BASE_TYPE));
    public static final PythonLikeType ELLIPSIS_TYPE = new PythonLikeType("EllipsisType", Ellipsis.class, List.of(BASE_TYPE));

    public static final PythonLikeType NUMBER_TYPE = new PythonLikeType("number", PythonNumber.class, List.of(BASE_TYPE));
    public static final PythonLikeType INT_TYPE = new PythonLikeType("int", PythonInteger.class, List.of(NUMBER_TYPE));
    public static final PythonLikeType BOOLEAN_TYPE = new PythonLikeType("bool", PythonBoolean.class, List.of(INT_TYPE));
    public static final PythonLikeType FLOAT_TYPE = new PythonLikeType("float", PythonFloat.class, List.of(NUMBER_TYPE));
    public final static PythonLikeType COMPLEX_TYPE = new PythonLikeType("complex", PythonComplex.class, List.of(NUMBER_TYPE));

    public static final PythonLikeType STRING_TYPE = new PythonLikeType("str", PythonString.class, List.of(BASE_TYPE));

    public static final PythonLikeType ITERATOR_TYPE = new PythonLikeType("iterator", PythonIterator.class, List.of(BASE_TYPE));
    public static final PythonLikeType DICT_TYPE = new PythonLikeType("dict", PythonLikeDict.class, List.of(BASE_TYPE));
    public static final PythonLikeType DICT_ITEM_VIEW_TYPE =
            new PythonLikeType("dict_items", DictItemView.class, List.of(BASE_TYPE));
    public static final PythonLikeType DICT_KEY_VIEW_TYPE =
            new PythonLikeType("dict_keys", DictKeyView.class, List.of(BASE_TYPE));
    public static final PythonLikeType DICT_VALUE_VIEW_TYPE =
            new PythonLikeType("dict_values", DictValueView.class, List.of(BASE_TYPE));
    public static final PythonLikeType SET_TYPE = new PythonLikeType("set", PythonLikeSet.class, List.of(BASE_TYPE));
    public static final PythonLikeType FROZEN_SET_TYPE =
            new PythonLikeType("frozenset", PythonLikeFrozenSet.class, List.of(BASE_TYPE));
    public static final PythonLikeType TUPLE_TYPE = new PythonLikeType("tuple", PythonLikeTuple.class, List.of(BASE_TYPE));
    public static final PythonLikeType LIST_TYPE = new PythonLikeType("list", PythonLikeList.class, List.of(BASE_TYPE));
    public static final PythonLikeType RANGE_TYPE = new PythonLikeType("range", PythonRange.class, List.of(BASE_TYPE));
    public static final PythonLikeType SLICE_TYPE = new PythonLikeType("slice", PythonSlice.class, List.of(BASE_TYPE));
    /**
     * The ASM generated bytecode. Used by
     * asmClassLoader to create the Java versions of Python methods
     */
    public static final Map<String, byte[]> classNameToBytecode = new HashMap<>();
    /**
     * A custom classloader that looks for the class in
     * classNameToBytecode
     */
    public static ClassLoader asmClassLoader = new ClassLoader() {
        // getName() is an abstract method in Java 11 but not in Java 8
        public String getName() {
            return "OptaPlanner Gizmo Python Bytecode ClassLoader";
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            if (classNameToBytecode.containsKey(name)) {
                // Gizmo generated class
                byte[] byteCode = classNameToBytecode.get(name);
                return defineClass(name, byteCode, 0, byteCode.length);
            } else {
                // Not a Gizmo generated class; load from parent class loader
                return PythonBytecodeToJavaBytecodeTranslator.class.getClassLoader().loadClass(name);
            }
        }
    };

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeType::registerBaseType);
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeType::registerTypeType);

        for (Field field : BuiltinTypes.class.getFields()) {
            try {
                if (!field.getType().equals(PythonLikeType.class)) {
                    continue;
                }

                PythonLikeType pythonLikeType = (PythonLikeType) field.get(null);
                Class<?> javaClass = pythonLikeType.getJavaClass();
                Class.forName(javaClass.getName(), true, javaClass.getClassLoader());
            } catch (IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        GlobalBuiltins.loadBuiltinConstants();
    }

    public static void load() {
    }

    // Class should not be initated; only holds static constants
    private BuiltinTypes() {
    }
}