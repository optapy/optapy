package org.optaplanner.optapy.translator.builtins;

import java.util.Formatter;

import org.optaplanner.optapy.PythonLikeObject;
import org.optaplanner.optapy.translator.types.PythonNumber;
import org.optaplanner.optapy.translator.types.PythonString;

public class NumberBuiltinOperations {
    public static PythonString format(PythonLikeObject number, PythonLikeObject formatString) {
        String javaFormatString = getJavaNumberFormatString(((PythonString)formatString).value);
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
