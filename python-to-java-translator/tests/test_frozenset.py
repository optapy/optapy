from .conftest import verifier_for


def test_len():
    len_verifier = verifier_for(lambda tested: len(tested))
    len_verifier.verify(frozenset(), expected_result=0)
    len_verifier.verify(frozenset({1, 2, 3}), expected_result=3)


def test_membership():
    membership_verifier = verifier_for(lambda tested, x: x in tested)
    not_membership_verifier = verifier_for(lambda tested, x: x not in tested)

    membership_verifier.verify(frozenset(), 1, expected_result=False)
    not_membership_verifier.verify(frozenset(), 1, expected_result=True)

    membership_verifier.verify(frozenset({1, 2, 3}), 1, expected_result=True)
    not_membership_verifier.verify(frozenset({1, 2, 3}), 1, expected_result=False)


def test_isdisjoint():
    isdisjoint_verifier = verifier_for(lambda x, y: x.isdisjoint(y))

    isdisjoint_verifier.verify(frozenset({1, 2, 3}), frozenset({4, 5, 6}), expected_result=True)
    isdisjoint_verifier.verify(frozenset({1, 2, 3}), frozenset({3, 4, 5}), expected_result=False)


def test_issubset():
    issubset_verifier = verifier_for(lambda x, y: x.issubset(y))
    subset_le_verifier = verifier_for(lambda x, y: x <= y)
    subset_strict_verifier = verifier_for(lambda x, y: x < y)

    issubset_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=True)
    subset_le_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=True)
    subset_strict_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=True)

    issubset_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=True)
    subset_le_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=True)
    subset_strict_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=True)

    issubset_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=True)
    subset_le_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=True)
    subset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=False)

    issubset_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)
    subset_le_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)
    subset_strict_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)

    issubset_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=False)
    subset_le_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=False)
    subset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=False)

    issubset_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=False)
    subset_le_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=False)
    subset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=False)


def test_issuperset():
    issuperset_verifier = verifier_for(lambda x, y: x.issuperset(y))
    superset_ge_verifier = verifier_for(lambda x, y: x >= y)
    superset_strict_verifier = verifier_for(lambda x, y: x > y)

    issuperset_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=False)
    superset_ge_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=False)
    superset_strict_verifier.verify(frozenset(), frozenset({1, 2, 3}), expected_result=False)

    issuperset_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=False)
    superset_ge_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=False)
    superset_strict_verifier.verify(frozenset({1}), frozenset({1, 2, 3}), expected_result=False)

    issuperset_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=True)
    superset_ge_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=True)
    superset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=False)

    issuperset_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)
    superset_ge_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)
    superset_strict_verifier.verify(frozenset({1, 4}), frozenset({1, 2, 3}), expected_result=False)

    issuperset_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=True)
    superset_ge_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=True)
    superset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset({1}), expected_result=True)

    issuperset_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=True)
    superset_ge_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=True)
    superset_strict_verifier.verify(frozenset({1, 2, 3}), frozenset(), expected_result=True)


def test_union():
    union_verifier = verifier_for(lambda x, y: x.union(y))
    union_or_verifier = verifier_for(lambda x, y: x | y)

    union_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1, 2}))
    union_or_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1, 2}))

    union_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1, 2, 3}))
    union_or_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1, 2, 3}))

    union_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset({1}))
    union_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset({1}))

    union_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset({1, 2, 3}))
    union_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset({1, 2, 3}))


def test_intersection():
    intersection_verifier = verifier_for(lambda x, y: x.intersection(y))
    intersection_and_verifier = verifier_for(lambda x, y: x & y)

    intersection_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset())
    intersection_and_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset())

    intersection_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({2}))
    intersection_and_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({2}))

    intersection_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset())
    intersection_and_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset())

    intersection_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset({1, 2, 3}))
    intersection_and_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset({1, 2, 3}))


def test_difference():
    difference_verifier = verifier_for(lambda x, y: x.difference(y))
    difference_subtract_verifier = verifier_for(lambda x, y: x - y)

    difference_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1}))
    difference_subtract_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1}))

    difference_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1}))
    difference_subtract_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1}))

    difference_verifier.verify(frozenset({2, 3}), frozenset({1, 2}), expected_result=frozenset({3}))
    difference_subtract_verifier.verify(frozenset({2, 3}), frozenset({1, 2}), expected_result=frozenset({3}))

    difference_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset())
    difference_subtract_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset())

    difference_verifier.verify(frozenset({1}), frozenset(), expected_result=frozenset({1}))
    difference_subtract_verifier.verify(frozenset({1}), frozenset(), expected_result=frozenset({1}))

    difference_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset())
    difference_subtract_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset())


def test_symmetric_difference():
    symmetric_difference_verifier = verifier_for(lambda x, y: x.symmetric_difference(y))
    symmetric_difference_xor_verifier = verifier_for(lambda x, y: x ^ y)

    symmetric_difference_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1, 2}))
    symmetric_difference_xor_verifier.verify(frozenset({1}), frozenset({2}), expected_result=frozenset({1, 2}))

    symmetric_difference_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1, 3}))
    symmetric_difference_xor_verifier.verify(frozenset({1, 2}), frozenset({2, 3}), expected_result=frozenset({1, 3}))

    symmetric_difference_verifier.verify(frozenset({2, 3}), frozenset({1, 2}), expected_result=frozenset({1, 3}))
    symmetric_difference_xor_verifier.verify(frozenset({2, 3}), frozenset({1, 2}), expected_result=frozenset({1, 3}))

    symmetric_difference_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset({1}))
    symmetric_difference_xor_verifier.verify(frozenset(), frozenset({1}), expected_result=frozenset({1}))

    symmetric_difference_verifier.verify(frozenset({1}), frozenset(), expected_result=frozenset({1}))
    symmetric_difference_xor_verifier.verify(frozenset({1}), frozenset(), expected_result=frozenset({1}))

    symmetric_difference_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset())
    symmetric_difference_xor_verifier.verify(frozenset({1, 2, 3}), frozenset({1, 2, 3}), expected_result=frozenset())


def test_copy():
    def copy_function(x):
        out = x.copy()
        return out, out is x

    copy_verifier = verifier_for(copy_function)

    copy_verifier.verify(frozenset(), expected_result=(frozenset(), True))
    copy_verifier.verify(frozenset({1}), expected_result=(frozenset({1}), True))
    copy_verifier.verify(frozenset({1, 2, 3}), expected_result=(frozenset({1, 2, 3}), True))
