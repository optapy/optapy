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


def test_virtual_keyword_arguments():
    class A:
        def helper(self, a: int, b: int) -> int:
            return a - b

    def my_function(a: int, b: int) -> int:
        return A().helper(b=b, a=a)

    verifier = verifier_for(my_function)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)


def test_virtual_default_arguments():
    class A:
        def helper(self, a: int, b: int = 1, c: str = '') -> int:
            return a - b - len(c)

    def my_function(a: int) -> int:
        return A().helper(a)

    verifier = verifier_for(my_function)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)


def test_virtual_vargs():
    class A:
        def helper(self, *items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(a: int, b: int, c: int) -> int:
        return A().helper(a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)


def test_virtual_kwargs():
    class A:
        def helper(self, **kwargs: int) -> frozenset:
            return frozenset(kwargs.items())

    def my_function(a: int, b: int) -> frozenset:
        return A().helper(first=a, second=b)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))


def test_virtual_unpack_iterable():
    class A:
        def helper(self, *items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(iterable: tuple) -> int:
        return A().helper(*iterable)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)


def test_virtual_unpack_keywords():
    class A:
        def helper(self, **kwargs: int) -> set:
            return set(kwargs.items())

    def my_function(items: dict) -> set:
        return A().helper(**items)

    verifier = verifier_for(my_function)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())


def test_virtual_unpack_iterable_and_keywords():
    class A:
        def helper(self, first: int, *positional: int, key: str, **keywords: str):
            return first, positional, key, keywords

    def my_function(items, keywords):
        return A().helper(*items, **keywords)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))


def test_virtual_default_with_vargs():
    class A:
        def helper(self, *items: int, start: int = 10) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(items):
        return A().helper(*items)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)


def test_virtual_vargs_with_manatory_args():
    class A:
        def helper(self, start: int, *items: int) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(a, b, c):
        return A().helper(10, a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)


def test_static_keyword_arguments():
    class A:
        @staticmethod
        def helper(a: int, b: int) -> int:
            return a - b

    def my_function(a: int, b: int) -> int:
        return A.helper(b=b, a=a)

    def my_function_instance(a: int, b: int) -> int:
        return A().helper(b=b, a=a)

    verifier = verifier_for(my_function)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)


def test_static_default_arguments():
    class A:
        @staticmethod
        def helper(a: int, b: int = 1, c: str = '') -> int:
            return a - b - len(c)

    def my_function(a: int) -> int:
        return A.helper(a)

    def my_function_instance(a: int) -> int:
        return A().helper(a)

    verifier = verifier_for(my_function)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)


def test_static_vargs():
    class A:
        @staticmethod
        def helper(*items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(a: int, b: int, c: int) -> int:
        return A.helper(a, b, c)

    def my_function_instance(a: int, b: int, c: int) -> int:
        return A().helper(a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)


def test_static_kwargs():
    class A:
        @staticmethod
        def helper(**kwargs: int) -> frozenset:
            return frozenset(kwargs.items())

    def my_function(a: int, b: int) -> frozenset:
        return A.helper(first=a, second=b)

    def my_function_instance(a: int, b: int) -> frozenset:
        return A().helper(first=a, second=b)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))


def test_static_unpack_iterable():
    class A:
        @staticmethod
        def helper(*items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(iterable: tuple) -> int:
        return A.helper(*iterable)

    def my_function_instance(iterable: tuple) -> int:
        return A().helper(*iterable)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)


def test_static_unpack_keywords():
    class A:
        @staticmethod
        def helper(**kwargs: int) -> set:
            return set(kwargs.items())

    def my_function(items: dict) -> set:
        return A.helper(**items)

    def my_function_instance(items: dict) -> set:
        return A().helper(**items)

    verifier = verifier_for(my_function)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())

    verifier = verifier_for(my_function_instance)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())


def test_static_unpack_iterable_and_keywords():
    class A:
        @staticmethod
        def helper(first: int, *positional: int, key: str, **keywords: str):
            return first, positional, key, keywords

    def my_function(items, keywords):
        return A.helper(*items, **keywords)

    def my_function_instance(items, keywords):
        return A().helper(*items, **keywords)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))
    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))


def test_static_default_with_vargs():
    class A:
        @staticmethod
        def helper(*items: int, start: int = 10) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(items):
        return A.helper(*items)

    def my_function_instance(items):
        return A().helper(*items)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)

    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)


def test_static_vargs_with_manatory_args():
    class A:
        @staticmethod
        def helper(start: int, *items: int) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(a, b, c):
        return A.helper(10, a, b, c)

    def my_function_instance(a, b, c):
        return A().helper(10, a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)


def test_class_keyword_arguments():
    class A:
        @classmethod
        def helper(cls: type, a: int, b: int) -> int:
            return a - b

    def my_function(a: int, b: int) -> int:
        return A.helper(b=b, a=a)

    def my_function_instance(a: int, b: int) -> int:
        return A().helper(b=b, a=a)

    verifier = verifier_for(my_function)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify(2, 1, expected_result=1)
    verifier.verify(1, 2, expected_result=-1)
    verifier.verify(1, 1, expected_result=0)


def test_class_default_arguments():
    class A:
        @classmethod
        def helper(cls: type, a: int, b: int = 1, c: str = '') -> int:
            return a - b - len(c)

    def my_function(a: int) -> int:
        return A.helper(a)

    def my_function_instance(a: int) -> int:
        return A().helper(a)

    verifier = verifier_for(my_function)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify(2, expected_result=1)
    verifier.verify(1, expected_result=0)


def test_class_vargs():
    class A:
        @classmethod
        def helper(cls: type, *items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(a: int, b: int, c: int) -> int:
        return A.helper(a, b, c)

    def my_function_instance(a: int, b: int, c: int) -> int:
        return A().helper(a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, 3, expected_result=6)
    verifier.verify(2, 4, 6, expected_result=12)
    verifier.verify(1, 1, 1, expected_result=3)


def test_class_kwargs():
    class A:
        @classmethod
        def helper(cls: type, **kwargs: int) -> frozenset:
            return frozenset(kwargs.items())

    def my_function(a: int, b: int) -> frozenset:
        return A.helper(first=a, second=b)

    def my_function_instance(a: int, b: int) -> frozenset:
        return A().helper(first=a, second=b)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, expected_result=frozenset({('first', 1), ('second', 2)}))
    verifier.verify(1, 1, expected_result=frozenset({('first', 1), ('second', 1)}))


def test_class_unpack_iterable():
    class A:
        @classmethod
        def helper(cls: type, *items: int) -> int:
            total = 0
            for i in items:
                total += i
            return total

    def my_function(iterable: tuple) -> int:
        return A.helper(*iterable)

    def my_function_instance(iterable: tuple) -> int:
        return A().helper(*iterable)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)

    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), expected_result=6)
    verifier.verify((2, 4), expected_result=6)
    verifier.verify((1,), expected_result=1)
    verifier.verify((1,), expected_result=1)
    verifier.verify((), expected_result=0)


def test_class_unpack_keywords():
    class A:
        @classmethod
        def helper(cls: type, **kwargs: int) -> set:
            return set(kwargs.items())

    def my_function(items: dict) -> set:
        return A.helper(**items)

    def my_function_instance(items: dict) -> set:
        return A().helper(**items)

    verifier = verifier_for(my_function)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())

    verifier = verifier_for(my_function_instance)

    verifier.verify({
        'first': 1,
        'second': 2
    }, expected_result={('first', 1), ('second', 2)})

    verifier.verify({
        'third': 3,
        'fourth': 3
    }, expected_result={('third', 3), ('fourth', 3)})

    verifier.verify({
        'alone': 0,
    }, expected_result={('alone', 0)})

    verifier.verify(dict(), expected_result=set())


def test_class_unpack_iterable_and_keywords():
    class A:
        @classmethod
        def helper(cls: type, first: int, *positional: int, key: str, **keywords: str):
            return first, positional, key, keywords

    def my_function(items, keywords):
        return A.helper(*items, **keywords)

    def my_function_instance(items, keywords):
        return A().helper(*items, **keywords)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))
    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), {'key': 'value', 'other': 'thing'}, expected_result=(1,
                                                                                    (2, 3),
                                                                                    'value',
                                                                                    {'other': 'thing'}))


def test_class_default_with_vargs():
    class A:
        @classmethod
        def helper(cls: type, *items: int, start: int = 10) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(items):
        return A.helper(*items)

    def my_function_instance(items):
        return A().helper(*items)

    verifier = verifier_for(my_function)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)

    verifier = verifier_for(my_function_instance)

    verifier.verify((1, 2, 3), expected_result=16)
    verifier.verify((1, 2), expected_result=13)


def test_class_vargs_with_manatory_args():
    class A:
        @classmethod
        def helper(cls: type, start: int, *items: int) -> int:
            total = start
            for item in items:
                total += item
            return total

    def my_function(a, b, c):
        return A.helper(10, a, b, c)

    def my_function_instance(a, b, c):
        return A().helper(10, a, b, c)

    verifier = verifier_for(my_function)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)

    verifier = verifier_for(my_function_instance)

    verifier.verify(1, 2, 3, expected_result=16)
    verifier.verify(2, 4, 6, expected_result=22)
    verifier.verify(1, 1, 1, expected_result=13)
