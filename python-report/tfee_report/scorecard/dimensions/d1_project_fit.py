"""D1. 과제 사업화 적합성 — KSIC/키워드 유사도. 입력은 외부 메트릭에서 끌어옴."""
from __future__ import annotations

from ...models import DimensionScore, PredictionContext, RuleDetail
from ..evaluator import evaluate
from ..rules import Dimension


def _months_since_end(ctx: PredictionContext) -> float:
    s, p = ctx.subject, ctx.pred_at
    if not s or not s.end_de or not p:
        return 0.0
    delta = p - s.end_de
    return delta.days / 30.0


def _input(key: str, ctx: PredictionContext):
    if key == "monthsSinceEnd":
        return _months_since_end(ctx)
    return ctx.metric(key, 0.0)


def calculate(ctx: PredictionContext, dim: Dimension) -> DimensionScore:
    detail: list[RuleDetail] = []
    total = 0.0
    for rule in dim.rules:
        val = _input(rule.input, ctx)
        score = evaluate(rule, val)
        total += score
        detail.append(RuleDetail(rule_name=rule.name, score=round(score, 2),
                                 max=rule.max, input_value=val))
    return DimensionScore(code="D1", label=dim.label,
                          score=round(min(total, dim.weight), 2),
                          max_score=dim.weight, detail=detail)
