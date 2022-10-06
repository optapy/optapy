import jpyinterpreter


def my_function(sequence, x, y):
    return sequence[x:y]


def my_function_with_step(sequence, x, y, z):
    return sequence[x:y:z]


java_function = jpyinterpreter.as_java(my_function)
java_function_with_step = jpyinterpreter.as_java(my_function_with_step)


def test_list_slice():
    assert java_function([2, 4, 6, 8, 10], 1, 3) == my_function([2, 4, 6, 8, 10], 1, 3)
    assert java_function([2, 4, 6, 8, 10], 2, -1) == my_function([2, 4, 6, 8, 10], 2, -1)
    assert java_function([2, 4, 6, 8, 10], -3, 3) == my_function([2, 4, 6, 8, 10], -3, 3)
    assert java_function_with_step([2, 4, 6, 8, 10], 0, 3, 2) == my_function_with_step([2, 4, 6, 8, 10], 0, 3, 2)
    assert java_function_with_step([2, 4, 6, 8, 10], -1, -3, -1) == my_function_with_step([2, 4, 6, 8, 10], -1, -3, -1)


def test_tuple_slice():
    assert java_function((2, 4, 6, 8, 10), 1, 3) == my_function((2, 4, 6, 8, 10), 1, 3)
    assert java_function((2, 4, 6, 8, 10), 2, -1) == my_function((2, 4, 6, 8, 10), 2, -1)
    assert java_function((2, 4, 6, 8, 10), -3, 3) == my_function((2, 4, 6, 8, 10), -3, 3)
    assert java_function_with_step((2, 4, 6, 8, 10), 0, 3, 2) == my_function_with_step((2, 4, 6, 8, 10), 0, 3, 2)
    assert java_function_with_step((2, 4, 6, 8, 10), -1, -3, -1) == my_function_with_step((2, 4, 6, 8, 10), -1, -3, -1)


def test_str_slice():
    assert java_function('abcde', 1, 3) == my_function('abcde', 1, 3)
    assert java_function('abcde', 2, -1) == my_function('abcde', 2, -1)
    assert java_function('abcde', -3, 3) == my_function('abcde', -3, 3)
    assert java_function_with_step('abcde', 0, 3, 2) == my_function_with_step('abcde', 0, 3, 2)
    assert java_function_with_step('abcde', -1, -3, -1) == my_function_with_step('abcde', -1, -3, -1)

