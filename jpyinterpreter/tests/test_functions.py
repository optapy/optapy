from .conftest import verifier_for


def test_recursion():
    def fib(x: int) -> int:
        if x <= 1:
            return 1
        return fib(x - 1) + fib(x - 2)

    fib_verifier = verifier_for(fib)

    fib_verifier.verify(0, expected_result=1)
    fib_verifier.verify(1, expected_result=1)
    fib_verifier.verify(2, expected_result=2)
    fib_verifier.verify(3, expected_result=3)
    fib_verifier.verify(4, expected_result=5)
    fib_verifier.verify(5, expected_result=8)


def test_alternative_recursion():
    def is_even(x: int) -> bool:
        if x == 0:
            return True
        return is_odd(x - 1)

    def is_odd(x: int) -> bool:
        if x == 0:
            return False
        return is_even(x - 1)

    is_even_verifier = verifier_for(is_even)

    is_even_verifier.verify(0, expected_result=True)
    is_even_verifier.verify(1, expected_result=False)
    is_even_verifier.verify(2, expected_result=True)
    is_even_verifier.verify(3, expected_result=False)
    is_even_verifier.verify(4, expected_result=True)
    is_even_verifier.verify(5, expected_result=False)


def test_inner_function():
    def my_function(x: int) -> int:
        def inner_function(y: int) -> int:
            return y * y

        return inner_function(x) + inner_function(x)

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=2)
    verifier.verify(2, expected_result=8)
    verifier.verify(3, expected_result=18)


def test_read_cell_variable():
    def my_function(x: int) -> int:
        def inner_function(y: int) -> int:
            nonlocal x
            return x * y

        a = inner_function(2)
        x += 1
        b = inner_function(3)

        return a + b

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=8)
    verifier.verify(2, expected_result=13)
    verifier.verify(3, expected_result=18)


def test_modify_cell_variable():
    def my_function(x: int) -> int:
        def inner_function(y: int) -> None:
            nonlocal x
            x += y

        inner_function(1)
        return x

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=2)
    verifier.verify(2, expected_result=3)
    verifier.verify(3, expected_result=4)


def test_nested_cell_variable():
    def my_function(x: int) -> tuple:
        def inner_function_1() -> int:
            nonlocal x
            y = x * x

            def inner_function_2() -> int:
                nonlocal x
                nonlocal y
                out = x + y
                x = y
                return out

            return inner_function_2()

        return inner_function_1(), x

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=(2, 1))
    verifier.verify(2, expected_result=(6, 4))
    verifier.verify(3, expected_result=(12, 9))


def test_code_works_if_compilation_failed():
    def my_function(x: int) -> tuple:
        def inner_function(y: int) -> type:
            nonlocal x

            class MyClass:  # TODO: Replace this with something else that fails when class creation is supported
                def __init__(self):
                    self.outer_arg = x
                    self.inner_arg = y

                def get_args(self):
                    return self.outer_arg, self.inner_arg

            return MyClass

        return inner_function(2 * x)().get_args()

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=(1, 2))
    verifier.verify(2, expected_result=(2, 4))
    verifier.verify(3, expected_result=(3, 6))
