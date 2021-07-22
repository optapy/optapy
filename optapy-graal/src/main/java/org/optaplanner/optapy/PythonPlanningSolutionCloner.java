package org.optaplanner.optapy;

import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;

import java.io.IOException;

public class PythonPlanningSolutionCloner implements SolutionCloner {
    private static final Source cloneFunctionSource = getCloneFunctionSource();

    private static Source getCloneFunctionSource() {
        try {
            return Source.newBuilder("python", "from copy import copy, deepcopy\n" +
                    // Shallow Copy solution since score cannot be deepcloned in python
                    "clone = copy(__optaplanner_object__)\n" +
                    "variables = vars(clone)\n" +
                    "for variable in variables:\n" +
                    // Deep clone each attribute that is not score
                    "    if variable != \"score\":\n" +
                    "        variables[variable] = deepcopy(variables[variable])\n" +
                    "clone", null).build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object cloneSolution(Object o) {
        try {
            Value pythonBinding = (Value) (o.getClass().getField(PythonWrapperGenerator.pythonBindingFieldName)
                    .get(o));
            pythonBinding.getContext().getBindings("python").putMember("__optaplanner_object__", pythonBinding);
            Value clone = pythonBinding.getContext().eval(cloneFunctionSource);
            Object out = PythonWrapperGenerator.wrap(o.getClass(), clone);
            return out;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
