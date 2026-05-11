"""D4. 시장·산업 신호 — 외부 수집기가 채운 external_metrics 사용."""
from __future__ import annotations

from ...models import DimensionScore, PredictionContext, RuleDetail
from ..evaluator import evaluate
from ..rules import Dimension


def calculate(ctx: PredictionContext, dim: Dimension) -> DimensionScore:
    detail: list[RuleDetail] = []
    total = 0.0
    for rule in dim.rules:
        val = ctx.metric(rule.input, 0.0)
        score = evaluate(rule, val)
        total += score
        detail.append(RuleDetail(rule_name=rule.name, score=round(score, 2),
                                 max=rule.max, input_value=val))
    return DimensionScore(code="D4", label=dim.label,
                          score=round(min(total, dim.weight), 2),
                          max_score=dim.weight, detail=detail)
