import pytest
from .conftest import verifier_for

def test_loop_generator():
    def my_function(x: int):
        i = 0
        while i < x:
            yield i
            i += 1

    def my_function_property(generator):
        for i in range(10):
            if next(generator) != i:
                return False
        with pytest.raises(StopIteration):
            next(generator)
        return True

    generator_verifier = verifier_for(my_function)
    generator_verifier.verify_property(10, predicate=my_function_property)
