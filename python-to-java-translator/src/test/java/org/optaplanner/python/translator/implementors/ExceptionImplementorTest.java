package org.optaplanner.python.translator.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.createInstance;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode;
import static org.optaplanner.python.translator.PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.optaplanner.python.translator.CompareOp;
import org.optaplanner.python.translator.PythonBytecodeInstruction;
import org.optaplanner.python.translator.PythonCompiledFunction;
import org.optaplanner.python.translator.PythonInterpreter;
import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonNone;
import org.optaplanner.python.translator.types.errors.PythonAssertionError;
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
                                block.loadConstant("Try").op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE);
                            })
                            .op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_ASSERTION_ERROR)
                            .op(PythonBytecodeInstruction.OpcodeIdentifier.RAISE_VARARGS, 1);
                })
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE);
                })
                .tryEnd()
                .loadConstant(null)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
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
                                block.op(PythonBytecodeInstruction.OpcodeIdentifier.LOAD_ASSERTION_ERROR)
                                        .op(PythonBytecodeInstruction.OpcodeIdentifier.RAISE_VARARGS, 1);
                            })
                            .loadParameter("item")
                            .loadConstant(2)
                            .compare(CompareOp.EQUALS)
                            .ifTrue(block -> {
                                block.loadConstant(new StopIteration())
                                        .op(PythonBytecodeInstruction.OpcodeIdentifier.RAISE_VARARGS, 1);
                            });
                })
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").storeGlobalVariable("exception");
                })
                .andFinally(code -> {
                    code.loadConstant("Finally")
                            .storeGlobalVariable("finally");
                })
                .tryEnd()
                .loadConstant(1)
                .op(PythonBytecodeInstruction.OpcodeIdentifier.RETURN_VALUE)
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
}
