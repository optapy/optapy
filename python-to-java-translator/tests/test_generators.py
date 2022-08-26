import javapython
import pytest


def test_loop_generator():
    def my_function(x):
        i = 0
        while i < x:
            yield i
            i += 1

    java_function = javapython.as_java(my_function)
    my_function_generator = my_function(10)
    java_function_generator = java_function(10)

    for i in range (10):
        assert next(java_function_generator) == next(my_function_generator)

    with pytest.raises(StopIteration):
        next(my_function_generator)

    with pytest.raises(StopIteration):
        next(java_function_generator)
