from .conftest import verifier_for


def test_inner_function():
    def my_function(x):
        def inner_function(y):
            return y * y

        return inner_function(x) + inner_function(x)

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=2)
    verifier.verify(2, expected_result=8)
    verifier.verify(3, expected_result=18)


def test_read_cell_variable():
    def my_function(x):
        def inner_function(y):
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
    def my_function(x):
        def inner_function(y):
            nonlocal x
            x += y

        inner_function(1)
        return x

    verifier = verifier_for(my_function)
    verifier.verify(1, expected_result=2)
    verifier.verify(2, expected_result=3)
    verifier.verify(3, expected_result=4)


def test_nested_cell_variable():
    def my_function(x):
        def inner_function_1():
            nonlocal x
            y = x * x

            def inner_function_2():
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
