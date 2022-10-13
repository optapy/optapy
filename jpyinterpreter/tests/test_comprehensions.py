from typing import Callable
from .conftest import verifier_for


def test_list_comprehensions():
    def my_function(predicate: Callable, iterable: list) -> list:
        return [x for x in iterable if predicate(x)]

    def my_predicate(x):
        return x % 2 == 0

    verifier = verifier_for(my_function)
    verifier.verify(my_predicate, [1, 2, 3, 4], expected_result=[2, 4])


def test_cell_variable_in_comprehensions():
    def my_function(items: list) -> bool:
        return any(items[index] == index + 1 for index in range(len(items)))

    verifier = verifier_for(my_function)
    verifier.verify([0, 1, 2], expected_result=False)
    verifier.verify([0, 1, 3], expected_result=True)
