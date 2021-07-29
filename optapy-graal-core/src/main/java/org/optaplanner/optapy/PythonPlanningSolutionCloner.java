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
                    "import java\n" +
                    "memo = dict()\n" +
                    "Score = java.type(\"org.optaplanner.core.api.score.Score\")\n" +
                    "variables = vars(__optaplanner_object__)\n" +
                    "for attribute, value in variables.items():\n" +
                    "    if java.instanceof(value, Score):\n" +
                    "        memo[id(value)] = value\n" +
                    "deepcopy(__optaplanner_object__, memo)\n", null).build();
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
