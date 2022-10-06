from .conftest import verifier_for

MAX_LONG = 0xFFFF_FFFF_FFFF_FFFF
MIN_LONG = -MAX_LONG


def test_add():
    add_verifier = verifier_for(lambda a, b: a + b)

    add_verifier.verify(1.0, 1, expected_result=2.0)
    add_verifier.verify(1.0, -1, expected_result=0.0)
    add_verifier.verify(-1.0, 1, expected_result=0.0)
    add_verifier.verify(0.0, 1, expected_result=1.0)
    add_verifier.verify(MAX_LONG + 0.0, 1, expected_result=(MAX_LONG + 1.0))
    add_verifier.verify(MIN_LONG + 0.0, -1, expected_result=(MIN_LONG - 1.0))

    add_verifier.verify(1.0, 1.0, expected_result=2.0)
    add_verifier.verify(1.0, -1.0, expected_result=0.0)
    add_verifier.verify(-1.0, 1.0, expected_result=0.0)
    add_verifier.verify(0.0, 1.0, expected_result=1.0)
    add_verifier.verify(MAX_LONG + 0.0, 1.0, expected_result=(MAX_LONG + 1.0))
    add_verifier.verify(MIN_LONG + 0.0, -1.0, expected_result=(MIN_LONG - 1.0))


def test_iadd():
    def iadd(x, y):
        old = x
        x += y
        if y != 0:
            assert old is not x
        return x

    iadd_verifier = verifier_for(iadd)

    iadd_verifier.verify(1.0, 1, expected_result=2.0)
    iadd_verifier.verify(1.0, -1, expected_result=0.0)
    iadd_verifier.verify(-1.0, 1, expected_result=0.0)
    iadd_verifier.verify(0.0, 1, expected_result=1.0)
    iadd_verifier.verify(MAX_LONG + 0.0, 1, expected_result=(MAX_LONG + 1.0))
    iadd_verifier.verify(MIN_LONG + 0.0, -1, expected_result=(MIN_LONG - 1.0))

    iadd_verifier.verify(1.0, 1.0, expected_result=2.0)
    iadd_verifier.verify(1.0, -1.0, expected_result=0.0)
    iadd_verifier.verify(-1.0, 1.0, expected_result=0.0)
    iadd_verifier.verify(0.0, 1.0, expected_result=1.0)
    iadd_verifier.verify(MAX_LONG + 0.0, 1.0, expected_result=(MAX_LONG + 1.0))
    iadd_verifier.verify(MIN_LONG + 0.0, -1.0, expected_result=(MIN_LONG - 1.0))


def test_sub():
    sub_verifier = verifier_for(lambda a, b: a - b)

    sub_verifier.verify(1.0, 1, expected_result=0.0)
    sub_verifier.verify(1.0, -1, expected_result=2.0)
    sub_verifier.verify(-1.0, 1, expected_result=-2.0)
    sub_verifier.verify(0.0, 1, expected_result=-1.0)
    sub_verifier.verify(MAX_LONG + 0.0, -1, expected_result=(MAX_LONG + 1.0))
    sub_verifier.verify(MIN_LONG + 0.0, 1, expected_result=(MIN_LONG - 1.0))

    sub_verifier.verify(1.0, 1.0, expected_result=0.0)
    sub_verifier.verify(1.0, -1.0, expected_result=2.0)
    sub_verifier.verify(-1.0, 1.0, expected_result=-2.0)
    sub_verifier.verify(0.0, 1.0, expected_result=-1.0)
    sub_verifier.verify(MAX_LONG + 0.0, -1.0, expected_result=(MAX_LONG + 1.0))
    sub_verifier.verify(MIN_LONG + 0.0, 1.0, expected_result=(MIN_LONG - 1.0))


def test_isub():
    def isub(x, y):
        old = x
        x -= y
        if y != 0:
            assert old is not x
        return x

    isub_verifier = verifier_for(isub)

    isub_verifier.verify(1.0, 1, expected_result=0.0)
    isub_verifier.verify(1.0, -1, expected_result=2.0)
    isub_verifier.verify(-1.0, 1, expected_result=-2.0)
    isub_verifier.verify(0.0, 1, expected_result=-1.0)
    isub_verifier.verify(MAX_LONG + 0.0, -1, expected_result=(MAX_LONG + 1.0))
    isub_verifier.verify(MIN_LONG + 0.0, 1, expected_result=(MIN_LONG - 1.0))

    isub_verifier.verify(1.0, 1.0, expected_result=0.0)
    isub_verifier.verify(1.0, -1.0, expected_result=2.0)
    isub_verifier.verify(-1.0, 1.0, expected_result=-2.0)
    isub_verifier.verify(0.0, 1.0, expected_result=-1.0)
    isub_verifier.verify(MAX_LONG + 0.0, -1.0, expected_result=(MAX_LONG + 1.0))
    isub_verifier.verify(MIN_LONG + 0.0, 1.0, expected_result=(MIN_LONG - 1.0))


def test_multiply():
    multiply_verifier = verifier_for(lambda a, b: a * b)

    multiply_verifier.verify(1.0, 1, expected_result=1.0)
    multiply_verifier.verify(1.0, -1, expected_result=-1.0)
    multiply_verifier.verify(-1.0, 1, expected_result=-1.0)
    multiply_verifier.verify(0.0, 1, expected_result=0.0)
    multiply_verifier.verify(2.0, 3, expected_result=6.0)
    multiply_verifier.verify(MAX_LONG + 0.0, 2, expected_result=(2.0 * MAX_LONG))
    multiply_verifier.verify(MIN_LONG + 0.0, 2, expected_result=(2.0 * MIN_LONG))

    multiply_verifier.verify(1.0, 1.0, expected_result=1.0)
    multiply_verifier.verify(1.0, -1.0, expected_result=-1.0)
    multiply_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    multiply_verifier.verify(0.0, 1.0, expected_result=0.0)
    multiply_verifier.verify(2.0, 3.0, expected_result=6.0)
    multiply_verifier.verify(MAX_LONG + 0.0, 2.0, expected_result=(2.0 * MAX_LONG))
    multiply_verifier.verify(MIN_LONG + 0.0, 2.0, expected_result=(2.0 * MIN_LONG))


def test_imultiply():
    def imultiply(x, y):
        old = x
        x *= y
        if y != 1:
            assert old is not x
        return x

    imultiply_verifier = verifier_for(imultiply)

    imultiply_verifier.verify(1.0, 1, expected_result=1.0)
    imultiply_verifier.verify(1.0, -1, expected_result=-1.0)
    imultiply_verifier.verify(-1.0, 1, expected_result=-1.0)
    imultiply_verifier.verify(0.0, 1, expected_result=0.0)
    imultiply_verifier.verify(2.0, 3, expected_result=6.0)
    imultiply_verifier.verify(MAX_LONG + 0.0, 2, expected_result=(2.0 * MAX_LONG))
    imultiply_verifier.verify(MIN_LONG + 0.0, 2, expected_result=(2.0 * MIN_LONG))

    imultiply_verifier.verify(1.0, 1.0, expected_result=1.0)
    imultiply_verifier.verify(1.0, -1.0, expected_result=-1.0)
    imultiply_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    imultiply_verifier.verify(0.0, 1.0, expected_result=0.0)
    imultiply_verifier.verify(2.0, 3.0, expected_result=6.0)
    imultiply_verifier.verify(MAX_LONG + 0.0, 2.0, expected_result=(2.0 * MAX_LONG))
    imultiply_verifier.verify(MIN_LONG + 0.0, 2.0, expected_result=(2.0 * MIN_LONG))


def test_truediv():
    truediv_verifier = verifier_for(lambda a, b: a / b)

    truediv_verifier.verify(1.0, 1, expected_result=1.0)
    truediv_verifier.verify(1.0, -1, expected_result=-1.0)
    truediv_verifier.verify(-1.0, 1, expected_result=-1.0)
    truediv_verifier.verify(0.0, 1, expected_result=0.0)
    truediv_verifier.verify(3.0, 2, expected_result=1.5)
    truediv_verifier.verify(2.0 * MAX_LONG, 2, expected_result=1.8446744073709552e+19)
    truediv_verifier.verify(2.0 * MIN_LONG, 2, expected_result=-1.8446744073709552e+19)

    truediv_verifier.verify(1.0, 1.0, expected_result=1.0)
    truediv_verifier.verify(1.0, -1.0, expected_result=-1.0)
    truediv_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    truediv_verifier.verify(0.0, 1.0, expected_result=0.0)
    truediv_verifier.verify(3.0, 2.0, expected_result=1.5)
    truediv_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    truediv_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)


def test_itruediv():
    def itruediv(x, y):
        old = x
        x /= y
        if y != 1:
            assert old is not x
        return x

    itruediv_verifier = verifier_for(itruediv)

    itruediv_verifier.verify(1.0, 1, expected_result=1.0)
    itruediv_verifier.verify(1.0, -1, expected_result=-1.0)
    itruediv_verifier.verify(-1.0, 1, expected_result=-1.0)
    itruediv_verifier.verify(0.0, 1, expected_result=0.0)
    itruediv_verifier.verify(3.0, 2, expected_result=1.5)
    itruediv_verifier.verify(2.0 * MAX_LONG, 2, expected_result=1.8446744073709552e+19)
    itruediv_verifier.verify(2.0 * MIN_LONG, 2, expected_result=-1.8446744073709552e+19)

    itruediv_verifier.verify(1.0, 1.0, expected_result=1.0)
    itruediv_verifier.verify(1.0, -1.0, expected_result=-1.0)
    itruediv_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    itruediv_verifier.verify(0.0, 1.0, expected_result=0.0)
    itruediv_verifier.verify(3.0, 2.0, expected_result=1.5)
    itruediv_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    itruediv_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)


def test_floordiv():
    floordiv_verifier = verifier_for(lambda a, b: a // b)

    floordiv_verifier.verify(1.0, 1, expected_result=1.0)
    floordiv_verifier.verify(1.0, -1, expected_result=-1.0)
    floordiv_verifier.verify(-1.0, 1, expected_result=-1.0)
    floordiv_verifier.verify(0.0, 1, expected_result=0.0)
    floordiv_verifier.verify(3.0, 2, expected_result=1.0)
    floordiv_verifier.verify(2.0 * MAX_LONG, 2, expected_result=(MAX_LONG + 0.0))
    floordiv_verifier.verify(2.0 * MIN_LONG, 2, expected_result=(MIN_LONG + 0.0))

    floordiv_verifier.verify(1.0, 1.0, expected_result=1.0)
    floordiv_verifier.verify(1.0, -1.0, expected_result=-1.0)
    floordiv_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    floordiv_verifier.verify(0.0, 1.0, expected_result=0.0)
    floordiv_verifier.verify(3.0, 2.0, expected_result=1.0)
    floordiv_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    floordiv_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)


def test_ifloordiv():
    def ifloordiv(x, y):
        old = x
        x //= y
        if y != 1:
            assert old is not x
        return x

    ifloordiv_verifier = verifier_for(ifloordiv)

    ifloordiv_verifier.verify(1.0, 1, expected_result=1.0)
    ifloordiv_verifier.verify(1.0, -1, expected_result=-1.0)
    ifloordiv_verifier.verify(-1.0, 1, expected_result=-1.0)
    ifloordiv_verifier.verify(0.0, 1, expected_result=0.0)
    ifloordiv_verifier.verify(3.0, 2, expected_result=1.0)
    ifloordiv_verifier.verify(2.0 * MAX_LONG, 2, expected_result=(MAX_LONG + 0.0))
    ifloordiv_verifier.verify(2.0 * MIN_LONG, 2, expected_result=(MIN_LONG + 0.0))

    ifloordiv_verifier.verify(1.0, 1.0, expected_result=1.0)
    ifloordiv_verifier.verify(1.0, -1.0, expected_result=-1.0)
    ifloordiv_verifier.verify(-1.0, 1.0, expected_result=-1.0)
    ifloordiv_verifier.verify(0.0, 1.0, expected_result=0.0)
    ifloordiv_verifier.verify(3.0, 2.0, expected_result=1.0)
    ifloordiv_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    ifloordiv_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)


def test_negate():
    negate_verifier = verifier_for(lambda x: -x)

    negate_verifier.verify(1.0, expected_result=-1.0)
    negate_verifier.verify(-1.0, expected_result=1.0)
    negate_verifier.verify(MAX_LONG + 0.0, expected_result=-(MAX_LONG + 0.0))
    negate_verifier.verify(MIN_LONG + 0.0, expected_result=-(MIN_LONG + 0.0))


def test_pos():
    pos_verifier = verifier_for(lambda x: +x)

    pos_verifier.verify(1.0, expected_result=1.0)
    pos_verifier.verify(-1.0, expected_result=-1.0)
    pos_verifier.verify(MAX_LONG + 0.0, expected_result=(MAX_LONG + 0.0))
    pos_verifier.verify(MIN_LONG + 0.0, expected_result=(MIN_LONG + 0.0))


def test_abs():
    abs_verifier = verifier_for(lambda x: abs(x))

    abs_verifier.verify(1.0, expected_result=1.0)
    abs_verifier.verify(-1.0, expected_result=1.0)
    abs_verifier.verify(MAX_LONG + 0.0, expected_result=(MAX_LONG + 0.0))
    abs_verifier.verify(MIN_LONG + 0.0, expected_result=-(MIN_LONG + 0.0))


def test_divmod():
    divmod_verifier = verifier_for(lambda x, y: divmod(x, y))

    divmod_verifier.verify(1.0, 1, expected_result=(1.0, 0.0))
    divmod_verifier.verify(1.0, -1, expected_result=(-1.0, 0.0))
    divmod_verifier.verify(-1.0, 1, expected_result=(-1.0, 0.0))
    divmod_verifier.verify(0.0, 1, expected_result=(0.0, 0.0))
    divmod_verifier.verify(3.0, 2, expected_result=(1.0, 1.0))
    divmod_verifier.verify(-3.0, -2, expected_result=(1.0, -1.0))
    divmod_verifier.verify(2.0 * MAX_LONG, 2, expected_result=(MAX_LONG + 0.0, 0.0))
    divmod_verifier.verify(2.0 * MIN_LONG, 2, expected_result=(MIN_LONG + 0.0, 0.0))

    divmod_verifier.verify(1.0, 1.0, expected_result=(1.0, 0.0))
    divmod_verifier.verify(1.0, -1.0, expected_result=(-1.0, 0.0))
    divmod_verifier.verify(-1.0, 1.0, expected_result=(-1.0, 0.0))
    divmod_verifier.verify(0.0, 1.0, expected_result=(0.0, 0.0))
    divmod_verifier.verify(3.0, 2.0, expected_result=(1.0, 1.0))
    divmod_verifier.verify(-3.0, -2.0, expected_result=(1.0, -1.0))
    divmod_verifier.verify(2.0 * MAX_LONG, 2.0, expected_result=(1.8446744073709552e+19, 0.0))
    divmod_verifier.verify(2.0 * MIN_LONG, 2.0, expected_result=(-1.8446744073709552e+19, 0.0))


def test_pow():
    pow_verifier = verifier_for(lambda x, y: x ** y)

    pow_verifier.verify(0.0, 0, expected_result=1.0)
    pow_verifier.verify(1.0, 2, expected_result=1.0)
    pow_verifier.verify(2.0, 2, expected_result=4.0)
    pow_verifier.verify(-2.0, 2, expected_result=4.0)
    pow_verifier.verify(-2.0, 3, expected_result=-8.0)
    pow_verifier.verify(3.0, 2, expected_result=9.0)
    pow_verifier.verify(2.0, 3, expected_result=8.0)
    pow_verifier.verify(2.0, -1, expected_result=0.5)
    pow_verifier.verify(2.0, -2, expected_result=0.25)

    pow_verifier.verify(0.0, 0.0, expected_result=1.0)
    pow_verifier.verify(1.0, 2.0, expected_result=1.0)
    pow_verifier.verify(2.0, 2.0, expected_result=4.0)
    pow_verifier.verify(-2.0, 2.0, expected_result=4.0)
    pow_verifier.verify(-2.0, 3.0, expected_result=-8.0)
    pow_verifier.verify(3.0, 2.0, expected_result=9.0)
    pow_verifier.verify(2.0, 3.0, expected_result=8.0)
    pow_verifier.verify(2.0, -1.0, expected_result=0.5)
    pow_verifier.verify(2.0, -2.0, expected_result=0.25)
