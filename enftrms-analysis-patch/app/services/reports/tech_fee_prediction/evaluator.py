"""단일 룰 평가기. Java RuleEvaluator 와 동치."""
from __future__ import annotations

import math
from typing import Any

from .rules import Rule


def evaluate(rule: Rule, value: Any) -> float:
    if value is None:
        return 0.0
    t = rule.type or "threshold"
    if t == "binary":
        return _binary(rule, value)
    if t == "threshold":
        return _threshold(rule, _to_float(value))
    if t == "bucket":
        return _bucket(rule, _to_float(value))
    if t == "linear":
        return _linear(rule, _to_float(value))
    if t == "percentile":
        return _percentile(rule, _to_float(value))
    if t == "log_scaled":
        return _log_scaled(rule, _to_float(value))
    return 0.0


def _binary(rule: Rule, v: Any) -> float:
    if isinstance(v, str):
        truthy = v.strip().lower() in {"y", "yes", "true", "1"}
    else:
        truthy = bool(v)
    if not truthy:
        return 0.0
    return rule.score_when_true if rule.score_when_true is not None else rule.max


def _threshold(rule: Rule, v: float) -> float:
    for t in rule.thresholds:
        if t.gte is not None and v >= t.gte:
            return min(t.score, rule.max)
        if t.lte is not None and v <= t.lte:
            return min(t.score, rule.max)
    return 0.0


def _bucket(rule: Rule, v: float) -> float:
    for b in rule.buckets:
        if len(b.range) < 2:
            continue
        lo, hi = b.range[0], b.range[1]
        if lo <= v < hi:
            return min(b.score, rule.max)
    return 0.0


def _linear(rule: Rule, v: float) -> float:
    slope = rule.slope if rule.slope is not None else 1.0
    raw = max(0.0, v) * slope
    cap = rule.cap if rule.cap is not None else rule.max
    return min(raw, cap)


def _percentile(rule: Rule, v: float) -> float:
    clipped = max(0.0, min(1.0, v))
    return min(clipped * rule.max, rule.max)


def _log_scaled(rule: Rule, v: float) -> float:
    if v <= 0:
        return 0.0
    raw = math.log10(v + 1.0)
    cap = rule.cap if rule.cap is not None else rule.max
    return min(raw, cap)


def _to_float(v: Any) -> float:
    if isinstance(v, bool):
        return 1.0 if v else 0.0
    if isinstance(v, (int, float)):
        return float(v)
    try:
        return float(str(v))
    except (TypeError, ValueError):
        return 0.0
