package org.optaplanner.optapy.translator;

public class PythonBytecodeInstruction {
    /**
     * The {@link OpCode} for this operation
     */
    public OpCode opcode;

    /**
     * Human readable name for operation
     */
    public String opname;

    /**
     * Numeric argument to operation (if any), otherwise null
     */
    public Integer arg;

    /**
     * Resolved arg value (if known), otherwise same as arg
     */
    public Object argval;

    /**
     * Human readable description of operation argument
     */
    public String argrepr;

    /**
     * Start index of operation within bytecode sequence
     */
    public int offset;

    /**
     * Line started by this opcode (if any), otherwise None
     */
    public Integer startsLine;

    /**
     * True if other code jumps to here, otherwise False
     */
    public boolean isJumpTarget;

    @Override
    public String toString() {
        return "[" + offset + "] " + opcode.name() + " (" + arg + ")";
    }

    public enum OpCode {
        // *****************************
        // General instructions
        // *****************************
        /**
         * Do nothing code. Used as a placeholder by the bytecode optimizer.
         */
        NOP(0),

        /**
         * Removes the top-of-stack (TOS) item.
         */
        POP_TOP(1),

        /**
         * Swaps the two top-most stack items.
         */
        ROT_TWO(2),

        /**
         * Lifts second and third stack item one position up, moves top down to position three.
         */
        ROT_THREE(3),

        /**
         * Lifts second, third and fourth stack items one position up, moves top down to position four.
         */
        ROT_FOUR(4),

        /**
         * Duplicates the reference on top of the stack.
         */
        DUP_TOP(0),

        /**
         * Duplicates the two references on top of the stack, leaving them in the same order.
         */
        DUP_TOP_TWO(0),

        // *****************************
        // Unary operations
        //
        // Unary operations take the top of the stack, apply the operation, and push the result back on the stack.
        // *****************************
        /**
         * Implements TOS = +TOS.
         */
        UNARY_POSITIVE(1),

        /**
         * Implements TOS = -TOS.
         */
        UNARY_NEGATIVE(1),

        /**
         * Implements TOS = not TOS.
         */
        UNARY_NOT(1),

        /**
         * Implements TOS = ~TOS.
         */
        UNARY_INVERT(1),

        /**
         * Implements TOS = iter(TOS).
         */
        GET_ITER(1),

        /**
         * If TOS is a generator iterator or coroutine object it is left as is. Otherwise, implements TOS = iter(TOS).
         */
        GET_YIELD_FROM_ITER(1),

        // *****************************
        // Binary operations
        //
        // Binary operations remove the top of the stack (TOS) and the second top-most stack item (TOS1) from the stack.
        // They perform the operation, and put the result back on the stack.
        // *****************************
        /**
         * Implements TOS = TOS1 ** TOS.
         */
        BINARY_POWER(2),

        /**
         * Implements TOS = TOS1 * TOS.
         */
        BINARY_MULTIPLY(2),

        /**
         * Implements TOS = TOS1 @ TOS.
         */
        BINARY_MATRIX_MULTIPLY(2),

        /**
         * Implements TOS = TOS1 // TOS.
         */
        BINARY_FLOOR_DIVIDE(2),

        /**
         * Implements TOS = TOS1 / TOS.
         */
        BINARY_TRUE_DIVIDE(2),

        /**
         * Implements TOS = TOS1 % TOS.
         */
        BINARY_MODULO(2),

        /**
         * Implements TOS = TOS1 + TOS.
         */
        BINARY_ADD(2),

        /**
         * Implements TOS = TOS1 - TOS.
         */
        BINARY_SUBTRACT(2),

        /**
         * Implements TOS = TOS1[TOS].
         */
        BINARY_SUBSCR(2),

        /**
         * Implements TOS = TOS1 << TOS.
         */
        BINARY_LSHIFT(2),

        /**
         * Implements TOS = TOS1 >> TOS.
         */
        BINARY_RSHIFT(2),

        /**
         * Implements TOS = TOS1 & TOS.
         */
        BINARY_AND(2),

        /**
         * Implements TOS = TOS1 ^ TOS.
         */
        BINARY_XOR(2),

        /**
         * Implements TOS = TOS1 | TOS.
         */
        BINARY_OR(2),

        // *****************************
        // In-place operations
        //
        // In-place operations are like binary operations, in that they remove TOS and TOS1,
        // and push the result back on the stack, but the operation is done in-place when TOS1 supports it,
        // and the resulting TOS may be (but does not have to be) the original TOS1.
        // *****************************

        /**
         * Implements in-place TOS = TOS1 ** TOS.
         */
        INPLACE_POWER(2),

        /**
         * Implements in-place TOS = TOS1 * TOS.
         */
        INPLACE_MULTIPLY(2),

        /**
         * Implements in-place TOS = TOS1 @ TOS.
         */
        INPLACE_MATRIX_MULTIPLY(2),

        /**
         * Implements in-place TOS = TOS1 // TOS.
         */
        INPLACE_FLOOR_DIVIDE(2),

        /**
         * Implements in-place TOS = TOS1 / TOS.
         */
        INPLACE_TRUE_DIVIDE(2),

        /**
         * Implements in-place TOS = TOS1 % TOS.
         */
        INPLACE_MODULO(2),

        /**
         * Implements in-place TOS = TOS1 + TOS.
         */
        INPLACE_ADD(2),

        /**
         * Implements in-place TOS = TOS1 - TOS.
         */
        INPLACE_SUBTRACT(2),

        /**
         * Implements in-place TOS = TOS1 << TOS.
         */
        INPLACE_LSHIFT(2),

        /**
         * Implements in-place TOS = TOS1 >> TOS.
         */
        INPLACE_RSHIFT(2),

        /**
         * Implements in-place TOS = TOS1 & TOS.
         */
        INPLACE_AND(2),

        /**
         * Implements in-place TOS = TOS1 ^ TOS.
         */
        INPLACE_XOR(2),

        /**
         * Implements in-place TOS = TOS1 | TOS.
         */
        INPLACE_OR(2),

        /**
         * Implements TOS1[TOS] = TOS2.
         */
        STORE_SUBSCR(3),

        /**
         * Implements del TOS1[TOS].
         */
        DEL_SUBSCR(2),

        // *****************************
        // Coroutine opcodes
        // *****************************

        /**
         * Implements TOS = get_awaitable(TOS), where get_awaitable(o) returns o if o is a coroutine object or a generator
         * object with the CO_ITERABLE_COROUTINE flag, or resolves o.__await__.
         */
        GET_AWAITABLE(1),

        /**
         * Implements TOS = TOS.__aiter__().
         */
        GET_AITER(1),

        /**
         * Implements PUSH(get_awaitable(TOS.__anext__())). See {@link GET_AWAITABLE} for details about get_awaitable
         */
        GET_ANEXT(0),

        /**
         * Terminates an async for loop. Handles an exception raised when awaiting a next item. If TOS is StopAsyncIteration
         * pop 7 values from the stack and restore the exception state using the second three of them. Otherwise re-raise the
         * exception using the three values from the stack. An exception handler block is removed from the block stack.
         */
        END_ASYNC_FOR(0),

        /**
         * Resolves __aenter__ and __aexit__ from the object on top of the stack.
         * Pushes __aexit__ and result of __aenter__() to the stack.
         */
        BEFORE_ASYNC_WITH(0),

        /**
         * Creates a new frame object.
         */
        SETUP_ASYNC_WITH(0),

        // *****************************
        // Miscellaneous opcodes
        // *****************************

        /**
         * Implements the expression statement for the interactive mode. TOS is removed from the stack and printed.
         * In non-interactive mode, an expression statement is terminated with POP_TOP.
         */
        PRINT_EXPR(1),

        /**
         * Calls set.add(TOS1[-i], TOS). Used to implement set comprehensions.
         *
         * The added value is popped off, the container object remains on the stack so that it is available for further
         * iterations of the loop.
         */
        SET_ADD(1),

        /**
         * Calls list.append(TOS1[-i], TOS). Used to implement list comprehensions.
         *
         * The added value is popped off, the container object remains on the stack so that it is available for further
         * iterations of the loop.
         */
        LIST_APPEND(1),

        /**
         * Calls dict.__setitem__(TOS1[-i], TOS1, TOS). Used to implement dict comprehensions.
         *
         * The key/value pair is popped off, the container object remains on the stack so that it is available for further
         * iterations of the loop.
         */
        MAP_ADD(1),

        /**
         * Returns with TOS to the caller of the function.
         */
        RETURN_VALUE(1),

        /**
         * Pops TOS and yields it from a generator.
         */
        YIELD_VALUE(1),

        /**
         * Pops TOS and delegates to it as a subiterator from a generator.
         */
        YIELD_FROM(1),

        /**
         * Checks whether __annotations__ is defined in locals(), if not it is set up to an empty dict. This opcode is only
         * emitted if a class or module body contains variable annotations statically.
         */
        SETUP_ANNOTATIONS(0),

        /**
         * Loads all symbols not starting with '_' directly from the module TOS to the local namespace. The module is popped
         * after
         * loading all names. This opcode implements from module import *.
         */
        IMPORT_STAR(1),

        /**
         * Removes one block from the block stack. Per frame, there is a stack of blocks,
         * denoting try statements, and such.
         */
        POP_BLOCK(0),

        /**
         * Removes one block from the block stack. The popped block must be an exception handler block, as implicitly created
         * when entering an except handler. In addition to popping extraneous values from the frame stack, the last three
         * popped values are used to restore the exception state.
         */
        POP_EXCEPT(0),

        /**
         * Re-raises the exception currently on top of the stack.
         */
        RERAISE(0),

        /**
         * Calls the function in position 7 on the stack with the top three items on the stack as arguments. Used to implement
         * the call context_manager.__exit__(*exc_info()) when an exception has occurred in a with statement.
         */
        WITH_EXCEPT_START(0),

        /**
         * Pushes AssertionError onto the stack. Used by the assert statement.
         */
        LOAD_ASSERTION_ERROR(0),

        /**
         * Pushes builtins.__build_class__() onto the stack.
         * It is later called by CALL_FUNCTION to construct a class.
         */
        LOAD_BUILD_CLASS(0),

        /**
         * This opcode performs several operations before a with block starts. First, it loads __exit__() from the
         * context manager and pushes it onto the stack for later use by WITH_CLEANUP_START. Then, __enter__() is called,
         * and a finally block pointing to delta is pushed. Finally, the result of calling the __enter__() method is pushed
         * onto the stack. The next opcode will either ignore it (POP_TOP), or store it
         * in (a) variable(s) (STORE_FAST, STORE_NAME, or UNPACK_SEQUENCE).
         */
        SETUP_WITH(0),

        /**
         * Implements name = TOS. namei is the index of name in the attribute co_names of the code object.
         * The compiler tries to use STORE_FAST or STORE_GLOBAL if possible.
         */
        STORE_NAME(0),

        /**
         * Implements del name, where namei is the index into co_names attribute of the code object.
         */
        DELETE_NAME(0),

        /**
         * Unpacks TOS into count individual values, which are put onto the stack right-to-left.
         */
        UNPACK_SEQUENCE(0),

        /**
         * Implements assignment with a starred target:
         * Unpacks an iterable in TOS into individual values, where the total number of values can be smaller than the
         * number of items in the iterable: one of the new values will be a list of all leftover items.
         *
         * The low byte of counts is the number of values before the list value, the high byte of counts the number of
         * values after it. The resulting values are put onto the stack right-to-left.
         */
        UNPACK_EX(0),

        /**
         * Implements TOS.name = TOS1, where namei is the index of name in co_names.
         */
        STORE_ATTR(0),

        /**
         * Implements del TOS.name, using namei as index into co_names.
         */
        DELETE_ATTR(0),

        /**
         * Works as {@link STORE_NAME}, but stores the name as a global.
         */
        STORE_GLOBAL(0),

        /**
         * Works as {@link DELETE_NAME}, but deletes a global name.
         */
        DELETE_GLOBAL(0),

        /**
         * Pushes co_consts[consti] onto the stack.
         */
        LOAD_CONST(0),

        /**
         * Pushes the value associated with co_names[namei] onto the stack.
         */
        LOAD_NAME(0),

        /**
         * Creates a tuple consuming count items from the stack, and pushes the resulting tuple onto the stack.
         */
        BUILD_TUPLE(0),

        /**
         * Works as {@link BUILD_TUPLE}, but creates a list.
         */
        BUILD_LIST(0),

        /**
         * Works as {@link BUILD_TUPLE}, but creates a set.
         */
        BUILD_SET(0),

        /**
         * Pushes a new dictionary object onto the stack.
         * Pops 2 * count items so that the dictionary holds count entries: {..., TOS3: TOS2, TOS1: TOS}.
         */
        BUILD_MAP(0),

        /**
         * The version of {@link BUILD_MAP} specialized for constant keys. Pops the top element on the stack which contains a
         * tuple of keys,
         * then starting from TOS1, pops count values to form values in the built dictionary.
         */
        BUILD_CONST_KEY_MAP(0),

        /**
         * Concatenates count strings from the stack and pushes the resulting string onto the stack.
         */
        BUILD_STRING(0),

        /**
         * Pops a list from the stack and pushes a tuple containing the same values.
         */
        LIST_TO_TUPLE(1),

        /**
         * Calls list.extend(TOS1[-i], TOS). Used to build lists.
         */
        LIST_EXTEND(0),

        /**
         * Calls set.update(TOS1[-i], TOS). Used to build sets.
         */
        SET_UPDATE(0),

        /**
         * Calls dict.update(TOS1[-i], TOS). Used to build dicts.
         */
        DICT_UPDATE(0),

        /**
         * Like {@link DICT_UPDATE} but raises an exception for duplicate keys.
         */
        DICT_MERGE(0),

        /**
         * Replaces TOS with getattr(TOS, co_names[namei]).
         */
        LOAD_ATTR(1),

        /**
         * Performs a Boolean operation. The operation name can be found in cmp_op[opname].
         */
        COMPARE_OP(0),

        /**
         * Performs is comparison, or is not if invert is 1.
         */
        IS_OP(0),

        /**
         * Performs in comparison, or not in if invert is 1.
         */
        CONTAINS_OP(0),

        /**
         * Imports the module co_names[namei].
         * TOS and TOS1 are popped and provide the fromlist and level arguments of __import__().
         * The module object is pushed onto the stack. The current namespace is not affected: for a proper import statement,
         * a subsequent STORE_FAST instruction modifies the namespace.
         */
        IMPORT_NAME(0),

        /**
         * Loads the attribute co_names[namei] from the module found in TOS. The resulting object is pushed onto the stack,
         * to be subsequently stored by a STORE_FAST instruction.
         */
        IMPORT_FROM(0),

        /**
         * Increments bytecode counter by delta.
         */
        JUMP_FORWARD(0),

        /**
         * If TOS is true, sets the bytecode counter to target. TOS is popped.
         */
        POP_JUMP_IF_TRUE(1),

        /**
         * If TOS is false, sets the bytecode counter to target. TOS is popped.
         */
        POP_JUMP_IF_FALSE(1),

        /**
         * Tests whether the second value on the stack is an exception matching TOS, and jumps if it is not.
         * Pops two values from the stack.
         */
        JUMP_IF_NOT_EXC_MATCH(2),

        /**
         * If TOS is true, sets the bytecode counter to target and leaves TOS on the stack. Otherwise (TOS is false), TOS is
         * popped.
         */
        JUMP_IF_TRUE_OR_POP(0),

        /**
         * If TOS is false, sets the bytecode counter to target and leaves TOS on the stack. Otherwise (TOS is true), TOS is
         * popped.
         */
        JUMP_IF_FALSE_OR_POP(0),

        /**
         * Set bytecode counter to target.
         */
        JUMP_ABSOLUTE(0),

        /**
         * TOS is an iterator. Call its __next__() method. If this yields a new value, push it on the stack (leaving the
         * iterator below it).
         * If the iterator indicates it is exhausted, TOS is popped, and the byte code counter is incremented by delta.
         */
        FOR_ITER(0),

        /**
         * Loads the global named co_names[namei] onto the stack.
         */
        LOAD_GLOBAL(0),

        /**
         * Pushes a try block from a try-finally or try-except clause onto the block stack.
         * delta points to the finally block or the first except block.
         */
        SETUP_FINALLY(0),

        /**
         * Pushes a reference to the local co_varnames[var_num] onto the stack.
         */
        LOAD_FAST(0),

        /**
         * Stores TOS into the local co_varnames[var_num].
         */
        STORE_FAST(0),

        /**
         * Deletes local co_varnames[var_num].
         */
        DELETE_FAST(0),

        /**
         * Pushes a reference to the cell contained in slot i of the cell and free variable storage.
         * The name of the variable is co_cellvars[i] if i is less than the length of co_cellvars.
         * Otherwise it is co_freevars[i - len(co_cellvars)].
         */
        LOAD_CLOSURE(0),

        /**
         * Loads the cell contained in slot i of the cell and free variable storage.
         * Pushes a reference to the object the cell contains on the stack.
         */
        LOAD_DEREF(0),

        /**
         * Much like {@link LOAD_DEREF} but first checks the locals dictionary before consulting the cell.
         * This is used for loading free variables in class bodies.
         */
        LOAD_CLASSDEREF(0),

        /**
         * Stores TOS into the cell contained in slot i of the cell and free variable storage.
         */
        STORE_DEREF(0),

        /**
         * Empties the cell contained in slot i of the cell and free variable storage. Used by the del statement.
         */
        DELETE_DEREF(0),

        /**
         * Raises an exception using one of the 3 forms of the raise statement, depending on the value of argc:
         *
         * 0: raise (re-raise previous exception)
         * 1: raise TOS (raise exception instance or type at TOS)
         * 2: raise TOS1 from TOS (raise exception instance or type at TOS1 with __cause__ set to TOS)
         */
        RAISE_VARARGS(0),

        /**
         * Calls a callable object with positional arguments. argc indicates the number of positional arguments.
         * The top of the stack contains positional arguments, with the right-most argument on top.
         * Below the arguments is a callable object to call. CALL_FUNCTION pops all arguments and the callable object off the
         * stack,
         * calls the callable object with those arguments, and pushes the return value returned by the callable object.
         */
        CALL_FUNCTION(0),

        /**
         * Calls a callable object with positional (if any) and keyword arguments. argc indicates the total number of positional
         * and keyword arguments. The top element on the stack contains a tuple with the names of the keyword arguments,
         * which must be strings. Below that are the values for the keyword arguments, in the order corresponding to the tuple.
         * Below that are positional arguments, with the right-most parameter on top. Below the arguments is a callable object
         * to call. CALL_FUNCTION_KW pops all arguments and the callable object off the stack, calls the callable object with
         * those arguments, and pushes the return value returned by the callable object.
         */
        CALL_FUNCTION_KW(0),

        /**
         * Calls a callable object with variable set of positional and keyword arguments. If the lowest bit of flags is set,
         * the top of the stack contains a mapping object containing additional keyword arguments. Before the callable is
         * called,
         * the mapping object and iterable object are each “unpacked” and their contents passed in as keyword and positional
         * arguments respectively. CALL_FUNCTION_EX pops all arguments and the callable object off the stack, calls the callable
         * object with those arguments, and pushes the return value returned by the callable object.
         */
        CALL_FUNCTION_EX(0),

        /**
         * Loads a method named co_names[namei] from the TOS object. TOS is popped.
         * This bytecode distinguishes two cases: if TOS has a method with the correct name, the bytecode pushes the unbound
         * method and TOS.
         * TOS will be used as the first argument (self) by CALL_METHOD when calling the unbound method.
         * Otherwise, NULL and the object return by the attribute lookup are pushed.
         */
        LOAD_METHOD(1),

        /**
         * Calls a method. argc is the number of positional arguments.
         * Keyword arguments are not supported. This opcode is designed to be used with LOAD_METHOD.
         * Positional arguments are on top of the stack. Below them, the two items described in LOAD_METHOD are on the stack
         * (either self and an unbound method object or NULL and an arbitrary callable). All of them are popped and the return
         * value is pushed.
         */
        CALL_METHOD(0),

        /**
         * Pushes a new function object on the stack. From bottom to top, the consumed stack must consist of values if the
         * argument carries a specified flag value
         *
         * 0x01 a tuple of default values for positional-only and positional-or-keyword parameters in positional order
         * 0x02 a dictionary of keyword-only parameters’ default values
         * 0x04 an annotation dictionary
         * 0x08 a tuple containing cells for free variables, making a closure
         *
         * the code associated with the function (at TOS1)
         * the qualified name of the function (at TOS)
         */
        MAKE_FUNCTION(0),

        /**
         * Pushes a slice object on the stack. argc must be 2 or 3. If it is 2, slice(TOS1, TOS) is pushed;
         * if it is 3, slice(TOS2, TOS1, TOS) is pushed. See the slice() built-in function for more information.
         */
        BUILD_SLICE(0),

        /**
         * Prefixes any opcode which has an argument too big to fit into the default one byte. ext holds an additional byte
         * which act as higher bits in the argument.
         * For each opcode, at most three prefixal EXTENDED_ARG are allowed, forming an argument from two-byte to four-byte.
         */
        EXTENDED_ARG(0),

        /**
         * Used for implementing formatted literal strings (f-strings). Pops an optional fmt_spec from the stack, then a
         * required value.
         * flags is interpreted as follows:
         *
         * (flags & 0x03) == 0x00: value is formatted as-is.
         *
         * (flags & 0x03) == 0x01: call str() on value before formatting it.
         *
         * (flags & 0x03) == 0x02: call repr() on value before formatting it.
         *
         * (flags & 0x03) == 0x03: call ascii() on value before formatting it.
         *
         * (flags & 0x04) == 0x04: pop fmt_spec from the stack and use it, else use an empty fmt_spec.
         *
         * Formatting is performed using PyObject_Format(). The result is pushed on the stack.
         */
        FORMAT_VALUE(0);

        int args;

        OpCode(int args) {
            this.args = args;
        }
    }
}
