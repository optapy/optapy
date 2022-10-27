package org.optaplanner.optapy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.types.collections.PythonLikeList;
import org.optaplanner.jpyinterpreter.types.wrappers.OpaquePythonReference;

public class PythonList<T> extends PythonLikeList<T> implements PythonObject, List<T> {
    private static Function<OpaquePythonReference, Object> clearPythonList;
    private static Function<OpaquePythonReference, Integer> getPythonListLength;
    private static BiFunction<OpaquePythonReference, Integer, Object> getItemAtIndexInPythonList;
    private static TriFunction<OpaquePythonReference, Integer, Object, Object> setItemAtIndexInPythonList;
    private static BiFunction<OpaquePythonReference, Object, Boolean> addItemToPythonList;
    private static TriFunction<OpaquePythonReference, Integer, Object, Object> addItemAtIndexInPythonList;
    private static BiFunction<OpaquePythonReference, Object, Boolean> removeItemFromPythonList;
    private static BiFunction<OpaquePythonReference, Integer, Boolean> removeItemAtIndexFromPythonList;
    private static BiFunction<OpaquePythonReference, Object, Boolean> doesPythonListContainItem;
    private static TriFunction<OpaquePythonReference, Integer, Integer, OpaquePythonReference> slicePythonList;

    public static void setClearPythonList(Function<OpaquePythonReference, Object> clearPythonList) {
        PythonList.clearPythonList = clearPythonList;
    }

    public static void setGetPythonListLength(Function<OpaquePythonReference, Integer> getPythonListLength) {
        PythonList.getPythonListLength = getPythonListLength;
    }

    public static void
            setGetItemAtIndexInPythonList(BiFunction<OpaquePythonReference, Integer, Object> getItemAtIndexInPythonList) {
        PythonList.getItemAtIndexInPythonList = getItemAtIndexInPythonList;
    }

    public static void setSetItemAtIndexInPythonList(
            TriFunction<OpaquePythonReference, Integer, Object, Object> setItemAtIndexInPythonList) {
        PythonList.setItemAtIndexInPythonList = setItemAtIndexInPythonList;
    }

    public static void setAddItemToPythonList(BiFunction<OpaquePythonReference, Object, Boolean> addItemToPythonList) {
        PythonList.addItemToPythonList = addItemToPythonList;
    }

    public static void setAddItemAtIndexInPythonList(
            TriFunction<OpaquePythonReference, Integer, Object, Object> addItemAtIndexInPythonList) {
        PythonList.addItemAtIndexInPythonList = addItemAtIndexInPythonList;
    }

    public static void
            setRemoveItemFromPythonList(BiFunction<OpaquePythonReference, Object, Boolean> removeItemFromPythonList) {
        PythonList.removeItemFromPythonList = removeItemFromPythonList;
    }

    public static void setRemoveItemAtIndexFromPythonList(
            BiFunction<OpaquePythonReference, Integer, Boolean> removeItemAtIndexFromPythonList) {
        PythonList.removeItemAtIndexFromPythonList = removeItemAtIndexFromPythonList;
    }

    public static void
            setDoesPythonListContainItem(BiFunction<OpaquePythonReference, Object, Boolean> doesPythonListContainItem) {
        PythonList.doesPythonListContainItem = doesPythonListContainItem;
    }

    public static void
            setSlicePythonList(TriFunction<OpaquePythonReference, Integer, Integer, OpaquePythonReference> slicePythonList) {
        PythonList.slicePythonList = slicePythonList;
    }

    private OpaquePythonReference pythonListOpaqueReference;
    private Map<Number, Object> idMap;

    private TriFunction<OpaquePythonReference, String, Object, Object> pythonSetter;

    private final List<Object> cachedObjectList;
    private final List<PythonLikeObject> cachedPythonLikeObjectList;

    public PythonList(OpaquePythonReference pythonListOpaqueReference, Number id, Map<Number, Object> idMap,
            TriFunction<OpaquePythonReference, String, Object, Object> pythonSetter) {
        this.pythonListOpaqueReference = pythonListOpaqueReference;
        this.idMap = idMap;
        this.pythonSetter = pythonSetter;
        int size = getPythonListLength.apply(pythonListOpaqueReference);
        this.cachedObjectList = new ArrayList<>(size);
        this.cachedPythonLikeObjectList = getDelegate();
        for (int i = 0; i < size; i++) {
            cachedObjectList.add(null);
            cachedPythonLikeObjectList.add(null);
        }
        for (int i = 0; i < size; i++) {
            get(i); // use side-effect of populating cachedObjectList
        }
    }

    public PythonList(OpaquePythonReference pythonListOpaqueReference, Number id, Map<Number, Object> idMap,
            TriFunction<OpaquePythonReference, String, Object, Object> pythonSetter, List cachedObjectList,
            List cachedPythonLikeObjectList) {
        this.pythonListOpaqueReference = pythonListOpaqueReference;
        this.idMap = idMap;
        this.pythonSetter = pythonSetter;
        this.cachedObjectList = cachedObjectList;
        this.cachedPythonLikeObjectList = cachedPythonLikeObjectList;
    }

    @Override
    public OpaquePythonReference get__optapy_Id() {
        return pythonListOpaqueReference;
    }

    @Override
    public Map<Number, Object> get__optapy_reference_map() {
        return idMap;
    }

    @Override
    public void forceUpdate() {
        clearPythonList.apply(pythonListOpaqueReference);
        for (Object o : cachedObjectList) {
            if (o instanceof OpaquePythonReference) {
                addItemToPythonList.apply(pythonListOpaqueReference, o);
            } else if (o instanceof PythonObject) {
                addItemToPythonList.apply(pythonListOpaqueReference, ((PythonObject) o).get__optapy_Id());
            } else {
                addItemToPythonList.apply(pythonListOpaqueReference, o);
            }
        }
    }

    @Override
    public void $setFields(OpaquePythonReference reference, Number id, Map referenceMap, TriFunction setter) {

    }

    @Override
    public void readFromPythonObject(Set doneSet, Map<Number, Object> referenceMap) {

    }

    @Override
    public void visitIds(Map<Number, Object> referenceMap) {

    }

    @Override
    public int size() {
        return cachedObjectList.size();
    }

    @Override
    public boolean isEmpty() {
        return cachedObjectList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return cachedObjectList.contains(o);
    }

    @Override
    public Iterator iterator() {
        return new Iterator() {
            int index = 0;
            final int length = size();

            @Override
            public boolean hasNext() {
                return index < length;
            }

            @Override
            public Object next() {
                Object out = get(index);
                index++;
                return out;
            }
        };
    }

    @Override
    public Object[] toArray() {
        int length = size();
        Object[] out = new Object[length];
        for (int i = 0; i < length; i++) {
            out[i] = get(i);
        }
        return out;
    }

    @Override
    public Object[] toArray(Object[] t1s) {
        int length = size();
        if (t1s.length == length) {
            for (int i = 0; i < length; i++) {
                t1s[i] = get(i);
            }
            return t1s;
        } else {
            return toArray();
        }
    }

    @Override
    public boolean add(Object t) {
        cachedObjectList.add(t);
        cachedPythonLikeObjectList.add(JavaPythonTypeConversionImplementor.wrapJavaObject(t));

        if (pythonSetter != PythonWrapperGenerator.NONE_PYTHON_SETTER) {
            if (t instanceof OpaquePythonReference) {
                addItemToPythonList.apply(pythonListOpaqueReference, t);
            } else if (t instanceof PythonObject) {
                addItemToPythonList.apply(pythonListOpaqueReference, ((PythonObject) t).get__optapy_Id());
            } else {
                addItemToPythonList.apply(pythonListOpaqueReference, t);
            }
        }
        return true;
    }

    @Override
    public boolean remove(Object t) {
        boolean out = cachedObjectList.remove(t);
        cachedPythonLikeObjectList.remove(JavaPythonTypeConversionImplementor.wrapJavaObject(t));

        if (pythonSetter != PythonWrapperGenerator.NONE_PYTHON_SETTER) {
            if (t instanceof OpaquePythonReference) {
                return removeItemFromPythonList.apply(pythonListOpaqueReference, t);
            } else if (t instanceof PythonObject) {
                return removeItemFromPythonList.apply(pythonListOpaqueReference, ((PythonObject) t).get__optapy_Id());
            } else {
                return removeItemFromPythonList.apply(pythonListOpaqueReference, t);
            }
        } else {
            return out;
        }
    }

    @Override
    public boolean containsAll(Collection collection) {
        for (Object o : collection) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection collection) {
        for (Object o : collection) {
            add(o);
        }
        return true;
    }

    @Override
    public boolean addAll(int i, Collection collection) {
        int index = i;
        for (Object o : collection) {
            add(index, o);
            index++;
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection collection) {
        boolean anyRemoved = false;
        for (Object o : collection) {
            anyRemoved |= remove(o);
        }
        return anyRemoved;
    }

    @Override
    public boolean retainAll(Collection collection) {
        boolean anyRemoved = false;
        for (Object o : collection) {
            if (!contains(o)) {
                anyRemoved |= remove(o);
            }
        }
        return anyRemoved;
    }

    @Override
    public void clear() {
        cachedObjectList.clear();
        cachedPythonLikeObjectList.clear();

        if (pythonSetter != PythonWrapperGenerator.NONE_PYTHON_SETTER) {
            clearPythonList.apply(pythonListOpaqueReference);
        }
    }

    @Override
    public T get(int i) {
        if (i < 0 || i >= cachedObjectList.size()) {
            throw new IndexOutOfBoundsException();
        }

        Object maybeResult = cachedObjectList.get(i);
        if (maybeResult != null) {
            return (T) maybeResult;
        }

        Object out = getItemAtIndexInPythonList.apply(pythonListOpaqueReference, i);

        if (out instanceof Number || out instanceof Boolean || out instanceof String) {
            if (out instanceof Long) {
                cachedObjectList.set(i, ((Long) out).intValue());
            } else {
                cachedObjectList.set(i, out);
            }
            cachedPythonLikeObjectList.set(i, JavaPythonTypeConversionImplementor.wrapJavaObject(out));
            return (T) out;
        }

        if (out instanceof PythonLikeObject) {
            cachedObjectList.set(i, out);
            cachedPythonLikeObjectList.set(i, (PythonLikeObject) out);
            return (T) out;
        }

        // Different proxies of the same object are different objects according to IdentityHashMap,
        // so wrap it (which will return the same Proxy if it was already created)
        Object wrapped_out = PythonWrapperGenerator.wrap(PythonWrapperGenerator.getJavaClass((OpaquePythonReference) out),
                (OpaquePythonReference) out, idMap, pythonSetter);
        cachedObjectList.set(i, wrapped_out);
        cachedPythonLikeObjectList.set(i, (PythonLikeObject) wrapped_out);
        return (T) wrapped_out;
    }

    @Override
    public Object set(int i, Object t) {
        Object old = get(i);
        cachedObjectList.set(i, t);
        cachedPythonLikeObjectList.set(i, JavaPythonTypeConversionImplementor.wrapJavaObject(t));

        if (pythonSetter != PythonWrapperGenerator.NONE_PYTHON_SETTER) {
            if (t instanceof OpaquePythonReference) {
                setItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, t);
            } else if (t instanceof PythonObject) {
                setItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, ((PythonObject) t).get__optapy_Id());
            } else {
                setItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, t);
            }
        }
        return old;
    }

    @Override
    public void add(int i, Object t) {
        cachedObjectList.add(i, t);
        cachedPythonLikeObjectList.add(i, JavaPythonTypeConversionImplementor.wrapJavaObject(t));

        if (pythonSetter != PythonWrapperGenerator.NONE_PYTHON_SETTER) {
            if (t instanceof OpaquePythonReference) {
                addItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, t);
            } else if (t instanceof PythonObject) {
                addItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, ((PythonObject) t).get__optapy_Id());
            } else {
                addItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, t);
            }
        }
    }

    @Override
    public T remove(int i) {
        T out = get(i);
        cachedObjectList.remove(i);
        cachedPythonLikeObjectList.remove(i);

        if (pythonSetter != PythonWrapperGenerator.NONE_PYTHON_SETTER) {
            removeItemAtIndexFromPythonList.apply(pythonListOpaqueReference, i);
        }
        return out;
    }

    @Override
    public int indexOf(Object o) {
        int length = size();
        for (int i = 0; i < length; i++) {
            if (get(i).equals(o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int length = size();
        for (int i = length - 1; i >= 0; i--) {
            if (get(i).equals(o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ListIterator listIterator() {
        return new PythonListIterator(0);
    }

    @Override
    public ListIterator listIterator(int start) {
        return new PythonListIterator(start);
    }

    @Override
    public List subList(int start, int end) {
        return new PythonList(slicePythonList.apply(pythonListOpaqueReference, start, end), null, null, pythonSetter,
                cachedObjectList.subList(start, end), cachedPythonLikeObjectList.subList(start, end));
    }

    @Override
    public String toString() {
        return PythonWrapperGenerator.getPythonObjectString(pythonListOpaqueReference);
    }

    public class PythonListIterator implements ListIterator {

        int index;

        public PythonListIterator(int start) {
            index = start;
        }

        @Override
        public boolean hasNext() {
            return index < size();
        }

        @Override
        public Object next() {
            Object out = get(index);
            index++;
            return out;
        }

        @Override
        public boolean hasPrevious() {
            return index > 0;
        }

        @Override
        public Object previous() {
            index--;
            return get(index);
        }

        @Override
        public int nextIndex() {
            return index;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Object t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Object t) {
            throw new UnsupportedOperationException();
        }
    }
}
