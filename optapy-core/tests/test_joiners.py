from optapy.constraint import Joiners

from org.optaplanner.core.api.score.stream.bi import BiJoiner
from org.optaplanner.core.api.score.stream.tri import TriJoiner
from org.optaplanner.core.api.score.stream.quad import QuadJoiner
from org.optaplanner.core.api.score.stream.penta import PentaJoiner


def test_equal_joiners_work():
    assert isinstance(Joiners.equal(), BiJoiner)
    assert isinstance(Joiners.equal(lambda a: a), BiJoiner)
    assert isinstance(Joiners.equal(lambda a: a, lambda other: other), BiJoiner)
    assert isinstance(Joiners.equal(lambda a, b: a, lambda other: other), TriJoiner)
    assert isinstance(Joiners.equal(lambda a, b, c: a, lambda other: other), QuadJoiner)
    assert isinstance(Joiners.equal(lambda a, b, c, d: a, lambda other: other), PentaJoiner)


def test_filtering_joiners_work():
    assert isinstance(Joiners.filtering(lambda a, b: a), BiJoiner)
    assert isinstance(Joiners.filtering(lambda a, b, c: a), TriJoiner)
    assert isinstance(Joiners.filtering(lambda a, b, c, d: a), QuadJoiner)
    assert isinstance(Joiners.filtering(lambda a, b, c, d, e: a), PentaJoiner)


def test_comparison_joiners_work():
    comparison_joiners = ['lessThan', 'lessThanOrEqual', 'greaterThan', 'greaterThanOrEqual']
    for comparison_joiner in comparison_joiners:
        method = getattr(Joiners, comparison_joiner)
        assert isinstance(method(lambda a: a), BiJoiner)
        assert isinstance(method(lambda a: a, lambda other: other), BiJoiner)
        assert isinstance(method(lambda a, b: a, lambda other: other), TriJoiner)
        assert isinstance(method(lambda a, b, c: a, lambda other: other), QuadJoiner)
        assert isinstance(method(lambda a, b, c, d: a, lambda other: other), PentaJoiner)


def test_overlapping_joiners_work():
    assert isinstance(Joiners.overlapping(lambda start: start, lambda end: end), BiJoiner)
    assert isinstance(Joiners.overlapping(lambda a: a, lambda a: a, lambda a: a, lambda a: a), BiJoiner)
    assert isinstance(Joiners.overlapping(lambda a, b: a, lambda a, b: a, lambda a: a, lambda a: a), TriJoiner)
    assert isinstance(Joiners.overlapping(lambda a, b, c: a, lambda a, b, c: a, lambda a: a, lambda a: a), QuadJoiner)
    assert isinstance(Joiners.overlapping(lambda a, b, c, d: a, lambda a, b, c, d: a, lambda a: a, lambda a: a),
                      PentaJoiner)
