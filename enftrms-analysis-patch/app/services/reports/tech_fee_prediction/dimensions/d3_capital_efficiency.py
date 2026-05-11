"""D3. R&D 자본효율·기술료 이력 — 엑셀 양식 직접 매핑 (풀구현)."""
from __future__ import annotations

from ..evaluator import evaluate
from ..models import DimensionScore, PredictionContext, RuleDetail, TfeeHistory
from ..rules import Dimension


def _input(key: str, h: TfeeHistory | None):
    if h is None:
        return None
    return {
        "hasPastTfee": h.has_past_tfee,
        "recoveryRatePercentile": h.recovery_rate_percentile,
        "projectRecoveryRate": h.project_recovery_rate,
        "monthsToFirstTfee": h.months_to_first_tfee,
        "consecutiveYears": h.consecutive_years,
    }.get(key)


def calculate(ctx: PredictionContext, dim: Dimension) -> DimensionScore:
    detail: list[RuleDetail] = []
    total = 0.0
    for rule in dim.rules:
        val = _input(rule.input, ctx.tfee_history)
        score = evaluate(rule, val)
        total += score
        detail.append(RuleDetail(rule_name=rule.name, score=round(score, 2),
                                 max=rule.max, input_value=val))
    return DimensionScore(code="D3", label=dim.label,
                          score=round(min(total, dim.weight), 2),
                          max_score=dim.weight, detail=detail)
