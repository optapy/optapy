from .conftest import verifier_for


########################################
# Sequence methods
########################################

def test_membership():
    membership_verifier = verifier_for(lambda tested, x: x in tested)
    not_membership_verifier = verifier_for(lambda tested, x: x not in tested)

    membership_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), expected_result=True)
    not_membership_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), expected_result=False)

    membership_verifier.verify(bytearray(b'hello world'), bytearray(b'test'), expected_result=False)
    not_membership_verifier.verify(bytearray(b'hello world'), bytearray(b'test'), expected_result=True)

    membership_verifier.verify(bytearray(b'hello world'), bytearray(b''), expected_result=True)
    not_membership_verifier.verify(bytearray(b'hello world'), bytearray(b''), expected_result=False)


def test_concat():
    def concat(x, y):
        out = x + y
        return out, out is x, out is y

    concat_verifier = verifier_for(concat)

    concat_verifier.verify(bytearray(b'hello '), bytearray(b'world'), expected_result=(bytearray(b'hello world'), False, False))
    concat_verifier.verify(bytearray(b''), bytearray(b'hello world'), expected_result=(bytearray(b'hello world'), False, False))
    concat_verifier.verify(bytearray(b'hello world'), bytearray(b''), expected_result=(bytearray(b'hello world'), False, False))
    concat_verifier.verify(bytearray(b'world '), bytearray(b'hello'), expected_result=(bytearray(b'world hello'), False, False))


def test_repeat():
    def repeat(x, y):
        out = x * y
        return out, out is x, out is y

    repeat_verifier = verifier_for(repeat)

    repeat_verifier.verify(bytearray(b'hi'), 1, expected_result=(bytearray(b'hi'), False, False))
    repeat_verifier.verify(bytearray(b'abc'), 2, expected_result=(bytearray(b'abcabc'), False, False))
    repeat_verifier.verify(bytearray(b'a'), 4, expected_result=(bytearray(b'aaaa'), False, False))
    repeat_verifier.verify(bytearray(b'test'), 0, expected_result=(bytearray(b''), False, False))
    repeat_verifier.verify(bytearray(b'test'), -1, expected_result=(bytearray(b''), False, False))
    repeat_verifier.verify(bytearray(b'test'), -2, expected_result=(bytearray(b''), False, False))

    # TODO: Support right versions of binary operators in bytecode translator
    # repeat_verifier.verify(2, (1, 2, 3), expected_result=((1, 2, 3, 1, 2, 3), False, False))
    # repeat_verifier.verify(4, (1, 2), expected_result=((1, 2, 1, 2, 1, 2, 1, 2), False, False))
    # repeat_verifier.verify(0, (1, 2, 3), expected_result=((), False, False))
    # repeat_verifier.verify(-1, (1, 2, 3), expected_result=((), False, False))
    # repeat_verifier.verify(-2, (1, 2, 3), expected_result=((), False, False))


def test_get_item():
    get_item_verifier = verifier_for(lambda tested, index: tested[index])

    get_item_verifier.verify(bytearray(b'abc'), 1, expected_result=ord(bytearray(b'b')))
    get_item_verifier.verify(bytearray(b'abc'), -1, expected_result=ord(bytearray(b'c')))
    get_item_verifier.verify(bytearray(b'abcd'), -1, expected_result=ord(bytearray(b'd')))
    get_item_verifier.verify(bytearray(b'abcd'), -2, expected_result=ord(bytearray(b'c')))
    get_item_verifier.verify(bytearray(b'abcd'), 0, expected_result=ord(bytearray(b'a')))
    get_item_verifier.verify(bytearray(b'abc'), 3, expected_error=IndexError)
    get_item_verifier.verify(bytearray(b'abc'), -4, expected_error=IndexError)


def test_get_slice():
    get_slice_verifier = verifier_for(lambda tested, start, end: tested[start:end])

    get_slice_verifier.verify(bytearray(b'abcde'), 1, 3, expected_result=bytearray(b'bc'))
    get_slice_verifier.verify(bytearray(b'abcde'), -3, -1, expected_result=bytearray(b'cd'))

    get_slice_verifier.verify(bytearray(b'abcde'), 0, -2, expected_result=bytearray(b'abc'))
    get_slice_verifier.verify(bytearray(b'abcde'), -3, 4, expected_result=bytearray(b'cd'))

    get_slice_verifier.verify(bytearray(b'abcde'), 3, 1, expected_result=bytearray(b''))
    get_slice_verifier.verify(bytearray(b'abcde'), -1, -3, expected_result=bytearray(b''))

    get_slice_verifier.verify(bytearray(b'abcde'), 100, 1000, expected_result=bytearray(b''))
    get_slice_verifier.verify(bytearray(b'abcde'), 0, 1000, expected_result=bytearray(b'abcde'))

    get_slice_verifier.verify(bytearray(b'abcde'), 1, None, expected_result=bytearray(b'bcde'))
    get_slice_verifier.verify(bytearray(b'abcde'), None, 2, expected_result=bytearray(b'ab'))
    get_slice_verifier.verify(bytearray(b'abcde'), None, None, expected_result=bytearray(b'abcde'))


def test_get_slice_with_step():
    get_slice_verifier = verifier_for(lambda tested, start, end, step: tested[start:end:step])

    get_slice_verifier.verify(bytearray(b'abcde'), 0, None, 2, expected_result=bytearray(b'ace'))
    get_slice_verifier.verify(bytearray(b'abcde'), 1, None, 2, expected_result=bytearray(b'bd'))
    get_slice_verifier.verify(bytearray(b'abcde'), 0, 5, 2, expected_result=bytearray(b'ace'))
    get_slice_verifier.verify(bytearray(b'abcde'), 1, 5, 2, expected_result=bytearray(b'bd'))
    get_slice_verifier.verify(bytearray(b'abcde'), 0, -1, 2, expected_result=bytearray(b'ac'))
    get_slice_verifier.verify(bytearray(b'abcde'), 1, -1, 2, expected_result=bytearray(b'bd'))

    get_slice_verifier.verify(bytearray(b'abcde'), 4, None, -2, expected_result=bytearray(b'eca'))
    get_slice_verifier.verify(bytearray(b'abcde'), 3, None, -2, expected_result=bytearray(b'db'))
    get_slice_verifier.verify(bytearray(b'abcde'), -1, -6, -2, expected_result=bytearray(b'eca'))
    get_slice_verifier.verify(bytearray(b'abcde'), -2, -6, -2, expected_result=bytearray(b'db'))
    get_slice_verifier.verify(bytearray(b'abcde'), 4, 0, -2, expected_result=bytearray(b'ec'))
    get_slice_verifier.verify(bytearray(b'abcde'), 3, 0, -2, expected_result=bytearray(b'db'))

    get_slice_verifier.verify(bytearray(b'abcde'), 0, None, None, expected_result=bytearray(b'abcde'))
    get_slice_verifier.verify(bytearray(b'abcde'), 0, 3, None, expected_result=bytearray(b'abc'))

    get_slice_verifier.verify(bytearray(b'abcde'), 3, 1, -1, expected_result=bytearray(b'dc'))
    get_slice_verifier.verify(bytearray(b'abcde'), -1, -3, -1, expected_result=bytearray(b'ed'))
    get_slice_verifier.verify(bytearray(b'abcde'), 3, 1, 1, expected_result=bytearray(b''))
    get_slice_verifier.verify(bytearray(b'abcde'), -1, -3, 1, expected_result=bytearray(b''))


def test_len():
    len_verifier = verifier_for(lambda tested: len(tested))

    len_verifier.verify(bytearray(b''), expected_result=0)
    len_verifier.verify(bytearray(b'a'), expected_result=1)
    len_verifier.verify(bytearray(b'ab'), expected_result=2)
    len_verifier.verify(bytearray(b'cba'), expected_result=3)


def test_index():
    index_verifier = verifier_for(lambda tested, item: tested.index(item))
    index_start_verifier = verifier_for(lambda tested, item, start: tested.index(item, start))
    index_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.index(item, start, end))

    index_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), expected_result=0)
    index_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), expected_result=1)
    index_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), expected_error=ValueError)

    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, expected_result=3)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 5, expected_error=ValueError)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, expected_result=1)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, expected_result=2)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, expected_error=ValueError)

    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -3, expected_result=3)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, expected_result=4)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, expected_result=5)
    index_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, expected_error=ValueError)

    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, 2, expected_result=1)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, 2, expected_error=ValueError)

    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, -1, expected_result=4)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, -1, expected_error=ValueError)


def test_count():
    count_verifier = verifier_for(lambda tested, item: tested.count(item))

    count_verifier.verify(bytearray(b'abc'), bytearray(b'a'), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), bytearray(b'b'), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), bytearray(b'c'), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), bytearray(b'd'), expected_result=0)

    count_verifier.verify(bytearray(b'abca'), bytearray(b'a'), expected_result=2)
    count_verifier.verify(bytearray(b'aaca'), bytearray(b'a'), expected_result=3)
    count_verifier.verify(bytearray(b''), bytearray(b'a'), expected_result=0)


########################################
# Mutable Sequence operations
########################################


def test_set_item():
    def set_item(tested, index, value):
        tested[index] = value
        return tested

    set_item_verifier = verifier_for(set_item)
    set_item_verifier.verify(bytearray(b'abcde'), 0, ord('g'), expected_result=bytearray(b'gbcde'))
    set_item_verifier.verify(bytearray(b'abcde'), 2, ord('C'), expected_result=bytearray(b'abCde'))
    set_item_verifier.verify(bytearray(b'abcde'), -1, ord('F'), expected_result=bytearray(b'abcdF'))
    set_item_verifier.verify(bytearray(b'abcde'), -10, ord('a'), expected_error=IndexError)
    set_item_verifier.verify(bytearray(b'abcde'), 10, ord('a'), expected_error=IndexError)

def test_set_slice():
    def set_slice(tested, start, stop, value):
        tested[start:stop] = value
        return tested

    set_slice_verifier = verifier_for(set_slice)
    set_slice_verifier.verify(bytearray(b'abcde'), 1, 3, b'', expected_result=bytearray(b'ade'))
    set_slice_verifier.verify(bytearray(b'abcde'), 2, 2, b'H', expected_result=bytearray(b'abHcde'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, 3, b'H', expected_result=bytearray(b'aHde'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, 3, b'HI', expected_result=bytearray(b'aHIde'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, 3, b'HIJ', expected_result=bytearray(b'aHIJde'))

    set_slice_verifier.verify(bytearray(b'abcde'), -4, -2, b'HI', expected_result=bytearray(b'aHIde'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, -2, b'HI', expected_result=bytearray(b'aHIde'))

    set_slice_verifier.verify(bytearray(b'abcde'), 5, 5, b'F', expected_result=bytearray(b'abcdeF'))

    set_slice_verifier.verify(bytearray(b'abcde'), 1, None, b'', expected_result=bytearray(b'a'))
    set_slice_verifier.verify(bytearray(b'abcde'), 1, None, b'HI', expected_result=bytearray(b'aHI'))


def test_delete_slice():
    def delete_slice(tested, start, stop):
        del tested[start:stop]
        return tested

    delete_slice_verifier = verifier_for(delete_slice)
    delete_slice_verifier.verify(bytearray(b'abcde'), 1, 3, expected_result=bytearray(b'ade'))
    delete_slice_verifier.verify(bytearray(b'abcde'), 3, 5, expected_result=bytearray(b'abc'))
    delete_slice_verifier.verify(bytearray(b'abcde'), 1, None, expected_result=bytearray(b'a'))
    delete_slice_verifier.verify(bytearray(b'abcde'), None, 3, expected_result=bytearray(b'de'))
    delete_slice_verifier.verify(bytearray(b'abcde'), None, None, expected_result=bytearray(b''))


def test_set_slice_with_step():
    def set_slice_with_step(tested, start, stop, step, value):
        tested[start:stop:step] = value
        return tested

    set_slice_with_step_verifier = verifier_for(set_slice_with_step)

    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 3, 0, -1, b'BCD', expected_result=bytearray(b'aDCBe'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 1, 4, 2, b'BD', expected_result=bytearray(b'aBcDe'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 1, -1, 2, b'BD', expected_result=bytearray(b'aBcDe'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 0, None, 2, b'ACE',
                                        expected_result=bytearray(b'AbCdE'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), None, 4, 2, b'AC',
                                        expected_result=bytearray(b'AbCde'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), None, None, 2, b'ACE',
                                        expected_result=bytearray(b'AbCdE'))

    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 3, 0, -1, b'', expected_result=bytearray(b'ae'))
    set_slice_with_step_verifier.verify(bytearray(b'abcde'), 3, 0, -1, b'BCDE', expected_error=ValueError)


def test_delete_slice_with_step():
    def delete_slice_with_step(tested, start, stop, step):
        del tested[start:stop:step]
        return tested

    delete_slice_with_step_verifier = verifier_for(delete_slice_with_step)

    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), 3, 0, -1, expected_result=bytearray(b'ae'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), 1, 4, 2, expected_result=bytearray(b'ace'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), 1, -1, 2, expected_result=bytearray(b'ace'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), 0, None, 2,
                                           expected_result=bytearray(b'bd'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), None, 4, 2,
                                           expected_result=bytearray(b'bde'))
    delete_slice_with_step_verifier.verify(bytearray(b'abcde'), None, None, 2,
                                           expected_result=bytearray(b'bd'))


def test_append():
    def append(tested, item):
        tested.append(item)
        return tested

    append_verifier = verifier_for(append)

    append_verifier.verify(bytearray(b''), ord('a'), expected_result=bytearray(b'a'))
    append_verifier.verify(bytearray(b'a'), ord('b'), expected_result=bytearray(b'ab'))
    append_verifier.verify(bytearray(b'ab'), ord('c'), expected_result=bytearray(b'abc'))
    append_verifier.verify(bytearray(b'abc'), ord('c'), expected_result=bytearray(b'abcc'))


def test_clear():
    def clear(tested):
        tested.clear()
        return tested

    clear_verifier = verifier_for(clear)

    clear_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    clear_verifier.verify(bytearray(b'a'), expected_result=bytearray(b''))
    clear_verifier.verify(bytearray(b'ab'), expected_result=bytearray(b''))
    clear_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b''))


def test_copy():
    def copy(tested):
        out = tested.copy()
        return out, out is tested

    copy_verifier = verifier_for(copy)

    copy_verifier.verify(bytearray(b''), expected_result=(bytearray(b''), False))
    copy_verifier.verify(bytearray(b'a'), expected_result=(bytearray(b'a'), False))
    copy_verifier.verify(bytearray(b'ab'), expected_result=(bytearray(b'ab'), False))
    copy_verifier.verify(bytearray(b'abc'), expected_result=(bytearray(b'abc'), False))


def test_extend():
    def extend(tested, item):
        tested.extend(item)
        return tested

    extend_verifier = verifier_for(extend)

    extend_verifier.verify(bytearray(b''), bytearray(b'a'), expected_result=bytearray(b'a'))
    extend_verifier.verify(bytearray(b'a'), bytearray(b'b'), expected_result=bytearray(b'ab'))
    extend_verifier.verify(bytearray(b'ab'), bytearray(b'c'), expected_result=bytearray(b'abc'))
    extend_verifier.verify(bytearray(b'abc'), bytearray(b''), expected_result=bytearray(b'abc'))
    extend_verifier.verify(bytearray(b'abc'), bytearray(b'de'), expected_result=bytearray(b'abcde'))


def test_inplace_add():
    def extend(tested, item):
        tested += item
        return tested

    extend_verifier = verifier_for(extend)

    extend_verifier.verify(bytearray(b''), bytearray(b'a'), expected_result=bytearray(b'a'))
    extend_verifier.verify(bytearray(b'a'), bytearray(b'b'), expected_result=bytearray(b'ab'))
    extend_verifier.verify(bytearray(b'ab'), bytearray(b'c'), expected_result=bytearray(b'abc'))
    extend_verifier.verify(bytearray(b'abc'), bytearray(b''), expected_result=bytearray(b'abc'))
    extend_verifier.verify(bytearray(b'abc'), bytearray(b'de'), expected_result=bytearray(b'abcde'))


def test_inplace_multiply():
    def multiply(tested, item):
        tested *= item
        return tested

    multiply_verifier = verifier_for(multiply)

    multiply_verifier.verify(bytearray(b'abc'), 1, expected_result=bytearray(b'abc'))
    multiply_verifier.verify(bytearray(b'ab'), 2, expected_result=bytearray(b'abab'))
    multiply_verifier.verify(bytearray(b'ab'), 3, expected_result=bytearray(b'ababab'))
    multiply_verifier.verify(bytearray(b'abc'), 0, expected_result=bytearray(b''))
    multiply_verifier.verify(bytearray(b'abc'), -1, expected_result=bytearray(b''))



def test_insert():
    def insert(tested, index, item):
        tested.insert(index, item)
        return tested

    insert_verifier = verifier_for(insert)

    insert_verifier.verify(bytearray(b''), 0, ord('a'), expected_result=bytearray(b'a'))
    insert_verifier.verify(bytearray(b'a'), 0, ord('b'), expected_result=bytearray(b'ba'))
    insert_verifier.verify(bytearray(b'a'), 1, ord('b'), expected_result=bytearray(b'ab'))
    insert_verifier.verify(bytearray(b'ab'), 0, ord('c'), expected_result=bytearray(b'cab'))
    insert_verifier.verify(bytearray(b'ab'), 1, ord('c'), expected_result=bytearray(b'acb'))
    insert_verifier.verify(bytearray(b'ab'), 2, ord('c'), expected_result=bytearray(b'abc'))
    insert_verifier.verify(bytearray(b'abc'), -1, ord('d'), expected_result=bytearray(b'abdc'))
    insert_verifier.verify(bytearray(b'abc'), -2, ord('d'), expected_result=bytearray(b'adbc'))
    insert_verifier.verify(bytearray(b'abc'), 3, ord('d'), expected_result=bytearray(b'abcd'))
    insert_verifier.verify(bytearray(b'abc'), 4, ord('d'), expected_result=bytearray(b'abcd'))
    insert_verifier.verify(bytearray(b'abc'), -4, ord('d'), expected_result=bytearray(b'dabc'))
    insert_verifier.verify(bytearray(b'abc'), -5, ord('d'), expected_result=bytearray(b'dabc'))


def test_pop():
    def pop(tested):
        item = tested.pop()
        return item, tested

    pop_verifier = verifier_for(pop)

    pop_verifier.verify(bytearray(b'abc'), expected_result=(ord('c'), bytearray(b'ab')))
    pop_verifier.verify(bytearray(b'ab'), expected_result=(ord('b'), bytearray(b'a')))
    pop_verifier.verify(bytearray(b'a'), expected_result=(ord('a'), bytearray(b'')))

    pop_verifier.verify(bytearray(b'abe'), expected_result=(ord('e'), bytearray(b'ab')))

    pop_verifier.verify(bytearray(b''), expected_error=IndexError)


def test_pop_at_index():
    def pop_at_index(tested, index):
        item = tested.pop(index)
        return item, tested

    pop_at_index_verifier = verifier_for(pop_at_index)

    pop_at_index_verifier.verify(bytearray(b'abc'), -1, expected_result=(ord('c'), bytearray(b'ab')))
    pop_at_index_verifier.verify(bytearray(b'ab'), -1, expected_result=(ord('b'), bytearray(b'a')))
    pop_at_index_verifier.verify(bytearray(b'a'), -1, expected_result=(ord('a'), bytearray(b'')))

    pop_at_index_verifier.verify(bytearray(b'abc'), 1, expected_result=(ord('b'), bytearray(b'ac')))
    pop_at_index_verifier.verify(bytearray(b'abc'), 0, expected_result=(ord('a'), bytearray(b'bc')))
    pop_at_index_verifier.verify(bytearray(b'abc'), 2, expected_result=(ord('c'), bytearray(b'ab')))
    pop_at_index_verifier.verify(bytearray(b'abc'), -2, expected_result=(ord('b'), bytearray(b'ac')))

    pop_at_index_verifier.verify(bytearray(b'abc'), -4, expected_error=IndexError)
    pop_at_index_verifier.verify(bytearray(b'abc'), 4, expected_error=IndexError)
    pop_at_index_verifier.verify(bytearray(b''), 0, expected_error=IndexError)


def test_remove():
    def remove(tested, item):
        tested.remove(item)
        return tested

    remove_verifier = verifier_for(remove)

    remove_verifier.verify(bytearray(b'abc'), ord('a'), expected_result=bytearray(b'bc'))
    remove_verifier.verify(bytearray(b'abc'), ord('b'), expected_result=bytearray(b'ac'))
    remove_verifier.verify(bytearray(b'abc'), ord('c'), expected_result=bytearray(b'ab'))

    remove_verifier.verify(bytearray(b'abe'), ord('b'), expected_result=bytearray(b'ae'))

    remove_verifier.verify(bytearray(b'abc'), ord('d'), expected_error=ValueError)
    remove_verifier.verify(bytearray(b''), ord('a'), expected_error=ValueError)


def test_reverse():
    def reverse(tested):
        tested.reverse()
        return tested

    reverse_verifier = verifier_for(reverse)

    reverse_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'cba'))
    reverse_verifier.verify(bytearray(b'cba'), expected_result=bytearray(b'abc'))
    reverse_verifier.verify(bytearray(b'ab'), expected_result=bytearray(b'ba'))
    reverse_verifier.verify(bytearray(b'ba'), expected_result=bytearray(b'ab'))
    reverse_verifier.verify(bytearray(b'a'), expected_result=bytearray(b'a'))
    reverse_verifier.verify(bytearray(b''), expected_result=bytearray(b''))


########################################
# Bytes operations
########################################
def test_interpolation():
    interpolation_verifier = verifier_for(lambda tested, values: tested % values)

    interpolation_verifier.verify(bytearray(b'%d'), 100, expected_result=bytearray(b'100'))
    interpolation_verifier.verify(bytearray(b'%d'), 0b1111, expected_result=bytearray(b'15'))
    interpolation_verifier.verify(bytearray(b'%s'), bytearray(b'foo'), expected_result=bytearray(b'foo'))
    interpolation_verifier.verify(bytearray(b'%s %s'), (bytearray(b'foo'), bytearray(b'bar')),
                                  expected_result=bytearray(b'foo bar'))
    interpolation_verifier.verify(bytearray(b'%(foo)s'), {b'foo': bytearray(b'10'), b'bar': bytearray(b'20')},
                                  expected_result=bytearray(b'10'))

    interpolation_verifier.verify(bytearray(b'%d'), 101, expected_result=bytearray(b'101'))
    interpolation_verifier.verify(bytearray(b'%i'), 101, expected_result=bytearray(b'101'))

    interpolation_verifier.verify(bytearray(b'%o'), 27, expected_result=bytearray(b'33'))
    interpolation_verifier.verify(bytearray(b'%#o'), 27, expected_result=bytearray(b'0o33'))

    interpolation_verifier.verify(bytearray(b'%x'), 27, expected_result=bytearray(b'1b'))
    interpolation_verifier.verify(bytearray(b'%X'), 27, expected_result=bytearray(b'1B'))
    interpolation_verifier.verify(bytearray(b'%#x'), 27, expected_result=bytearray(b'0x1b'))
    interpolation_verifier.verify(bytearray(b'%#X'), 27, expected_result=bytearray(b'0X1B'))

    interpolation_verifier.verify(bytearray(b'%03d'), 1, expected_result=bytearray(b'001'))
    interpolation_verifier.verify(bytearray(b'%-5d'), 1, expected_result=bytearray(b'1    '))
    interpolation_verifier.verify(bytearray(b'%0-5d'), 1, expected_result=bytearray(b'1    '))

    interpolation_verifier.verify(bytearray(b'%d'), 1, expected_result=bytearray(b'1'))
    interpolation_verifier.verify(bytearray(b'%d'), -1, expected_result=bytearray(b'-1'))
    interpolation_verifier.verify(bytearray(b'% d'), 1, expected_result=bytearray(b' 1'))
    interpolation_verifier.verify(bytearray(b'% d'), -1, expected_result=bytearray(b'-1'))
    interpolation_verifier.verify(bytearray(b'%+d'), 1, expected_result=bytearray(b'+1'))
    interpolation_verifier.verify(bytearray(b'%+d'), -1, expected_result=bytearray(b'-1'))

    interpolation_verifier.verify(bytearray(b'%f'), 3.14, expected_result=bytearray(b'3.140000'))
    interpolation_verifier.verify(bytearray(b'%F'), 3.14, expected_result=bytearray(b'3.140000'))
    interpolation_verifier.verify(bytearray(b'%.1f'), 3.14, expected_result=bytearray(b'3.1'))
    interpolation_verifier.verify(bytearray(b'%.2f'), 3.14, expected_result=bytearray(b'3.14'))
    interpolation_verifier.verify(bytearray(b'%.3f'), 3.14, expected_result=bytearray(b'3.140'))

    interpolation_verifier.verify(bytearray(b'%g'), 1234567890, expected_result=bytearray(b'1.23457e+09'))
    interpolation_verifier.verify(bytearray(b'%G'), 1234567890, expected_result=bytearray(b'1.23457E+09'))
    interpolation_verifier.verify(bytearray(b'%e'), 1234567890, expected_result=bytearray(b'1.234568e+09'))
    interpolation_verifier.verify(bytearray(b'%E'), 1234567890, expected_result=bytearray(b'1.234568E+09'))

    interpolation_verifier.verify(bytearray(b'ABC %c'), 10, expected_result=bytearray(b'ABC \n'))
    interpolation_verifier.verify(bytearray(b'ABC %c'), 67, expected_result=bytearray(b'ABC C'))
    interpolation_verifier.verify(bytearray(b'ABC %c'), 68, expected_result=bytearray(b'ABC D'))
    interpolation_verifier.verify(bytearray(b'ABC %c'), bytearray(b'D'), expected_result=bytearray(b'ABC D'))
    interpolation_verifier.verify(bytearray(b'ABC %s'), bytearray(b'test'), expected_result=bytearray(b'ABC test'))
    interpolation_verifier.verify(bytearray(b'ABC %r'), bytearray(b'test'),
                                  expected_result=bytearray(b'ABC bytearray(b\'test\')'))

    interpolation_verifier.verify(bytearray(b'Give it %d%%!'), 100, expected_result=bytearray(b'Give it 100%!'))
    interpolation_verifier.verify(bytearray(b'Give it %(all-you-got)d%%!'), {b'all-you-got': 100},
                                  expected_result=bytearray(b'Give it 100%!'))


########################################
# String methods
########################################


def test_capitalize():
    capitalize_verifier = verifier_for(lambda tested: tested.capitalize())

    capitalize_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    capitalize_verifier.verify(bytearray(b'test'), expected_result=bytearray(b'Test'))
    capitalize_verifier.verify(bytearray(b'TEST'), expected_result=bytearray(b'Test'))
    capitalize_verifier.verify(bytearray(b'hello world'), expected_result=bytearray(b'Hello world'))
    capitalize_verifier.verify(bytearray(b'Hello World'), expected_result=bytearray(b'Hello world'))
    capitalize_verifier.verify(bytearray(b'HELLO WORLD'), expected_result=bytearray(b'Hello world'))


def test_center():
    center_verifier = verifier_for(lambda tested, width: tested.center(width))
    center_with_fill_verifier = verifier_for(lambda tested, width, fill: tested.center(width, fill))

    center_verifier.verify(bytearray(b'test'), 10, expected_result=bytearray(b'   test   '))
    center_verifier.verify(bytearray(b'test'), 9, expected_result=bytearray(b'   test  '))
    center_verifier.verify(bytearray(b'test'), 4, expected_result=bytearray(b'test'))
    center_verifier.verify(bytearray(b'test'), 2, expected_result=bytearray(b'test'))

    center_with_fill_verifier.verify(bytearray(b'test'), 10, bytearray(b'#'), expected_result=bytearray(b'###test###'))
    center_with_fill_verifier.verify(bytearray(b'test'), 9, bytearray(b'#'), expected_result=bytearray(b'###test##'))
    center_with_fill_verifier.verify(bytearray(b'test'), 4, bytearray(b'#'), expected_result=bytearray(b'test'))
    center_with_fill_verifier.verify(bytearray(b'test'), 2, bytearray(b'#'), expected_result=bytearray(b'test'))


def test_count_byte():
    count_verifier = verifier_for(lambda tested, item: tested.count(item))
    count_from_start_verifier = verifier_for(lambda tested, item, start: tested.count(item, start))
    count_between_verifier = verifier_for(lambda tested, item, start, end: tested.count(item, start, end))

    count_verifier.verify(bytearray(b'abc'), ord(bytearray(b'a')), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), ord(bytearray(b'b')), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), ord(bytearray(b'c')), expected_result=1)
    count_verifier.verify(bytearray(b'abc'), ord(bytearray(b'd')), expected_result=0)

    count_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), expected_result=2)
    count_verifier.verify(bytearray(b'aaca'), ord(bytearray(b'a')), expected_result=3)
    count_verifier.verify(bytearray(b''), ord(bytearray(b'a')), expected_result=0)

    count_from_start_verifier.verify(bytearray(b'abc'), ord(bytearray(b'a')), 1, expected_result=0)
    count_from_start_verifier.verify(bytearray(b'abc'), ord(bytearray(b'b')), 1, expected_result=1)
    count_from_start_verifier.verify(bytearray(b'abc'), ord(bytearray(b'c')), 1, expected_result=1)
    count_from_start_verifier.verify(bytearray(b'abc'), ord(bytearray(b'd')), 1, expected_result=0)

    count_from_start_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), 1, expected_result=1)
    count_from_start_verifier.verify(bytearray(b'aaca'), ord(bytearray(b'a')), 1, expected_result=2)
    count_from_start_verifier.verify(bytearray(b''), ord(bytearray(b'a')), 1, expected_result=0)

    count_between_verifier.verify(bytearray(b'abc'), ord(bytearray(b'a')), 1, 2, expected_result=0)
    count_between_verifier.verify(bytearray(b'abc'), ord(bytearray(b'b')), 1, 2, expected_result=1)
    count_between_verifier.verify(bytearray(b'abc'), ord(bytearray(b'c')), 1, 2, expected_result=0)
    count_between_verifier.verify(bytearray(b'abc'), ord(bytearray(b'd')), 1, 2, expected_result=0)

    count_between_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), 1, 2, expected_result=0)
    count_between_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), 1, 4, expected_result=1)
    count_between_verifier.verify(bytearray(b'abca'), ord(bytearray(b'a')), 0, 2, expected_result=1)
    count_between_verifier.verify(bytearray(b'aaca'), ord(bytearray(b'a')), 1, 2, expected_result=1)
    count_between_verifier.verify(bytearray(b''), ord(bytearray(b'a')), 1, 2, expected_result=0)


def test_endswith():
    endswith_verifier = verifier_for(lambda tested, suffix: tested.endswith(suffix))
    endswith_start_verifier = verifier_for(lambda tested, suffix, start: tested.endswith(suffix, start))
    endswith_between_verifier = verifier_for(lambda tested, suffix, start, end: tested.endswith(suffix, start, end))

    endswith_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), expected_result=True)
    endswith_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), expected_result=False)
    endswith_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), expected_result=False)
    endswith_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), expected_result=True)

    endswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 6, expected_result=True)
    endswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), 6, expected_result=False)
    endswith_start_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), 6, expected_result=False)
    endswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), 6, expected_result=False)

    endswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 6, 11, expected_result=True)
    endswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 7, 11, expected_result=False)
    endswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), 0, 5, expected_result=True)
    endswith_between_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), 0, 5, expected_result=False)
    endswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), 5, 11, expected_result=False)


def test_expandtabs():
    expandtabs_verifier = verifier_for(lambda tested: tested.expandtabs())
    expandtabs_with_tabsize_verifier = verifier_for(lambda tested, tabsize: tested.expandtabs(tabsize))

    expandtabs_verifier.verify(bytearray(b'01\t012\t0123\t01234'), expected_result=bytearray(b'01      012     0123    01234'))
    expandtabs_with_tabsize_verifier.verify(bytearray(b'01\t012\t0123\t01234'), 8, expected_result=bytearray(b'01      012     0123    01234'))
    expandtabs_with_tabsize_verifier.verify(bytearray(b'01\t012\t0123\t01234'), 4, expected_result=bytearray(b'01  012 0123    01234'))


def test_find():
    find_verifier = verifier_for(lambda tested, item: tested.find(item))
    find_start_verifier = verifier_for(lambda tested, item, start: tested.find(item, start))
    find_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.find(item, start, end))

    find_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), expected_result=0)
    find_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), expected_result=1)
    find_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), expected_result=-1)

    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, expected_result=3)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 5, expected_result=-1)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, expected_result=1)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, expected_result=2)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, expected_result=-1)

    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -3, expected_result=3)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, expected_result=4)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, expected_result=5)
    find_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, expected_result=-1)

    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, 2, expected_result=-1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, 2, expected_result=1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, 2, expected_result=-1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, 2, expected_result=-1)

    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -2, -1, expected_result=-1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, -1, expected_result=4)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, -1, expected_result=-1)
    find_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, -1, expected_result=-1)


def test_isalnum():
    isalnum_verifier = verifier_for(lambda tested: tested.isalnum())

    isalnum_verifier.verify(bytearray(b''), expected_result=False)
    isalnum_verifier.verify(bytearray(b'abc'), expected_result=True)
    isalnum_verifier.verify(bytearray(b'ABC'), expected_result=True)
    isalnum_verifier.verify(bytearray(b'123'), expected_result=True)
    isalnum_verifier.verify(bytearray(b'ABC123'), expected_result=True)
    isalnum_verifier.verify(bytearray(b'+'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'[]'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'-'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'%'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'\n'), expected_result=False)
    isalnum_verifier.verify(bytearray(b'\t'), expected_result=False)
    isalnum_verifier.verify(bytearray(b' '), expected_result=False)


def test_isalpha():
    isalpha_verifier = verifier_for(lambda tested: tested.isalpha())

    isalpha_verifier.verify(bytearray(b''), expected_result=False)
    isalpha_verifier.verify(bytearray(b'abc'), expected_result=True)
    isalpha_verifier.verify(bytearray(b'ABC'), expected_result=True)
    isalpha_verifier.verify(bytearray(b'123'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'+'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'[]'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'-'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'%'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'\n'), expected_result=False)
    isalpha_verifier.verify(bytearray(b'\t'), expected_result=False)
    isalpha_verifier.verify(bytearray(b' '), expected_result=False)


def test_isascii():
    isascii_verifier = verifier_for(lambda tested: tested.isascii())

    isascii_verifier.verify(bytearray(b''), expected_result=True)
    isascii_verifier.verify(bytearray(b'abc'), expected_result=True)
    isascii_verifier.verify(bytearray(b'ABC'), expected_result=True)
    isascii_verifier.verify(bytearray(b'123'), expected_result=True)
    isascii_verifier.verify(bytearray(b'ABC123'), expected_result=True)
    isascii_verifier.verify(bytearray(b'+'), expected_result=True)
    isascii_verifier.verify(bytearray(b'[]'), expected_result=True)
    isascii_verifier.verify(bytearray(b'-'), expected_result=True)
    isascii_verifier.verify(bytearray(b'%'), expected_result=True)
    isascii_verifier.verify(bytearray(b'\n'), expected_result=True)
    isascii_verifier.verify(bytearray(b'\t'), expected_result=True)
    isascii_verifier.verify(bytearray(b' '), expected_result=True)


def test_isdigit():
    isdigit_verifier = verifier_for(lambda tested: tested.isdigit())

    isdigit_verifier.verify(bytearray(b''), expected_result=False)
    isdigit_verifier.verify(bytearray(b'abc'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'ABC'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'123'), expected_result=True)
    isdigit_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'+'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'[]'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'-'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'%'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'\n'), expected_result=False)
    isdigit_verifier.verify(bytearray(b'\t'), expected_result=False)
    isdigit_verifier.verify(bytearray(b' '), expected_result=False)


def test_islower():
    islower_verifier = verifier_for(lambda tested: tested.islower())

    islower_verifier.verify(bytearray(b''), expected_result=False)
    islower_verifier.verify(bytearray(b'abc'), expected_result=True)
    islower_verifier.verify(bytearray(b'ABC'), expected_result=False)
    islower_verifier.verify(bytearray(b'123'), expected_result=False)
    islower_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    islower_verifier.verify(bytearray(b'+'), expected_result=False)
    islower_verifier.verify(bytearray(b'[]'), expected_result=False)
    islower_verifier.verify(bytearray(b'-'), expected_result=False)
    islower_verifier.verify(bytearray(b'%'), expected_result=False)
    islower_verifier.verify(bytearray(b'\n'), expected_result=False)
    islower_verifier.verify(bytearray(b'\t'), expected_result=False)
    islower_verifier.verify(bytearray(b' '), expected_result=False)


def test_isspace():
    isspace_verifier = verifier_for(lambda tested: tested.isspace())

    isspace_verifier.verify(bytearray(b''), expected_result=False)
    isspace_verifier.verify(bytearray(b'abc'), expected_result=False)
    isspace_verifier.verify(bytearray(b'ABC'), expected_result=False)
    isspace_verifier.verify(bytearray(b'123'), expected_result=False)
    isspace_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    isspace_verifier.verify(bytearray(b'+'), expected_result=False)
    isspace_verifier.verify(bytearray(b'[]'), expected_result=False)
    isspace_verifier.verify(bytearray(b'-'), expected_result=False)
    isspace_verifier.verify(bytearray(b'%'), expected_result=False)
    isspace_verifier.verify(bytearray(b'\n'), expected_result=True)
    isspace_verifier.verify(bytearray(b'\t'), expected_result=True)
    isspace_verifier.verify(bytearray(b' '), expected_result=True)


def test_istitle():
    istitle_verifier = verifier_for(lambda tested: tested.istitle())

    istitle_verifier.verify(bytearray(b''), expected_result=False)

    istitle_verifier.verify(bytearray(b'Abc'), expected_result=True)
    istitle_verifier.verify(bytearray(b'The Title'), expected_result=True)
    istitle_verifier.verify(bytearray(b'The title'), expected_result=False)

    istitle_verifier.verify(bytearray(b'abc'), expected_result=False)
    istitle_verifier.verify(bytearray(b'ABC'), expected_result=False)
    istitle_verifier.verify(bytearray(b'123'), expected_result=False)
    istitle_verifier.verify(bytearray(b'ABC123'), expected_result=False)
    istitle_verifier.verify(bytearray(b'+'), expected_result=False)
    istitle_verifier.verify(bytearray(b'[]'), expected_result=False)
    istitle_verifier.verify(bytearray(b'-'), expected_result=False)
    istitle_verifier.verify(bytearray(b'%'), expected_result=False)
    istitle_verifier.verify(bytearray(b'\n'), expected_result=False)
    istitle_verifier.verify(bytearray(b'\t'), expected_result=False)
    istitle_verifier.verify(bytearray(b' '), expected_result=False)


def test_isupper():
    isupper_verifier = verifier_for(lambda tested: tested.isupper())

    isupper_verifier.verify(bytearray(b''), expected_result=False)
    isupper_verifier.verify(bytearray(b'abc'), expected_result=False)
    isupper_verifier.verify(bytearray(b'ABC'), expected_result=True)
    isupper_verifier.verify(bytearray(b'123'), expected_result=False)
    isupper_verifier.verify(bytearray(b'ABC123'), expected_result=True)
    isupper_verifier.verify(bytearray(b'+'), expected_result=False)
    isupper_verifier.verify(bytearray(b'[]'), expected_result=False)
    isupper_verifier.verify(bytearray(b'-'), expected_result=False)
    isupper_verifier.verify(bytearray(b'%'), expected_result=False)
    isupper_verifier.verify(bytearray(b'\n'), expected_result=False)
    isupper_verifier.verify(bytearray(b'\t'), expected_result=False)
    isupper_verifier.verify(bytearray(b' '), expected_result=False)


def test_join():
    join_verifier = verifier_for(lambda tested, iterable: tested.join(iterable))

    join_verifier.verify(bytearray(b', '), [], expected_result=bytearray(b''))
    join_verifier.verify(bytearray(b', '), [bytearray(b'a')], expected_result=bytearray(b'a'))
    join_verifier.verify(bytearray(b', '), [bytearray(b'a'), bytearray(b'b')], expected_result=bytearray(b'a, b'))
    join_verifier.verify(bytearray(b' '), [bytearray(b'hello'), bytearray(b'world'), bytearray(b'again')], expected_result=bytearray(b'hello world again'))
    join_verifier.verify(bytearray(b'\n'), [bytearray(b'1'), bytearray(b'2'), bytearray(b'3')], expected_result=bytearray(b'1\n2\n3'))
    join_verifier.verify(bytearray(b', '), [1, 2], expected_error=TypeError)


def test_ljust():
    ljust_verifier = verifier_for(lambda tested, width: tested.ljust(width))
    ljust_with_fill_verifier = verifier_for(lambda tested, width, fill: tested.ljust(width, fill))

    ljust_verifier.verify(bytearray(b'test'), 10, expected_result=bytearray(b'test      '))
    ljust_verifier.verify(bytearray(b'test'), 9, expected_result=bytearray(b'test     '))
    ljust_verifier.verify(bytearray(b'test'), 4, expected_result=bytearray(b'test'))
    ljust_verifier.verify(bytearray(b'test'), 2, expected_result=bytearray(b'test'))

    ljust_with_fill_verifier.verify(bytearray(b'test'), 10, bytearray(b'#'), expected_result=bytearray(b'test######'))
    ljust_with_fill_verifier.verify(bytearray(b'test'), 9, bytearray(b'#'), expected_result=bytearray(b'test#####'))
    ljust_with_fill_verifier.verify(bytearray(b'test'), 4, bytearray(b'#'), expected_result=bytearray(b'test'))
    ljust_with_fill_verifier.verify(bytearray(b'test'), 2, bytearray(b'#'), expected_result=bytearray(b'test'))


def test_lower():
    lower_verifier = verifier_for(lambda tested: tested.lower())

    lower_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    lower_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'abc'))
    lower_verifier.verify(bytearray(b'ABC'), expected_result=bytearray(b'abc'))
    lower_verifier.verify(bytearray(b'123'), expected_result=bytearray(b'123'))
    lower_verifier.verify(bytearray(b'ABC123'), expected_result=bytearray(b'abc123'))
    lower_verifier.verify(bytearray(b'+'), expected_result=bytearray(b'+'))
    lower_verifier.verify(bytearray(b'[]'), expected_result=bytearray(b'[]'))
    lower_verifier.verify(bytearray(b'-'), expected_result=bytearray(b'-'))
    lower_verifier.verify(bytearray(b'%'), expected_result=bytearray(b'%'))
    lower_verifier.verify(bytearray(b'\n'), expected_result=bytearray(b'\n'))
    lower_verifier.verify(bytearray(b'\t'), expected_result=bytearray(b'\t'))
    lower_verifier.verify(bytearray(b' '), expected_result=bytearray(b' '))


def test_lstrip():
    lstrip_verifier = verifier_for(lambda tested: tested.lstrip())
    lstrip_with_chars_verifier = verifier_for(lambda tested, chars: tested.lstrip(chars))

    lstrip_verifier.verify(bytearray(b'   spacious   '), expected_result=bytearray(b'spacious   '))
    lstrip_with_chars_verifier.verify(bytearray(b'www.example.com'), bytearray(b'cmowz.'), expected_result=bytearray(b'example.com'))


def test_partition():
    partition_verifier = verifier_for(lambda tested, sep: tested.partition(sep))

    partition_verifier.verify(bytearray(b'before+after+extra'), bytearray(b'+'), expected_result=(bytearray(b'before'), bytearray(b'+'), bytearray(b'after+extra')))
    partition_verifier.verify(bytearray(b'before and after and extra'), bytearray(b'+'), expected_result=(bytearray(b'before and after and extra'),
                                                                                    bytearray(b''), bytearray(b'')))
    partition_verifier.verify(bytearray(b'before and after and extra'), bytearray(b' and '),
                              expected_result=(bytearray(b'before'), bytearray(b' and '), bytearray(b'after and extra')))
    partition_verifier.verify(bytearray(b'before+after+extra'), bytearray(b' and '), expected_result=(bytearray(b'before+after+extra'), bytearray(b''), bytearray(b'')))


def test_removeprefix():
    removeprefix_verifier = verifier_for(lambda tested, prefix: tested.removeprefix(prefix))

    removeprefix_verifier.verify(bytearray(b'TestHook'), bytearray(b'Test'), expected_result=bytearray(b'Hook'))
    removeprefix_verifier.verify(bytearray(b'BaseTestCase'), bytearray(b'Test'), expected_result=bytearray(b'BaseTestCase'))
    removeprefix_verifier.verify(bytearray(b'BaseCaseTest'), bytearray(b'Test'), expected_result=bytearray(b'BaseCaseTest'))
    removeprefix_verifier.verify(bytearray(b'BaseCase'), bytearray(b'Test'), expected_result=bytearray(b'BaseCase'))


def test_removesuffix():
    removesuffix_verifier = verifier_for(lambda tested, suffix: tested.removesuffix(suffix))

    removesuffix_verifier.verify(bytearray(b'MiscTests'), bytearray(b'Tests'), expected_result=bytearray(b'Misc'))
    removesuffix_verifier.verify(bytearray(b'TmpTestsDirMixin'), bytearray(b'Tests'), expected_result=bytearray(b'TmpTestsDirMixin'))
    removesuffix_verifier.verify(bytearray(b'TestsTmpDirMixin'), bytearray(b'Tests'), expected_result=bytearray(b'TestsTmpDirMixin'))
    removesuffix_verifier.verify(bytearray(b'TmpDirMixin'), bytearray(b'Tests'), expected_result=bytearray(b'TmpDirMixin'))


def test_replace():
    replace_verifier = verifier_for(lambda tested, substring, replacement: tested.replace(substring, replacement))
    replace_with_count_verifier = verifier_for(lambda tested, substring, replacement, count:
                                               tested.replace(substring, replacement, count))

    replace_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'),
                            expected_result=bytearray(b'all dogs, including the dog Aldogo, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 0,
                                        expected_result=bytearray(b'all cats, including the cat Alcato, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 1,
                                       expected_result=bytearray(b'all dogs, including the cat Alcato, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 2,
                                       expected_result=bytearray(b'all dogs, including the dog Alcato, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 3,
                                       expected_result=bytearray(b'all dogs, including the dog Aldogo, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), 4,
                                       expected_result=bytearray(b'all dogs, including the dog Aldogo, are animals'))
    replace_with_count_verifier.verify(bytearray(b'all cats, including the cat Alcato, are animals'), bytearray(b'cat'), bytearray(b'dog'), -1,
                                       expected_result=bytearray(b'all dogs, including the dog Aldogo, are animals'))


def test_rfind():
    rfind_verifier = verifier_for(lambda tested, item: tested.rfind(item))
    rfind_start_verifier = verifier_for(lambda tested, item, start: tested.rfind(item, start))
    rfind_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.rfind(item, start, end))

    rfind_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), expected_result=3)
    rfind_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), expected_result=4)
    rfind_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), expected_result=-1)

    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, expected_result=3)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 5, expected_result=-1)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, expected_result=4)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, expected_result=5)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, expected_result=-1)

    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -3, expected_result=3)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, expected_result=4)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, expected_result=5)
    rfind_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, expected_result=-1)

    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, 2, expected_result=-1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, 2, expected_result=1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, 2, expected_result=-1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, 2, expected_result=-1)

    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -2, -1, expected_result=-1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, -1, expected_result=4)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, -1, expected_result=-1)
    rfind_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, -1, expected_result=-1)


def test_rindex():
    rindex_verifier = verifier_for(lambda tested, item: tested.rindex(item))
    rindex_start_verifier = verifier_for(lambda tested, item, start: tested.rindex(item, start))
    rindex_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.rindex(item, start, end))

    rindex_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), expected_result=3)
    rindex_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), expected_result=4)
    rindex_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), expected_error=ValueError)

    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, expected_result=3)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 5, expected_error=ValueError)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, expected_result=4)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, expected_result=5)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, expected_error=ValueError)

    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -3, expected_result=3)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, expected_result=4)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, expected_result=5)
    rindex_start_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, expected_error=ValueError)

    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), 1, 2, expected_error=ValueError)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), 1, 2, expected_result=1)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), 1, 2, expected_error=ValueError)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), 1, 2, expected_error=ValueError)

    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'a'), -2, -1, expected_error=ValueError)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'b'), -2, -1, expected_result=4)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'c'), -2, -1, expected_error=ValueError)
    rindex_start_end_verifier.verify(bytearray(b'abcabc'), bytearray(b'd'), -2, -1, expected_error=ValueError)

def test_rjust():
    rjust_verifier = verifier_for(lambda tested, width: tested.rjust(width))
    rjust_with_fill_verifier = verifier_for(lambda tested, width, fill: tested.rjust(width, fill))

    rjust_verifier.verify(bytearray(b'test'), 10, expected_result=bytearray(b'      test'))
    rjust_verifier.verify(bytearray(b'test'), 9, expected_result=bytearray(b'     test'))
    rjust_verifier.verify(bytearray(b'test'), 4, expected_result=bytearray(b'test'))
    rjust_verifier.verify(bytearray(b'test'), 2, expected_result=bytearray(b'test'))

    rjust_with_fill_verifier.verify(bytearray(b'test'), 10, bytearray(b'#'), expected_result=bytearray(b'######test'))
    rjust_with_fill_verifier.verify(bytearray(b'test'), 9, bytearray(b'#'), expected_result=bytearray(b'#####test'))
    rjust_with_fill_verifier.verify(bytearray(b'test'), 4, bytearray(b'#'), expected_result=bytearray(b'test'))
    rjust_with_fill_verifier.verify(bytearray(b'test'), 2, bytearray(b'#'), expected_result=bytearray(b'test'))


def test_rpartition():
    rpartition_verifier = verifier_for(lambda tested, sep: tested.rpartition(sep))

    rpartition_verifier.verify(bytearray(b'before+after+extra'), bytearray(b'+'), expected_result=(bytearray(b'before+after'), bytearray(b'+'), bytearray(b'extra')))
    rpartition_verifier.verify(bytearray(b'before and after and extra'), bytearray(b'+'), expected_result=(bytearray(b''), bytearray(b''),
                                                                                     bytearray(b'before and after and extra')))
    rpartition_verifier.verify(bytearray(b'before and after and extra'), bytearray(b' and '),
                               expected_result=(bytearray(b'before and after'), bytearray(b' and '), bytearray(b'extra')))
    rpartition_verifier.verify(bytearray(b'before+after+extra'), bytearray(b' and '), expected_result=(bytearray(b''), bytearray(b''), bytearray(b'before+after+extra')))


def test_rsplit():
    rsplit_verifier = verifier_for(lambda tested: tested.rsplit())
    rsplit_with_sep_verifier = verifier_for(lambda tested, sep: tested.rsplit(sep))
    rsplit_with_sep_and_count_verifier = verifier_for(lambda tested, sep, count: tested.rsplit(sep, count))

    rsplit_verifier.verify(bytearray(b'123'), expected_result=[bytearray(b'123')])
    rsplit_verifier.verify(bytearray(b'1 2 3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    rsplit_verifier.verify(bytearray(b' 1 2 3 '), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    rsplit_verifier.verify(bytearray(b'1\n2\n3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    rsplit_verifier.verify(bytearray(b'1\t2\t3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])

    rsplit_with_sep_verifier.verify(bytearray(b'1,2,3'), bytearray(b','), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    rsplit_with_sep_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''), bytearray(b'3'), bytearray(b'')])
    rsplit_with_sep_verifier.verify(bytearray(b',1,2,,3,'), bytearray(b','), expected_result=[bytearray(b''), bytearray(b'1'), bytearray(b'2'), bytearray(b''), bytearray(b'3'), bytearray(b'')])

    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,3'), bytearray(b','), 1, expected_result=[bytearray(b'1,2'), bytearray(b'3')])
    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 1, expected_result=[bytearray(b'1,2,,3'), bytearray(b'')])
    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 2, expected_result=[bytearray(b'1,2,'), bytearray(b'3'), bytearray(b'')])
    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 3, expected_result=[bytearray(b'1,2'), bytearray(b''),  bytearray(b'3'), bytearray(b'')])
    rsplit_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 4, expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''),  bytearray(b'3'), bytearray(b'')])


def test_rstrip():
    rstrip_verifier = verifier_for(lambda tested: tested.rstrip())
    rstrip_with_chars_verifier = verifier_for(lambda tested, chars: tested.rstrip(chars))

    rstrip_verifier.verify(bytearray(b'   spacious   '), expected_result=bytearray(b'   spacious'))
    rstrip_with_chars_verifier.verify(bytearray(b'www.example.com'), bytearray(b'cmowz.'), expected_result=bytearray(b'www.example'))



def test_split():
    split_verifier = verifier_for(lambda tested: tested.split())
    split_with_sep_verifier = verifier_for(lambda tested, sep: tested.split(sep))
    split_with_sep_and_count_verifier = verifier_for(lambda tested, sep, count: tested.split(sep, count))

    split_verifier.verify(bytearray(b'123'), expected_result=[bytearray(b'123')])
    split_verifier.verify(bytearray(b'1 2 3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    split_verifier.verify(bytearray(b' 1 2 3 '), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    split_verifier.verify(bytearray(b'1\n2\n3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    split_verifier.verify(bytearray(b'1\t2\t3'), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])

    split_with_sep_verifier.verify(bytearray(b'1,2,3'), bytearray(b','), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')])
    split_with_sep_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''), bytearray(b'3'), bytearray(b'')])
    split_with_sep_verifier.verify(bytearray(b',1,2,,3,'), bytearray(b','), expected_result=[bytearray(b''), bytearray(b'1'), bytearray(b'2'), bytearray(b''), bytearray(b'3'), bytearray(b'')])

    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,3'), bytearray(b','), 1, expected_result=[bytearray(b'1'), bytearray(b'2,3')])
    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 1, expected_result=[bytearray(b'1'), bytearray(b'2,,3,')])
    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 2, expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b',3,')])
    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 3, expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''),  bytearray(b'3,')])
    split_with_sep_and_count_verifier.verify(bytearray(b'1,2,,3,'), bytearray(b','), 4, expected_result=[bytearray(b'1'), bytearray(b'2'), bytearray(b''),  bytearray(b'3'), bytearray(b'')])


def test_splitlines():
    splitlines_verifier = verifier_for(lambda tested: tested.splitlines())
    splitlines_keep_ends_verifier = verifier_for(lambda tested, keep_ends: tested.splitlines(keep_ends))

    splitlines_verifier.verify(bytearray(b'ab c\n\nde fg\rkl\r\n'), expected_result=[bytearray(b'ab c'), bytearray(b''), bytearray(b'de fg'), bytearray(b'kl')])
    splitlines_verifier.verify(bytearray(b''), expected_result=[])
    splitlines_verifier.verify(bytearray(b'One line\n'), expected_result=[bytearray(b'One line')])

    splitlines_keep_ends_verifier.verify(bytearray(b'ab c\n\nde fg\rkl\r\n'), False, expected_result=[bytearray(b'ab c'), bytearray(b''), bytearray(b'de fg'), bytearray(b'kl')])
    splitlines_keep_ends_verifier.verify(bytearray(b'ab c\n\nde fg\rkl\r\n'), True,
                                         expected_result=[bytearray(b'ab c\n'), bytearray(b'\n'), bytearray(b'de fg\r'), bytearray(b'kl\r\n')])
    splitlines_keep_ends_verifier.verify(bytearray(b''), True, expected_result=[])
    splitlines_keep_ends_verifier.verify(bytearray(b''), False, expected_result=[])
    splitlines_keep_ends_verifier.verify(bytearray(b'One line\n'), True, expected_result=[bytearray(b'One line\n')])
    splitlines_keep_ends_verifier.verify(bytearray(b'One line\n'), False, expected_result=[bytearray(b'One line')])


def test_startswith():
    startswith_verifier = verifier_for(lambda tested, prefix: tested.startswith(prefix))
    startswith_start_verifier = verifier_for(lambda tested, prefix, start: tested.startswith(prefix, start))
    startswith_between_verifier = verifier_for(lambda tested, prefix, start, end: tested.startswith(prefix, start, end))

    startswith_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), expected_result=True)
    startswith_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), expected_result=False)
    startswith_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), expected_result=False)
    startswith_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), expected_result=True)

    startswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 6, expected_result=True)
    startswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), 6, expected_result=False)
    startswith_start_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), 6, expected_result=False)
    startswith_start_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), 6, expected_result=False)

    startswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 6, 11, expected_result=True)
    startswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'world'), 7, 11, expected_result=False)
    startswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'hello'), 0, 5, expected_result=True)
    startswith_between_verifier.verify(bytearray(b'hello'), bytearray(b'hello world'), 0, 5, expected_result=False)
    startswith_between_verifier.verify(bytearray(b'hello world'), bytearray(b'hello world'), 5, 11, expected_result=False)


def test_strip():
    strip_verifier = verifier_for(lambda tested: tested.strip())
    strip_with_chars_verifier = verifier_for(lambda tested, chars: tested.strip(chars))

    strip_verifier.verify(bytearray(b'   spacious   '), expected_result=bytearray(b'spacious'))
    strip_with_chars_verifier.verify(bytearray(b'www.example.com'), bytearray(b'cmowz.'), expected_result=bytearray(b'example'))


def test_swapcase():
    swapcase_verifier = verifier_for(lambda tested: tested.swapcase())

    swapcase_verifier.verify(bytearray(b''), expected_result=bytearray(b''))

    swapcase_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'ABC'))
    swapcase_verifier.verify(bytearray(b'ABC'), expected_result=bytearray(b'abc'))
    swapcase_verifier.verify(bytearray(b'123'), expected_result=bytearray(b'123'))
    swapcase_verifier.verify(bytearray(b'ABC123'), expected_result=bytearray(b'abc123'))
    swapcase_verifier.verify(bytearray(b'+'), expected_result=bytearray(b'+'))
    swapcase_verifier.verify(bytearray(b'[]'), expected_result=bytearray(b'[]'))
    swapcase_verifier.verify(bytearray(b'-'), expected_result=bytearray(b'-'))
    swapcase_verifier.verify(bytearray(b'%'), expected_result=bytearray(b'%'))
    swapcase_verifier.verify(bytearray(b'\n'), expected_result=bytearray(b'\n'))
    swapcase_verifier.verify(bytearray(b'\t'), expected_result=bytearray(b'\t'))
    swapcase_verifier.verify(bytearray(b' '), expected_result=bytearray(b' '))


def test_title():
    title_verifier = verifier_for(lambda tested: tested.title())

    title_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    title_verifier.verify(bytearray(b'Hello world'), expected_result=bytearray(b'Hello World'))
    title_verifier.verify(b"they're bill's friends from the UK",
                              expected_result=b"They'Re Bill'S Friends From The Uk")

    title_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'Abc'))
    title_verifier.verify(bytearray(b'ABC'), expected_result=bytearray(b'Abc'))
    title_verifier.verify(bytearray(b'123'), expected_result=bytearray(b'123'))
    title_verifier.verify(bytearray(b'ABC123'), expected_result=bytearray(b'Abc123'))
    title_verifier.verify(bytearray(b'+'), expected_result=bytearray(b'+'))
    title_verifier.verify(bytearray(b'[]'), expected_result=bytearray(b'[]'))
    title_verifier.verify(bytearray(b'-'), expected_result=bytearray(b'-'))
    title_verifier.verify(bytearray(b'%'), expected_result=bytearray(b'%'))
    title_verifier.verify(bytearray(b'\n'), expected_result=bytearray(b'\n'))
    title_verifier.verify(bytearray(b'\t'), expected_result=bytearray(b'\t'))
    title_verifier.verify(bytearray(b' '), expected_result=bytearray(b' '))


def test_translate():
    translate_verifier = verifier_for(lambda tested, mapping: tested.translate(mapping))

    mapping = bytearray(b'').join([bytes([(i + 1) % 256]) for i in range(256)])

    translate_verifier.verify(bytearray(b'hello world'),
                              mapping, expected_result=bytearray(b'ifmmp!xpsme'))


def test_upper():
    upper_verifier = verifier_for(lambda tested: tested.upper())

    upper_verifier.verify(bytearray(b''), expected_result=bytearray(b''))
    upper_verifier.verify(bytearray(b'abc'), expected_result=bytearray(b'ABC'))
    upper_verifier.verify(bytearray(b'ABC'), expected_result=bytearray(b'ABC'))
    upper_verifier.verify(bytearray(b'123'), expected_result=bytearray(b'123'))
    upper_verifier.verify(bytearray(b'ABC123'), expected_result=bytearray(b'ABC123'))
    upper_verifier.verify(bytearray(b'+'), expected_result=bytearray(b'+'))
    upper_verifier.verify(bytearray(b'[]'), expected_result=bytearray(b'[]'))
    upper_verifier.verify(bytearray(b'-'), expected_result=bytearray(b'-'))
    upper_verifier.verify(bytearray(b'%'), expected_result=bytearray(b'%'))
    upper_verifier.verify(bytearray(b'\n'), expected_result=bytearray(b'\n'))
    upper_verifier.verify(bytearray(b'\t'), expected_result=bytearray(b'\t'))
    upper_verifier.verify(bytearray(b' '), expected_result=bytearray(b' '))


def test_zfill():
    zfill_verifier = verifier_for(lambda tested, padding: tested.zfill(padding))

    zfill_verifier.verify(bytearray(b'42'), 5, expected_result=bytearray(b'00042'))
    zfill_verifier.verify(bytearray(b'-42'), 5, expected_result=bytearray(b'-0042'))
    zfill_verifier.verify(bytearray(b'+42'), 5, expected_result=bytearray(b'+0042'))
    zfill_verifier.verify(bytearray(b'42'), 1, expected_result=bytearray(b'42'))
    zfill_verifier.verify(bytearray(b'-42'), 1, expected_result=bytearray(b'-42'))
    zfill_verifier.verify(bytearray(b'+42'), 1, expected_result=bytearray(b'+42'))
    zfill_verifier.verify(bytearray(b'abc'), 10, expected_result=bytearray(b'0000000abc'))
