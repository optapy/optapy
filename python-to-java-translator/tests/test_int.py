from .conftest import verifier_for

MAX_LONG = 0xFFFF_FFFF_FFFF_FFFF
MIN_LONG = -MAX_LONG


def test_add():
    add_verifier = verifier_for(lambda a, b: a + b)

    add_verifier.verify(1, 1, expected_result=2)
    add_verifier.verify(1, -1, expected_result=0)
    add_verifier.verify(-1, 1, expected_result=0)
    add_verifier.verify(0, 1, expected_result=1)
    add_verifier.verify(MAX_LONG, 1, expected_result=(MAX_LONG + 1))
    add_verifier.verify(MIN_LONG, -1, expected_result=(MIN_LONG - 1))

    add_verifier.verify(1, 1.0, expected_result=2.0)
    add_verifier.verify(1, -1.0, expected_result=0.0)
    add_verifier.verify(-1, 1.0, expected_result=0.0)
    add_verifier.verify(0, 1.0, expected_result=1.0)
    add_verifier.verify(MAX_LONG, 1.0, expected_result=(MAX_LONG + 1.0))
    add_verifier.verify(MIN_LONG, -1.0, expected_result=(MIN_LONG - 1.0))


def test_iadd():
    def iadd(x, y):
        old = x
        x += y
        if y != 0:
            assert old is not x
        return x

    iadd_verifier = verifier_for(iadd)

    iadd_verifier.verify(1, 1, expected_result=2)
    iadd_verifier.verify(1, -1, expected_result=0)
    iadd_verifier.verify(-1, 1, expected_result=0)
    iadd_verifier.verify(0, 1, expected_result=1)
    iadd_verifier.verify(MAX_LONG, 1, expected_result=(MAX_LONG + 1))
    iadd_verifier.verify(MIN_LONG, -1, expected_result=(MIN_LONG - 1))

    iadd_verifier.verify(1, 1.0, expected_result=2.0)
    iadd_verifier.verify(1, -1.0, expected_result=0.0)
    iadd_verifier.verify(-1, 1.0, expected_result=0.0)
    iadd_verifier.verify(0, 1.0, expected_result=1.0)
    iadd_verifier.verify(MAX_LONG, 1.0, expected_result=(MAX_LONG + 1.0))
    iadd_verifier.verify(MIN_LONG, -1.0, expected_result=(MIN_LONG - 1.0))


def test_sub():
    sub_verifier = verifier_for(lambda a, b: a - b)

    sub_verifier.verify(1, 1, expected_result=0)
    sub_verifier.verify(1, -1, expected_result=2)
    sub_verifier.verify(-1, 1, expected_result=-2)
    sub_verifier.verify(0, 1, expected_result=-1)
    sub_verifier.verify(MAX_LONG, -1, expected_result=(MAX_LONG + 1))
    sub_verifier.verify(MIN_LONG, 1, expected_result=(MIN_LONG - 1))

    sub_verifier.verify(1, 1.0, expected_result=0.0)
    sub_verifier.verify(1, -1.0, expected_result=2.0)
    sub_verifier.verify(-1, 1.0, expected_result=-2.0)
    sub_verifier.verify(0, 1.0, expected_result=-1.0)
    sub_verifier.verify(MAX_LONG, -1.0, expected_result=(MAX_LONG + 1.0))
    sub_verifier.verify(MIN_LONG, 1.0, expected_result=(MIN_LONG - 1.0))


def test_isub():
    def isub(x, y):
        old = x
        x -= y
        if y != 0:
            assert old is not x
        return x

    isub_verifier = verifier_for(isub)

    isub_verifier.verify(1, 1, expected_result=0)
    isub_verifier.verify(1, -1, expected_result=2)
    isub_verifier.verify(-1, 1, expected_result=-2)
    isub_verifier.verify(0, 1, expected_result=-1)
    isub_verifier.verify(MAX_LONG, -1, expected_result=(MAX_LONG + 1))
    isub_verifier.verify(MIN_LONG, 1, expected_result=(MIN_LONG - 1))

    isub_verifier.verify(1, 1.0, expected_result=0.0)
    isub_verifier.verify(1, -1.0, expected_result=2.0)
    isub_verifier.verify(-1, 1.0, expected_result=-2.0)
    isub_verifier.verify(0, 1.0, expected_result=-1.0)
    isub_verifier.verify(MAX_LONG, -1.0, expected_result=(MAX_LONG + 1.0))
    isub_verifier.verify(MIN_LONG, 1.0, expected_result=(MIN_LONG - 1.0))


def test_multiply():
    multiply_verifier = verifier_for(lambda a, b: a * b)

    multiply_verifier.verify(1, 1, expected_result=1)
    multiply_verifier.verify(1, -1, expected_result=-1)
    multiply_verifier.verify(-1, 1, expected_result=-1)
    multiply_verifier.verify(0, 1, expected_result=0)
    multiply_verifier.verify(2, 3, expected_result=6)
    multiply_verifier.verify(MAX_LONG, 2, expected_result=(2 * MAX_LONG))
    multiply_verifier.verify(MIN_LONG, 2, expected_result=(2 * MIN_LONG))

    multiply_verifier.verify(1, 1.0, expected_result=1.0)
    multiply_verifier.verify(1, -1.0, expected_result=-1.0)
    multiply_verifier.verify(-1, 1.0, expected_result=-1.0)
    multiply_verifier.verify(0, 1.0, expected_result=0.0)
    multiply_verifier.verify(2, 3.0, expected_result=6.0)
    multiply_verifier.verify(MAX_LONG, 2.0, expected_result=(2.0 * MAX_LONG))
    multiply_verifier.verify(MIN_LONG, 2.0, expected_result=(2.0 * MIN_LONG))


def test_imultiply():
    def imultiply(x, y):
        old = x
        x *= y
        if y != 1:
            assert old is not x
        return x

    imultiply_verifier = verifier_for(imultiply)

    imultiply_verifier.verify(1, 1, expected_result=1)
    imultiply_verifier.verify(1, -1, expected_result=-1)
    imultiply_verifier.verify(-1, 1, expected_result=-1)
    imultiply_verifier.verify(0, 1, expected_result=0)
    imultiply_verifier.verify(2, 3, expected_result=6)
    imultiply_verifier.verify(MAX_LONG, 2, expected_result=(2 * MAX_LONG))
    imultiply_verifier.verify(MIN_LONG, 2, expected_result=(2 * MIN_LONG))

    imultiply_verifier.verify(1, 1.0, expected_result=1.0)
    imultiply_verifier.verify(1, -1.0, expected_result=-1.0)
    imultiply_verifier.verify(-1, 1.0, expected_result=-1.0)
    imultiply_verifier.verify(0, 1.0, expected_result=0.0)
    imultiply_verifier.verify(2, 3.0, expected_result=6.0)
    imultiply_verifier.verify(MAX_LONG, 2.0, expected_result=(2.0 * MAX_LONG))
    imultiply_verifier.verify(MIN_LONG, 2.0, expected_result=(2.0 * MIN_LONG))


def test_truediv():
    truediv_verifier = verifier_for(lambda a, b: a / b)

    truediv_verifier.verify(1, 1, expected_result=1.0)
    truediv_verifier.verify(1, -1, expected_result=-1.0)
    truediv_verifier.verify(-1, 1, expected_result=-1.0)
    truediv_verifier.verify(0, 1, expected_result=0.0)
    truediv_verifier.verify(3, 2, expected_result=1.5)
    truediv_verifier.verify(2 * MAX_LONG, 2, expected_result=1.8446744073709552e+19)
    truediv_verifier.verify(2 * MIN_LONG, 2, expected_result=-1.8446744073709552e+19)

    truediv_verifier.verify(1, 1.0, expected_result=1.0)
    truediv_verifier.verify(1, -1.0, expected_result=-1.0)
    truediv_verifier.verify(-1, 1.0, expected_result=-1.0)
    truediv_verifier.verify(0, 1.0, expected_result=0.0)
    truediv_verifier.verify(3, 2.0, expected_result=1.5)
    truediv_verifier.verify(2 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    truediv_verifier.verify(2 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)


def test_itruediv():
    def itruediv(x, y):
        old = x
        x /= y
        if y != 1:
            assert old is not x
        return x

    itruediv_verifier = verifier_for(itruediv)

    itruediv_verifier.verify(1, 1, expected_result=1.0)
    itruediv_verifier.verify(1, -1, expected_result=-1.0)
    itruediv_verifier.verify(-1, 1, expected_result=-1.0)
    itruediv_verifier.verify(0, 1, expected_result=0.0)
    itruediv_verifier.verify(3, 2, expected_result=1.5)
    itruediv_verifier.verify(2 * MAX_LONG, 2, expected_result=1.8446744073709552e+19)
    itruediv_verifier.verify(2 * MIN_LONG, 2, expected_result=-1.8446744073709552e+19)

    itruediv_verifier.verify(1, 1.0, expected_result=1.0)
    itruediv_verifier.verify(1, -1.0, expected_result=-1.0)
    itruediv_verifier.verify(-1, 1.0, expected_result=-1.0)
    itruediv_verifier.verify(0, 1.0, expected_result=0.0)
    itruediv_verifier.verify(3, 2.0, expected_result=1.5)
    itruediv_verifier.verify(2 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    itruediv_verifier.verify(2 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)


def test_floordiv():
    floordiv_verifier = verifier_for(lambda a, b: a // b)

    floordiv_verifier.verify(1, 1, expected_result=1)
    floordiv_verifier.verify(1, -1, expected_result=-1)
    floordiv_verifier.verify(-1, 1, expected_result=-1)
    floordiv_verifier.verify(0, 1, expected_result=0)
    floordiv_verifier.verify(3, 2, expected_result=1)
    floordiv_verifier.verify(2 * MAX_LONG, 2, expected_result=MAX_LONG)
    floordiv_verifier.verify(2 * MIN_LONG, 2, expected_result=MIN_LONG)

    floordiv_verifier.verify(1, 1.0, expected_result=1.0)
    floordiv_verifier.verify(1, -1.0, expected_result=-1.0)
    floordiv_verifier.verify(-1, 1.0, expected_result=-1.0)
    floordiv_verifier.verify(0, 1.0, expected_result=0.0)
    floordiv_verifier.verify(3, 2.0, expected_result=1.0)
    floordiv_verifier.verify(2 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    floordiv_verifier.verify(2 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)


def test_ifloordiv():
    def ifloordiv(x, y):
        old = x
        x //= y
        if y != 1:
            assert old is not x
        return x

    ifloordiv_verifier = verifier_for(ifloordiv)

    ifloordiv_verifier.verify(1, 1, expected_result=1)
    ifloordiv_verifier.verify(1, -1, expected_result=-1)
    ifloordiv_verifier.verify(-1, 1, expected_result=-1)
    ifloordiv_verifier.verify(0, 1, expected_result=0)
    ifloordiv_verifier.verify(3, 2, expected_result=1)
    ifloordiv_verifier.verify(2 * MAX_LONG, 2, expected_result=MAX_LONG)
    ifloordiv_verifier.verify(2 * MIN_LONG, 2, expected_result=MIN_LONG)

    ifloordiv_verifier.verify(1, 1.0, expected_result=1.0)
    ifloordiv_verifier.verify(1, -1.0, expected_result=-1.0)
    ifloordiv_verifier.verify(-1, 1.0, expected_result=-1.0)
    ifloordiv_verifier.verify(0, 1.0, expected_result=0.0)
    ifloordiv_verifier.verify(3, 2.0, expected_result=1.0)
    ifloordiv_verifier.verify(2 * MAX_LONG, 2.0, expected_result=1.8446744073709552e+19)
    ifloordiv_verifier.verify(2 * MIN_LONG, 2.0, expected_result=-1.8446744073709552e+19)


def test_negate():
    negate_verifier = verifier_for(lambda x: -x)

    negate_verifier.verify(1, expected_result=-1)
    negate_verifier.verify(-1, expected_result=1)
    negate_verifier.verify(MAX_LONG, expected_result=-MAX_LONG)
    negate_verifier.verify(MIN_LONG, expected_result=-MIN_LONG)


def test_pos():
    pos_verifier = verifier_for(lambda x: +x)

    pos_verifier.verify(1, expected_result=1)
    pos_verifier.verify(-1, expected_result=-1)
    pos_verifier.verify(MAX_LONG, expected_result=MAX_LONG)
    pos_verifier.verify(MIN_LONG, expected_result=MIN_LONG)


def test_abs():
    abs_verifier = verifier_for(lambda x: abs(x))

    abs_verifier.verify(1, expected_result=1)
    abs_verifier.verify(-1, expected_result=1)
    abs_verifier.verify(MAX_LONG, expected_result=MAX_LONG)
    abs_verifier.verify(MIN_LONG, expected_result=-MIN_LONG)


def test_divmod():
    divmod_verifier = verifier_for(lambda x, y: divmod(x, y))

    divmod_verifier.verify(1, 1, expected_result=(1, 0))
    divmod_verifier.verify(1, -1, expected_result=(-1, 0))
    divmod_verifier.verify(-1, 1, expected_result=(-1, 0))
    divmod_verifier.verify(0, 1, expected_result=(0, 0))
    divmod_verifier.verify(3, 2, expected_result=(1, 1))
    divmod_verifier.verify(-3, -2, expected_result=(1, -1))
    divmod_verifier.verify(2 * MAX_LONG, 2, expected_result=(MAX_LONG, 0))
    divmod_verifier.verify(2 * MIN_LONG, 2, expected_result=(MIN_LONG, 0))

    divmod_verifier.verify(1, 1.0, expected_result=(1, 0))
    divmod_verifier.verify(1, -1.0, expected_result=(-1, 0))
    divmod_verifier.verify(-1, 1.0, expected_result=(-1, 0))
    divmod_verifier.verify(0, 1.0, expected_result=(0, 0))
    divmod_verifier.verify(3, 2.0, expected_result=(1, 1))
    divmod_verifier.verify(-3, -2.0, expected_result=(1, -1))
    divmod_verifier.verify(2 * MAX_LONG, 2.0, expected_result=(1.8446744073709552e+19, 0))
    divmod_verifier.verify(2 * MIN_LONG, 2.0, expected_result=(-1.8446744073709552e+19, 0))


def test_pow():
    pow_verifier = verifier_for(lambda x, y: x ** y)

    pow_verifier.verify(0, 0, expected_result=1)
    pow_verifier.verify(1, 2, expected_result=1)
    pow_verifier.verify(2, 2, expected_result=4)
    pow_verifier.verify(-2, 2, expected_result=4)
    pow_verifier.verify(-2, 3, expected_result=-8)
    pow_verifier.verify(3, 2, expected_result=9)
    pow_verifier.verify(2, 3, expected_result=8)
    pow_verifier.verify(2, -1, expected_result=0.5)
    pow_verifier.verify(2, -2, expected_result=0.25)

    pow_verifier.verify(0, 0.0, expected_result=1.0)
    pow_verifier.verify(1, 2.0, expected_result=1.0)
    pow_verifier.verify(2, 2.0, expected_result=4.0)
    pow_verifier.verify(-2, 2.0, expected_result=4.0)
    pow_verifier.verify(-2, 3.0, expected_result=-8.0)
    pow_verifier.verify(3, 2.0, expected_result=9.0)
    pow_verifier.verify(2, 3.0, expected_result=8.0)
    pow_verifier.verify(2, -1.0, expected_result=0.5)
    pow_verifier.verify(2, -2.0, expected_result=0.25)


def test_mod_pow():
    mod_pow_verifier = verifier_for(lambda x, y, z: pow(x, y, z))

    mod_pow_verifier.verify(2, 3, 3, expected_result=2)
    mod_pow_verifier.verify(2, -1, 3, expected_result=2)
