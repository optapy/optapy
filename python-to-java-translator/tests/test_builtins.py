import javapython
import pytest
import sys


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


def test_iter():
    def my_function(x):
        return iter(x)

    java_function = javapython.as_java(my_function)
    java_iter = java_function([1, 2, 3, 4])
    python_iter = my_function([1, 2, 3, 4])

    for item in python_iter:
        assert next(java_iter) == item

    with pytest.raises(StopIteration):
        next(java_iter)


def test_len():
    def my_function(x):
        return len(x)

    java_function = javapython.as_java(my_function)
    assert java_function([1, 2, 3]) == my_function([1, 2, 3])
    assert java_function((1, 2)) == my_function((1, 2))
    assert java_function({1}) == my_function({1})

    a_dict = {
        'a': 1,
        'b': 2,
        'c': 3
    }
    assert java_function(a_dict) == my_function(a_dict)


def test_list():
    def my_function(x):
        return list(x)

    def my_function_no_args():
        return list()

    java_function = javapython.as_java(my_function)
    java_function_no_args = javapython.as_java(my_function_no_args)

    assert java_function_no_args() == my_function_no_args()
    assert java_function((1, 2, 3)) == my_function((1, 2, 3))


def test_locals():
    def my_function():
        return locals()


    java_function = javapython.as_java(my_function)

    with pytest.raises(ValueError) as excinfo:
        java_function()

    assert 'builtin locals is not supported when executed in Java bytecode' == str(excinfo.value)


def test_map():
    def my_function(function, iterable):
        return map(function, iterable)

    def my_function_two_args(function, iterable1, iterable2):
        return map(function, iterable1, iterable2)

    java_function = javapython.as_java(my_function)
    java_function_two_args = javapython.as_java(my_function_two_args)

    assert list(java_function(lambda x: x + 1, [1, 2, 3])) == list(my_function(lambda x: x + 1, [1, 2, 3]))
    assert list(java_function_two_args(lambda x, y: x + y,
                                       [1, 2, 3],
                                       [2, 3, 4])) == \
           list(my_function_two_args(lambda x, y: x + y,
                                     [1, 2, 3],
                                     [2, 3, 4]))

    assert list(java_function_two_args(lambda x, y: x + y,
                                       [1, 2],
                                       [2, 3, 4])) == \
           list(my_function_two_args(lambda x, y: x + y,
                                     [1, 2],
                                     [2, 3, 4]))

    assert list(java_function_two_args(lambda x, y: x + y,
                                       [1, 2, 3],
                                       [2, 3])) == \
           list(my_function_two_args(lambda x, y: x + y,
                                     [1, 2, 3],
                                     [2, 3]))


def test_min():
    def my_function(x):
        return min(x)

    def my_function_two_args(x, y):
        return min(x, y)

    java_function = javapython.as_java(my_function)
    java_function_two_args = javapython.as_java(my_function_two_args)

    assert java_function([1, 2, 3]) == my_function([1, 2, 3])
    assert java_function([3, 2, 1]) == my_function([3, 2, 1])
    assert java_function_two_args(1, 2) == my_function_two_args(1, 2)
    assert java_function_two_args(2, 1) == my_function_two_args(2, 1)


def test_max():
    def my_function(x):
        return max(x)

    def my_function_two_args(x, y):
        return max(x, y)

    java_function = javapython.as_java(my_function)
    java_function_two_args = javapython.as_java(my_function_two_args)

    assert java_function([1, 2, 3]) == my_function([1, 2, 3])
    assert java_function([3, 2, 1]) == my_function([3, 2, 1])
    assert java_function_two_args(1, 2) == my_function_two_args(1, 2)
    assert java_function_two_args(2, 1) == my_function_two_args(2, 1)


def test_next():
    def my_function(x):
        i = iter(x)
        return next(i)

    java_function = javapython.as_java(my_function)
    assert java_function([1, 2, 3]) == my_function([1, 2, 3])


def test_object():
    def my_function(x):
        # cannot really do anything with a plain object
        return isinstance(x, object)

    java_function = javapython.as_java(my_function)
    assert java_function(1) == my_function(1)
    assert java_function('a') == my_function('a')
    assert java_function(int) == my_function(int)


def test_oct():
    def my_function(x):
        return oct(x)

    java_function = javapython.as_java(my_function)
    assert java_function(15) == my_function(15)
    assert java_function(-15) == my_function(-15)


def test_ord():
    def my_function(x):
        return ord(x)

    java_function = javapython.as_java(my_function)
    assert java_function('a') == my_function('a')
    assert java_function('\n') == my_function('\n')


def test_pow():
    def my_function(x, y):
        return pow(x, y)

    def my_function_with_mod(x, y, z):
        return pow(x, y, z)

    java_function = javapython.as_java(my_function)
    java_function_with_mod = javapython.as_java(my_function_with_mod)
    assert java_function(2, 3) == my_function(2, 3)
    assert java_function(2, -3) == my_function(2, -3)
    assert java_function_with_mod(2, 3, 3) == my_function_with_mod(2, 3, 3)
    assert java_function_with_mod(2, -1, 3) == my_function_with_mod(2, -1, 3)


def test_range():
    def my_function(x):
        return range(x)

    def my_function_with_start(start, stop):
        return range(start, stop)

    def my_function_with_start_and_step(start, stop, step):
        return range(start, stop, step)

    java_function = javapython.as_java(my_function)
    java_function_with_start = javapython.as_java(my_function_with_start)
    java_function_with_start_and_step = javapython.as_java(my_function_with_start_and_step)

    assert list(java_function(5)) == list(my_function(5))
    assert list(java_function_with_start(5, 10)) == list(my_function_with_start(5, 10))
    assert list(java_function_with_start_and_step(5, 10, 2)) == list(my_function_with_start_and_step(5, 10, 2))
    assert list(java_function_with_start_and_step(10, 5, -2)) == list(my_function_with_start_and_step(10, 5, -2))


def test_repr():
    def my_function(x):
        return repr(x)

    java_function = javapython.as_java(my_function)

    assert java_function(10) == my_function(10)
    assert java_function('a\nstring\nwith\nnew lines') == my_function('a\nstring\nwith\nnew lines')
    assert java_function([1, '2', 3]) == my_function([1, '2', 3])


def test_reversed():
    def my_function(x):
        return reversed(x)

    class TestClass:
        def __reversed__(self):
            yield from range(10, 1, -1)

    java_function = javapython.as_java(my_function)

    assert list(java_function([1, 2, 3])) == list(my_function([1, 2, 3]))
    assert list(java_function((1, 2, 3))) == list(my_function((1, 2, 3)))
    assert list(java_function(TestClass())) == list(my_function(TestClass()))


def test_round():
    def my_function(x):
        return round(x)

    def my_function_with_precision(x, y):
        return round(x, y)

    java_function = javapython.as_java(my_function)
    java_function_with_precision = javapython.as_java(my_function_with_precision)
    assert java_function(15) == my_function(15)
    assert java_function(1.5) == my_function(1.5)
    assert java_function(1.2) == my_function(1.2)
    assert java_function(1.7) == my_function(1.7)
    assert java_function(2.5) == my_function(2.5)

    assert java_function_with_precision(15, 0) == my_function_with_precision(15, 0)
    assert java_function_with_precision(15, 2) == my_function_with_precision(15, 2)
    assert java_function_with_precision(15, -1) == my_function_with_precision(15, -1)
    assert java_function_with_precision(25, -1) == my_function_with_precision(25, -1)

    assert java_function_with_precision(1.076, 2) == my_function_with_precision(1.076, 2)
    assert java_function_with_precision(1.024, 2) == my_function_with_precision(1.024, 2)
    assert java_function_with_precision(1.045, 2) == my_function_with_precision(1.045, 2)
    assert java_function_with_precision(1.055, 2) == my_function_with_precision(1.055, 2)

    assert java_function_with_precision(176.0, -1) == my_function_with_precision(176.0, -1)
    assert java_function_with_precision(124.0, -1) == my_function_with_precision(124.0, -1)
    assert java_function_with_precision(145.0, -1) == my_function_with_precision(145.0, -1)
    assert java_function_with_precision(155.0, -1) == my_function_with_precision(155.0, -1)


def test_set():
    def my_function():
        return set()

    def my_function_with_arg(x):
        return set(x)

    java_function = javapython.as_java(my_function)
    java_function_with_arg = javapython.as_java(my_function_with_arg)

    assert java_function() == my_function()
    assert java_function_with_arg([1, 2, 2, 3]) == my_function_with_arg([1, 2, 2, 3])


def test_setattr():
    def my_function(x, name, value):
        setattr(x, name, value)
        return getattr(x, name)

    class TestObject:
        pass

    java_function = javapython.as_java(my_function)

    a = TestObject()
    a.test = 'value 1'

    b = TestObject()
    b.test = 'value 1'

    assert java_function(a, 'test', 'value 2') == my_function(b, 'test', 'value 2')


def test_slice():
    def my_function(start, stop):
        return slice(start, stop)

    def my_function_with_step(start, stop, step):
        return slice(start, stop, step)

    class MyClassWithIndex:
        def __init__(self, index):
            self.index = index

        def __index__(self):
            return self.index

        def __eq__(self, other):
            return self.index == other.index

    java_function = javapython.as_java(my_function)
    java_function_with_step = javapython.as_java(my_function_with_step)

    assert java_function(1, 5) == my_function(1, 5)
    assert java_function(1, MyClassWithIndex(3)) == my_function(1, MyClassWithIndex(3))
    assert java_function_with_step(1, 5, 2) == my_function_with_step(1, 5, 2)


def test_sorted():
    def my_function(x):
        return sorted(x)

    def my_function_with_key(x, y):
        return sorted(x, key=y)

    def my_function_with_reverse(x, y):
        return sorted(x, reverse=y)

    def my_function_with_key_and_reverse(x, y, z):
        return sorted(x, key=y, reverse=z)

    java_function = javapython.as_java(my_function)
    java_function_with_key = javapython.as_java(my_function_with_key)
    java_function_with_reverse = javapython.as_java(my_function_with_reverse)
    java_function_with_key_and_reverse = javapython.as_java(my_function_with_key_and_reverse)

    assert java_function([2, 3, 1, 4]) == my_function([2, 3, 1, 4])
    assert java_function_with_key([2, 3, 1, 4], lambda x: -x) == my_function_with_key([2, 3, 1, 4], lambda x: -x)
    assert java_function_with_reverse([2, 3, 1, 4], True) == my_function_with_reverse([2, 3, 1, 4], True)
    assert java_function_with_key_and_reverse([2, 3, 1, 4], lambda x: -x, True) == \
           my_function_with_key_and_reverse([2, 3, 1, 4], lambda x: -x, True)


def test_str():
    def my_function(x):
        return str(x)

    class TestObject:
        def __str__(self):
            return 'A TestObject Instance'

    java_function = javapython.as_java(my_function)
    assert java_function(10) == my_function(10)
    assert java_function(1.0) == my_function(1.0)
    assert java_function('text') == my_function('text')
    assert java_function([1, '2', 3]) == my_function([1, '2', 3])

    a = TestObject()
    assert java_function(a) == my_function(a)


def test_sum():
    def my_function(x):
        return sum(x)

    def my_function_with_start(x, y):
        return sum(x, y)

    java_function = javapython.as_java(my_function)
    java_function_with_start = javapython.as_java(my_function_with_start)

    assert java_function([]) == my_function([])
    assert java_function([1, 3, 5]) == my_function([1, 3, 5])
    assert java_function([1.0, 3.0, 5.0]) == my_function([1.0, 3.0, 5.0])
    assert java_function_with_start([], 1.5) == my_function_with_start([], 1.5)
    assert java_function_with_start([1, 3, 5], 1.5) == my_function_with_start([1, 3, 5], 1.5)
    assert java_function_with_start([1.0, 3.0, 5.0], 1.5) == my_function_with_start([1.0, 3.0, 5.0], 1.5)


def test_tuple():
    def my_function():
        return tuple()

    def my_function_with_arg(x):
        return tuple(x)

    java_function = javapython.as_java(my_function)
    java_function_with_arg = javapython.as_java(my_function_with_arg)

    assert java_function() == my_function()
    assert java_function_with_arg([1, 2, 2, 3]) == my_function_with_arg([1, 2, 2, 3])


def test_type():
    def my_function(x):
        return type(x)

    class MyObject:
        pass

    java_function = javapython.as_java(my_function)

    assert java_function(10) == my_function(10)
    assert java_function('text') == my_function('text')
    assert java_function([1, 2]) == my_function([1, 2])
    assert java_function(MyObject()) == my_function(MyObject())
    assert java_function(MyObject) == my_function(MyObject)


def test_zip():
    def my_function(iterables):
        return zip(*iterables)

    def my_function_strict(iterables, is_strict):
        return zip(*iterables, strict=is_strict)

    java_function = javapython.as_java(my_function)
    java_function_strict = javapython.as_java(my_function_strict)

    assert list(java_function([])) == list(my_function([]))
    assert list(java_function(((1, 2, 3),))) == list(my_function(((1, 2, 3),)))
    assert list(java_function(((1, 2, 3), (4, 5, 6)))) == list(my_function(((1, 2, 3), (4, 5, 6))))
    assert list(java_function(((1, 2, 3), (4, 5)))) == list(my_function(((1, 2, 3), (4, 5))))
    assert list(java_function(((1, 2), (4, 5, 6)))) == list(my_function(((1, 2), (4, 5, 6))))

    if sys.version_info >= (3, 10):
        assert list(java_function_strict(((1, 2, 3),), True)) == list(my_function_strict(((1, 2, 3),), True))
        assert list(java_function_strict(((1, 2, 3), (4, 5, 6)), True)) == list(my_function_strict(((1, 2, 3), (4, 5, 6)), True))

        with pytest.raises(ValueError):
            try:
                list(java_function_strict(((1, 2, 3), (4, 5)), True))
            except Exception as e:
                raise javapython.unwrap_python_like_object(e)

        with pytest.raises(ValueError):
            try:
                list(java_function_strict(((1, 2), (4, 5, 6)), True))
            except Exception as e:
                raise javapython.unwrap_python_like_object(e)
