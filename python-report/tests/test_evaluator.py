from tfee_report.scorecard import evaluator
from tfee_report.scorecard.rules import Bucket, Rule, Threshold


def test_threshold_picks_highest_matching_gte():
    rule = Rule(name="t", max=5, type="threshold", thresholds=[
        Threshold(gte=0.2, score=6),
        Threshold(gte=0.1, score=3),
        Threshold(gte=0.0, score=1),
    ])
    assert evaluator.evaluate(rule, 0.25) == 5    # capped at max
    assert evaluator.evaluate(rule, 0.15) == 3
    assert evaluator.evaluate(rule, 0.0) == 1


def test_threshold_lte():
    rule = Rule(name="t", max=3, type="threshold", thresholds=[
        Threshold(lte=1.5, score=3),
        Threshold(lte=3.0, score=1),
    ])
    assert evaluator.evaluate(rule, 1.0) == 3
    assert evaluator.evaluate(rule, 2.5) == 1
    assert evaluator.evaluate(rule, 5.0) == 0


def test_bucket():
    rule = Rule(name="b", max=5, type="bucket", buckets=[
        Bucket(range=[12, 24], score=5),
        Bucket(range=[6, 12], score=3),
    ])
    assert evaluator.evaluate(rule, 18) == 5
    assert evaluator.evaluate(rule, 8) == 3
    assert evaluator.evaluate(rule, 24) == 0     # upper bound exclusive
    assert evaluator.evaluate(rule, 3) == 0


def test_linear_with_cap():
    rule = Rule(name="l", max=7, type="linear", slope=1.5, cap=7)
    assert evaluator.evaluate(rule, 3) == 4.5
    assert evaluator.evaluate(rule, 100) == 7    # capped


def test_binary():
    rule = Rule(name="bn", max=6, type="binary", score_when_true=6)
    assert evaluator.evaluate(rule, True) == 6
    assert evaluator.evaluate(rule, False) == 0
    assert evaluator.evaluate(rule, "Y") == 6
    assert evaluator.evaluate(rule, "N") == 0


def test_percentile():
    rule = Rule(name="p", max=5, type="percentile")
    assert evaluator.evaluate(rule, 0.0) == 0
    assert evaluator.evaluate(rule, 0.5) == 2.5
    assert evaluator.evaluate(rule, 1.0) == 5.0
    assert evaluator.evaluate(rule, 1.5) == 5.0   # clipped


def test_none_returns_zero():
    rule = Rule(name="t", max=5, type="threshold", thresholds=[Threshold(gte=0, score=5)])
    assert evaluator.evaluate(rule, None) == 0
