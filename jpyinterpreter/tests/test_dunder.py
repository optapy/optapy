from .conftest import verifier_for


def test_same_operand():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    verifier = verifier_for(lambda a, b: a - b)
    verifier.verify(A(3), A(2), expected_result=1)


def test_only_left_defined():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    class B:
        def __init__(self, value):
            self.value = value

    verifier = verifier_for(lambda a, b: a - b)
    verifier.verify(A(3), B(2), expected_result=1)


def test_only_right_defined():
    class A:
        def __init__(self, value):
            self.value = value

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return other.value - self.value

    verifier = verifier_for(lambda a, b: a - b)
    verifier.verify(A(3), B(2), expected_result=1)


def test_both_defined():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return self.value - other.value

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return self.value - other.value

    verifier = verifier_for(lambda a, b: a - b)
    verifier.verify(A(3), B(2), expected_result=1)


def test_neither_defined():
    class A:
        def __init__(self, value):
            self.value = value

    class B:
        def __init__(self, value):
            self.value = value

    verifier = verifier_for(lambda a, b: a - b)
    verifier.verify(A(3), B(2), expected_error=TypeError)


def test_left_return_not_implemented():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return NotImplemented

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return other.value - self.value

    verifier = verifier_for(lambda a, b: a - b)
    verifier.verify(A(3), B(2), expected_result=1)


def test_both_return_not_implemented():
    class A:
        def __init__(self, value):
            self.value = value

        def __sub__(self, other):
            return NotImplemented

    class B:
        def __init__(self, value):
            self.value = value

        def __rsub__(self, other):
            return NotImplemented

    verifier = verifier_for(lambda a, b: a - b)
    verifier.verify(A(3), B(2), expected_error=TypeError)


def test_inverted_comparisons():
    class A:
        def __init__(self, value):
            self.value = value

    class B:
        def __init__(self, value):
            self.value = value

        def __lt__(self, other):
            return self.value < other.value

        def __gt__(self, other):
            return self.value > other.value

        def __le__(self, other):
            return self.value <= other.value

        def __ge__(self, other):
            return self.value >= other.value

        def __eq__(self, other):
            return self.value == other.value

        def __ne__(self, other):
            return self.value != other.value

    verifier = verifier_for(lambda a, b: a < b)
    verifier.verify(A(1), B(2), expected_result=True)
    verifier.verify(A(1), B(1), expected_result=False)
    verifier.verify(A(2), B(1), expected_result=False)

    verifier = verifier_for(lambda a, b: a > b)
    verifier.verify(A(1), B(2), expected_result=False)
    verifier.verify(A(1), B(1), expected_result=False)
    verifier.verify(A(2), B(1), expected_result=True)

    verifier = verifier_for(lambda a, b: a <= b)
    verifier.verify(A(1), B(2), expected_result=True)
    verifier.verify(A(1), B(1), expected_result=True)
    verifier.verify(A(2), B(1), expected_result=False)

    verifier = verifier_for(lambda a, b: a >= b)
    verifier.verify(A(1), B(2), expected_result=False)
    verifier.verify(A(1), B(1), expected_result=True)
    verifier.verify(A(2), B(1), expected_result=True)

    verifier = verifier_for(lambda a, b: a == b)
    verifier.verify(A(1), B(2), expected_result=False)
    verifier.verify(A(1), B(1), expected_result=True)
    verifier.verify(A(2), B(1), expected_result=False)

    verifier = verifier_for(lambda a, b: a != b)
    verifier.verify(A(1), B(2), expected_result=True)
    verifier.verify(A(1), B(1), expected_result=False)
    verifier.verify(A(2), B(1), expected_result=True)
