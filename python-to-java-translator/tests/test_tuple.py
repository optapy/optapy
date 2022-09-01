from .conftest import verifier_for


def test_membership():
    membership_verifier = verifier_for(lambda tested, x: x in tested)
    not_membership_verifier = verifier_for(lambda tested, x: x not in tested)

    membership_verifier.verify((1, 2, 3), 1, expected_result=True)
    not_membership_verifier.verify((1, 2, 3), 1, expected_result=False)

    membership_verifier.verify((1, 2, 3), 4, expected_result=False)
    not_membership_verifier.verify((1, 2, 3), 4, expected_result=True)

    membership_verifier.verify((1, 2, 3), 3, expected_result=True)
    not_membership_verifier.verify((1, 2, 3), 3, expected_result=False)


def test_concat():
    def concat(x, y):
        out = x + y
        return out, out is x, out is y

    concat_verifier = verifier_for(concat)

    concat_verifier.verify((1, 2), (3, 4), expected_result=((1, 2, 3, 4), False, False))
    concat_verifier.verify((), (1, 2, 3), expected_result=((1, 2, 3), False, True))
    concat_verifier.verify((1, 2, 3), (), expected_result=((1, 2, 3), True, False))
    concat_verifier.verify((3,), (2, 1), expected_result=((3, 2, 1), False, False))


def test_repeat():
    def repeat(x, y):
        out = x * y
        return out, out is x, out is y

    repeat_verifier = verifier_for(repeat)

    repeat_verifier.verify((1, 2, 3), 1, expected_result=((1, 2, 3), True, False))
    repeat_verifier.verify((1, 2, 3), 2, expected_result=((1, 2, 3, 1, 2, 3), False, False))
    repeat_verifier.verify((1, 2), 4, expected_result=((1, 2, 1, 2, 1, 2, 1, 2), False, False))
    repeat_verifier.verify((1, 2, 3), 0, expected_result=((), False, False))
    repeat_verifier.verify((1, 2, 3), -1, expected_result=((), False, False))
    repeat_verifier.verify((1, 2, 3), -2, expected_result=((), False, False))

    # TODO: Support right versions of binary operators in bytecode translator
    # repeat_verifier.verify(2, (1, 2, 3), expected_result=((1, 2, 3, 1, 2, 3), False, False))
    # repeat_verifier.verify(4, (1, 2), expected_result=((1, 2, 1, 2, 1, 2, 1, 2), False, False))
    # repeat_verifier.verify(0, (1, 2, 3), expected_result=((), False, False))
    # repeat_verifier.verify(-1, (1, 2, 3), expected_result=((), False, False))
    # repeat_verifier.verify(-2, (1, 2, 3), expected_result=((), False, False))


def test_get_item():
    get_item_verifier = verifier_for(lambda tested, index: tested[index])

    get_item_verifier.verify((1, 2, 3), 1, expected_result=2)
    get_item_verifier.verify((1, 2, 3), -1, expected_result=3)
    get_item_verifier.verify((1, 2, 3, 4), -1, expected_result=4)
    get_item_verifier.verify((1, 2, 3, 4), -2, expected_result=3)
    get_item_verifier.verify((1, 2, 3, 4), 0, expected_result=1)
    get_item_verifier.verify((1, 2, 3), 3, expected_error=IndexError)
    get_item_verifier.verify((1, 2, 3), -4, expected_error=IndexError)


def test_get_slice():
    get_slice_verifier = verifier_for(lambda tested, start, end: tested[start:end])

    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, 3, expected_result=(2, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -3, -1, expected_result=(3, 4))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, -2, expected_result=(1, 2, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -3, 4, expected_result=(3, 4))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, 1, expected_result=())
    get_slice_verifier.verify((1, 2, 3, 4, 5), -1, -3, expected_result=())

    get_slice_verifier.verify((1, 2, 3, 4, 5), 100, 1000, expected_result=())
    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, 1000, expected_result=(1, 2, 3, 4, 5))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, None, expected_result=(2, 3, 4, 5))
    get_slice_verifier.verify((1, 2, 3, 4, 5), None, 2, expected_result=(1, 2))
    get_slice_verifier.verify((1, 2, 3, 4, 5), None, None, expected_result=(1, 2, 3, 4, 5))


def test_get_slice_with_step():
    get_slice_verifier = verifier_for(lambda tested, start, end, step: tested[start:end:step])

    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, None, 2, expected_result=(1, 3, 5))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, None, 2, expected_result=(2, 4))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, 5, 2, expected_result=(1, 3, 5))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, 5, 2, expected_result=(2, 4))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, -1, 2, expected_result=(1, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 1, -1, 2, expected_result=(2, 4))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 4, None, -2, expected_result=(5, 3, 1))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, None, -2, expected_result=(4, 2))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -1, -6, -2, expected_result=(5, 3, 1))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -2, -6, -2, expected_result=(4, 2))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 4, 0, -2, expected_result=(5, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, 0, -2, expected_result=(4, 2))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, None, None, expected_result=(1, 2, 3, 4, 5))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 0, 3, None, expected_result=(1, 2, 3))

    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, 1, -1, expected_result=(4, 3))
    get_slice_verifier.verify((1, 2, 3, 4, 5), -1, -3, -1, expected_result=(5, 4))
    get_slice_verifier.verify((1, 2, 3, 4, 5), 3, 1, 1, expected_result=())
    get_slice_verifier.verify((1, 2, 3, 4, 5), -1, -3, 1, expected_result=())


def test_len():
    len_verifier = verifier_for(lambda tested: len(tested))

    len_verifier.verify((), expected_result=0)
    len_verifier.verify((1,), expected_result=1)
    len_verifier.verify((1, 2), expected_result=2)
    len_verifier.verify((3, 2, 1), expected_result=3)


def test_index():
    index_verifier = verifier_for(lambda tested, item: tested.index(item))
    index_start_verifier = verifier_for(lambda tested, item, start: tested.index(item, start))
    index_start_end_verifier = verifier_for(lambda tested, item, start, end: tested.index(item, start, end))

    index_verifier.verify((1, 2, 3), 1, expected_result=0)
    index_verifier.verify((1, 2, 3), 2, expected_result=1)
    index_verifier.verify((1, 2, 3), 5, expected_error=ValueError)

    index_start_verifier.verify((1, 2, 3), 1, 1, expected_error=ValueError)
    index_start_verifier.verify((1, 2, 3), 2, 1, expected_result=1)
    index_start_verifier.verify((1, 2, 3), 3, 1, expected_result=2)
    index_start_verifier.verify((1, 2, 3), 5, 1, expected_error=ValueError)

    index_start_verifier.verify((1, 2, 3), 1, -2, expected_error=ValueError)
    index_start_verifier.verify((1, 2, 3), 2, -2, expected_result=1)
    index_start_verifier.verify((1, 2, 3), 3, -2, expected_result=2)
    index_start_verifier.verify((1, 2, 3), 5, -2, expected_error=ValueError)

    index_start_end_verifier.verify((1, 2, 3), 1, 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify((1, 2, 3), 2, 1, 2, expected_result=1)
    index_start_end_verifier.verify((1, 2, 3), 3, 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify((1, 2, 3), 5, 1, 2, expected_error=ValueError)

    index_start_end_verifier.verify((1, 2, 3), 1, -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify((1, 2, 3), 2, -2, -1, expected_result=1)
    index_start_end_verifier.verify((1, 2, 3), 3, -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify((1, 2, 3), 5, -2, -1, expected_error=ValueError)


def test_count():
    count_verifier = verifier_for(lambda tested, item: tested.count(item))

    count_verifier.verify((1, 2, 3), 1, expected_result=1)
    count_verifier.verify((1, 2, 3), 2, expected_result=1)
    count_verifier.verify((1, 2, 3), 3, expected_result=1)
    count_verifier.verify((1, 2, 3), 4, expected_result=0)

    count_verifier.verify((1, 2, 3, 1), 1, expected_result=2)
    count_verifier.verify((1, 1, 3, 1), 1, expected_result=3)
    count_verifier.verify((), 1, expected_result=0)

