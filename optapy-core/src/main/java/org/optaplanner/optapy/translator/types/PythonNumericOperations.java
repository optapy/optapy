package org.optaplanner.optapy.translator.types;

import java.util.Map;

import org.optaplanner.optapy.PythonLikeObject;

public class PythonNumericOperations {
    private PythonNumericOperations() {

    }

    public static void setup(Map<String, PythonLikeObject> dict) {
        // MATRIX_MULTIPLY("__matmul__"),

        var BOOL_CAST = new UnaryLambdaReference(a -> PythonBoolean.valueOf(((PythonNumber) a).compareTo(PythonInteger.valueOf(0)) == 0),
                                            Map.of());
        var INT_CAST = new UnaryNumericLambdaReference(Long::longValue, Double::longValue);
        var FLOAT_CAST = new UnaryNumericLambdaReference(Long::doubleValue, Double::doubleValue);

        var NEGATE = new UnaryNumericLambdaReference(a -> -a, a -> -a);
        var POS = new UnaryNumericLambdaReference(a -> a, a -> a);
        var INVERT = new UnaryNumericLambdaReference(a -> ~a, a -> {
            throw new IllegalArgumentException("Cannot invert float");
        });

        var POW = new BinaryNumericLambdaReference(Math::pow, Math::pow);
        var MUL = new BinaryNumericLambdaReference((a,b) -> a*b, (a,b) -> a*b);
        var FLOOR_DIVIDE = new BinaryNumericLambdaReference(Math::floorDiv, (a,b) -> Math.floor(a/b));
        var TRUE_DIVIDE = new BinaryNumericLambdaReference((a,b) -> a.doubleValue() / b.doubleValue(),
                                                           (a,b) -> a / b);
        var MODULO = new BinaryNumericLambdaReference((a,b) -> a % b, (a,b) -> a % b);
        var ADD = new BinaryNumericLambdaReference(Long::sum, Double::sum);
        var SUBTRACT = new BinaryNumericLambdaReference((a,b) -> a - b, (a,b) -> a - b);
        var LSHIFT = new BinaryNumericLambdaReference((a,b) -> a << b, (a,b) -> {
            throw new ArithmeticException("Cannot LSHIFT doubles");
        });
        var RSHIFT = new BinaryNumericLambdaReference((a,b) -> a >> b, (a,b) -> {
            throw new ArithmeticException("Cannot RSHIFT doubles");
        });
        var AND = new BinaryNumericLambdaReference((a,b) -> a & b, (a,b) -> {
            throw new ArithmeticException("Cannot AND doubles");
        });
        var XOR = new BinaryNumericLambdaReference((a,b) -> a ^ b, (a,b) -> {
            throw new ArithmeticException("Cannot XOR doubles");
        });
        var OR = new BinaryNumericLambdaReference((a,b) -> a | b, (a,b) -> {
            throw new ArithmeticException("Cannot OR doubles");
        });

        // Casts
        dict.put("__bool__", BOOL_CAST);
        dict.put("__index__", INT_CAST);
        dict.put("__float__", FLOAT_CAST);

        // Unary Operations
        dict.put("__neg__", NEGATE);
        dict.put("__pos__", POS);
        dict.put("__invert__", INVERT);

        // Binary Operations
        dict.put("__pow__", POW);
        dict.put("__mul__", MUL);
        // dict.put("__matmul__", ?);
        dict.put("__floordiv__", FLOOR_DIVIDE);
        dict.put("__truediv__", TRUE_DIVIDE);
        dict.put("__mod__", MODULO);
        dict.put("__add__", ADD);
        dict.put("__sub__", SUBTRACT);
        dict.put("__lshift__", LSHIFT);
        dict.put("__rshift__", RSHIFT);
        dict.put("__and__", AND);
        dict.put("__xor__", XOR);
        dict.put("__or__", OR);

        // Numbers don't support in-place modification in Python, so it reuses add / etc.
        dict.put("__ipow__", POW);
        dict.put("__imul__", MUL);
        // dict.put("__imatmul__");
        dict.put("__ifloordiv__", FLOOR_DIVIDE);
        dict.put("__itruediv__", TRUE_DIVIDE);
        dict.put("__imod__", MODULO);
        dict.put("__iadd__", ADD);
        dict.put("__isub__", SUBTRACT);
        dict.put("__ilshift__", LSHIFT);
        dict.put("__irshift__", RSHIFT);
        dict.put("__iand__", AND);
        dict.put("__ixor__", XOR);
        dict.put("__ior__", OR);
    }
}
