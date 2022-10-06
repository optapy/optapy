import jpyinterpreter


def test_list_comprehensions():
    def my_function(predicate, iterable):
        return [x for x in iterable if predicate(x)]

    def my_predicate(x):
        return x % 2 == 0

    java_function = jpyinterpreter.as_java(my_function)
    assert java_function(my_predicate, [1, 2, 3, 4]) == my_function(my_predicate, [1, 2, 3, 4])


def test_cell_variable_in_comprehensions():
    def my_function(items):
        return any(items[index] != index + 1 for index in range(len(items)))

    java_function = jpyinterpreter.as_java(my_function)
    assert java_function([0, 1, 2]) == my_function([0, 1, 2])
    assert java_function([0, 1, 3]) == my_function([0, 1, 3])
