package org.optaplanner.jpyinterpreter.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.optaplanner.jpyinterpreter.CompareOp;
import org.optaplanner.jpyinterpreter.OpcodeIdentifier;
import org.optaplanner.jpyinterpreter.PythonBytecodeInstruction;
import org.optaplanner.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.jpyinterpreter.PythonCompiledFunction;
import org.optaplanner.jpyinterpreter.PythonExceptionTable;
import org.optaplanner.jpyinterpreter.PythonLikeObject;
import org.optaplanner.jpyinterpreter.PythonVersion;
import org.optaplanner.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import org.optaplanner.jpyinterpreter.types.PythonLikeType;

/**
 * A builder for Python bytecode.
 */
public class PythonFunctionBuilder {

    /**
     * The list of bytecode instructions
     */
    List<PythonBytecodeInstruction> instructionList = new ArrayList<>();

    /**
     * List of global names used in the bytecode
     */
    List<String> co_names = new ArrayList<>();

    /**
     * List of names of local variables in the bytecode
     */
    List<String> co_varnames = new ArrayList<>();

    /**
     * List of names of shared variables in the bytecode
     */
    List<String> co_cellvars = new ArrayList<>();

    /**
     * List of free variables in the bytecode
     */
    List<String> co_freevars = new ArrayList<>();

    /**
     * Constants used in the bytecode
     */
    List<PythonLikeObject> co_consts = new ArrayList<>();

    Map<String, PythonLikeObject> globalsMap = new HashMap<>();

    Map<String, PythonLikeType> typeAnnotations = new HashMap<>();

    int co_argcount = 0;
    int co_kwonlyargcount = 0;

    /**
     * Creates a new function builder for a Python function with the given parameters
     *
     * @param parameters The names of the function's parameters
     * @return
     */
    public static PythonFunctionBuilder newFunction(String... parameters) {
        PythonFunctionBuilder out = new PythonFunctionBuilder();
        out.co_varnames.addAll(Arrays.asList(parameters));
        out.co_names.addAll(out.co_varnames);
        out.co_argcount = parameters.length;
        out.co_kwonlyargcount = 0;
        return out;
    }

    /**
     * Creates the bytecode data for the function
     *
     * @return The bytecode data, which can be used by
     *         {@link PythonBytecodeToJavaBytecodeTranslator#translatePythonBytecode(PythonCompiledFunction, Class)} or
     *         {@link PythonBytecodeToJavaBytecodeTranslator#translatePythonBytecodeToClass(PythonCompiledFunction, Class)}.
     */
    public PythonCompiledFunction build() {
        PythonCompiledFunction out = new PythonCompiledFunction();
        out.module = "test";
        out.qualifiedName = "TestFunction";
        out.instructionList = instructionList;
        out.typeAnnotations = typeAnnotations;
        out.globalsMap = globalsMap;
        out.co_exceptiontable = new PythonExceptionTable(); // we use an empty exception table since it for Python 3.10
                                                            // (i.e. use block try...except instead of co_exceptiontable)
        out.co_constants = co_consts;
        out.co_varnames = co_varnames;
        out.co_names = co_names;
        out.co_argcount = co_argcount;
        out.co_kwonlyargcount = co_kwonlyargcount;
        out.co_cellvars = co_cellvars;
        out.co_freevars = co_freevars;
        out.pythonVersion = PythonVersion.PYTHON_3_10;
        return out;
    }

    /**
     * Perform the specified opcode with no argument; look at the {@link OpcodeIdentifier} javadoc for
     * information regarding the opcode.
     */
    public PythonFunctionBuilder op(OpcodeIdentifier opcode) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = opcode;
        instruction.offset = instructionList.size();
        instructionList.add(instruction);
        return this;
    }

    /**
     * Perform the specified opcode with an argument; look at the {@link OpcodeIdentifier} javadoc for
     * information regarding the opcode.
     */
    public PythonFunctionBuilder op(OpcodeIdentifier opcode, int arg) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = opcode;
        instruction.offset = instructionList.size();
        instruction.arg = arg;
        instructionList.add(instruction);
        return this;
    }

    /**
     * TOS is an iterator. While the iterator is not empty, push the
     * iterator's next element to the top of the stack and run the specified
     * code. Repeat until the iterator is empty, and then pop the iterator off the stack.
     *
     * @param blockBuilder the bytecode to run in the loop
     */
    public PythonFunctionBuilder loop(Consumer<PythonFunctionBuilder> blockBuilder) {
        return loop(blockBuilder, false);
    }

    /**
     * TOS is an iterator. While the iterator is not empty, push the
     * iterator's next element to the top of the stack and run the specified
     * code. Repeat until the iterator is empty, and then pop the iterator off the stack.
     *
     * @param blockBuilder the bytecode to run in the loop
     */
    public PythonFunctionBuilder loop(Consumer<PythonFunctionBuilder> blockBuilder, boolean alwaysExitEarly) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.FOR_ITER;
        instruction.offset = instructionList.size();
        instruction.isJumpTarget = true;
        instructionList.add(instruction);

        blockBuilder.accept(this);

        if (!alwaysExitEarly) {
            PythonBytecodeInstruction jumpBackInstruction = new PythonBytecodeInstruction();
            jumpBackInstruction.opcode = OpcodeIdentifier.JUMP_ABSOLUTE;
            jumpBackInstruction.offset = instructionList.size();
            jumpBackInstruction.arg = instruction.offset;
            jumpBackInstruction.isJumpTarget = false;
            instructionList.add(jumpBackInstruction);
        }

        instruction.arg = instructionList.size() - instruction.offset - 1;

        PythonBytecodeInstruction afterLoopInstruction = new PythonBytecodeInstruction();
        afterLoopInstruction.opcode = OpcodeIdentifier.NOP;
        afterLoopInstruction.offset = instructionList.size();
        afterLoopInstruction.isJumpTarget = true;
        instructionList.add(afterLoopInstruction);

        return this;
    }

    /**
     * Declare a try block, and return an except builder for that try block.
     *
     * @param tryBlockBuilder The code to execute inside the try block
     * @return An {@link ExceptBuilder} for the try block
     */
    public ExceptBuilder tryCode(Consumer<PythonFunctionBuilder> tryBlockBuilder, boolean tryExitEarly) {
        PythonBytecodeInstruction notCatchedFinallyBlock = new PythonBytecodeInstruction();
        notCatchedFinallyBlock.opcode = OpcodeIdentifier.SETUP_FINALLY;
        notCatchedFinallyBlock.offset = instructionList.size();
        instructionList.add(notCatchedFinallyBlock);

        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.SETUP_FINALLY;
        instruction.offset = instructionList.size();
        instructionList.add(instruction);
        int tryStart = instructionList.size();

        tryBlockBuilder.accept(this);

        instruction.arg = instructionList.size() - tryStart + (tryExitEarly ? 0 : 1);

        instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.JUMP_ABSOLUTE;
        instruction.offset = instructionList.size();
        instruction.arg = 0;

        if (!tryExitEarly) {
            instructionList.add(instruction);
        }

        return new ExceptBuilder(this, instruction, notCatchedFinallyBlock);
    }

    /**
     * Execute the code generated by the parameter if TOS is True; skip it otherwise.
     * TOS is popped.
     *
     * @param blockBuilder The code inside the if statement
     */
    public PythonFunctionBuilder ifTrue(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.POP_JUMP_IF_FALSE; // Skip block if False (i.e. enter block if True)
        instruction.offset = instructionList.size();
        instructionList.add(instruction);

        blockBuilder.accept(this);

        PythonBytecodeInstruction afterIfInstruction = new PythonBytecodeInstruction();
        afterIfInstruction.opcode = OpcodeIdentifier.NOP;
        afterIfInstruction.offset = instructionList.size();
        afterIfInstruction.isJumpTarget = true;
        instructionList.add(afterIfInstruction);

        instruction.arg = afterIfInstruction.offset;

        return this;
    }

    /**
     * Execute the code generated by the parameter if TOS is False; skip it otherwise.
     * TOS is popped.
     *
     * @param blockBuilder The code inside the if statement
     */
    public PythonFunctionBuilder ifFalse(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.POP_JUMP_IF_TRUE; // Skip block if True (i.e. enter block if False)
        instruction.offset = instructionList.size();
        instructionList.add(instruction);

        blockBuilder.accept(this);

        PythonBytecodeInstruction afterIfInstruction = new PythonBytecodeInstruction();
        afterIfInstruction.opcode = OpcodeIdentifier.NOP;
        afterIfInstruction.offset = instructionList.size();
        afterIfInstruction.isJumpTarget = true;
        instructionList.add(afterIfInstruction);

        instruction.arg = afterIfInstruction.offset;

        return this;
    }

    /**
     * Execute the code generated by the parameter if TOS is True; skip it otherwise.
     * If TOS is True, TOS is popped; otherwise it remains on the stack.
     *
     * @param blockBuilder The code inside the if statement
     */
    public PythonFunctionBuilder ifTruePopTop(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.JUMP_IF_FALSE_OR_POP; // Skip block if False (i.e. enter block if True)
        instruction.offset = instructionList.size();
        instructionList.add(instruction);

        blockBuilder.accept(this);

        PythonBytecodeInstruction afterIfInstruction = new PythonBytecodeInstruction();
        afterIfInstruction.opcode = OpcodeIdentifier.NOP;
        afterIfInstruction.offset = instructionList.size();
        afterIfInstruction.isJumpTarget = true;
        instructionList.add(afterIfInstruction);

        instruction.arg = afterIfInstruction.offset;

        return this;
    }

    /**
     * Execute the code generated by the parameter if TOS is False; skip it otherwise.
     * If TOS is False, TOS is popped; otherwise it remains on the stack.
     *
     * @param blockBuilder The code inside the if statement
     */
    public PythonFunctionBuilder ifFalsePopTop(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.JUMP_IF_TRUE_OR_POP; // Skip block if True (i.e. enter block if False)
        instruction.offset = instructionList.size();
        instructionList.add(instruction);

        blockBuilder.accept(this);

        PythonBytecodeInstruction afterIfInstruction = new PythonBytecodeInstruction();
        afterIfInstruction.opcode = OpcodeIdentifier.NOP;
        afterIfInstruction.offset = instructionList.size();
        afterIfInstruction.isJumpTarget = true;
        instructionList.add(afterIfInstruction);

        instruction.arg = afterIfInstruction.offset;

        return this;
    }

    /**
     * Use TOS as a context_manager, pushing the result of its __enter__ method to TOS, and calling its
     * __exit__ method on exit of the with block (both normal and exceptional exits)
     *
     * @param blockBuilder The code inside the with block
     */
    public PythonFunctionBuilder with(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.SETUP_WITH;
        instruction.offset = instructionList.size();
        instructionList.add(instruction);

        blockBuilder.accept(this);

        // Call the exit function
        loadConstant(null);
        loadConstant(null);
        loadConstant(null);
        callFunction(3);
        op(OpcodeIdentifier.POP_TOP);

        PythonBytecodeInstruction skipExceptionHandler = new PythonBytecodeInstruction();
        skipExceptionHandler.opcode = OpcodeIdentifier.JUMP_ABSOLUTE;
        skipExceptionHandler.offset = instructionList.size();
        instructionList.add(skipExceptionHandler);

        PythonBytecodeInstruction exceptionHandler = new PythonBytecodeInstruction();
        exceptionHandler.opcode = OpcodeIdentifier.WITH_EXCEPT_START;
        exceptionHandler.offset = instructionList.size();
        exceptionHandler.isJumpTarget = true;
        instructionList.add(exceptionHandler);

        instruction.arg = exceptionHandler.offset - instruction.offset - 1;

        ifFalse(reraiseExceptionBlock -> reraiseExceptionBlock
                .op(OpcodeIdentifier.POP_TOP).op(OpcodeIdentifier.RERAISE));

        op(OpcodeIdentifier.POP_TOP);
        op(OpcodeIdentifier.POP_TOP);
        op(OpcodeIdentifier.POP_TOP);
        op(OpcodeIdentifier.POP_EXCEPT);
        op(OpcodeIdentifier.POP_TOP);

        skipExceptionHandler.arg = instructionList.size();

        PythonBytecodeInstruction afterWithInstruction = new PythonBytecodeInstruction();
        afterWithInstruction.opcode = OpcodeIdentifier.NOP;
        afterWithInstruction.offset = instructionList.size();
        afterWithInstruction.isJumpTarget = true;
        instructionList.add(afterWithInstruction);

        return this;
    }

    /**
     * Create a list from the {@code count} top items on the stack. TOS is the last element in the list
     *
     * @param count The number of elements to pop and put into the list.
     */
    public PythonFunctionBuilder list(int count) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.BUILD_LIST;
        instruction.offset = instructionList.size();
        instruction.arg = count;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Create a tuple from the {@code count} top items on the stack. TOS is the last element in the tuple
     *
     * @param count The number of elements to pop and put into the tuple.
     */
    public PythonFunctionBuilder tuple(int count) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.BUILD_TUPLE;
        instruction.offset = instructionList.size();
        instruction.arg = count;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Create a dict from the {@code 2 * count} top items on the stack, which are read as key-value pairs.
     *
     * @param count The number of key-value pairs to pop and put into the dict.
     */
    public PythonFunctionBuilder dict(int count) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.BUILD_MAP;
        instruction.offset = instructionList.size();
        instruction.arg = count;
        instructionList.add(instruction);
        return this;
    }

    /**
     * TOS is a tuple containing keys, and below TOS are {@code count} items representing the keys values.
     * The last item in the tuple maps to TOS1, the second last item to TOS2, etc.
     *
     * @param count The number of values in the dict
     */
    public PythonFunctionBuilder constDict(int count) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.BUILD_CONST_KEY_MAP;
        instruction.offset = instructionList.size();
        instruction.arg = count;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Creates a set from the top {@code count} items in the stack.
     *
     * @param count The number of elements to pop and put into the set.
     */
    public PythonFunctionBuilder set(int count) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.BUILD_SET;
        instruction.offset = instructionList.size();
        instruction.arg = count;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Call a function with {@code argc} parameters. TOS[argc+1] is the function; above it are its arguments.
     *
     * @param argc The number of arguments the function takes
     */
    public PythonFunctionBuilder callFunction(int argc) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.CALL_FUNCTION;
        instruction.offset = instructionList.size();
        instruction.arg = argc;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Call a function with {@code argc} parameters, some of which are keywords.
     * TOS[argc+1] is the function; above it are its arguments; keyword-only parameters are store
     * in a dict at TOS, and positional parameters are stored in the stack.
     *
     * @param argc The number of arguments the function takes
     */
    public PythonFunctionBuilder callFunctionWithKeywords(int argc) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.CALL_FUNCTION_KW;
        instruction.offset = instructionList.size();
        instruction.arg = argc;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Call the function at TOS1 with the parameters specified in the tuple at TOS is {@code hasKeywords} is false,
     * otherwise call the function at TOS2 with the parameters specified in the tuple at TOS1 and the keyword dict at TOS.
     *
     * @param hasKeywords true if keyword-only parameters are being passed
     */
    public PythonFunctionBuilder callFunctionUnpack(boolean hasKeywords) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.CALL_FUNCTION_EX;
        instruction.offset = instructionList.size();
        instruction.arg = (hasKeywords) ? 1 : 0;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Load the specified method on TOS. If type(TOS) has the method, self, method is pushed; otherwise
     * null, TOS.__getattribute__(method) is pushed.
     *
     * @param methodName the method to load
     */
    public PythonFunctionBuilder loadMethod(String methodName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.LOAD_METHOD;
        instruction.offset = instructionList.size();

        int methodIndex = co_names.indexOf(methodName);
        if (methodIndex == -1) {
            methodIndex = co_names.size();
            co_names.add(methodName);
        }

        instruction.arg = methodIndex;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Call a method with {@code argc} arguments. Keyword-only arguments are not allowed.
     *
     * @param argc The number of arguments the method accepts
     */
    public PythonFunctionBuilder callMethod(int argc) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.CALL_METHOD;
        instruction.offset = instructionList.size();
        instruction.arg = argc;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Get an attribute on TOS.
     *
     * @param attributeName The attribute to get
     */
    public PythonFunctionBuilder getAttribute(String attributeName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.LOAD_ATTR;
        instruction.offset = instructionList.size();

        int attributeIndex = co_names.indexOf(attributeName);
        if (attributeIndex == -1) {
            attributeIndex = co_names.size();
            co_names.add(attributeName);
        }

        instruction.arg = attributeIndex;
        instructionList.add(instruction);
        return this;
    }

    /**
     * TOS is an object, and TOS1 is a value. Store TOS1 into the {@code attributeName} attribute of TOS.
     * TOS and TOS1 are popped.
     *
     * @param attributeName The attribute to store.
     * @return
     */
    public PythonFunctionBuilder storeAttribute(String attributeName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.STORE_ATTR;
        instruction.offset = instructionList.size();

        int attributeIndex = co_names.indexOf(attributeName);
        if (attributeIndex == -1) {
            attributeIndex = co_names.size();
            co_names.add(attributeName);
        }

        instruction.arg = attributeIndex;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Loads a constant (converting it to the Interpreter equivalent if required).
     *
     * @param constant The Java constant to load
     */
    public PythonFunctionBuilder loadConstant(Object constant) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.LOAD_CONST;
        instruction.offset = instructionList.size();
        PythonLikeObject wrappedConstant = JavaPythonTypeConversionImplementor.wrapJavaObject(constant);

        int index = co_consts.indexOf(wrappedConstant);
        if (index == -1) {
            index = co_consts.size();
            co_consts.add(JavaPythonTypeConversionImplementor.wrapJavaObject(constant));
        }

        instruction.arg = index;

        instructionList.add(instruction);
        return this;
    }

    /**
     * Load the specified parameter
     *
     * @param parameterName The parameter to load
     * @throws IllegalArgumentException if the parameter is not in the function's parameter list
     */
    public PythonFunctionBuilder loadParameter(String parameterName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.LOAD_FAST;
        instruction.offset = instructionList.size();
        instruction.arg = co_varnames.indexOf(parameterName);

        if (instruction.arg == -1) {
            throw new IllegalArgumentException("Parameter (" + parameterName + ") is not in the parameter list (" +
                    co_varnames + ").");
        }

        instructionList.add(instruction);
        return this;
    }

    /**
     * Loads a variable with the given name (creating an entry in co_varnames if needed).
     *
     * @param variableName The variable to load
     */
    public PythonFunctionBuilder loadVariable(String variableName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.LOAD_FAST;
        instruction.offset = instructionList.size();
        instruction.arg = co_varnames.indexOf(variableName);

        if (instruction.arg == -1) {
            co_varnames.add(variableName);
            instruction.arg = co_varnames.size() - 1;
        }

        instructionList.add(instruction);
        return this;
    }

    /**
     * Store TOS into a variable with the given name (creating an entry in co_varnames if needed).
     *
     * @param variableName The variable to store TOS in
     */
    public PythonFunctionBuilder storeVariable(String variableName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.STORE_FAST;
        instruction.offset = instructionList.size();
        instruction.arg = co_varnames.indexOf(variableName);

        if (instruction.arg == -1) {
            co_varnames.add(variableName);
            instruction.arg = co_varnames.size() - 1;
        }

        instructionList.add(instruction);
        return this;
    }

    /**
     * Loads a variable that is shared with an inner function with the given name (creating an entry in co_cellvars if needed).
     *
     * @param variableName The variable to load
     */
    public PythonFunctionBuilder loadCellVariable(String variableName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.LOAD_DEREF;
        instruction.offset = instructionList.size();
        instruction.arg = co_cellvars.indexOf(variableName);

        if (instruction.arg == -1) {
            co_cellvars.add(variableName);
            instruction.arg = co_cellvars.size() - 1;

            if (!co_varnames.contains(variableName)) {
                co_varnames.add(variableName);
            }
        }

        instructionList.add(instruction);
        return this;
    }

    /**
     * Stores TOS into a variable that is shared with an inner function with the given name (creating an entry in co_cellvars if
     * needed).
     *
     * @param variableName The variable to store TOS in
     */
    public PythonFunctionBuilder storeCellVariable(String variableName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.STORE_DEREF;
        instruction.offset = instructionList.size();
        instruction.arg = co_cellvars.indexOf(variableName);

        if (instruction.arg == -1) {
            co_cellvars.add(variableName);
            instruction.arg = co_cellvars.size() - 1;

            if (!co_varnames.contains(variableName)) {
                co_varnames.add(variableName);
            }
        }

        instructionList.add(instruction);
        return this;
    }

    /**
     * Loads a free variable (creating an entry in co_freevars if needed).
     *
     * @param variableName The variable to load
     */
    public PythonFunctionBuilder loadFreeVariable(String variableName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.LOAD_DEREF;
        instruction.offset = instructionList.size();
        instruction.arg = co_freevars.indexOf(variableName);

        if (instruction.arg == -1) {
            co_freevars.add(variableName);
            instruction.arg = co_freevars.size() - 1;

            if (!co_varnames.contains(variableName)) {
                co_varnames.add(variableName);
            }
        }

        instructionList.add(instruction);
        return this;
    }

    /**
     * Stores TOS into a free variable (creating an entry in co_freevars if needed).
     *
     * @param variableName The variable to store TOS in
     */
    public PythonFunctionBuilder storeFreeVariable(String variableName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.STORE_DEREF;
        instruction.offset = instructionList.size();
        instruction.arg = co_freevars.indexOf(variableName);

        if (instruction.arg == -1) {
            co_freevars.add(variableName);
            instruction.arg = co_freevars.size() - 1;

            if (!co_varnames.contains(variableName)) {
                co_varnames.add(variableName);
            }
        }

        instructionList.add(instruction);
        return this;
    }

    public PythonFunctionBuilder usingGlobalsMap(Map<String, PythonLikeObject> globalsMap) {
        this.globalsMap = globalsMap;
        return this;
    }

    /**
     * Loads a global variable
     *
     * @param variableName The variable to load
     */
    public PythonFunctionBuilder loadGlobalVariable(String variableName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.LOAD_GLOBAL;
        instruction.offset = instructionList.size();
        instruction.arg = co_names.indexOf(variableName);

        if (instruction.arg == -1) {
            co_names.add(variableName);
            instruction.arg = co_names.size() - 1;
        }

        instructionList.add(instruction);
        return this;
    }

    /**
     * Store TOS into a global variable.
     *
     * @param variableName The global variable to store TOS in
     */
    public PythonFunctionBuilder storeGlobalVariable(String variableName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.STORE_GLOBAL;
        instruction.offset = instructionList.size();
        instruction.arg = co_names.indexOf(variableName);

        if (instruction.arg == -1) {
            co_names.add(variableName);
            instruction.arg = co_names.size() - 1;
        }

        instructionList.add(instruction);
        return this;
    }

    /**
     * Loads a module using TOS as level and TOS1 as from_list
     *
     * @param moduleName The module to get
     */
    public PythonFunctionBuilder loadModule(String moduleName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.IMPORT_NAME;
        instruction.offset = instructionList.size();

        int attributeIndex = co_names.indexOf(moduleName);
        if (attributeIndex == -1) {
            attributeIndex = co_names.size();
            co_names.add(moduleName);
        }

        instruction.arg = attributeIndex;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Loads an attribute from TOS (which is a module)
     *
     * @param attributeName The attribute to get
     */
    public PythonFunctionBuilder getFromModule(String attributeName) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.IMPORT_FROM;
        instruction.offset = instructionList.size();

        int attributeIndex = co_names.indexOf(attributeName);
        if (attributeIndex == -1) {
            attributeIndex = co_names.size();
            co_names.add(attributeName);
        }

        instruction.arg = attributeIndex;
        instructionList.add(instruction);
        return this;
    }

    /**
     * Perform a comparison on TOS and TOS1, popping TOS, TOS1 and pushing the result.
     *
     * @param compareOp The comparison to perform
     */
    public PythonFunctionBuilder compare(CompareOp compareOp) {
        PythonBytecodeInstruction instruction = new PythonBytecodeInstruction();
        instruction.opcode = OpcodeIdentifier.COMPARE_OP;
        instruction.offset = instructionList.size();
        instruction.arg = compareOp.id;

        instructionList.add(instruction);
        return this;
    }
}
