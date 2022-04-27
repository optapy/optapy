package org.optaplanner.python.translator.types;

import java.util.Map;

import org.optaplanner.python.translator.PythonLikeObject;

public class PythonLikeComparable {
    final static BinaryLambdaReference __LT__ =
            new BinaryLambdaReference((a, b) -> PythonBoolean.valueOf(((Comparable) a).compareTo(b) < 0), Map.of());
    final static BinaryLambdaReference __LE__ =
            new BinaryLambdaReference((a, b) -> PythonBoolean.valueOf(((Comparable) a).compareTo(b) <= 0), Map.of());
    final static BinaryLambdaReference __EQ__ =
            new BinaryLambdaReference((a, b) -> PythonBoolean.valueOf(((Comparable) a).compareTo(b) == 0), Map.of());
    final static BinaryLambdaReference __NE__ =
            new BinaryLambdaReference((a, b) -> PythonBoolean.valueOf(((Comparable) a).compareTo(b) != 0), Map.of());
    final static BinaryLambdaReference __GE__ =
            new BinaryLambdaReference((a, b) -> PythonBoolean.valueOf(((Comparable) a).compareTo(b) >= 0), Map.of());
    final static BinaryLambdaReference __GT__ =
            new BinaryLambdaReference((a, b) -> PythonBoolean.valueOf(((Comparable) a).compareTo(b) > 0), Map.of());

    private PythonLikeComparable() {
    };

    public static void setup(Map<String, PythonLikeObject> map) {
        map.put("__lt__", __LT__);
        map.put("__le__", __LE__);
        map.put("__eq__", __EQ__);
        map.put("__ne__", __NE__);
        map.put("__gt__", __GT__);
        map.put("__ge__", __GE__);
    }
}
