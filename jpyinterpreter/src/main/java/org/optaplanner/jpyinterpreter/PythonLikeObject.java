package org.optaplanner.jpyinterpreter;

import java.util.Objects;

import org.optaplanner.jpyinterpreter.builtins.TernaryDunderBuiltin;
import org.optaplanner.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import org.optaplanner.jpyinterpreter.types.PythonLikeFunction;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;
import org.optaplanner.jpyinterpreter.types.PythonNone;
import org.optaplanner.jpyinterpreter.types.PythonString;
import org.optaplanner.jpyinterpreter.types.errors.AttributeError;
import org.optaplanner.jpyinterpreter.types.errors.TypeError;
import org.optaplanner.jpyinterpreter.types.numeric.PythonBoolean;
import org.optaplanner.jpyinterpreter.types.numeric.PythonInteger;

/**
 * Represents an Object that can be interacted with like a Python Object.
 * A PythonLikeObject can refer to a Java Object, a CPython Object, or
 * be an Object constructed by the Java Python Interpreter.
 */
public interface PythonLikeObject {

    /**
     * Gets an attribute by name.
     *
     * @param attributeName Name of the attribute to get
     * @return The attribute of the object that corresponds with attributeName
     * @throws AttributeError if the attribute does not exist
     */
    PythonLikeObject __getAttributeOrNull(String attributeName);

    /**
     * Gets an attribute by name.
     *
     * @param attributeName Name of the attribute to get
     * @return The attribute of the object that corresponds with attributeName
     * @throws AttributeError if the attribute does not exist
     */
    default PythonLikeObject __getAttributeOrError(String attributeName) {
        PythonLikeObject out = this.__getAttributeOrNull(attributeName);
        if (out == null) {
            throw new AttributeError("object '" + this + "' does not have attribute '" + attributeName + "'");
        }
        return out;
    }

    /**
     * Sets an attribute by name.
     *
     * @param attributeName Name of the attribute to set
     * @param value Value to set the attribute to
     */
    void __setAttribute(String attributeName, PythonLikeObject value);

    /**
     * Delete an attribute by name.
     *
     * @param attributeName Name of the attribute to delete
     */
    void __deleteAttribute(String attributeName);

    /**
     * Returns the type describing the object
     *
     * @return the type describing the object
     */
    PythonLikeType __getType();

    /**
     * Return a generic version of {@link PythonLikeObject#__getType()}. This is used in bytecode
     * generation and not at runtime. For example, for a list of integers, this return
     * list[int], while getType returns list. Both methods are needed so type([1,2,3]) is type(['a', 'b', 'c'])
     * return True.
     *
     * @return the generic version of this object's type. Must not be used in identity checks.
     */
    default PythonLikeType __getGenericType() {
        return __getType();
    }

    default PythonLikeObject $method$__getattribute__(PythonString pythonName) {
        String name = pythonName.value;
        PythonLikeObject objectResult = __getAttributeOrNull(name);
        if (objectResult != null) {
            return objectResult;
        }

        PythonLikeType type = __getType();
        PythonLikeObject typeResult = type.__getAttributeOrNull(name);
        if (typeResult != null) {
            PythonLikeObject maybeDescriptor = typeResult.__getAttributeOrNull(PythonTernaryOperators.GET.dunderMethod);
            if (maybeDescriptor == null) {
                maybeDescriptor = typeResult.__getType().__getAttributeOrNull(PythonTernaryOperators.GET.dunderMethod);
            }

            if (maybeDescriptor != null) {
                if (!(maybeDescriptor instanceof PythonLikeFunction)) {
                    throw new UnsupportedOperationException("'" + maybeDescriptor.__getType() + "' is not callable");
                }
                return TernaryDunderBuiltin.GET_DESCRIPTOR.invoke(typeResult, this, type);
            }
            return typeResult;
        }

        throw new AttributeError("object '" + this + "' does not have attribute '" + name + "'");
    }

    default PythonLikeObject $method$__setattr__(PythonString pythonName, PythonLikeObject value) {
        String name = pythonName.value;
        __setAttribute(name, value);
        return PythonNone.INSTANCE;
    }

    default PythonLikeObject $method$__delattr__(PythonString pythonName) {
        String name = pythonName.value;
        __deleteAttribute(name);
        return PythonNone.INSTANCE;
    }

    default PythonLikeObject $method$__eq__(PythonLikeObject other) {
        return PythonBoolean.valueOf(Objects.equals(this, other));
    }

    default PythonLikeObject $method$__ne__(PythonLikeObject other) {
        return PythonBoolean.valueOf(!Objects.equals(this, other));
    }

    default PythonLikeObject $method$__str__() {
        return PythonString.valueOf(this.toString());
    }

    default PythonLikeObject $method$__repr__() {
        String position;
        if (this instanceof CPythonBackedPythonLikeObject) {
            PythonInteger id = ((CPythonBackedPythonLikeObject) this).$cpythonId;
            if (id != null) {
                position = id.toString();
            } else {
                position = String.valueOf(System.identityHashCode(this));
            }
        } else {
            position = String.valueOf(System.identityHashCode(this));
        }
        return PythonString.valueOf("<" + __getType().getTypeName() + " object at " + position + ">");
    }

    default PythonLikeObject $method$__format__() {
        return $method$__format__(PythonNone.INSTANCE);
    }

    default PythonLikeObject $method$__format__(PythonLikeObject formatString) {
        return $method$__str__().$method$__format__(formatString);
    }

    default PythonLikeObject $method$__hash__() {
        throw new TypeError("unhashable type: '" + __getType().getTypeName() + "'");
    }
}
