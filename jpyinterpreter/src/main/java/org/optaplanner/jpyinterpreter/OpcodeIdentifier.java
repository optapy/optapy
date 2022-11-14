package org.optaplanner.jpyinterpreter;

public enum OpcodeIdentifier {
    // *****************************
    // General instructions
    // *****************************
    /**
     * Do nothing code. Used as a placeholder by the bytecode optimizer.
     */
    NOP,

    /**
     * Another do nothing code. Performs internal tracing, debugging and optimization checks in CPython
     */
    RESUME,

    /**
     * A no-op code used by CPython to hold arbitrary data for JIT.
     */
    CACHE,

    /**
     * Removes the top-of-stack (TOS) item.
     */
    POP_TOP,

    /**
     * Swaps the two top-most stack items.
     */
    ROT_TWO,

    /**
     * Lifts second and third stack item one position up, moves top down to position three.
     */
    ROT_THREE,

    /**
     * Lifts second, third and fourth stack items one position up, moves top down to position four.
     */
    ROT_FOUR,

    /**
     * Duplicates the reference on top of the stack.
     */
    DUP_TOP,

    /**
     * Duplicates the two references on top of the stack, leaving them in the same order.
     */
    DUP_TOP_TWO,

    /**
     * Push the i-th item to the top of the stack. The item is not removed from its original location.
     * Uses 1-based indexing (TOS is 1, TOS1 is 2, ...) instead of 0-based indexing.
     */
    COPY,

    /**
     * Swap TOS with the item at position i. Uses 1-based indexing (TOS is 1, TOS1 is 2, ...) instead of 0-based indexing.
     */
    SWAP,

    // *****************************
    // Unary operations
    //
    // Unary operations take the top of the stack, apply the operation, and push the result back on the stack.
    // *****************************
    /**
     * Implements TOS = +TOS.
     */
    UNARY_POSITIVE,

    /**
     * Implements TOS = -TOS.
     */
    UNARY_NEGATIVE,

    /**
     * Implements TOS = not TOS.
     */
    UNARY_NOT,

    /**
     * Implements TOS = ~TOS.
     */
    UNARY_INVERT,

    /**
     * Implements TOS = iter(TOS).
     */
    GET_ITER,

    /**
     * If TOS is a generator iterator or coroutine object it is left as is. Otherwise, implements TOS = iter(TOS).
     */
    GET_YIELD_FROM_ITER,

    // *****************************
    // Binary operations
    //
    // Binary operations remove the top of the stack (TOS) and the second top-most stack item (TOS1) from the stack.
    // They perform the operation, and put the result back on the stack.
    // *****************************

    /**
     * Implements any binary op. Its argument represent the arg to implement, which
     * may or may not be inplace.
     */
    BINARY_OP,

    /**
     * Implements TOS = TOS1 ** TOS.
     */
    BINARY_POWER,

    /**
     * Implements TOS = TOS1 * TOS.
     */
    BINARY_MULTIPLY,

    /**
     * Implements TOS = TOS1 @ TOS.
     */
    BINARY_MATRIX_MULTIPLY,

    /**
     * Implements TOS = TOS1 // TOS.
     */
    BINARY_FLOOR_DIVIDE,

    /**
     * Implements TOS = TOS1 / TOS.
     */
    BINARY_TRUE_DIVIDE,

    /**
     * Implements TOS = TOS1 % TOS.
     */
    BINARY_MODULO,

    /**
     * Implements TOS = TOS1 + TOS.
     */
    BINARY_ADD,

    /**
     * Implements TOS = TOS1 - TOS.
     */
    BINARY_SUBTRACT,

    /**
     * Implements TOS = TOS1[TOS].
     */
    BINARY_SUBSCR,

    /**
     * Implements TOS = TOS1 << TOS.
     */
    BINARY_LSHIFT,

    /**
     * Implements TOS = TOS1 >> TOS.
     */
    BINARY_RSHIFT,

    /**
     * Implements TOS = TOS1 & TOS.
     */
    BINARY_AND,

    /**
     * Implements TOS = TOS1 ^ TOS.
     */
    BINARY_XOR,

    /**
     * Implements TOS = TOS1 | TOS.
     */
    BINARY_OR,

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
    INPLACE_POWER,

    /**
     * Implements in-place TOS = TOS1 * TOS.
     */
    INPLACE_MULTIPLY,

    /**
     * Implements in-place TOS = TOS1 @ TOS.
     */
    INPLACE_MATRIX_MULTIPLY,

    /**
     * Implements in-place TOS = TOS1 // TOS.
     */
    INPLACE_FLOOR_DIVIDE,

    /**
     * Implements in-place TOS = TOS1 / TOS.
     */
    INPLACE_TRUE_DIVIDE,

    /**
     * Implements in-place TOS = TOS1 % TOS.
     */
    INPLACE_MODULO,

    /**
     * Implements in-place TOS = TOS1 + TOS.
     */
    INPLACE_ADD,

    /**
     * Implements in-place TOS = TOS1 - TOS.
     */
    INPLACE_SUBTRACT,

    /**
     * Implements in-place TOS = TOS1 << TOS.
     */
    INPLACE_LSHIFT,

    /**
     * Implements in-place TOS = TOS1 >> TOS.
     */
    INPLACE_RSHIFT,

    /**
     * Implements in-place TOS = TOS1 & TOS.
     */
    INPLACE_AND,

    /**
     * Implements in-place TOS = TOS1 ^ TOS.
     */
    INPLACE_XOR,

    /**
     * Implements in-place TOS = TOS1 | TOS.
     */
    INPLACE_OR,

    /**
     * Implements TOS1[TOS] = TOS2.
     */
    STORE_SUBSCR,

    /**
     * Implements del TOS1[TOS].
     */
    DELETE_SUBSCR,

    // *****************************
    // Coroutine opcodes
    // *****************************

    /**
     * Implements TOS = get_awaitable(TOS), where get_awaitable(o) returns o if o is a coroutine object or a generator
     * object with the CO_ITERABLE_COROUTINE flag, or resolves o.__await__.
     */
    GET_AWAITABLE,

    /**
     * Implements TOS = TOS.__aiter__().
     */
    GET_AITER,

    /**
     * Implements PUSH(get_awaitable(TOS.__anext__())). See {@link #GET_AWAITABLE} for details about get_awaitable
     */
    GET_ANEXT,

    /**
     * Terminates an async for loop. Handles an exception raised when awaiting a next item. If TOS is StopAsyncIteration
     * pop 7 values from the stack and restore the exception state using the second three of them. Otherwise re-raise the
     * exception using the three values from the stack. An exception handler block is removed from the block stack.
     */
    END_ASYNC_FOR,

    /**
     * Resolves __aenter__ and __aexit__ from the object on top of the stack.
     * Pushes __aexit__ and result of __aenter__() to the stack.
     */
    BEFORE_ASYNC_WITH,

    /**
     * Creates a new frame object.
     */
    SETUP_ASYNC_WITH,

    // *****************************
    // Miscellaneous opcodes
    // *****************************

    /**
     * Implements the expression statement for the interactive mode. TOS is removed from the stack and printed.
     * In non-interactive mode, an expression statement is terminated with POP_TOP.
     */
    PRINT_EXPR,

    /**
     * Calls set.add(TOS1[-i], TOS). Used to implement set comprehensions.
     * <p>
     * The added value is popped off, the container object remains on the stack so that it is available for further
     * iterations of the loop.
     */
    SET_ADD,

    /**
     * Calls list.append(TOS1[-i], TOS). Used to implement list comprehensions.
     * <p>
     * The added value is popped off, the container object remains on the stack so that it is available for further
     * iterations of the loop.
     */
    LIST_APPEND,

    /**
     * Calls dict.__setitem__(TOS1[-i], TOS1, TOS). Used to implement dict comprehensions.
     * <p>
     * The key/value pair is popped off, the container object remains on the stack so that it is available for further
     * iterations of the loop.
     */
    MAP_ADD,

    /**
     * Returns with TOS to the caller of the function.
     */
    RETURN_VALUE,

    /**
     * Create a generator, coroutine, or async generator from the current frame.
     * Clear the current frame and return the newly created generator. A no-op for us, since we detect if
     * the code represent a generator (and if so, generate a wrapper function for it that act like
     * RETURN_GENERATOR) before interpreting it
     */
    RETURN_GENERATOR,

    /**
     * Pops TOS. The kind operand corresponds to the type of generator or coroutine.
     * The legal kinds are 0 for generator, 1 for coroutine, and 2 for async generator.
     */
    GEN_START,

    /**
     * TOS1 is a subgenerator, TOS is a value. Calls TOS1.send(TOS) if self.thrownValue is null.
     * Otherwise, set self.thrownValue to null and call TOS1.throwValue(TOS) instead. TOS is replaced by the subgenerator
     * yielded value; TOS1 remains. When the subgenerator is exhausted, jump forward by its argument.
     */
    SEND,

    /**
     * Pops TOS and yields it from a generator.
     */
    YIELD_VALUE,

    /**
     * Pops TOS and delegates to it as a subiterator from a generator.
     */
    YIELD_FROM,

    /**
     * Checks whether __annotations__ is defined in locals(), if not it is set up to an empty dict. This opcode is only
     * emitted if a class or module body contains variable annotations statically.
     */
    SETUP_ANNOTATIONS,

    /**
     * Loads all symbols not starting with '_' directly from the module TOS to the local namespace. The module is popped
     * after
     * loading all names. This opcode implements from module import *.
     */
    IMPORT_STAR,

    /**
     * Removes one block from the block stack. Per frame, there is a stack of blocks,
     * denoting try statements, and such.
     */
    POP_BLOCK,

    /**
     * Removes one block from the block stack. The popped block must be an exception handler block, as implicitly created
     * when entering an except handler. In addition to popping extraneous values from the frame stack, the last three
     * popped values are used to restore the exception state.
     */
    POP_EXCEPT,

    /**
     * Pops a value from the stack. Pushes the current exception to the top of the stack.
     * Pushes the value originally popped back to the stack. Used in exception handlers.
     */
    PUSH_EXC_INFO,

    /**
     * Performs exception matching for except. Tests whether the TOS1 is an exception matching TOS.
     * Pops TOS and pushes the boolean result of the test.
     */
    CHECK_EXC_MATCH,

    /**
     * Re-raises the exception currently on top of the stack.
     */
    RERAISE,

    /**
     * Calls the function in position 7 on the stack with the top three items on the stack as arguments. Used to implement
     * the call context_manager.__exit__(*exc_info()) when an exception has occurred in a with statement.
     */
    WITH_EXCEPT_START,

    /**
     * Pushes AssertionError onto the stack. Used by the assert statement.
     */
    LOAD_ASSERTION_ERROR,

    /**
     * Pushes builtins.__build_class__() onto the stack.
     * It is later called by CALL_FUNCTION to construct a class.
     */
    LOAD_BUILD_CLASS,

    /**
     * This opcode performs several operations before a with block starts. First, it loads __exit__() from the
     * context manager and pushes it onto the stack for later use by {@link #WITH_EXCEPT_START}. Then, __enter__() is called,
     * and a finally block pointing to delta is pushed. Finally, the result of calling the __enter__() method is pushed
     * onto the stack. The next opcode will either ignore it (POP_TOP), or store it
     * in (a) variable(s) (STORE_FAST, STORE_NAME, or UNPACK_SEQUENCE).
     */
    SETUP_WITH,

    /**
     * Implements name = TOS. namei is the index of name in the attribute co_names of the code object.
     * The compiler tries to use STORE_FAST or STORE_GLOBAL if possible.
     */
    STORE_NAME,

    /**
     * Implements del name, where namei is the index into co_names attribute of the code object.
     */
    DELETE_NAME,

    /**
     * Unpacks TOS into count individual values, which are put onto the stack right-to-left.
     */
    UNPACK_SEQUENCE,

    /**
     * Implements assignment with a starred target:
     * Unpacks an iterable in TOS into individual values, where the total number of values can be smaller than the
     * number of items in the iterable: one of the new values will be a list of all leftover items.
     * <p>
     * The low byte of counts is the number of values before the list value, the high byte of counts the number of
     * values after it. The resulting values are put onto the stack right-to-left.
     */
    UNPACK_EX,

    /**
     * Implements TOS.name = TOS1, where namei is the index of name in co_names.
     */
    STORE_ATTR,

    /**
     * Implements del TOS.name, using namei as index into co_names.
     */
    DELETE_ATTR,

    /**
     * Works as {@link #STORE_NAME}, but stores the name as a global.
     */
    STORE_GLOBAL,

    /**
     * Works as {@link #DELETE_NAME}, but deletes a global name.
     */
    DELETE_GLOBAL,

    /**
     * Pushes co_consts[consti] onto the stack.
     */
    LOAD_CONST,

    /**
     * Pushes the value associated with co_names[namei] onto the stack.
     */
    LOAD_NAME,

    /**
     * Creates a tuple consuming count items from the stack, and pushes the resulting tuple onto the stack.
     */
    BUILD_TUPLE,

    /**
     * Works as {@link #BUILD_TUPLE}, but creates a list.
     */
    BUILD_LIST,

    /**
     * Works as {@link #BUILD_TUPLE}, but creates a set.
     */
    BUILD_SET,

    /**
     * Pushes a new dictionary object onto the stack.
     * Pops 2 * count items so that the dictionary holds count entries: {..., TOS3: TOS2, TOS1: TOS}.
     */
    BUILD_MAP,

    /**
     * The version of {@link #BUILD_MAP} specialized for constant keys. Pops the top element on the stack which contains a
     * tuple of keys,
     * then starting from TOS1, pops count values to form values in the built dictionary.
     */
    BUILD_CONST_KEY_MAP,

    /**
     * Concatenates count strings from the stack and pushes the resulting string onto the stack.
     */
    BUILD_STRING,

    /**
     * Pops a list from the stack and pushes a tuple containing the same values.
     */
    LIST_TO_TUPLE,

    /**
     * Calls list.extend(TOS1[-i], TOS). Used to build lists.
     */
    LIST_EXTEND,

    /**
     * Calls set.update(TOS1[-i], TOS). Used to build sets.
     */
    SET_UPDATE,

    /**
     * Calls dict.update(TOS1[-i], TOS). Used to build dicts.
     */
    DICT_UPDATE,

    /**
     * Like {@link #DICT_UPDATE} but raises an exception for duplicate keys.
     */
    DICT_MERGE,

    /**
     * Replaces TOS with getattr(TOS, co_names[namei]).
     */
    LOAD_ATTR,

    /**
     * Performs a Boolean operation. The operation name can be found in cmp_op[opname].
     */
    COMPARE_OP,

    /**
     * Performs is comparison, or is not if invert is 1.
     */
    IS_OP,

    /**
     * Performs in comparison, or not in if invert is 1.
     */
    CONTAINS_OP,

    /**
     * Imports the module co_names[namei].
     * TOS and TOS1 are popped and provide the fromlist and level arguments of __import__().
     * The module object is pushed onto the stack. The current namespace is not affected: for a proper import statement,
     * a subsequent STORE_FAST instruction modifies the namespace.
     */
    IMPORT_NAME,

    /**
     * Loads the attribute co_names[namei] from the module found in TOS. The resulting object is pushed onto the stack,
     * to be subsequently stored by a STORE_FAST instruction.
     */
    IMPORT_FROM,

    /**
     * Increments bytecode counter by delta.
     */
    JUMP_FORWARD,

    /**
     * Decrements bytecode counter by delta.
     */
    JUMP_BACKWARD,

    /**
     * Decrements bytecode counter by delta. Does not check for interrupts.
     */
    JUMP_BACKWARD_NO_INTERRUPT,

    /**
     * If TOS is true, sets the bytecode counter to target. TOS is popped.
     */
    POP_JUMP_IF_TRUE,

    /**
     * Same as {@link OpcodeIdentifier#POP_JUMP_IF_TRUE}, but argument is relative
     */
    POP_JUMP_FORWARD_IF_TRUE,

    /**
     * Same as {@link OpcodeIdentifier#POP_JUMP_IF_TRUE}, but argument is relative
     */
    POP_JUMP_BACKWARD_IF_TRUE,

    /**
     * If TOS is false, sets the bytecode counter to target. TOS is popped.
     */
    POP_JUMP_IF_FALSE,

    /**
     * Same as {@link OpcodeIdentifier#POP_JUMP_IF_FALSE}, but argument is relative
     */
    POP_JUMP_FORWARD_IF_FALSE,

    /**
     * Same as {@link OpcodeIdentifier#POP_JUMP_IF_FALSE}, but argument is relative
     */
    POP_JUMP_BACKWARD_IF_FALSE,

    /**
     * If TOS is not None, increments the bytecode counter by delta. TOS is popped.
     */
    POP_JUMP_FORWARD_IF_NOT_NONE,

    /**
     * If TOS is not None, decrements the bytecode counter by delta. TOS is popped.
     */
    POP_JUMP_BACKWARD_IF_NOT_NONE,

    /**
     * If TOS is None, increments the bytecode counter by delta. TOS is popped.
     */
    POP_JUMP_FORWARD_IF_NONE,

    /**
     * If TOS is None, decrements the bytecode counter by delta. TOS is popped.
     */
    POP_JUMP_BACKWARD_IF_NONE,

    /**
     * Tests whether the second value on the stack is an exception matching TOS, and jumps if it is not.
     * Pops two values from the stack.
     */
    JUMP_IF_NOT_EXC_MATCH,

    /**
     * If TOS is true, sets the bytecode counter to target and leaves TOS on the stack. Otherwise (TOS is false), TOS is
     * popped.
     */
    JUMP_IF_TRUE_OR_POP,

    /**
     * If TOS is false, sets the bytecode counter to target and leaves TOS on the stack. Otherwise (TOS is true), TOS is
     * popped.
     */
    JUMP_IF_FALSE_OR_POP,

    /**
     * Set bytecode counter to target.
     */
    JUMP_ABSOLUTE,

    /**
     * TOS is an iterator. Call its __next__() method. If this yields a new value, push it on the stack (leaving the
     * iterator below it).
     * If the iterator indicates it is exhausted, TOS is popped, and the byte code counter is incremented by delta.
     */
    FOR_ITER,

    /**
     * Loads the global named co_names[namei] onto the stack.
     */
    LOAD_GLOBAL,

    /**
     * Pushes a try block from a try-finally or try-except clause onto the block stack.
     * delta points to the finally block or the first except block.
     */
    SETUP_FINALLY,

    /**
     * Pushes a reference to the local co_varnames[var_num] onto the stack.
     */
    LOAD_FAST,

    /**
     * Stores TOS into the local co_varnames[var_num].
     */
    STORE_FAST,

    /**
     * Deletes local co_varnames[var_num].
     */
    DELETE_FAST,

    /**
     * Creates a new cell in slot i. If that slot is empty then that value is stored into the new cell.
     */
    MAKE_CELL,

    /**
     * Copies the n free variables from the closure into the frame. Removes the need for special code on the caller’s
     * side when calling closures.
     */
    COPY_FREE_VARS,

    /**
     * Pushes a reference to the cell contained in slot i of the cell and free variable storage.
     * The name of the variable is co_cellvars[i] if i is less than the length of co_cellvars.
     * Otherwise it is co_freevars[i - len(co_cellvars)].
     */
    LOAD_CLOSURE,

    /**
     * Loads the cell contained in slot i of the cell and free variable storage.
     * Pushes a reference to the object the cell contains on the stack.
     */
    LOAD_DEREF,

    /**
     * Much like {@link #LOAD_DEREF} but first checks the locals dictionary before consulting the cell.
     * This is used for loading free variables in class bodies.
     */
    LOAD_CLASSDEREF,

    /**
     * Stores TOS into the cell contained in slot i of the cell and free variable storage.
     */
    STORE_DEREF,

    /**
     * Empties the cell contained in slot i of the cell and free variable storage. Used by the del statement.
     */
    DELETE_DEREF,

    /**
     * Raises an exception using one of the 3 forms of the raise statement, depending on the value of argc:
     * <p>
     * 0: raise (re-raise previous exception)
     * 1: raise TOS (raise exception instance or type at TOS)
     * 2: raise TOS1 from TOS (raise exception instance or type at TOS1 with __cause__ set to TOS)
     */
    RAISE_VARARGS,

    /**
     * Prefixes PRECALL. Stores a reference to co_consts[consti] into an internal variable for use by CALL.
     * co_consts[consti] must be a tuple of strings.
     */
    KW_NAMES,

    /**
     * Prefixes CALL. Logically this is a no op.
     * It exists to enable effective specialization of calls. argc is the number of arguments as described in CALL.
     */
    PRECALL,

    /**
     * Pushes a NULL to the stack. Used in the call sequence to match the NULL pushed by LOAD_METHOD for non-method calls.
     */
    PUSH_NULL,

    /**
     * Calls a callable object with the number of arguments specified by argc, including the named arguments specified by
     * the preceding KW_NAMES, if any. On the stack are (in ascending order), either:
     * NULL
     * The callable
     * The positional arguments
     * The named arguments
     * or:
     * The callable
     * self
     * The remaining positional arguments
     * The named arguments
     * argc is the total of the positional and named arguments, excluding self when a NULL is not present.
     * CALL pops all arguments and the callable object off the stack, calls the callable object with those arguments,
     * and pushes the return value returned by the callable object.
     */
    CALL,

    /**
     * Calls a callable object with positional arguments. argc indicates the number of positional arguments.
     * The top of the stack contains positional arguments, with the right-most argument on top.
     * Below the arguments is a callable object to call. CALL_FUNCTION pops all arguments and the callable object off the
     * stack,
     * calls the callable object with those arguments, and pushes the return value returned by the callable object.
     */
    CALL_FUNCTION,

    /**
     * Calls a callable object with positional (if any) and keyword arguments. argc indicates the total number of positional
     * and keyword arguments. The top element on the stack contains a tuple with the names of the keyword arguments,
     * which must be strings. Below that are the values for the keyword arguments, in the order corresponding to the tuple.
     * Below that are positional arguments, with the right-most parameter on top. Below the arguments is a callable object
     * to call. CALL_FUNCTION_KW pops all arguments and the callable object off the stack, calls the callable object with
     * those arguments, and pushes the return value returned by the callable object.
     */
    CALL_FUNCTION_KW,

    /**
     * Calls a callable object with variable set of positional and keyword arguments. If the lowest bit of flags is set,
     * the top of the stack contains a mapping object containing additional keyword arguments. Before the callable is
     * called,
     * the mapping object and iterable object are each “unpacked” and their contents passed in as keyword and positional
     * arguments respectively. CALL_FUNCTION_EX pops all arguments and the callable object off the stack, calls the callable
     * object with those arguments, and pushes the return value returned by the callable object.
     */
    CALL_FUNCTION_EX,

    /**
     * Loads a method named co_names[namei] from the TOS object. TOS is popped.
     * This bytecode distinguishes two cases: if TOS has a method with the correct name, the bytecode pushes the unbound
     * method and TOS.
     * TOS will be used as the first argument (self) by CALL_METHOD when calling the unbound method.
     * Otherwise, NULL and the object return by the attribute lookup are pushed.
     */
    LOAD_METHOD,

    /**
     * Calls a method. argc is the number of positional arguments.
     * Keyword arguments are not supported. This opcode is designed to be used with LOAD_METHOD.
     * Positional arguments are on top of the stack. Below them, the two items described in LOAD_METHOD are on the stack
     * (either self and an unbound method object or NULL and an arbitrary callable). All of them are popped and the return
     * value is pushed.
     */
    CALL_METHOD,

    /**
     * Pushes a new function object on the stack. From bottom to top, the consumed stack must consist of values if the
     * argument carries a specified flag value
     * <p>
     * 0x01 a tuple of default values for positional-only and positional-or-keyword parameters in positional order
     * 0x02 a dictionary of keyword-only parameters’ default values
     * 0x04 an annotation dictionary
     * 0x08 a tuple containing cells for free variables, making a closure
     * <p>
     * the code associated with the function (at TOS1)
     * the qualified name of the function (at TOS)
     */
    MAKE_FUNCTION,

    /**
     * Pushes a slice object on the stack. argc must be 2 or 3. If it is 2, slice(TOS1, TOS) is pushed;
     * if it is 3, slice(TOS2, TOS1, TOS) is pushed. See the slice() built-in function for more information.
     */
    BUILD_SLICE,

    /**
     * Prefixes any opcode which has an argument too big to fit into the default one byte. ext holds an additional byte
     * which act as higher bits in the argument.
     * For each opcode, at most three prefixal EXTENDED_ARG are allowed, forming an argument from two-byte to four-byte.
     */
    EXTENDED_ARG,

    /**
     * Used for implementing formatted literal strings (f-strings). Pops an optional fmt_spec from the stack, then a
     * required value.
     * flags is interpreted as follows:
     * <p>
     * (flags & 0x03) == 0x00: value is formatted as-is.
     * <p>
     * (flags & 0x03) == 0x01: call str() on value before formatting it.
     * <p>
     * (flags & 0x03) == 0x02: call repr() on value before formatting it.
     * <p>
     * (flags & 0x03) == 0x03: call ascii() on value before formatting it.
     * <p>
     * (flags & 0x04) == 0x04: pop fmt_spec from the stack and use it, else use an empty fmt_spec.
     * <p>
     * Formatting is performed using PyObject_Format(). The result is pushed on the stack.
     */
    FORMAT_VALUE
}
