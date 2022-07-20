package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.createInstance;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.optaplanner.python.translator.CompareOp;
import org.optaplanner.python.translator.OpcodeIdentifier;
import org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.errors.PythonAssertionError;
import org.optaplanner.python.translator.types.errors.PythonException;
import org.optaplanner.python.translator.types.errors.StopIteration;
import org.optaplanner.python.translator.util.PythonFunctionBuilder;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ExceptionImplementorTest {
    @Test
    public void testTryExcept() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .tryCode(code -> {
                    code.loadParameter("item")
                            .loadConstant(5)
                            .compare(CompareOp.LESS_THAN)
                            .ifTrue(block -> {
                                block.loadConstant("Try").op(OpcodeIdentifier.RETURN_VALUE);
                            })
                            .op(OpcodeIdentifier.LOAD_ASSERTION_ERROR)
                            .op(OpcodeIdentifier.RAISE_VARARGS, 1);
                }, true)
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").op(OpcodeIdentifier.RETURN_VALUE);
                }, true)
                .tryEnd()
                .build();

        Function javaFunction = translatePythonBytecode(pythonCompiledFunction, Function.class);

        assertThat(javaFunction.apply(1)).isEqualTo("Try");
        assertThat(javaFunction.apply(6)).isEqualTo("Assert");
    }

    @Test
    public void testTryExceptFinally() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadConstant(null).storeGlobalVariable("exception")
                .loadConstant("Before Try").storeGlobalVariable("finally")
                .tryCode(code -> {
                    code.loadParameter("item")
                            .loadConstant(1)
                            .compare(CompareOp.EQUALS)
                            .ifTrue(block -> {
                                block.op(OpcodeIdentifier.LOAD_ASSERTION_ERROR)
                                        .op(OpcodeIdentifier.RAISE_VARARGS, 1);
                            })
                            .loadParameter("item")
                            .loadConstant(2)
                            .compare(CompareOp.EQUALS)
                            .ifTrue(block -> {
                                block.loadConstant(new StopIteration())
                                        .op(OpcodeIdentifier.RAISE_VARARGS, 1);
                            });
                }, false)
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").storeGlobalVariable("exception");
                }, false)
                .andFinally(code -> {
                    code.loadConstant("Finally")
                            .storeGlobalVariable("finally");
                }, false)
                .tryEnd()
                .loadConstant(1)
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        Class javaFunctionClass = translatePythonBytecodeToClass(pythonCompiledFunction, Function.class);

        Map<String, PythonLikeObject> globalsMap = new HashMap<>();
        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);

        Mockito.when(interpreter.getGlobal(Mockito.any(), Mockito.any()))
                .thenAnswer(invocationOnMock -> globalsMap.get(invocationOnMock.getArgument(0, String.class)));
        Mockito.doAnswer(invocationOnMock -> {
            globalsMap.put(invocationOnMock.getArgument(1, String.class),
                    invocationOnMock.getArgument(2, PythonLikeObject.class));
            return null;
        }).when(interpreter).setGlobal(Mockito.any(), Mockito.any(), Mockito.any());

        Function javaFunction = (Function) createInstance(javaFunctionClass, interpreter);

        assertThat(javaFunction.apply(0)).isEqualTo(1);
        assertThat(globalsMap.get("exception")).isEqualTo(PythonNone.INSTANCE);
        assertThat(globalsMap.get("finally")).isEqualTo("Finally");

        assertThat(javaFunction.apply(1)).isEqualTo(1);
        assertThat(globalsMap.get("exception")).isEqualTo("Assert");
        assertThat(globalsMap.get("finally")).isEqualTo("Finally");

        assertThat(javaFunction.apply(2)).isEqualTo(1);
        assertThat(globalsMap.get("exception")).isEqualTo(PythonNone.INSTANCE);
        assertThat(globalsMap.get("finally")).isEqualTo("Finally");
    }

    @Test
    public void testExceptionInLoop() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("start", "last_values")
                .loadGlobalVariable("reversed")
                .loadParameter("last_values")
                .callFunction(1)
                .op(OpcodeIdentifier.GET_ITER)
                .loop(loopBuilder -> {
                    loopBuilder.storeVariable("last_value")
                            .tryCode(tryBuilder -> {
                                tryBuilder.loadVariable("last_value")
                                        .loadConstant(1)
                                        .op(OpcodeIdentifier.BINARY_ADD)
                                        .op(OpcodeIdentifier.POP_BLOCK)
                                        .op(OpcodeIdentifier.ROT_TWO)
                                        .op(OpcodeIdentifier.POP_TOP)
                                        .op(OpcodeIdentifier.RETURN_VALUE);
                            }, true).except(PythonException.EXCEPTION_TYPE, exceptBuilder -> {
                                exceptBuilder
                                        .op(OpcodeIdentifier.JUMP_ABSOLUTE, 4);
                            }, true)
                            .tryEnd();
                }, true)
                .loadParameter("start")
                .op(OpcodeIdentifier.RETURN_VALUE)
                .build();

        BiFunction biFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
    }
}
