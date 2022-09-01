from .conftest import verifier_for


def test_membership():
    membership_verifier = verifier_for(lambda tested, x: x in tested)
    not_membership_verifier = verifier_for(lambda tested, x: x not in tested)

    membership_verifier.verify([1, 2, 3], 1, expected_result=True)
    not_membership_verifier.verify([1, 2, 3], 1, expected_result=False)

    membership_verifier.verify([1, 2, 3], 4, expected_result=False)
    not_membership_verifier.verify([1, 2, 3], 4, expected_result=True)

    membership_verifier.verify([1, 2, 3], 3, expected_result=True)
    not_membership_verifier.verify([1, 2, 3], 3, expected_result=False)


def test_concat():
    def concat(x, y):
        out = x + y
        return out, out is x, out is y

    concat_verifier = verifier_for(concat)

    concat_verifier.verify([1, 2], [3, 4], expected_result=([1, 2, 3, 4], False, False))
    concat_verifier.verify([], [1, 2, 3], expected_result=([1, 2, 3], False, False))
    concat_verifier.verify([1, 2, 3], [], expected_result=([1, 2, 3], False, False))
    concat_verifier.verify([3], [2, 1], expected_result=([3, 2, 1], False, False))


def test_repeat():
    def repeat(x, y):
        out = x * y
        return out, out is x, out is y

    repeat_verifier = verifier_for(repeat)

    repeat_verifier.verify([1, 2, 3], 2, expected_result=([1, 2, 3, 1, 2, 3], False, False))
    repeat_verifier.verify([1, 2], 4, expected_result=([1, 2, 1, 2, 1, 2, 1, 2], False, False))
    repeat_verifier.verify([1, 2, 3], 0, expected_result=([], False, False))
    repeat_verifier.verify([1, 2, 3], -1, expected_result=([], False, False))
    repeat_verifier.verify([1, 2, 3], -2, expected_result=([], False, False))

    # TODO: Support right versions of binary operators in bytecode translator
    # repeat_verifier.verify(2, [1, 2, 3], expected_result=([1, 2, 3, 1, 2, 3], False, False))
    # repeat_verifier.verify(4, [1, 2], expected_result=([1, 2, 1, 2, 1, 2, 1, 2], False, False))
    # repeat_verifier.verify(0, [1, 2, 3], expected_result=([], False, False))
    # repeat_verifier.verify(-1, [1, 2, 3], expected_result=([], False, False))
    # repeat_verifier.verify(-2, [1, 2, 3], expected_result=([], False, False))


def test_get_item():
    get_item_verifier = verifier_for(lambda tested, index: tested[index])

    get_item_verifier.verify([1, 2, 3], 1, expected_result=2)
    get_item_verifier.verify([1, 2, 3], -1, expected_result=3)
    get_item_verifier.verify([1, 2, 3, 4], -1, expected_result=4)
    get_item_verifier.verify([1, 2, 3, 4], -2, expected_result=3)
    get_item_verifier.verify([1, 2, 3, 4], 0, expected_result=1)
    get_item_verifier.verify([1, 2, 3], 3, expected_error=IndexError)
    get_item_verifier.verify([1, 2, 3], -4, expected_error=IndexError)


def test_get_slice():
    get_slice_verifier = verifier_for(lambda tested, start, end: tested[start:end])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, expected_result=[2, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -3, -1, expected_result=[3, 4])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, -2, expected_result=[1, 2, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -3, 4, expected_result=[3, 4])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, 1, expected_result=[])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -1, -3, expected_result=[])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 100, 1000, expected_result=[])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, 1000, expected_result=[1, 2, 3, 4, 5])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, expected_result=[2, 3, 4, 5])
    get_slice_verifier.verify([1, 2, 3, 4, 5], None, 2, expected_result=[1, 2])
    get_slice_verifier.verify([1, 2, 3, 4, 5], None, None, expected_result=[1, 2, 3, 4, 5])


def test_get_slice_with_step():
    get_slice_verifier = verifier_for(lambda tested, start, end, step: tested[start:end:step])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, None, 2, expected_result=[1, 3, 5])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, 2, expected_result=[2, 4])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, 5, 2, expected_result=[1, 3, 5])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, 5, 2, expected_result=[2, 4])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, -1, 2, expected_result=[1, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 1, -1, 2, expected_result=[2, 4])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 4, None, -2, expected_result=[5, 3, 1])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, None, -2, expected_result=[4, 2])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -1, -6, -2, expected_result=[5, 3, 1])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -2, -6, -2, expected_result=[4, 2])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 4, 0, -2, expected_result=[5, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, 0, -2, expected_result=[4, 2])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, None, None, expected_result=[1, 2, 3, 4, 5])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 0, 3, None, expected_result=[1, 2, 3])

    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, 1, -1, expected_result=[4, 3])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -1, -3, -1, expected_result=[5, 4])
    get_slice_verifier.verify([1, 2, 3, 4, 5], 3, 1, 1, expected_result=[])
    get_slice_verifier.verify([1, 2, 3, 4, 5], -1, -3, 1, expected_result=[])


def test_len():
    len_verifier = verifier_for(lambda tested: len(tested))

    len_verifier.verify([], expected_result=0)
    len_verifier.verify([1], expected_result=1)
    len_verifier.verify([1, 2], expected_result=2)
    len_verifier.verify([3, 2, 1], expected_result=3)


def test_index():
    index_verifier = verifier_for(lambda tested, item: tested.index(item))
    index_start_verifier = verifier_for(lambda tested, item, start: tested.index(item, start))
    index_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.index(item, start, end))

    index_verifier.verify([1, 2, 3], 1, expected_result=0)
    index_verifier.verify([1, 2, 3], 2, expected_result=1)
    index_verifier.verify([1, 2, 3], 5, expected_error=ValueError)

    index_start_verifier.verify([1, 2, 3], 1, 1, expected_error=ValueError)
    index_start_verifier.verify([1, 2, 3], 2, 1, expected_result=1)
    index_start_verifier.verify([1, 2, 3], 3, 1, expected_result=2)
    index_start_verifier.verify([1, 2, 3], 5, 1, expected_error=ValueError)

    index_start_verifier.verify([1, 2, 3], 1, -2, expected_error=ValueError)
    index_start_verifier.verify([1, 2, 3], 2, -2, expected_result=1)
    index_start_verifier.verify([1, 2, 3], 3, -2, expected_result=2)
    index_start_verifier.verify([1, 2, 3], 5, -2, expected_error=ValueError)

    index_start_end_verifier.verify([1, 2, 3], 1, 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify([1, 2, 3], 2, 1, 2, expected_result=1)
    index_start_end_verifier.verify([1, 2, 3], 3, 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify([1, 2, 3], 5, 1, 2, expected_error=ValueError)

    index_start_end_verifier.verify([1, 2, 3], 1, -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify([1, 2, 3], 2, -2, -1, expected_result=1)
    index_start_end_verifier.verify([1, 2, 3], 3, -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify([1, 2, 3], 5, -2, -1, expected_error=ValueError)


def test_count():
    count_verifier = verifier_for(lambda tested, item: tested.count(item))

    count_verifier.verify([1, 2, 3], 1, expected_result=1)
    count_verifier.verify([1, 2, 3], 2, expected_result=1)
    count_verifier.verify([1, 2, 3], 3, expected_result=1)
    count_verifier.verify([1, 2, 3], 4, expected_result=0)

    count_verifier.verify([1, 2, 3, 1], 1, expected_result=2)
    count_verifier.verify([1, 1, 3, 1], 1, expected_result=3)
    count_verifier.verify([], 1, expected_result=0)


def test_set_item():
    def set_item(tested, index, value):
        tested[index] = value
        return tested

    set_item_verifier = verifier_for(set_item)
    set_item_verifier.verify([1, 2, 3, 4, 5], 0, 10, expected_result=[10, 2, 3, 4, 5])
    set_item_verifier.verify([1, 2, 3, 4, 5], 2, -3, expected_result=[1, 2, -3, 4, 5])
    set_item_verifier.verify([1, 2, 3, 4, 5], -1, -5, expected_result=[1, 2, 3, 4, -5])
    set_item_verifier.verify([1, 2, 3, 4, 5], -10, 1, expected_error=IndexError)
    set_item_verifier.verify([1, 2, 3, 4, 5], 10, 1, expected_error=IndexError)

def test_set_slice():
    def set_slice(tested, start, stop, value):
        tested[start:stop] = value
        return tested

    set_slice_verifier = verifier_for(set_slice)
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, [], expected_result=[1, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 2, 2, [30], expected_result=[1, 2, 30, 3, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, [20], expected_result=[1, 20, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, [20, 30], expected_result=[1, 20, 30, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, [20, 30, 40], expected_result=[1, 20, 30, 40, 4, 5])

    set_slice_verifier.verify([1, 2, 3, 4, 5], -4, -2, [20, 30], expected_result=[1, 20, 30, 4, 5])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, -2, [20, 30], expected_result=[1, 20, 30, 4, 5])

    set_slice_verifier.verify([1, 2, 3, 4, 5], 5, 5, [6], expected_result=[1, 2, 3, 4, 5, 6])

    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, [], expected_result=[1])
    set_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, [20, 30], expected_result=[1, 20, 30])


def test_delete_slice():
    def delete_slice(tested, start, stop):
        del tested[start:stop]
        return tested

    delete_slice_verifier = verifier_for(delete_slice)
    delete_slice_verifier.verify([1, 2, 3, 4, 5], 1, 3, expected_result=[1, 4, 5])
    delete_slice_verifier.verify([1, 2, 3, 4, 5], 3, 5, expected_result=[1, 2, 3])
    delete_slice_verifier.verify([1, 2, 3, 4, 5], 1, None, expected_result=[1])
    delete_slice_verifier.verify([1, 2, 3, 4, 5], None, 3, expected_result=[4, 5])
    delete_slice_verifier.verify([1, 2, 3, 4, 5], None, None, expected_result=[])


def test_set_slice_with_step():
    def set_slice_with_step(tested, start, stop, step, value):
        tested[start:stop:step] = value
        return tested

    set_slice_with_step_verifier = verifier_for(set_slice_with_step)

    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 3, 0, -1, [20, 30, 40], expected_result=[1, 40, 30, 20, 5])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 1, 4, 2, [20, 40], expected_result=[1, 20, 3, 40, 5])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 1, -1, 2, [20, 40], expected_result=[1, 20, 3, 40, 5])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 0, None, 2, [10, 30, 50],
                                        expected_result=[10, 2, 30, 4, 50])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], None, 4, 2, [10, 30],
                                        expected_result=[10, 2, 30, 4, 5])
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], None, None, 2, [10, 30, 50],
                                        expected_result=[10, 2, 30, 4, 50])

    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 3, 0, -1, [], expected_error=ValueError)
    set_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 3, 0, -1, [20, 30, 40, 50], expected_error=ValueError)


def test_delete_slice_with_step():
    def delete_slice_with_step(tested, start, stop, step):
        del tested[start:stop:step]
        return tested

    delete_slice_with_step_verifier = verifier_for(delete_slice_with_step)

    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 3, 0, -1, expected_result=[1, 5])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 1, 4, 2, expected_result=[1, 3, 5])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 1, -1, 2, expected_result=[1, 3, 5])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], 0, None, 2,
                                        expected_result=[2, 4])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], None, 4, 2,
                                        expected_result=[2, 4, 5])
    delete_slice_with_step_verifier.verify([1, 2, 3, 4, 5], None, None, 2,
                                        expected_result=[2, 4])


def test_append():
    def append(tested, item):
        tested.append(item)
        return tested

    append_verifier = verifier_for(append)

    append_verifier.verify([], 1, expected_result=[1])
    append_verifier.verify([1], 2, expected_result=[1, 2])
    append_verifier.verify([1, 2], 3, expected_result=[1, 2, 3])
    append_verifier.verify([1, 2, 3], 3, expected_result=[1, 2, 3, 3])


def test_clear():
    def clear(tested):
        tested.clear()
        return tested

    clear_verifier = verifier_for(clear)

    clear_verifier.verify([], expected_result=[])
    clear_verifier.verify([1], expected_result=[])
    clear_verifier.verify([1, 2], expected_result=[])
    clear_verifier.verify([1, 2, 3], expected_result=[])


def test_copy():
    def copy(tested):
        out = tested.copy()
        return out, out is tested

    copy_verifier = verifier_for(copy)

    copy_verifier.verify([], expected_result=([], False))
    copy_verifier.verify([1], expected_result=([1], False))
    copy_verifier.verify([1, 2], expected_result=([1, 2], False))
    copy_verifier.verify([1, 2, 3], expected_result=([1, 2, 3], False))


def test_extend():
    def extend(tested, item):
        tested.extend(item)
        return tested

    extend_verifier = verifier_for(extend)

    extend_verifier.verify([], [1], expected_result=[1])
    extend_verifier.verify([1], [2], expected_result=[1, 2])
    extend_verifier.verify([1, 2], [3], expected_result=[1, 2, 3])
    extend_verifier.verify([1, 2, 3], [], expected_result=[1, 2, 3])
    extend_verifier.verify([1, 2, 3], [4, 5], expected_result=[1, 2, 3, 4, 5])
    extend_verifier.verify([1, 2, 3], [[4, 5], [6, 7]], expected_result=[1, 2, 3, [4, 5], [6, 7]])


def test_inplace_add():
    def extend(tested, item):
        tested += item
        return tested

    extend_verifier = verifier_for(extend)

    extend_verifier.verify([], [1], expected_result=[1])
    extend_verifier.verify([1], [2], expected_result=[1, 2])
    extend_verifier.verify([1, 2], [3], expected_result=[1, 2, 3])
    extend_verifier.verify([1, 2, 3], [], expected_result=[1, 2, 3])
    extend_verifier.verify([1, 2, 3], [4, 5], expected_result=[1, 2, 3, 4, 5])
    extend_verifier.verify([1, 2, 3], [[4, 5], [6, 7]], expected_result=[1, 2, 3, [4, 5], [6, 7]])


def test_inplace_multiply():
    def multiply(tested, item):
        tested *= item
        return tested

    multiply_verifier = verifier_for(multiply)

    multiply_verifier.verify([1, 2, 3], 1, expected_result=[1, 2, 3])
    multiply_verifier.verify([1, 2], 2, expected_result=[1, 2, 1, 2])
    multiply_verifier.verify([1, 2], 3, expected_result=[1, 2, 1, 2, 1, 2])
    multiply_verifier.verify([1, 2, 3], 0, expected_result=[])
    multiply_verifier.verify([1, 2, 3], -1, expected_result=[])



def test_insert():
    def insert(tested, index, item):
        tested.insert(index, item)
        return tested

    insert_verifier = verifier_for(insert)

    insert_verifier.verify([], 0, 1, expected_result=[1])
    insert_verifier.verify([1], 0, 2, expected_result=[2, 1])
    insert_verifier.verify([1], 1, 2, expected_result=[1, 2])
    insert_verifier.verify([1, 2], 0, 3, expected_result=[3, 1, 2])
    insert_verifier.verify([1, 2], 1, 3, expected_result=[1, 3, 2])
    insert_verifier.verify([1, 2], 2, 3, expected_result=[1, 2, 3])
    insert_verifier.verify([1, 2, 3], -1, 4, expected_result=[1, 2, 4, 3])
    insert_verifier.verify([1, 2, 3], -2, 4, expected_result=[1, 4, 2, 3])
    insert_verifier.verify([1, 2, 3], 3, 4, expected_result=[1, 2, 3, 4])
    insert_verifier.verify([1, 2, 3], 4, 4, expected_result=[1, 2, 3, 4])
    insert_verifier.verify([1, 2, 3], -4, 4, expected_result=[4, 1, 2, 3])
    insert_verifier.verify([1, 2, 3], -5, 4, expected_result=[4, 1, 2, 3])


def test_pop():
    def pop(tested):
        item = tested.pop()
        return item, tested

    pop_verifier = verifier_for(pop)

    pop_verifier.verify([1, 2, 3], expected_result=(3, [1, 2]))
    pop_verifier.verify([1, 2], expected_result=(2, [1]))
    pop_verifier.verify([1], expected_result=(1, []))

    pop_verifier.verify([1, 2, 5], expected_result=(5, [1, 2]))

    pop_verifier.verify([], expected_error=IndexError)


def test_pop_at_index():
    def pop_at_index(tested, index):
        item = tested.pop(index)
        return item, tested

    pop_at_index_verifier = verifier_for(pop_at_index)

    pop_at_index_verifier.verify([1, 2, 3], -1, expected_result=(3, [1, 2]))
    pop_at_index_verifier.verify([1, 2], -1, expected_result=(2, [1]))
    pop_at_index_verifier.verify([1], -1, expected_result=(1, []))

    pop_at_index_verifier.verify([1, 2, 3], 1, expected_result=(2, [1, 3]))
    pop_at_index_verifier.verify([1, 2, 3], 0, expected_result=(1, [2, 3]))
    pop_at_index_verifier.verify([1, 2, 3], 2, expected_result=(3, [1, 2]))
    pop_at_index_verifier.verify([1, 2, 3], -2, expected_result=(2, [1, 3]))

    pop_at_index_verifier.verify([1, 2, 3], -4, expected_error=IndexError)
    pop_at_index_verifier.verify([1, 2, 3], 4, expected_error=IndexError)
    pop_at_index_verifier.verify([], 0, expected_error=IndexError)


def test_remove():
    def remove(tested, item):
        tested.remove(item)
        return tested

    remove_verifier = verifier_for(remove)

    remove_verifier.verify([1, 2, 3], 1, expected_result=[2, 3])
    remove_verifier.verify([1, 2, 3], 2, expected_result=[1, 3])
    remove_verifier.verify([1, 2, 3], 3, expected_result=[1, 2])

    remove_verifier.verify([1, 3, 5], 3, expected_result=[1, 5])

    remove_verifier.verify([1, 2, 3], 4, expected_error=ValueError)
    remove_verifier.verify([], 1, expected_error=ValueError)


def test_reverse():
    def reverse(tested):
        tested.reverse()
        return tested

    reverse_verifier = verifier_for(reverse)

    reverse_verifier.verify([1, 2, 3], expected_result=[3, 2, 1])
    reverse_verifier.verify([3, 2, 1], expected_result=[1, 2, 3])
    reverse_verifier.verify([1, 2], expected_result=[2, 1])
    reverse_verifier.verify([2, 1], expected_result=[1, 2])
    reverse_verifier.verify([1], expected_result=[1])
    reverse_verifier.verify([], expected_result=[])


def test_sort():
    def sort(tested):
        tested.sort()
        return tested

    sort_verifier = verifier_for(sort)

    sort_verifier.verify([1, 2, 3], expected_result=[1, 2, 3])
    sort_verifier.verify([1, 3, 2], expected_result=[1, 2, 3])
    sort_verifier.verify([2, 1, 3], expected_result=[1, 2, 3])
    sort_verifier.verify([2, 3, 1], expected_result=[1, 2, 3])
    sort_verifier.verify([3, 1, 2], expected_result=[1, 2, 3])
    sort_verifier.verify([3, 2, 1], expected_result=[1, 2, 3])
