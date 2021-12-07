package org.optaplanner.optapy;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.optaplanner.core.api.function.TriFunction;

public class PythonList<T> implements PythonObject, List<T> {
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

    public PythonList(OpaquePythonReference pythonListOpaqueReference, Number id, Map<Number, Object> idMap) {
        this.pythonListOpaqueReference = pythonListOpaqueReference;
        this.idMap = idMap;
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
    public int size() {
        return getPythonListLength.apply(pythonListOpaqueReference);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof OpaquePythonReference) {
            return doesPythonListContainItem.apply(pythonListOpaqueReference, o);
        } else if (o instanceof PythonObject) {
            return doesPythonListContainItem.apply(pythonListOpaqueReference, ((PythonObject) o).get__optapy_Id());
        } else {
            return doesPythonListContainItem.apply(pythonListOpaqueReference, o);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = 0;
            final int length = size();

            @Override
            public boolean hasNext() {
                return index < length;
            }

            @Override
            public T next() {
                T out = get(index);
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
    public <T1> T1[] toArray(T1[] t1s) {
        int length = size();
        if (t1s.length == length) {
            for (int i = 0; i < length; i++) {
                t1s[i] = (T1) get(i);
            }
            return t1s;
        } else {
            return (T1[]) toArray();
        }
    }

    @Override
    public boolean add(T t) {
        if (t instanceof OpaquePythonReference) {
            addItemToPythonList.apply(pythonListOpaqueReference, t);
        } else if (t instanceof PythonObject) {
            addItemToPythonList.apply(pythonListOpaqueReference, ((PythonObject) t).get__optapy_Id());
        } else {
            addItemToPythonList.apply(pythonListOpaqueReference, t);
        }
        return true;
    }

    @Override
    public boolean remove(Object t) {
        if (t instanceof OpaquePythonReference) {
            return removeItemFromPythonList.apply(pythonListOpaqueReference, t);
        } else if (t instanceof PythonObject) {
            return removeItemFromPythonList.apply(pythonListOpaqueReference, ((PythonObject) t).get__optapy_Id());
        } else {
            return removeItemFromPythonList.apply(pythonListOpaqueReference, t);
        }
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        for (Object o : collection) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        for (Object o : collection) {
            add((T) o);
        }
        return true;
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> collection) {
        int index = i;
        for (Object o : collection) {
            add(index, (T) o);
            index++;
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean anyRemoved = false;
        for (Object o : collection) {
            anyRemoved |= remove(o);
        }
        return anyRemoved;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
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
        clearPythonList.apply(pythonListOpaqueReference);
    }

    @Override
    public T get(int i) {
        return (T) getItemAtIndexInPythonList.apply(pythonListOpaqueReference, i);
    }

    @Override
    public T set(int i, T t) {
        T old = get(i);
        if (t instanceof OpaquePythonReference) {
            setItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, t);
        } else if (t instanceof PythonObject) {
            setItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, ((PythonObject) t).get__optapy_Id());
        } else {
            setItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, t);
        }
        return old;
    }

    @Override
    public void add(int i, T t) {
        if (t instanceof OpaquePythonReference) {
            addItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, t);
        } else if (t instanceof PythonObject) {
            addItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, ((PythonObject) t).get__optapy_Id());
        } else {
            addItemAtIndexInPythonList.apply(pythonListOpaqueReference, i, t);
        }
    }

    @Override
    public T remove(int i) {
        T out = get(i);
        removeItemAtIndexFromPythonList.apply(pythonListOpaqueReference, i);
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
    public ListIterator<T> listIterator() {
        return new PythonListIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int start) {
        return new PythonListIterator(start);
    }

    @Override
    public List<T> subList(int start, int end) {
        return new PythonList<>(slicePythonList.apply(pythonListOpaqueReference, start, end), null, null);
    }

    @Override
    public String toString() {
        return PythonWrapperGenerator.getPythonObjectString(pythonListOpaqueReference);
    }

    public class PythonListIterator implements ListIterator<T> {

        int index;

        public PythonListIterator(int start) {
            index = start;
        }

        @Override
        public boolean hasNext() {
            return index < size();
        }

        @Override
        public T next() {
            T out = get(index);
            index++;
            return out;
        }

        @Override
        public boolean hasPrevious() {
            return index > 0;
        }

        @Override
        public T previous() {
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
        public void set(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException();
        }
    }
}
