package org.optaplanner.python.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.optaplanner.python.translator.types.PythonLikeType;

public class StackMetadata {
    public LocalVariableHelper localVariableHelper;
    public List<PythonLikeType> stackTypes;

    public List<PythonLikeType> localVariableTypes;

    public List<PythonLikeType> cellVariableTypes;

    /**
     * Returns the type at the given stack index (stack index is how many
     * elements below TOS (i.e. 0 is TOS, 1 is TOS1)).
     *
     * @param index The stack index (how many elements below TOS)
     * @return The type at the given stack index
     */
    public PythonLikeType getTypeAtStackIndex(int index) {
        return stackTypes.get(stackTypes.size() - index - 1);
    }

    /**
     * Returns the type for the local variable in slot {@code index}
     *
     * @param index The slot
     * @return The type for the local variable in the given slot
     */
    public PythonLikeType getLocalVariableType(int index) {
        return localVariableTypes.get(index);
    }

    /**
     * Returns the type for the cell variable in slot {@code index}
     *
     * @param index The slot
     * @return The type for the cell variable in the given slot
     */
    public PythonLikeType getCellVariableType(int index) {
        return cellVariableTypes.get(index);
    }

    public PythonLikeType getTOSType() {
        return getTypeAtStackIndex(0);
    }

    public StackMetadata copy() {
        StackMetadata out = new StackMetadata();
        out.localVariableHelper = localVariableHelper;
        out.stackTypes = new ArrayList<>(stackTypes);
        out.localVariableTypes = new ArrayList<>(localVariableTypes);
        out.cellVariableTypes = new ArrayList<>(cellVariableTypes);
        return out;
    }

    public StackMetadata unifyWith(StackMetadata other) {
        StackMetadata out = copy();
        if (out.stackTypes.size() != other.stackTypes.size() ||
                out.localVariableTypes.size() != other.localVariableTypes.size() ||
                out.cellVariableTypes.size() != other.cellVariableTypes.size()) {
            throw new IllegalArgumentException("Impossible State: Bytecode stack metadata size does not match when " +
                    "unifying (" + this + ") with (" + other + ")");
        }

        for (int i = 0; i < out.stackTypes.size(); i++) {
            out.stackTypes.set(i, unifyTypes(stackTypes.get(i), other.stackTypes.get(i)));
        }

        for (int i = 0; i < out.localVariableTypes.size(); i++) {
            out.localVariableTypes.set(i, unifyTypes(localVariableTypes.get(i), other.localVariableTypes.get(i)));
        }

        for (int i = 0; i < out.cellVariableTypes.size(); i++) {
            out.cellVariableTypes.set(i, unifyTypes(cellVariableTypes.get(i), other.cellVariableTypes.get(i)));
        }

        return out;
    }

    private static PythonLikeType unifyTypes(PythonLikeType a, PythonLikeType b) {
        if (Objects.equals(a, b)) {
            return a;
        }

        if (a == null) { // a or b are null when they are deleted/are not set yet
            return b; // TODO: Optional type?
        }

        if (b == null) {
            return a;
        }

        return a.unifyWith(b);
    }

    /**
     * Return a new StackMetadata with {@code type} added as the new
     * TOS element.
     *
     * @param type The type to push to TOS
     */
    public StackMetadata push(PythonLikeType type) {
        StackMetadata out = copy();
        out.stackTypes.add(type);
        return out;
    }

    /**
     * Return a new StackMetadata with {@code types} added as the new
     * elements. The last element of {@code types} is TOS.
     *
     * @param types The types to push to TOS
     */
    public StackMetadata push(PythonLikeType... types) {
        StackMetadata out = copy();
        out.stackTypes.addAll(Arrays.asList(types));
        return out;
    }

    /**
     * Return a new StackMetadata with {@code types} as the stack;
     * The original stack is cleared.
     *
     * @param types The stack types.
     */
    public StackMetadata stack(PythonLikeType... types) {
        StackMetadata out = copy();
        out.stackTypes.clear();
        out.stackTypes.addAll(Arrays.asList(types));
        return out;
    }

    /**
     * Return a new StackMetadata with TOS popped
     */
    public StackMetadata pop() {
        StackMetadata out = copy();
        out.stackTypes.remove(stackTypes.size() - 1);
        return out;
    }

    /**
     * Return a new StackMetadata with the top {@code count} items popped.
     */
    public StackMetadata pop(int count) {
        StackMetadata out = copy();
        out.stackTypes.subList(stackTypes.size() - count, stackTypes.size()).clear();
        return out;
    }

    /**
     * Return a new StackMetadata with the local variable in slot {@code index} type set to
     * {@code type}.
     */
    public StackMetadata setLocalVariableType(int index, PythonLikeType type) {
        StackMetadata out = copy();
        out.localVariableTypes.set(index, type);
        return out;
    }

    /**
     * Return a new StackMetadata with the given local types. Throws {@link IllegalArgumentException} if
     * types.length != localVariableTypes.size().
     */
    public StackMetadata locals(PythonLikeType... types) {
        if (types.length != localVariableTypes.size()) {
            throw new IllegalArgumentException(
                    "Length mismatch: expected an array with {" + localVariableTypes.size() + "} elements but got " +
                            "{" + Arrays.toString(types) + "}");
        }
        StackMetadata out = copy();
        for (int i = 0; i < types.length; i++) {
            out.localVariableTypes.set(i, types[i]);
        }
        return out;
    }

    /**
     * Return a new StackMetadata with the cell variable in slot {@code index} type set to
     * {@code type}.
     */
    public StackMetadata setCellVariableType(int index, PythonLikeType type) {
        StackMetadata out = copy();
        out.cellVariableTypes.set(index, type);
        return out;
    }

    public String toString() {
        return "StackMetadata { stack: " + stackTypes.toString() + "; locals: " + localVariableTypes.toString() +
                "; cells: " + cellVariableTypes.toString() + "; }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StackMetadata that = (StackMetadata) o;
        return stackTypes.equals(that.stackTypes) && localVariableTypes.equals(that.localVariableTypes)
                && cellVariableTypes.equals(that.cellVariableTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackTypes, localVariableTypes, cellVariableTypes);
    }
}
