from datetime import date
from ..conftest import verifier_for


def test_constructor():
    def function(year: int, month: int, day: int) -> date:
        return date(year, month, day)

    verifier = verifier_for(function)

    verifier.verify(2000, 1, 1, expected_result=date(2000, 1, 1))
    verifier.verify(2000, 1, 30, expected_result=date(2000, 1, 30))
    verifier.verify(2000, 2, 3, expected_result=date(2000, 2, 3))
    verifier.verify(2001, 1, 1, expected_result=date(2001, 1, 1))

    verifier.verify(2000, 1, 0, expected_error=ValueError)
    verifier.verify(2000, 1, 32, expected_error=ValueError)
    verifier.verify(2000, 0, 1, expected_error=ValueError)
    verifier.verify(2000, 13, 1, expected_error=ValueError)


def test_fromordinal():
    def function(ordinal: int) -> date:
        return date.fromordinal(ordinal)

    verifier = verifier_for(function)

    verifier.verify(1, expected_result=date(1, 1, 1))
    verifier.verify(2, expected_result=date(1, 1, 2))
    verifier.verify(32, expected_result=date(1, 2, 1))
    verifier.verify(1000, expected_result=date(3, 9, 27))


def test_toordinal():
    def function(x: date) -> int:
        return x.toordinal()

    verifier = verifier_for(function)

    verifier.verify(date(1, 1, 1), expected_result=1)
    verifier.verify(date(1, 1, 2), expected_result=2)
    verifier.verify(date(1, 2, 1), expected_result=32)
    verifier.verify(date(3, 9, 27), expected_result=1000)
