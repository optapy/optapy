import javapython


def test_abs():
    def my_function(x):
        return abs(x)

    class MyClassWithAbs:
        def __abs__(self):
            return 10

    java_function = javapython.as_java(my_function)
    assert java_function(1) == my_function(1)
    assert java_function(-1) == my_function(-1)
    assert java_function(1.0) == my_function(1.0)
    assert java_function(-1.0) == my_function(-1.0)
    assert java_function(MyClassWithAbs()) == my_function(MyClassWithAbs())


def test_any():
    def my_function(x):
        return any(x)

    class MyClassWithoutBool:
        pass

    class MyClassWithBool:
        def __init__(self, is_true):
            self.is_true = is_true

        def __bool__(self):
            return self.is_true

    java_function = javapython.as_java(my_function)
    assert java_function([]) == my_function([])
    assert java_function([MyClassWithBool(False), None]) == my_function([False, 0, 0.0, MyClassWithBool(False), None])
    assert java_function([False, MyClassWithBool(True)]) == my_function([False, MyClassWithBool(True)])
    assert java_function([MyClassWithoutBool()]) == my_function([MyClassWithoutBool()])
    assert java_function([1]) == my_function([1])
    assert java_function([1.0]) == my_function([1.0])
    assert java_function([True]) == my_function([True])
