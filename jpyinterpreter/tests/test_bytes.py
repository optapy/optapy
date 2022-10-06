from .conftest import verifier_for


########################################
# Sequence methods
########################################

def test_membership():
    membership_verifier = verifier_for(lambda tested, x: x in tested)
    not_membership_verifier = verifier_for(lambda tested, x: x not in tested)

    membership_verifier.verify(b'hello world', b'world', expected_result=True)
    not_membership_verifier.verify(b'hello world', b'world', expected_result=False)

    membership_verifier.verify(b'hello world', b'test', expected_result=False)
    not_membership_verifier.verify(b'hello world', b'test', expected_result=True)

    membership_verifier.verify(b'hello world', b'', expected_result=True)
    not_membership_verifier.verify(b'hello world', b'', expected_result=False)


def test_concat():
    def concat(x, y):
        out = x + y
        return out, out is x, out is y

    concat_verifier = verifier_for(concat)

    concat_verifier.verify(b'hello ', b'world', expected_result=(b'hello world', False, False))
    concat_verifier.verify(b'', b'hello world', expected_result=(b'hello world', False, True))
    concat_verifier.verify(b'hello world', b'', expected_result=(b'hello world', True, False))
    concat_verifier.verify(b'world ', b'hello', expected_result=(b'world hello', False, False))


def test_repeat():
    def repeat(x, y):
        out = x * y
        return out, out is x, out is y

    repeat_verifier = verifier_for(repeat)

    repeat_verifier.verify(b'hi', 1, expected_result=(b'hi', True, False))
    repeat_verifier.verify(b'abc', 2, expected_result=(b'abcabc', False, False))
    repeat_verifier.verify(b'a', 4, expected_result=(b'aaaa', False, False))
    repeat_verifier.verify(b'test', 0, expected_result=(b'', False, False))
    repeat_verifier.verify(b'test', -1, expected_result=(b'', False, False))
    repeat_verifier.verify(b'test', -2, expected_result=(b'', False, False))

    repeat_verifier.verify(1, b'hi', expected_result=(b'hi', False, True))
    repeat_verifier.verify(2, b'abc', expected_result=(b'abcabc', False, False))
    repeat_verifier.verify(4, b'a', expected_result=(b'aaaa', False, False))
    repeat_verifier.verify(0, b'test', expected_result=(b'', False, False))
    repeat_verifier.verify(-1, b'test', expected_result=(b'', False, False))
    repeat_verifier.verify(-2, b'test', expected_result=(b'', False, False))


def test_get_item():
    get_item_verifier = verifier_for(lambda tested, index: tested[index])

    get_item_verifier.verify(b'abc', 1, expected_result=ord(b'b'))
    get_item_verifier.verify(b'abc', -1, expected_result=ord(b'c'))
    get_item_verifier.verify(b'abcd', -1, expected_result=ord(b'd'))
    get_item_verifier.verify(b'abcd', -2, expected_result=ord(b'c'))
    get_item_verifier.verify(b'abcd', 0, expected_result=ord(b'a'))
    get_item_verifier.verify(b'abc', 3, expected_error=IndexError)
    get_item_verifier.verify(b'abc', -4, expected_error=IndexError)


def test_get_slice():
    get_slice_verifier = verifier_for(lambda tested, start, end: tested[start:end])

    get_slice_verifier.verify(b'abcde', 1, 3, expected_result=b'bc')
    get_slice_verifier.verify(b'abcde', -3, -1, expected_result=b'cd')

    get_slice_verifier.verify(b'abcde', 0, -2, expected_result=b'abc')
    get_slice_verifier.verify(b'abcde', -3, 4, expected_result=b'cd')

    get_slice_verifier.verify(b'abcde', 3, 1, expected_result=b'')
    get_slice_verifier.verify(b'abcde', -1, -3, expected_result=b'')

    get_slice_verifier.verify(b'abcde', 100, 1000, expected_result=b'')
    get_slice_verifier.verify(b'abcde', 0, 1000, expected_result=b'abcde')

    get_slice_verifier.verify(b'abcde', 1, None, expected_result=b'bcde')
    get_slice_verifier.verify(b'abcde', None, 2, expected_result=b'ab')
    get_slice_verifier.verify(b'abcde', None, None, expected_result=b'abcde')


def test_get_slice_with_step():
    get_slice_verifier = verifier_for(lambda tested, start, end, step: tested[start:end:step])

    get_slice_verifier.verify(b'abcde', 0, None, 2, expected_result=b'ace')
    get_slice_verifier.verify(b'abcde', 1, None, 2, expected_result=b'bd')
    get_slice_verifier.verify(b'abcde', 0, 5, 2, expected_result=b'ace')
    get_slice_verifier.verify(b'abcde', 1, 5, 2, expected_result=b'bd')
    get_slice_verifier.verify(b'abcde', 0, -1, 2, expected_result=b'ac')
    get_slice_verifier.verify(b'abcde', 1, -1, 2, expected_result=b'bd')

    get_slice_verifier.verify(b'abcde', 4, None, -2, expected_result=b'eca')
    get_slice_verifier.verify(b'abcde', 3, None, -2, expected_result=b'db')
    get_slice_verifier.verify(b'abcde', -1, -6, -2, expected_result=b'eca')
    get_slice_verifier.verify(b'abcde', -2, -6, -2, expected_result=b'db')
    get_slice_verifier.verify(b'abcde', 4, 0, -2, expected_result=b'ec')
    get_slice_verifier.verify(b'abcde', 3, 0, -2, expected_result=b'db')

    get_slice_verifier.verify(b'abcde', 0, None, None, expected_result=b'abcde')
    get_slice_verifier.verify(b'abcde', 0, 3, None, expected_result=b'abc')

    get_slice_verifier.verify(b'abcde', 3, 1, -1, expected_result=b'dc')
    get_slice_verifier.verify(b'abcde', -1, -3, -1, expected_result=b'ed')
    get_slice_verifier.verify(b'abcde', 3, 1, 1, expected_result=b'')
    get_slice_verifier.verify(b'abcde', -1, -3, 1, expected_result=b'')


def test_len():
    len_verifier = verifier_for(lambda tested: len(tested))

    len_verifier.verify(b'', expected_result=0)
    len_verifier.verify(b'a', expected_result=1)
    len_verifier.verify(b'ab', expected_result=2)
    len_verifier.verify(b'cba', expected_result=3)


def test_index():
    index_verifier = verifier_for(lambda tested, item: tested.index(item))
    index_start_verifier = verifier_for(lambda tested, item, start: tested.index(item, start))
    index_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.index(item, start, end))

    index_verifier.verify(b'abcabc', b'a', expected_result=0)
    index_verifier.verify(b'abcabc', b'b', expected_result=1)
    index_verifier.verify(b'abcabc', b'd', expected_error=ValueError)

    index_start_verifier.verify(b'abcabc', b'a', 1, expected_result=3)
    index_start_verifier.verify(b'abcabc', b'a', 5, expected_error=ValueError)
    index_start_verifier.verify(b'abcabc', b'b', 1, expected_result=1)
    index_start_verifier.verify(b'abcabc', b'c', 1, expected_result=2)
    index_start_verifier.verify(b'abcabc', b'd', 1, expected_error=ValueError)

    index_start_verifier.verify(b'abcabc', b'a', -3, expected_result=3)
    index_start_verifier.verify(b'abcabc', b'b', -2, expected_result=4)
    index_start_verifier.verify(b'abcabc', b'c', -2, expected_result=5)
    index_start_verifier.verify(b'abcabc', b'd', -2, expected_error=ValueError)

    index_start_end_verifier.verify(b'abcabc', b'a', 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify(b'abcabc', b'b', 1, 2, expected_result=1)
    index_start_end_verifier.verify(b'abcabc', b'c', 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify(b'abcabc', b'd', 1, 2, expected_error=ValueError)

    index_start_end_verifier.verify(b'abcabc', b'a', -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify(b'abcabc', b'b', -2, -1, expected_result=4)
    index_start_end_verifier.verify(b'abcabc', b'c', -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify(b'abcabc', b'd', -2, -1, expected_error=ValueError)


def test_count():
    count_verifier = verifier_for(lambda tested, item: tested.count(item))

    count_verifier.verify(b'abc', b'a', expected_result=1)
    count_verifier.verify(b'abc', b'b', expected_result=1)
    count_verifier.verify(b'abc', b'c', expected_result=1)
    count_verifier.verify(b'abc', b'd', expected_result=0)

    count_verifier.verify(b'abca', b'a', expected_result=2)
    count_verifier.verify(b'aaca', b'a', expected_result=3)
    count_verifier.verify(b'', b'a', expected_result=0)


########################################
# Bytes operations
########################################
def test_interpolation():
    interpolation_verifier = verifier_for(lambda tested, values: tested % values)

    interpolation_verifier.verify(b'%d', 100, expected_result=b'100')
    interpolation_verifier.verify(b'%d', 0b1111, expected_result=b'15')
    interpolation_verifier.verify(b'%s', b'foo', expected_result=b'foo')
    interpolation_verifier.verify(b'%s %s', (b'foo', b'bar'), expected_result=b'foo bar')
    interpolation_verifier.verify(b'%(foo)s', {b'foo': b'10', b'bar': b'20'}, expected_result=b'10')

    interpolation_verifier.verify(b'%d', 101, expected_result=b'101')
    interpolation_verifier.verify(b'%i', 101, expected_result=b'101')

    interpolation_verifier.verify(b'%o', 27, expected_result=b'33')
    interpolation_verifier.verify(b'%#o', 27, expected_result=b'0o33')

    interpolation_verifier.verify(b'%x', 27, expected_result=b'1b')
    interpolation_verifier.verify(b'%X', 27, expected_result=b'1B')
    interpolation_verifier.verify(b'%#x', 27, expected_result=b'0x1b')
    interpolation_verifier.verify(b'%#X', 27, expected_result=b'0X1B')

    interpolation_verifier.verify(b'%03d', 1, expected_result=b'001')
    interpolation_verifier.verify(b'%-5d', 1, expected_result=b'1    ')
    interpolation_verifier.verify(b'%0-5d', 1, expected_result=b'1    ')

    interpolation_verifier.verify(b'%d', 1, expected_result=b'1')
    interpolation_verifier.verify(b'%d', -1, expected_result=b'-1')
    interpolation_verifier.verify(b'% d', 1, expected_result=b' 1')
    interpolation_verifier.verify(b'% d', -1, expected_result=b'-1')
    interpolation_verifier.verify(b'%+d', 1, expected_result=b'+1')
    interpolation_verifier.verify(b'%+d', -1, expected_result=b'-1')

    interpolation_verifier.verify(b'%f', 3.14, expected_result=b'3.140000')
    interpolation_verifier.verify(b'%F', 3.14, expected_result=b'3.140000')
    interpolation_verifier.verify(b'%.1f', 3.14, expected_result=b'3.1')
    interpolation_verifier.verify(b'%.2f', 3.14, expected_result=b'3.14')
    interpolation_verifier.verify(b'%.3f', 3.14, expected_result=b'3.140')

    interpolation_verifier.verify(b'%g', 1234567890, expected_result=b'1.23457e+09')
    interpolation_verifier.verify(b'%G', 1234567890, expected_result=b'1.23457E+09')
    interpolation_verifier.verify(b'%e', 1234567890, expected_result=b'1.234568e+09')
    interpolation_verifier.verify(b'%E', 1234567890, expected_result=b'1.234568E+09')

    interpolation_verifier.verify(b'ABC %c', 10, expected_result=b'ABC \n')
    interpolation_verifier.verify(b'ABC %c', 67, expected_result=b'ABC C')
    interpolation_verifier.verify(b'ABC %c', 68, expected_result=b'ABC D')
    interpolation_verifier.verify(b'ABC %c', b'D', expected_result=b'ABC D')
    interpolation_verifier.verify(b'ABC %s', b'test', expected_result=b'ABC test')
    interpolation_verifier.verify(b'ABC %r', b'test', expected_result=b'ABC b\'test\'')

    interpolation_verifier.verify(b'Give it %d%%!', 100, expected_result=b'Give it 100%!')
    interpolation_verifier.verify(b'Give it %(all-you-got)d%%!', {b'all-you-got': 100},
                                  expected_result=b'Give it 100%!')


########################################
# String methods
########################################


def test_capitalize():
    capitalize_verifier = verifier_for(lambda tested: tested.capitalize())

    capitalize_verifier.verify(b'', expected_result=b'')
    capitalize_verifier.verify(b'test', expected_result=b'Test')
    capitalize_verifier.verify(b'TEST', expected_result=b'Test')
    capitalize_verifier.verify(b'hello world', expected_result=b'Hello world')
    capitalize_verifier.verify(b'Hello World', expected_result=b'Hello world')
    capitalize_verifier.verify(b'HELLO WORLD', expected_result=b'Hello world')


def test_center():
    center_verifier = verifier_for(lambda tested, width: tested.center(width))
    center_with_fill_verifier = verifier_for(lambda tested, width, fill: tested.center(width, fill))

    center_verifier.verify(b'test', 10, expected_result=b'   test   ')
    center_verifier.verify(b'test', 9, expected_result=b'   test  ')
    center_verifier.verify(b'test', 4, expected_result=b'test')
    center_verifier.verify(b'test', 2, expected_result=b'test')

    center_with_fill_verifier.verify(b'test', 10, b'#', expected_result=b'###test###')
    center_with_fill_verifier.verify(b'test', 9, b'#', expected_result=b'###test##')
    center_with_fill_verifier.verify(b'test', 4, b'#', expected_result=b'test')
    center_with_fill_verifier.verify(b'test', 2, b'#', expected_result=b'test')


def test_count_byte():
    count_verifier = verifier_for(lambda tested, item: tested.count(item))
    count_from_start_verifier = verifier_for(lambda tested, item, start: tested.count(item, start))
    count_between_verifier = verifier_for(lambda tested, item, start, end: tested.count(item, start, end))

    count_verifier.verify(b'abc', ord(b'a'), expected_result=1)
    count_verifier.verify(b'abc', ord(b'b'), expected_result=1)
    count_verifier.verify(b'abc', ord(b'c'), expected_result=1)
    count_verifier.verify(b'abc', ord(b'd'), expected_result=0)

    count_verifier.verify(b'abca', ord(b'a'), expected_result=2)
    count_verifier.verify(b'aaca', ord(b'a'), expected_result=3)
    count_verifier.verify(b'', ord(b'a'), expected_result=0)

    count_from_start_verifier.verify(b'abc', ord(b'a'), 1, expected_result=0)
    count_from_start_verifier.verify(b'abc', ord(b'b'), 1, expected_result=1)
    count_from_start_verifier.verify(b'abc', ord(b'c'), 1, expected_result=1)
    count_from_start_verifier.verify(b'abc', ord(b'd'), 1, expected_result=0)

    count_from_start_verifier.verify(b'abca', ord(b'a'), 1, expected_result=1)
    count_from_start_verifier.verify(b'aaca', ord(b'a'), 1, expected_result=2)
    count_from_start_verifier.verify(b'', ord(b'a'), 1, expected_result=0)

    count_between_verifier.verify(b'abc', ord(b'a'), 1, 2, expected_result=0)
    count_between_verifier.verify(b'abc', ord(b'b'), 1, 2, expected_result=1)
    count_between_verifier.verify(b'abc', ord(b'c'), 1, 2, expected_result=0)
    count_between_verifier.verify(b'abc', ord(b'd'), 1, 2, expected_result=0)

    count_between_verifier.verify(b'abca', ord(b'a'), 1, 2, expected_result=0)
    count_between_verifier.verify(b'abca', ord(b'a'), 1, 4, expected_result=1)
    count_between_verifier.verify(b'abca', ord(b'a'), 0, 2, expected_result=1)
    count_between_verifier.verify(b'aaca', ord(b'a'), 1, 2, expected_result=1)
    count_between_verifier.verify(b'', ord(b'a'), 1, 2, expected_result=0)


def test_endswith():
    endswith_verifier = verifier_for(lambda tested, suffix: tested.endswith(suffix))
    endswith_start_verifier = verifier_for(lambda tested, suffix, start: tested.endswith(suffix, start))
    endswith_between_verifier = verifier_for(lambda tested, suffix, start, end: tested.endswith(suffix, start, end))

    endswith_verifier.verify(b'hello world', b'world', expected_result=True)
    endswith_verifier.verify(b'hello world', b'hello', expected_result=False)
    endswith_verifier.verify(b'hello', b'hello world', expected_result=False)
    endswith_verifier.verify(b'hello world', b'hello world', expected_result=True)

    endswith_start_verifier.verify(b'hello world', b'world', 6, expected_result=True)
    endswith_start_verifier.verify(b'hello world', b'hello', 6, expected_result=False)
    endswith_start_verifier.verify(b'hello', b'hello world', 6, expected_result=False)
    endswith_start_verifier.verify(b'hello world', b'hello world', 6, expected_result=False)

    endswith_between_verifier.verify(b'hello world', b'world', 6, 11, expected_result=True)
    endswith_between_verifier.verify(b'hello world', b'world', 7, 11, expected_result=False)
    endswith_between_verifier.verify(b'hello world', b'hello', 0, 5, expected_result=True)
    endswith_between_verifier.verify(b'hello', b'hello world', 0, 5, expected_result=False)
    endswith_between_verifier.verify(b'hello world', b'hello world', 5, 11, expected_result=False)


def test_expandtabs():
    expandtabs_verifier = verifier_for(lambda tested: tested.expandtabs())
    expandtabs_with_tabsize_verifier = verifier_for(lambda tested, tabsize: tested.expandtabs(tabsize))

    expandtabs_verifier.verify(b'01\t012\t0123\t01234', expected_result=b'01      012     0123    01234')
    expandtabs_with_tabsize_verifier.verify(b'01\t012\t0123\t01234', 8, expected_result=b'01      012     0123    01234')
    expandtabs_with_tabsize_verifier.verify(b'01\t012\t0123\t01234', 4, expected_result=b'01  012 0123    01234')


def test_find():
    find_verifier = verifier_for(lambda tested, item: tested.find(item))
    find_start_verifier = verifier_for(lambda tested, item, start: tested.find(item, start))
    find_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.find(item, start, end))

    find_verifier.verify(b'abcabc', b'a', expected_result=0)
    find_verifier.verify(b'abcabc', b'b', expected_result=1)
    find_verifier.verify(b'abcabc', b'd', expected_result=-1)

    find_start_verifier.verify(b'abcabc', b'a', 1, expected_result=3)
    find_start_verifier.verify(b'abcabc', b'a', 5, expected_result=-1)
    find_start_verifier.verify(b'abcabc', b'b', 1, expected_result=1)
    find_start_verifier.verify(b'abcabc', b'c', 1, expected_result=2)
    find_start_verifier.verify(b'abcabc', b'd', 1, expected_result=-1)

    find_start_verifier.verify(b'abcabc', b'a', -3, expected_result=3)
    find_start_verifier.verify(b'abcabc', b'b', -2, expected_result=4)
    find_start_verifier.verify(b'abcabc', b'c', -2, expected_result=5)
    find_start_verifier.verify(b'abcabc', b'd', -2, expected_result=-1)

    find_start_end_verifier.verify(b'abcabc', b'a', 1, 2, expected_result=-1)
    find_start_end_verifier.verify(b'abcabc', b'b', 1, 2, expected_result=1)
    find_start_end_verifier.verify(b'abcabc', b'c', 1, 2, expected_result=-1)
    find_start_end_verifier.verify(b'abcabc', b'd', 1, 2, expected_result=-1)

    find_start_end_verifier.verify(b'abcabc', b'a', -2, -1, expected_result=-1)
    find_start_end_verifier.verify(b'abcabc', b'b', -2, -1, expected_result=4)
    find_start_end_verifier.verify(b'abcabc', b'c', -2, -1, expected_result=-1)
    find_start_end_verifier.verify(b'abcabc', b'd', -2, -1, expected_result=-1)


def test_isalnum():
    isalnum_verifier = verifier_for(lambda tested: tested.isalnum())

    isalnum_verifier.verify(b'', expected_result=False)
    isalnum_verifier.verify(b'abc', expected_result=True)
    isalnum_verifier.verify(b'ABC', expected_result=True)
    isalnum_verifier.verify(b'123', expected_result=True)
    isalnum_verifier.verify(b'ABC123', expected_result=True)
    isalnum_verifier.verify(b'+', expected_result=False)
    isalnum_verifier.verify(b'[]', expected_result=False)
    isalnum_verifier.verify(b'-', expected_result=False)
    isalnum_verifier.verify(b'%', expected_result=False)
    isalnum_verifier.verify(b'\n', expected_result=False)
    isalnum_verifier.verify(b'\t', expected_result=False)
    isalnum_verifier.verify(b' ', expected_result=False)


def test_isalpha():
    isalpha_verifier = verifier_for(lambda tested: tested.isalpha())

    isalpha_verifier.verify(b'', expected_result=False)
    isalpha_verifier.verify(b'abc', expected_result=True)
    isalpha_verifier.verify(b'ABC', expected_result=True)
    isalpha_verifier.verify(b'123', expected_result=False)
    isalpha_verifier.verify(b'ABC123', expected_result=False)
    isalpha_verifier.verify(b'+', expected_result=False)
    isalpha_verifier.verify(b'[]', expected_result=False)
    isalpha_verifier.verify(b'-', expected_result=False)
    isalpha_verifier.verify(b'%', expected_result=False)
    isalpha_verifier.verify(b'\n', expected_result=False)
    isalpha_verifier.verify(b'\t', expected_result=False)
    isalpha_verifier.verify(b' ', expected_result=False)


def test_isascii():
    isascii_verifier = verifier_for(lambda tested: tested.isascii())

    isascii_verifier.verify(b'', expected_result=True)
    isascii_verifier.verify(b'abc', expected_result=True)
    isascii_verifier.verify(b'ABC', expected_result=True)
    isascii_verifier.verify(b'123', expected_result=True)
    isascii_verifier.verify(b'ABC123', expected_result=True)
    isascii_verifier.verify(b'+', expected_result=True)
    isascii_verifier.verify(b'[]', expected_result=True)
    isascii_verifier.verify(b'-', expected_result=True)
    isascii_verifier.verify(b'%', expected_result=True)
    isascii_verifier.verify(b'\n', expected_result=True)
    isascii_verifier.verify(b'\t', expected_result=True)
    isascii_verifier.verify(b' ', expected_result=True)


def test_isdigit():
    isdigit_verifier = verifier_for(lambda tested: tested.isdigit())

    isdigit_verifier.verify(b'', expected_result=False)
    isdigit_verifier.verify(b'abc', expected_result=False)
    isdigit_verifier.verify(b'ABC', expected_result=False)
    isdigit_verifier.verify(b'123', expected_result=True)
    isdigit_verifier.verify(b'ABC123', expected_result=False)
    isdigit_verifier.verify(b'+', expected_result=False)
    isdigit_verifier.verify(b'[]', expected_result=False)
    isdigit_verifier.verify(b'-', expected_result=False)
    isdigit_verifier.verify(b'%', expected_result=False)
    isdigit_verifier.verify(b'\n', expected_result=False)
    isdigit_verifier.verify(b'\t', expected_result=False)
    isdigit_verifier.verify(b' ', expected_result=False)


def test_islower():
    islower_verifier = verifier_for(lambda tested: tested.islower())

    islower_verifier.verify(b'', expected_result=False)
    islower_verifier.verify(b'abc', expected_result=True)
    islower_verifier.verify(b'ABC', expected_result=False)
    islower_verifier.verify(b'123', expected_result=False)
    islower_verifier.verify(b'ABC123', expected_result=False)
    islower_verifier.verify(b'+', expected_result=False)
    islower_verifier.verify(b'[]', expected_result=False)
    islower_verifier.verify(b'-', expected_result=False)
    islower_verifier.verify(b'%', expected_result=False)
    islower_verifier.verify(b'\n', expected_result=False)
    islower_verifier.verify(b'\t', expected_result=False)
    islower_verifier.verify(b' ', expected_result=False)


def test_isspace():
    isspace_verifier = verifier_for(lambda tested: tested.isspace())

    isspace_verifier.verify(b'', expected_result=False)
    isspace_verifier.verify(b'abc', expected_result=False)
    isspace_verifier.verify(b'ABC', expected_result=False)
    isspace_verifier.verify(b'123', expected_result=False)
    isspace_verifier.verify(b'ABC123', expected_result=False)
    isspace_verifier.verify(b'+', expected_result=False)
    isspace_verifier.verify(b'[]', expected_result=False)
    isspace_verifier.verify(b'-', expected_result=False)
    isspace_verifier.verify(b'%', expected_result=False)
    isspace_verifier.verify(b'\n', expected_result=True)
    isspace_verifier.verify(b'\t', expected_result=True)
    isspace_verifier.verify(b' ', expected_result=True)


def test_istitle():
    istitle_verifier = verifier_for(lambda tested: tested.istitle())

    istitle_verifier.verify(b'', expected_result=False)

    istitle_verifier.verify(b'Abc', expected_result=True)
    istitle_verifier.verify(b'The Title', expected_result=True)
    istitle_verifier.verify(b'The title', expected_result=False)

    istitle_verifier.verify(b'abc', expected_result=False)
    istitle_verifier.verify(b'ABC', expected_result=False)
    istitle_verifier.verify(b'123', expected_result=False)
    istitle_verifier.verify(b'ABC123', expected_result=False)
    istitle_verifier.verify(b'+', expected_result=False)
    istitle_verifier.verify(b'[]', expected_result=False)
    istitle_verifier.verify(b'-', expected_result=False)
    istitle_verifier.verify(b'%', expected_result=False)
    istitle_verifier.verify(b'\n', expected_result=False)
    istitle_verifier.verify(b'\t', expected_result=False)
    istitle_verifier.verify(b' ', expected_result=False)


def test_isupper():
    isupper_verifier = verifier_for(lambda tested: tested.isupper())

    isupper_verifier.verify(b'', expected_result=False)
    isupper_verifier.verify(b'abc', expected_result=False)
    isupper_verifier.verify(b'ABC', expected_result=True)
    isupper_verifier.verify(b'123', expected_result=False)
    isupper_verifier.verify(b'ABC123', expected_result=True)
    isupper_verifier.verify(b'+', expected_result=False)
    isupper_verifier.verify(b'[]', expected_result=False)
    isupper_verifier.verify(b'-', expected_result=False)
    isupper_verifier.verify(b'%', expected_result=False)
    isupper_verifier.verify(b'\n', expected_result=False)
    isupper_verifier.verify(b'\t', expected_result=False)
    isupper_verifier.verify(b' ', expected_result=False)


def test_join():
    join_verifier = verifier_for(lambda tested, iterable: tested.join(iterable))

    join_verifier.verify(b', ', [], expected_result=b'')
    join_verifier.verify(b', ', [b'a'], expected_result=b'a')
    join_verifier.verify(b', ', [b'a', b'b'], expected_result=b'a, b')
    join_verifier.verify(b' ', [b'hello', b'world', b'again'], expected_result=b'hello world again')
    join_verifier.verify(b'\n', [b'1', b'2', b'3'], expected_result=b'1\n2\n3')
    join_verifier.verify(b', ', [1, 2], expected_error=TypeError)


def test_ljust():
    ljust_verifier = verifier_for(lambda tested, width: tested.ljust(width))
    ljust_with_fill_verifier = verifier_for(lambda tested, width, fill: tested.ljust(width, fill))

    ljust_verifier.verify(b'test', 10, expected_result=b'test      ')
    ljust_verifier.verify(b'test', 9, expected_result=b'test     ')
    ljust_verifier.verify(b'test', 4, expected_result=b'test')
    ljust_verifier.verify(b'test', 2, expected_result=b'test')

    ljust_with_fill_verifier.verify(b'test', 10, b'#', expected_result=b'test######')
    ljust_with_fill_verifier.verify(b'test', 9, b'#', expected_result=b'test#####')
    ljust_with_fill_verifier.verify(b'test', 4, b'#', expected_result=b'test')
    ljust_with_fill_verifier.verify(b'test', 2, b'#', expected_result=b'test')


def test_lower():
    lower_verifier = verifier_for(lambda tested: tested.lower())

    lower_verifier.verify(b'', expected_result=b'')
    lower_verifier.verify(b'abc', expected_result=b'abc')
    lower_verifier.verify(b'ABC', expected_result=b'abc')
    lower_verifier.verify(b'123', expected_result=b'123')
    lower_verifier.verify(b'ABC123', expected_result=b'abc123')
    lower_verifier.verify(b'+', expected_result=b'+')
    lower_verifier.verify(b'[]', expected_result=b'[]')
    lower_verifier.verify(b'-', expected_result=b'-')
    lower_verifier.verify(b'%', expected_result=b'%')
    lower_verifier.verify(b'\n', expected_result=b'\n')
    lower_verifier.verify(b'\t', expected_result=b'\t')
    lower_verifier.verify(b' ', expected_result=b' ')


def test_lstrip():
    lstrip_verifier = verifier_for(lambda tested: tested.lstrip())
    lstrip_with_chars_verifier = verifier_for(lambda tested, chars: tested.lstrip(chars))

    lstrip_verifier.verify(b'   spacious   ', expected_result=b'spacious   ')
    lstrip_with_chars_verifier.verify(b'www.example.com', b'cmowz.', expected_result=b'example.com')


def test_partition():
    partition_verifier = verifier_for(lambda tested, sep: tested.partition(sep))

    partition_verifier.verify(b'before+after+extra', b'+', expected_result=(b'before', b'+', b'after+extra'))
    partition_verifier.verify(b'before and after and extra', b'+', expected_result=(b'before and after and extra',
                                                                                    b'', b''))
    partition_verifier.verify(b'before and after and extra', b' and ',
                              expected_result=(b'before', b' and ', b'after and extra'))
    partition_verifier.verify(b'before+after+extra', b' and ', expected_result=(b'before+after+extra', b'', b''))


def test_removeprefix():
    removeprefix_verifier = verifier_for(lambda tested, prefix: tested.removeprefix(prefix))

    removeprefix_verifier.verify(b'TestHook', b'Test', expected_result=b'Hook')
    removeprefix_verifier.verify(b'BaseTestCase', b'Test', expected_result=b'BaseTestCase')
    removeprefix_verifier.verify(b'BaseCaseTest', b'Test', expected_result=b'BaseCaseTest')
    removeprefix_verifier.verify(b'BaseCase', b'Test', expected_result=b'BaseCase')


def test_removesuffix():
    removesuffix_verifier = verifier_for(lambda tested, suffix: tested.removesuffix(suffix))

    removesuffix_verifier.verify(b'MiscTests', b'Tests', expected_result=b'Misc')
    removesuffix_verifier.verify(b'TmpTestsDirMixin', b'Tests', expected_result=b'TmpTestsDirMixin')
    removesuffix_verifier.verify(b'TestsTmpDirMixin', b'Tests', expected_result=b'TestsTmpDirMixin')
    removesuffix_verifier.verify(b'TmpDirMixin', b'Tests', expected_result=b'TmpDirMixin')


def test_replace():
    replace_verifier = verifier_for(lambda tested, substring, replacement: tested.replace(substring, replacement))
    replace_with_count_verifier = verifier_for(lambda tested, substring, replacement, count:
                                               tested.replace(substring, replacement, count))

    replace_verifier.verify(b'all cats, including the cat Alcato, are animals', b'cat', b'dog',
                            expected_result=b'all dogs, including the dog Aldogo, are animals')
    replace_with_count_verifier.verify(b'all cats, including the cat Alcato, are animals', b'cat', b'dog', 0,
                                        expected_result=b'all cats, including the cat Alcato, are animals')
    replace_with_count_verifier.verify(b'all cats, including the cat Alcato, are animals', b'cat', b'dog', 1,
                                       expected_result=b'all dogs, including the cat Alcato, are animals')
    replace_with_count_verifier.verify(b'all cats, including the cat Alcato, are animals', b'cat', b'dog', 2,
                                       expected_result=b'all dogs, including the dog Alcato, are animals')
    replace_with_count_verifier.verify(b'all cats, including the cat Alcato, are animals', b'cat', b'dog', 3,
                                       expected_result=b'all dogs, including the dog Aldogo, are animals')
    replace_with_count_verifier.verify(b'all cats, including the cat Alcato, are animals', b'cat', b'dog', 4,
                                       expected_result=b'all dogs, including the dog Aldogo, are animals')
    replace_with_count_verifier.verify(b'all cats, including the cat Alcato, are animals', b'cat', b'dog', -1,
                                       expected_result=b'all dogs, including the dog Aldogo, are animals')


def test_rfind():
    rfind_verifier = verifier_for(lambda tested, item: tested.rfind(item))
    rfind_start_verifier = verifier_for(lambda tested, item, start: tested.rfind(item, start))
    rfind_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.rfind(item, start, end))

    rfind_verifier.verify(b'abcabc', b'a', expected_result=3)
    rfind_verifier.verify(b'abcabc', b'b', expected_result=4)
    rfind_verifier.verify(b'abcabc', b'd', expected_result=-1)

    rfind_start_verifier.verify(b'abcabc', b'a', 1, expected_result=3)
    rfind_start_verifier.verify(b'abcabc', b'a', 5, expected_result=-1)
    rfind_start_verifier.verify(b'abcabc', b'b', 1, expected_result=4)
    rfind_start_verifier.verify(b'abcabc', b'c', 1, expected_result=5)
    rfind_start_verifier.verify(b'abcabc', b'd', 1, expected_result=-1)

    rfind_start_verifier.verify(b'abcabc', b'a', -3, expected_result=3)
    rfind_start_verifier.verify(b'abcabc', b'b', -2, expected_result=4)
    rfind_start_verifier.verify(b'abcabc', b'c', -2, expected_result=5)
    rfind_start_verifier.verify(b'abcabc', b'd', -2, expected_result=-1)

    rfind_start_end_verifier.verify(b'abcabc', b'a', 1, 2, expected_result=-1)
    rfind_start_end_verifier.verify(b'abcabc', b'b', 1, 2, expected_result=1)
    rfind_start_end_verifier.verify(b'abcabc', b'c', 1, 2, expected_result=-1)
    rfind_start_end_verifier.verify(b'abcabc', b'd', 1, 2, expected_result=-1)

    rfind_start_end_verifier.verify(b'abcabc', b'a', -2, -1, expected_result=-1)
    rfind_start_end_verifier.verify(b'abcabc', b'b', -2, -1, expected_result=4)
    rfind_start_end_verifier.verify(b'abcabc', b'c', -2, -1, expected_result=-1)
    rfind_start_end_verifier.verify(b'abcabc', b'd', -2, -1, expected_result=-1)


def test_rindex():
    rindex_verifier = verifier_for(lambda tested, item: tested.rindex(item))
    rindex_start_verifier = verifier_for(lambda tested, item, start: tested.rindex(item, start))
    rindex_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.rindex(item, start, end))

    rindex_verifier.verify(b'abcabc', b'a', expected_result=3)
    rindex_verifier.verify(b'abcabc', b'b', expected_result=4)
    rindex_verifier.verify(b'abcabc', b'd', expected_error=ValueError)

    rindex_start_verifier.verify(b'abcabc', b'a', 1, expected_result=3)
    rindex_start_verifier.verify(b'abcabc', b'a', 5, expected_error=ValueError)
    rindex_start_verifier.verify(b'abcabc', b'b', 1, expected_result=4)
    rindex_start_verifier.verify(b'abcabc', b'c', 1, expected_result=5)
    rindex_start_verifier.verify(b'abcabc', b'd', 1, expected_error=ValueError)

    rindex_start_verifier.verify(b'abcabc', b'a', -3, expected_result=3)
    rindex_start_verifier.verify(b'abcabc', b'b', -2, expected_result=4)
    rindex_start_verifier.verify(b'abcabc', b'c', -2, expected_result=5)
    rindex_start_verifier.verify(b'abcabc', b'd', -2, expected_error=ValueError)

    rindex_start_end_verifier.verify(b'abcabc', b'a', 1, 2, expected_error=ValueError)
    rindex_start_end_verifier.verify(b'abcabc', b'b', 1, 2, expected_result=1)
    rindex_start_end_verifier.verify(b'abcabc', b'c', 1, 2, expected_error=ValueError)
    rindex_start_end_verifier.verify(b'abcabc', b'd', 1, 2, expected_error=ValueError)

    rindex_start_end_verifier.verify(b'abcabc', b'a', -2, -1, expected_error=ValueError)
    rindex_start_end_verifier.verify(b'abcabc', b'b', -2, -1, expected_result=4)
    rindex_start_end_verifier.verify(b'abcabc', b'c', -2, -1, expected_error=ValueError)
    rindex_start_end_verifier.verify(b'abcabc', b'd', -2, -1, expected_error=ValueError)

def test_rjust():
    rjust_verifier = verifier_for(lambda tested, width: tested.rjust(width))
    rjust_with_fill_verifier = verifier_for(lambda tested, width, fill: tested.rjust(width, fill))

    rjust_verifier.verify(b'test', 10, expected_result=b'      test')
    rjust_verifier.verify(b'test', 9, expected_result=b'     test')
    rjust_verifier.verify(b'test', 4, expected_result=b'test')
    rjust_verifier.verify(b'test', 2, expected_result=b'test')

    rjust_with_fill_verifier.verify(b'test', 10, b'#', expected_result=b'######test')
    rjust_with_fill_verifier.verify(b'test', 9, b'#', expected_result=b'#####test')
    rjust_with_fill_verifier.verify(b'test', 4, b'#', expected_result=b'test')
    rjust_with_fill_verifier.verify(b'test', 2, b'#', expected_result=b'test')


def test_rpartition():
    rpartition_verifier = verifier_for(lambda tested, sep: tested.rpartition(sep))

    rpartition_verifier.verify(b'before+after+extra', b'+', expected_result=(b'before+after', b'+', b'extra'))
    rpartition_verifier.verify(b'before and after and extra', b'+', expected_result=(b'', b'',
                                                                                     b'before and after and extra'))
    rpartition_verifier.verify(b'before and after and extra', b' and ',
                               expected_result=(b'before and after', b' and ', b'extra'))
    rpartition_verifier.verify(b'before+after+extra', b' and ', expected_result=(b'', b'', b'before+after+extra'))


def test_rsplit():
    rsplit_verifier = verifier_for(lambda tested: tested.rsplit())
    rsplit_with_sep_verifier = verifier_for(lambda tested, sep: tested.rsplit(sep))
    rsplit_with_sep_and_count_verifier = verifier_for(lambda tested, sep, count: tested.rsplit(sep, count))

    rsplit_verifier.verify(b'123', expected_result=[b'123'])
    rsplit_verifier.verify(b'1 2 3', expected_result=[b'1', b'2', b'3'])
    rsplit_verifier.verify(b' 1 2 3 ', expected_result=[b'1', b'2', b'3'])
    rsplit_verifier.verify(b'1\n2\n3', expected_result=[b'1', b'2', b'3'])
    rsplit_verifier.verify(b'1\t2\t3', expected_result=[b'1', b'2', b'3'])

    rsplit_with_sep_verifier.verify(b'1,2,3', b',', expected_result=[b'1', b'2', b'3'])
    rsplit_with_sep_verifier.verify(b'1,2,,3,', b',', expected_result=[b'1', b'2', b'', b'3', b''])
    rsplit_with_sep_verifier.verify(b',1,2,,3,', b',', expected_result=[b'', b'1', b'2', b'', b'3', b''])

    rsplit_with_sep_and_count_verifier.verify(b'1,2,3', b',', 1, expected_result=[b'1,2', b'3'])
    rsplit_with_sep_and_count_verifier.verify(b'1,2,,3,', b',', 1, expected_result=[b'1,2,,3', b''])
    rsplit_with_sep_and_count_verifier.verify(b'1,2,,3,', b',', 2, expected_result=[b'1,2,', b'3', b''])
    rsplit_with_sep_and_count_verifier.verify(b'1,2,,3,', b',', 3, expected_result=[b'1,2', b'',  b'3', b''])
    rsplit_with_sep_and_count_verifier.verify(b'1,2,,3,', b',', 4, expected_result=[b'1', b'2', b'',  b'3', b''])


def test_rstrip():
    rstrip_verifier = verifier_for(lambda tested: tested.rstrip())
    rstrip_with_chars_verifier = verifier_for(lambda tested, chars: tested.rstrip(chars))

    rstrip_verifier.verify(b'   spacious   ', expected_result=b'   spacious')
    rstrip_with_chars_verifier.verify(b'www.example.com', b'cmowz.', expected_result=b'www.example')



def test_split():
    split_verifier = verifier_for(lambda tested: tested.split())
    split_with_sep_verifier = verifier_for(lambda tested, sep: tested.split(sep))
    split_with_sep_and_count_verifier = verifier_for(lambda tested, sep, count: tested.split(sep, count))

    split_verifier.verify(b'123', expected_result=[b'123'])
    split_verifier.verify(b'1 2 3', expected_result=[b'1', b'2', b'3'])
    split_verifier.verify(b' 1 2 3 ', expected_result=[b'1', b'2', b'3'])
    split_verifier.verify(b'1\n2\n3', expected_result=[b'1', b'2', b'3'])
    split_verifier.verify(b'1\t2\t3', expected_result=[b'1', b'2', b'3'])

    split_with_sep_verifier.verify(b'1,2,3', b',', expected_result=[b'1', b'2', b'3'])
    split_with_sep_verifier.verify(b'1,2,,3,', b',', expected_result=[b'1', b'2', b'', b'3', b''])
    split_with_sep_verifier.verify(b',1,2,,3,', b',', expected_result=[b'', b'1', b'2', b'', b'3', b''])

    split_with_sep_and_count_verifier.verify(b'1,2,3', b',', 1, expected_result=[b'1', b'2,3'])
    split_with_sep_and_count_verifier.verify(b'1,2,,3,', b',', 1, expected_result=[b'1', b'2,,3,'])
    split_with_sep_and_count_verifier.verify(b'1,2,,3,', b',', 2, expected_result=[b'1', b'2', b',3,'])
    split_with_sep_and_count_verifier.verify(b'1,2,,3,', b',', 3, expected_result=[b'1', b'2', b'',  b'3,'])
    split_with_sep_and_count_verifier.verify(b'1,2,,3,', b',', 4, expected_result=[b'1', b'2', b'',  b'3', b''])


def test_splitlines():
    splitlines_verifier = verifier_for(lambda tested: tested.splitlines())
    splitlines_keep_ends_verifier = verifier_for(lambda tested, keep_ends: tested.splitlines(keep_ends))

    splitlines_verifier.verify(b'ab c\n\nde fg\rkl\r\n', expected_result=[b'ab c', b'', b'de fg', b'kl'])
    splitlines_verifier.verify(b'', expected_result=[])
    splitlines_verifier.verify(b'One line\n', expected_result=[b'One line'])

    splitlines_keep_ends_verifier.verify(b'ab c\n\nde fg\rkl\r\n', False, expected_result=[b'ab c', b'', b'de fg', b'kl'])
    splitlines_keep_ends_verifier.verify(b'ab c\n\nde fg\rkl\r\n', True,
                                         expected_result=[b'ab c\n', b'\n', b'de fg\r', b'kl\r\n'])
    splitlines_keep_ends_verifier.verify(b'', True, expected_result=[])
    splitlines_keep_ends_verifier.verify(b'', False, expected_result=[])
    splitlines_keep_ends_verifier.verify(b'One line\n', True, expected_result=[b'One line\n'])
    splitlines_keep_ends_verifier.verify(b'One line\n', False, expected_result=[b'One line'])


def test_startswith():
    startswith_verifier = verifier_for(lambda tested, prefix: tested.startswith(prefix))
    startswith_start_verifier = verifier_for(lambda tested, prefix, start: tested.startswith(prefix, start))
    startswith_between_verifier = verifier_for(lambda tested, prefix, start, end: tested.startswith(prefix, start, end))

    startswith_verifier.verify(b'hello world', b'hello', expected_result=True)
    startswith_verifier.verify(b'hello world', b'world', expected_result=False)
    startswith_verifier.verify(b'hello', b'hello world', expected_result=False)
    startswith_verifier.verify(b'hello world', b'hello world', expected_result=True)

    startswith_start_verifier.verify(b'hello world', b'world', 6, expected_result=True)
    startswith_start_verifier.verify(b'hello world', b'hello', 6, expected_result=False)
    startswith_start_verifier.verify(b'hello', b'hello world', 6, expected_result=False)
    startswith_start_verifier.verify(b'hello world', b'hello world', 6, expected_result=False)

    startswith_between_verifier.verify(b'hello world', b'world', 6, 11, expected_result=True)
    startswith_between_verifier.verify(b'hello world', b'world', 7, 11, expected_result=False)
    startswith_between_verifier.verify(b'hello world', b'hello', 0, 5, expected_result=True)
    startswith_between_verifier.verify(b'hello', b'hello world', 0, 5, expected_result=False)
    startswith_between_verifier.verify(b'hello world', b'hello world', 5, 11, expected_result=False)


def test_strip():
    strip_verifier = verifier_for(lambda tested: tested.strip())
    strip_with_chars_verifier = verifier_for(lambda tested, chars: tested.strip(chars))

    strip_verifier.verify(b'   spacious   ', expected_result=b'spacious')
    strip_with_chars_verifier.verify(b'www.example.com', b'cmowz.', expected_result=b'example')


def test_swapcase():
    swapcase_verifier = verifier_for(lambda tested: tested.swapcase())

    swapcase_verifier.verify(b'', expected_result=b'')

    swapcase_verifier.verify(b'abc', expected_result=b'ABC')
    swapcase_verifier.verify(b'ABC', expected_result=b'abc')
    swapcase_verifier.verify(b'123', expected_result=b'123')
    swapcase_verifier.verify(b'ABC123', expected_result=b'abc123')
    swapcase_verifier.verify(b'+', expected_result=b'+')
    swapcase_verifier.verify(b'[]', expected_result=b'[]')
    swapcase_verifier.verify(b'-', expected_result=b'-')
    swapcase_verifier.verify(b'%', expected_result=b'%')
    swapcase_verifier.verify(b'\n', expected_result=b'\n')
    swapcase_verifier.verify(b'\t', expected_result=b'\t')
    swapcase_verifier.verify(b' ', expected_result=b' ')


def test_title():
    title_verifier = verifier_for(lambda tested: tested.title())

    title_verifier.verify(b'', expected_result=b'')
    title_verifier.verify(b'Hello world', expected_result=b'Hello World')
    title_verifier.verify(b"they're bill's friends from the UK",
                              expected_result=b"They'Re Bill'S Friends From The Uk")

    title_verifier.verify(b'abc', expected_result=b'Abc')
    title_verifier.verify(b'ABC', expected_result=b'Abc')
    title_verifier.verify(b'123', expected_result=b'123')
    title_verifier.verify(b'ABC123', expected_result=b'Abc123')
    title_verifier.verify(b'+', expected_result=b'+')
    title_verifier.verify(b'[]', expected_result=b'[]')
    title_verifier.verify(b'-', expected_result=b'-')
    title_verifier.verify(b'%', expected_result=b'%')
    title_verifier.verify(b'\n', expected_result=b'\n')
    title_verifier.verify(b'\t', expected_result=b'\t')
    title_verifier.verify(b' ', expected_result=b' ')


def test_translate():
    translate_verifier = verifier_for(lambda tested, mapping: tested.translate(mapping))

    mapping = b''.join([bytes([(i + 1) % 256]) for i in range(256)])

    translate_verifier.verify(b'hello world',
                              mapping, expected_result=b'ifmmp!xpsme')


def test_upper():
    upper_verifier = verifier_for(lambda tested: tested.upper())

    upper_verifier.verify(b'', expected_result=b'')
    upper_verifier.verify(b'abc', expected_result=b'ABC')
    upper_verifier.verify(b'ABC', expected_result=b'ABC')
    upper_verifier.verify(b'123', expected_result=b'123')
    upper_verifier.verify(b'ABC123', expected_result=b'ABC123')
    upper_verifier.verify(b'+', expected_result=b'+')
    upper_verifier.verify(b'[]', expected_result=b'[]')
    upper_verifier.verify(b'-', expected_result=b'-')
    upper_verifier.verify(b'%', expected_result=b'%')
    upper_verifier.verify(b'\n', expected_result=b'\n')
    upper_verifier.verify(b'\t', expected_result=b'\t')
    upper_verifier.verify(b' ', expected_result=b' ')


def test_zfill():
    zfill_verifier = verifier_for(lambda tested, padding: tested.zfill(padding))

    zfill_verifier.verify(b'42', 5, expected_result=b'00042')
    zfill_verifier.verify(b'-42', 5, expected_result=b'-0042')
    zfill_verifier.verify(b'+42', 5, expected_result=b'+0042')
    zfill_verifier.verify(b'42', 1, expected_result=b'42')
    zfill_verifier.verify(b'-42', 1, expected_result=b'-42')
    zfill_verifier.verify(b'+42', 1, expected_result=b'+42')
    zfill_verifier.verify(b'abc', 10, expected_result=b'0000000abc')
