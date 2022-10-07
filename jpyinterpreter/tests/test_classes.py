from .conftest import verifier_for


def test_create_instance():
    class A:
        def __init__(self, value):
            self.value = value

        def __eq__(self, other):
            if not isinstance(other, A):
                return False
            return self.value == other.value

    verifier = verifier_for(lambda x: A(x))

    verifier.verify(1, expected_result=A(1))
    verifier.verify(2, expected_result=A(2))
    verifier.verify(3, expected_result=A(3))


def test_virtual_method():
    class A:
        def __init__(self, value):
            self.value = value

        def my_method(self, param):
            return self.value + param

    verifier = verifier_for(lambda x, y: x.my_method(y))

    verifier.verify(A(1), 1, expected_result=2)
    verifier.verify(A(1), 2, expected_result=3)
    verifier.verify(A(2), 1, expected_result=3)
    verifier.verify(A(2), 2, expected_result=4)


def test_static_method():
    class A:
        @staticmethod
        def my_method(param):
            return 1 + param

    verifier = verifier_for(lambda x, y: x.my_method(y))

    verifier.verify(A, 1, expected_result=2)
    verifier.verify(A, 2, expected_result=3)
    verifier.verify(A(), 3, expected_result=4)
    verifier.verify(A(), 4, expected_result=5)


def test_class_method():
    class A:
        @classmethod
        def my_method(cls, parameter):
            return cls.__name__ + parameter

    class B(A):
        pass

    verifier = verifier_for(lambda x, y: x.my_method(y))

    verifier.verify(A(), '1', expected_result='A1')
    verifier.verify(B(), '2', expected_result='B2')
    verifier.verify(A, '3', expected_result='A3')
    verifier.verify(B, '4', expected_result='B4')


def test_override_method():
    class A:
        def __init__(self, value):
            self.value = value

        def my_method(self):
            return self.value

    class B(A):
        def my_method(self):
            return self.value + 1

    verifier = verifier_for(lambda x: x.my_method())

    verifier.verify(A(1), expected_result=1)
    verifier.verify(B(1), expected_result=2)

    verifier.verify(A(2), expected_result=2)
    verifier.verify(B(2), expected_result=3)
