from typing import Type
from .conftest import verifier_for


def test_create_instance():
    class A:
        value: int

        def __init__(self, value):
            self.value = value

        def __eq__(self, other):
            if not isinstance(other, A):
                return False
            return self.value == other.value

    def create_instance(x: int) -> A:
        return A(x)

    verifier = verifier_for(create_instance)

    verifier.verify(1, expected_result=A(1))
    verifier.verify(2, expected_result=A(2))
    verifier.verify(3, expected_result=A(3))


def test_virtual_method():
    class A:
        value: int

        def __init__(self, value):
            self.value = value

        def my_method(self, param):
            return self.value + param

    def my_method(x: A, y: int) -> int:
        return x.my_method(y)

    verifier = verifier_for(my_method)

    verifier.verify(A(1), 1, expected_result=2)
    verifier.verify(A(1), 2, expected_result=3)
    verifier.verify(A(2), 1, expected_result=3)
    verifier.verify(A(2), 2, expected_result=4)


def test_static_method():
    class A:
        @staticmethod
        def my_method(param: int):
            return 1 + param

    def instance_my_method(x: A, y: int) -> int:
        return x.my_method(y)

    def static_my_method(x: Type[A], y: int) -> int:
        return x.my_method(y)

    instance_verifier = verifier_for(instance_my_method)
    static_verifier = verifier_for(static_my_method)

    static_verifier.verify(A, 1, expected_result=2)
    static_verifier.verify(A, 2, expected_result=3)
    instance_verifier.verify(A(), 3, expected_result=4)
    instance_verifier.verify(A(), 4, expected_result=5)


def test_class_method():
    class A:
        @classmethod
        def my_method(cls: type, parameter: str):
            return cls.__name__ + parameter

    class B(A):
        pass

    def instance_my_method(x: A, y: str) -> str:
        return x.my_method(y)

    def static_my_method(x: Type[A], y: str) -> str:
        return x.my_method(y)

    instance_verifier = verifier_for(instance_my_method)
    static_verifier = verifier_for(static_my_method)

    instance_verifier.verify(A(), '1', expected_result='A1')
    instance_verifier.verify(B(), '2', expected_result='B2')
    static_verifier.verify(A, '3', expected_result='A3')
    static_verifier.verify(B, '4', expected_result='B4')


def test_override_method():
    class A:
        def __init__(self, value):
            self.value = value

        def my_method(self):
            return self.value

    class B(A):
        def my_method(self):
            return self.value + 1

    def my_method(x: A) -> int:
        return x.my_method()

    verifier = verifier_for(my_method)

    verifier.verify(A(1), expected_result=1)
    verifier.verify(B(1), expected_result=2)

    verifier.verify(A(2), expected_result=2)
    verifier.verify(B(2), expected_result=3)


def test_simple_super_method():
    class A:
        def __init__(self, start):
            self.start = start

        def my_method(self, text):
            return self.start + text

    class B(A):
        def __init__(self, start, end):
            super().__init__(start)
            self.end = end

        def my_method(self, text):
            return super().my_method(text) + self.end

    def my_function(start: str, end: str, text: str) -> str:
        return B(start, end).my_method(text)

    verifier = verifier_for(my_function)

    verifier.verify('start', 'end', ' middle ', expected_result='start middle end')


def test_chained_super_method():
    class A:
        def __init__(self, start):
            self.start = start

        def my_method(self, text):
            return self.start + text

    class B(A):
        def __init__(self, start, end):
            super().__init__(start)
            self.end = end

        def my_method(self, text):
            return super().my_method(text) + self.end

    class C(B):
        def __init__(self, start, end):
            super().__init__(start, end)

        def my_method(self, text):
            return '[' + super().my_method(text) + ']'

    def my_function(start: str, end: str, text: str) -> str:
        return C(start, end).my_method(text)

    verifier = verifier_for(my_function)

    verifier.verify('start', 'end', ' middle ', expected_result='[start middle end]')
