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


def test_all():
    def my_function(x):
        return all(x)

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


def test_ascii():
    def my_function(x):
        return ascii(x)

    java_function = javapython.as_java(my_function)
    assert java_function(10) == my_function(10)
    assert java_function("text") == my_function("text")
    assert java_function("text\nwith\nlines") == my_function("text\nwith\nlines")


def test_bin():
    def my_function(x):
        return bin(x)

    java_function = javapython.as_java(my_function)
    assert java_function(10) == my_function(10)
    assert java_function(-10) == my_function(-10)


def test_bool():
    def my_function(x):
        return bool(x)

    java_function = javapython.as_java(my_function)
    assert java_function(0) == my_function(0)
    assert java_function(1) == my_function(1)
    assert java_function(-1) == my_function(-1)
    assert java_function("") == my_function("")
    assert java_function("test") == my_function("test")
    assert java_function(True) == my_function(True)
    assert java_function(False) == my_function(False)


def test_callable():
    def my_function(x):
        return callable(x)

    java_function = javapython.as_java(my_function)
    assert java_function(10) == my_function(10)
    assert java_function(my_function) == my_function(my_function)
    assert java_function(int) == my_function(int)


def test_chr():
    def my_function(x):
        return chr(x)

    java_function = javapython.as_java(my_function)
    assert java_function(30) == my_function(30)
    assert java_function(2400) == my_function(2400)


def test_delattr():
    def my_function(x):
        delattr(x, 'my_attr')
        return hasattr(x, 'my_attr')

    class TestObject:
        pass

    a = TestObject()
    b = TestObject()
    a.my_attr = 'test'
    b.my_attr = 'test'
    java_function = javapython.as_java(my_function)
    assert java_function(a) == my_function(b)


def test_divmod():
    def my_function(x, y):
        return divmod(x, y)

    java_function = javapython.as_java(my_function)
    assert java_function(16, 5) == my_function(16, 5)
    assert java_function(-16, 5) == my_function(-16, 5)
    assert java_function(16, -5) == my_function(16, -5)
    assert java_function(-16, -5) == my_function(-16, -5)

    assert java_function(16, 5.0) == my_function(16, 5.0)
    assert java_function(-16, 5.0) == my_function(-16, 5.0)
    assert java_function(16, -5.0) == my_function(16, -5.0)
    assert java_function(-16, -5.0) == my_function(-16, -5.0)

    assert java_function(16.0, 5.0) == my_function(16.0, 5.0)
    assert java_function(-16.0, 5.0) == my_function(-16.0, 5.0)
    assert java_function(16.0, -5.0) == my_function(16.0, -5.0)
    assert java_function(-16.0, -5.0) == my_function(-16.0, -5.0)


def test_dict():
    def my_function(x):
        out = dict()
        out['key'] = x
        return out

    java_function = javapython.as_java(my_function)
    assert java_function('value') == my_function('value')
    assert java_function(10) == my_function(10)


def test_enumerate():
    def my_function_without_start(x):
        return enumerate(x)

    def my_function_with_start(x, start):
        return enumerate(x, start)

    java_function = javapython.as_java(my_function_without_start)
    java_function_with_start = javapython.as_java(my_function_with_start)
    assert list(java_function(['a', 'b', 'c'])) == list(my_function_without_start(['a', 'b', 'c']))
    assert list(java_function_with_start(['a', 'b', 'c'], 5)) == list(my_function_with_start(['a', 'b', 'c'], 5))


def test_filter():
    def my_function(function, iterable):
        return filter(function, iterable)

    java_function = javapython.as_java(my_function)
    assert list(java_function(None, [0, 1, False, 2])) == list(my_function(None, [0, 1, False, 2]))
    assert list(java_function(lambda x: x == 2 or x is False, [0, 1, False, 2])) == \
           list(my_function(lambda x: x == 2 or x is False, [0, 1, False, 2]))


def test_float():
    def my_function(x):
        return float(x)

    java_function = javapython.as_java(my_function)
    assert java_function(10) == my_function(10)
    assert type(java_function(10)) == type(my_function(10))


def test_format():
    def my_function(x):
        return format(x)

    def my_function_with_spec(x, spec):
        return format(x, spec)

    java_function = javapython.as_java(my_function)
    java_function_with_spec = javapython.as_java(my_function_with_spec)

    assert java_function(10) == my_function(10)
    assert java_function_with_spec(10, '') == my_function_with_spec(10, '')


def test_getattr():
    def my_function(x, name):
        return getattr(x, name)

    def my_function_with_default(x, name, default):
        return getattr(x, name, default)

    class TestObject:
        pass

    java_function = javapython.as_java(my_function)
    java_function_with_default = javapython.as_java(my_function_with_default)

    a = TestObject()
    a.test = 'value'

    assert java_function(a, 'test') == my_function(a, 'test')
    assert java_function_with_default(a, 'missing', 10) == my_function_with_default(a, 'missing', 10)


global_variable = 10
def test_globals():
    def my_function():
        global global_variable
        x = global_variable
        return globals()

    java_function = javapython.as_java(my_function)
    # The globals directory in Java only stores used globals
    assert java_function()['global_variable'] == 10


def test_hasattr():
    def my_function(x, name):
        return hasattr(x, name)

    class TestObject:
        pass

    java_function = javapython.as_java(my_function)
    a = TestObject()
    a.test = 'value'
    assert java_function(a, 'test') == my_function(a, 'test')
    assert java_function(a, 'other') == my_function(a, 'other')


def test_hash():
    def my_function(x):
        return hash(x)

    java_function = javapython.as_java(my_function)
    assert java_function(1) == my_function(1)
    assert java_function(1.0) == my_function(1.0)
    assert java_function(True) == my_function(True)

def test_id():
    def my_function(x):
        return id(x)

    a = object()
    java_function = javapython.as_java(my_function)
    assert java_function(a) == my_function(a)


def test_int():
    def my_function(x):
        return int(x)

    java_function = javapython.as_java(my_function)
    assert java_function(1.5) == my_function(1.5)
    assert java_function(1.0) == my_function(1.0)
    assert type(java_function(1.0)) == type(my_function(1.0))


def test_isinstance():
    def my_function(x, y):
        return isinstance(x, y)

    java_function = javapython.as_java(my_function)
    assert java_function(1, int) == my_function(1, int)
    assert java_function(True, int) == my_function(True, int)
    assert java_function(1.0, int) == my_function(1.0, int)
    assert java_function(int, int) == my_function(int, int)
    assert java_function(1.0, (int, float)) == my_function(1.0, (int, float))


def test_issubclass():
    def my_function(x, y):
        return issubclass(x, y)

    java_function = javapython.as_java(my_function)
    assert java_function(int, int) == my_function(int, int)
    assert java_function(bool, int) == my_function(bool, int)
    assert java_function(int, bool) == my_function(int, bool)
    assert java_function(int, (float, int)) == my_function(int, (float, int))

