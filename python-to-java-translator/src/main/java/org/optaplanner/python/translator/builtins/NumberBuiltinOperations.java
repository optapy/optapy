package org.optaplanner.python.translator.builtins;

import java.util.Formatter;

import org.optaplanner.python.translator.PythonLikeObject;
import org.optaplanner.python.translator.types.PythonNumber;
import org.optaplanner.python.translator.types.PythonString;

public class NumberBuiltinOperations {
    public static PythonString format(PythonLikeObject number, PythonLikeObject formatString) {
        String javaFormatString = getJavaNumberFormatString(((PythonString) formatString).value);
        return new PythonString(new Formatter().format(javaFormatString, ((PythonNumber) number).getValue()).toString());
    }

    public static String getJavaNumberFormatString(String pythonFormatString) {
        // TODO: actually implement this
        // Python format
        // [[fill]align][sign][#][0][minimumwidth][.precision][type]
        // Java format
        //  %[argument_index$][flags][width][.precision]conversion
        return "%" + pythonFormatString;
    }
}
